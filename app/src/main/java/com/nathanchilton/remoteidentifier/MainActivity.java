package com.nathanchilton.remoteidentifier;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.speech.tts.TextToSpeech;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static final int SERVICE_REQUEST_CODE = 101;
    private final String appName = "Remote Identifier"; // getString(R.string.app_name);
    private final String[] permissions = {Manifest.permission.RECORD_AUDIO};
    private final int DEFAULT_ANNOUNCEMENT_FREQUENCY = 15;
    private final int DEFAULT_THRESHOLD = 700;
    TextToSpeech textToSpeech;
    EditText thresholdEditText;
    EditText identificationText;
    EditText announcementFrequencyEditText;
    TextView currentAmplitude;
    TextView soundHeardSinceLastId;
    SharedPreferences sharedPreferences;
    private boolean permissionToRecordAccepted = false;
    private MediaRecorder mediaRecorder;
    private TextView timestampTextView;
    private long timeOfLastAnnouncement = 0;
    private long timeOfLastSoundWhichExceededTheThreshold = 0;
    private AudioTrack audioTrack;
    private PowerManager.WakeLock wakeLock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        sharedPreferences = getSharedPreferences(appName, MODE_PRIVATE);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sound_detection);

        timestampTextView = findViewById(R.id.timestampTextView);
        ((EditText) findViewById(R.id.thresholdEditText)).setText(String.valueOf(DEFAULT_THRESHOLD));

        currentAmplitude = findViewById(R.id.currentAmplitude);
        identificationText = findViewById(R.id.identificationText);
        announcementFrequencyEditText = findViewById(R.id.announcementFrequency);
        thresholdEditText = findViewById(R.id.thresholdEditText);
        soundHeardSinceLastId = findViewById(R.id.soundHeardSinceLastId);

        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);

        loadSettings();

        // Obtain a WakeLock to keep the CPU running even when the screen is off
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        if (powerManager != null) {
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    "SoundDetectionActivity:WakeLockTag");
        }

        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    textToSpeech.setLanguage(Locale.UK);
                }
            }
        });

        Button testSpeech = findViewById(R.id.testSpeech);
        testSpeech.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    makeAnnouncement();
                } catch (InterruptedException e) {
                    Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });

        findViewById(R.id.startButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startRecording();
                startForegroundService();
                findViewById(R.id.stopButton).setEnabled(true);
                findViewById(R.id.startButton).setEnabled(false);
            }
        });

        findViewById(R.id.stopButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopRecording();
                stopForegroundService();
                findViewById(R.id.stopButton).setEnabled(false);
                findViewById(R.id.startButton).setEnabled(true);
            }
        });

        thresholdEditText.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                saveSettings();
            }

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // Not used in this implementation
            }

            @Override
            public void afterTextChanged(Editable editable) {
                // Not used in this implementation
            }
        });

        announcementFrequencyEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                saveSettings();
            }

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // Not used in this implementation
            }

            @Override
            public void afterTextChanged(Editable editable) {
                // Not used in this implementation
            }
        });

        identificationText.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                saveSettings();
            }

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // Not used in this implementation
            }

            @Override
            public void afterTextChanged(Editable editable) {
                // Not used in this implementation
            }
        });
    }

    public void loadSettings() {
        int thresholdValue = sharedPreferences.getInt("threshold", DEFAULT_THRESHOLD);
        thresholdEditText.setText(String.valueOf(thresholdValue));

        int announcementFrequency = sharedPreferences.getInt("announcementFrequency", DEFAULT_ANNOUNCEMENT_FREQUENCY);
        announcementFrequencyEditText.setText(String.valueOf(announcementFrequency));

        String identificationTextString = sharedPreferences.getString("identificationText", "");
        identificationText.setText(identificationTextString);
    }

    public void saveSettings() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("threshold", getThreshold());
        editor.putInt("announcementFrequency", getAnnouncementFrequency());
        editor.putString("identificationText", getIdentificationText());
        editor.apply();
    }

    private int getThreshold() {
        int threshold = DEFAULT_THRESHOLD;
        try {
            threshold = Integer.parseInt(thresholdEditText.getText().toString());
        } catch (Exception e) {
            System.out.println("Failed to parse value for threshold:\n" + e.getMessage());
            e.printStackTrace();
        }
        return threshold;
    }

    private int getAnnouncementFrequency() {
        int howManyMinutes = DEFAULT_ANNOUNCEMENT_FREQUENCY;
        try {
            howManyMinutes = Integer.parseInt(announcementFrequencyEditText.getText().toString());
        } catch (Exception e) {
            System.out.println("Failed to parse value for announcementFrequency:\n" + e.getMessage());
            e.printStackTrace();
        }
        return howManyMinutes;
    }

    private String getIdentificationText() {
        String returnValue = identificationText.getText().toString();
        return returnValue;
    }

    private void makeAnnouncement() throws InterruptedException {
        long justBeforeMakingTheAnnouncement = System.currentTimeMillis();

        // If the voxTone switch is enabled, play a tone of durationSeconds
        if (((Switch) findViewById(R.id.voxTone)).isChecked()) {
            float durationSeconds = 0.5f;
            generateAndPlayTone(440.0f, durationSeconds);
            Thread.sleep((long) (durationSeconds * 1000));
        }

        String toSpeak = getIdentificationText();
        Toast.makeText(getApplicationContext(), toSpeak, Toast.LENGTH_SHORT).show();
        textToSpeech.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);

        // reset the timeOfLastAnnouncement to the current time
        timeOfLastAnnouncement = System.currentTimeMillis();
        timeOfLastSoundWhichExceededTheThreshold = justBeforeMakingTheAnnouncement;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            permissionToRecordAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
        }
        if (!permissionToRecordAccepted) {
            finish();
        }
    }

    private void generateAndPlayTone(float toneFrequency, float durationSeconds) throws InterruptedException {
        final int SAMPLE_RATE = 44100; // Standard audio sample rate
        if (toneFrequency < 1)
            toneFrequency = 440.0f;

        int numSamples = Math.round(SAMPLE_RATE * durationSeconds);
        double[] sample = new double[numSamples];
        byte[] generatedSnd = new byte[2 * numSamples];

        // Generate the tone
        for (int i = 0; i < numSamples; ++i) {
            sample[i] = Math.sin(2 * Math.PI * i / (SAMPLE_RATE / toneFrequency));
        }

        // Convert the generated sample to bytes
        int idx = 0;
        for (final double dVal : sample) {
            short val = (short) ((dVal * 32767)); // Convert to 16-bit PCM
            generatedSnd[idx++] = (byte) (val & 0x00ff);
            generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
        }

        // Play the generated sound
        audioTrack = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                generatedSnd.length,
                AudioTrack.MODE_STATIC);
        audioTrack.write(generatedSnd, 0, generatedSnd.length);
        audioTrack.play();
    }

    private void startRecording() {
        timestampTextView.setText("Started recording...");

        String fileName = "recording.3gp";
        File outputFile = new File(getExternalFilesDir(null), fileName);
        final String filePath = outputFile.getAbsolutePath();

        final EditText thresholdEditText = findViewById(R.id.thresholdEditText);

        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setOutputFile(filePath);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            // Acquire the WakeLock before starting the recording
            if (wakeLock != null && !wakeLock.isHeld()) {
                wakeLock.acquire();
            }

            mediaRecorder.prepare();
            mediaRecorder.start();

            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    int threshold = getThreshold();

                    if (mediaRecorder != null) {
                        double amplitude = getAmplitude();
                        currentAmplitude.setText("Current Amplitude: " + amplitude);
                        float minutesSinceLastAnnouncement = (float) ((System.currentTimeMillis()
                                - timeOfLastAnnouncement) / 1000 / 60);
                        if (amplitude > threshold) {
                            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                    .format(new Date());
                            timestampTextView
                                    .setText(timestamp + "\nAmplitude: " + amplitude + "\tThreshold: " + threshold);
                            timeOfLastSoundWhichExceededTheThreshold = System.currentTimeMillis();
                            soundHeardSinceLastId.setText("Yes");
                        } else {
                            // nobody is talking, so we can make an announcement, if appropriate
                            TextView minutesAgo = findViewById(R.id.minutesAgo);
                            minutesAgo.setText("(" + (int) (System.currentTimeMillis() - timeOfLastSoundWhichExceededTheThreshold) / 1000
                                    / 60
                                    + " minutes ago.)");

                            int minuteOfHour = new Date().getMinutes();
                            Switch alignment = findViewById(R.id.timeAlignment);

                            // if the last sound heard was at least two seconds ago
                            if (((System.currentTimeMillis() - 2000) > timeOfLastSoundWhichExceededTheThreshold)
                                    // and it has been at least one minute since the last announcement
                                    && ((timeOfLastSoundWhichExceededTheThreshold - 60 * 1000) > timeOfLastAnnouncement)
                                    // and the number of minutes specified in the "interval" has elapsed
                                    && ((minutesSinceLastAnnouncement >= getAnnouncementFrequency())
                                    // or the "alignment" option is enabled and the current minute is a multiple of the "interval"
                                    || (alignment.isChecked()
                                    && (minuteOfHour % getAnnouncementFrequency() == 0)))) {
                                try {
                                    makeAnnouncement();
                                    soundHeardSinceLastId.setText("No");
                                } catch (InterruptedException e) {
                                    Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }

                            TextView timeSinceLastIDTextView = findViewById(R.id.timeSinceLastID);
                            timeSinceLastIDTextView.setText(String.valueOf((int) minutesSinceLastAnnouncement));
                        }
                        handler.postDelayed(this, 1000); // Check every second
                    }
                }
            }, 1000); // Start after 1 second
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stopRecording() {
        timestampTextView.setText("Stopped recording.");
        currentAmplitude.setText("");

        if (mediaRecorder != null) {
            // Release the WakeLock when stopping the recording
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
            }

            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
        }
    }

    private double getAmplitude() {
        if (mediaRecorder != null) {
            return mediaRecorder.getMaxAmplitude();
        }
        return 0;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (audioTrack != null) {
            audioTrack.release();
            audioTrack = null;
        }
    }

    private void startForegroundService() {
        Intent serviceIntent = new Intent(this, SoundDetectionService.class);
        PendingIntent pendingIntent;
        int requestCode = 42;

        // Create the PendingIntent using the appropriate flag based on the API level
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Use FLAG_IMMUTABLE for Android S and above
            pendingIntent = PendingIntent.getForegroundService(this, requestCode, serviceIntent, PendingIntent.FLAG_IMMUTABLE);
        } else {
            // For older versions, use the default flag
            pendingIntent = PendingIntent.getService(this, requestCode, serviceIntent, 0);
        }

        // Use the PendingIntent to start the foreground service
        try {
            pendingIntent.send();
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
        }
    }


    private void stopForegroundService() {
        Intent serviceIntent = new Intent(this, SoundDetectionService.class);
        stopService(serviceIntent);
    }

}
