package com.nathanchilton.remoteidentifier;

import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;

import androidx.core.app.ActivityCompat;

import android.speech.tts.TextToSpeech;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.widget.EditText;
import android.widget.Toast;

//public class SoundDetectionActivity extends AppCompatActivity {
public class MainActivity extends AppCompatActivity {

    private final String appName = "Remote Identifier"; // getString(R.string.app_name);

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private boolean permissionToRecordAccepted = false;
    private String[] permissions = { Manifest.permission.RECORD_AUDIO };

    private MediaRecorder mediaRecorder;
    private TextView timestampTextView;
    private double initialThreshold = 700;
    private final int DEFAULT_ANNOUNCEMENT_FREQUENCY = 15;
    private final int DEFAULT_THRESHOLD = 700;

    private float announcementFrequency = DEFAULT_ANNOUNCEMENT_FREQUENCY;
    // private int threshold = DEFAULT_THRESHOLD;

    private long timeOfLastAnnouncement = 0;
    private long timeOfLastSoundWhichExceededTheThreshold = 0;

    TextToSpeech textToSpeech;

    EditText thresholdEditText;
    EditText identificationText;
    EditText announcementFrequencyEditText;

    TextView currentAmplitude;
    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        sharedPreferences = getSharedPreferences(appName, MODE_PRIVATE);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sound_detection);

        timestampTextView = findViewById(R.id.timestampTextView);
        ((EditText) findViewById(R.id.thresholdEditText)).setText(String.valueOf(initialThreshold));

        currentAmplitude = (TextView) findViewById(R.id.currentAmplitude);
        identificationText = (EditText) findViewById(R.id.identificationText);
        announcementFrequencyEditText = (EditText) findViewById(R.id.announcementFrequency);
        thresholdEditText = (EditText) findViewById(R.id.thresholdEditText);

        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);

        loadSettings();

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
                makeAnnouncement();
            }
        });

        findViewById(R.id.startButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startRecording();
                findViewById(R.id.stopButton).setEnabled(true);
                findViewById(R.id.startButton).setEnabled(false);
            }
        });

        findViewById(R.id.stopButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopRecording();
                findViewById(R.id.stopButton).setEnabled(false);
                findViewById(R.id.startButton).setEnabled(true);
            }
        });

        thresholdEditText.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                saveSettings();
                // loadSettings();
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
                // loadSettings();
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
                // loadSettings();
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
        identificationText.setText(String.valueOf(identificationTextString));
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

    private void makeAnnouncement() {
        String toSpeak = getIdentificationText();
        Toast.makeText(getApplicationContext(), toSpeak, Toast.LENGTH_SHORT).show();
        textToSpeech.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);

        // reset the timeOfLastAnnouncement to the current time
        timeOfLastAnnouncement = System.currentTimeMillis();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_RECORD_AUDIO_PERMISSION:
                permissionToRecordAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }
        if (!permissionToRecordAccepted) {
            finish();
        }
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
            mediaRecorder.prepare();
            mediaRecorder.start();

            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    int threshold = getThreshold();

                    if (mediaRecorder != null) {
                        double amplitude = getAmplitude();
                        currentAmplitude.setText("Current Amplitude: " + String.valueOf(amplitude));
                        float minutesSinceLastAnnouncement = (float) ((System.currentTimeMillis()
                                - timeOfLastAnnouncement) / 1000 / 60);
                        if (amplitude > threshold) {
                            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                    .format(new Date());
                            timestampTextView
                                    .setText(timestamp + "\nAmplitude: " + amplitude + "\nThreshold: " + threshold);
                            timeOfLastSoundWhichExceededTheThreshold = System.currentTimeMillis();
                        } else {
                            // nobody is talking, so we can make an announcement, if appropriate
                            TextView minutesAgo = (TextView) findViewById(R.id.minutesAgo);
                            minutesAgo.setText("(" + String.valueOf(
                                    (int) (System.currentTimeMillis() - timeOfLastSoundWhichExceededTheThreshold) / 1000
                                            / 60)
                                    + " minutes ago.)");

                            // if timeSinceLastAnnouncement > announcementFrequency

                            if ((minutesSinceLastAnnouncement > getAnnouncementFrequency())
                                    && (timeOfLastSoundWhichExceededTheThreshold > timeOfLastAnnouncement)) {
                                makeAnnouncement();
                            }

                            TextView timeSinceLastIDTextView = (TextView) findViewById(R.id.timeSinceLastID);
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
}
