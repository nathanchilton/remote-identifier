package com.nathanchilton.remoteidentifier;

import androidx.appcompat.app.AppCompatActivity;

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

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private boolean permissionToRecordAccepted = false;
    private String[] permissions = { Manifest.permission.RECORD_AUDIO };

    private MediaRecorder mediaRecorder;
    private TextView timestampTextView;
    private double initialThreshold = 700;
    private double threshold = initialThreshold;

    TextToSpeech textToSpeech;
    EditText identificationText;
    TextView currentAmplitude;
    Button testSpeech;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sound_detection);

        timestampTextView = findViewById(R.id.timestampTextView);
        ((EditText) findViewById(R.id.thresholdEditText)).setText(String.valueOf(initialThreshold));
        currentAmplitude = (TextView) findViewById(R.id.currentAmplitude);

        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);

        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    textToSpeech.setLanguage(Locale.UK);
                }
            }
        });

        identificationText = findViewById(R.id.identificationText);
        testSpeech = findViewById(R.id.testSpeech);

        testSpeech.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String toSpeak = identificationText.getText().toString();
                Toast.makeText(getApplicationContext(), toSpeak, Toast.LENGTH_SHORT).show();
                textToSpeech.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);
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

        EditText thresholdEditText = findViewById(R.id.thresholdEditText);
        thresholdEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // Not used in this implementation
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // Check if the input is a valid double and update the threshold value
                try {
                    threshold = Double.parseDouble(charSequence.toString());
                } catch (NumberFormatException e) {
                    threshold = 0.0; // Set a default threshold if parsing fails
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
                // Not used in this implementation
            }
        });
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
        threshold = Double.parseDouble(thresholdEditText.getText().toString());

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
                    if (mediaRecorder != null) {
                        double amplitude = getAmplitude();
                        currentAmplitude.setText("Current Amplitude: " + String.valueOf(amplitude));
                        if (amplitude > threshold) {
                            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                    .format(new Date());
                            timestampTextView
                                    .setText(timestamp + "\nAmplitude: " + amplitude + "\nThreshold: " + threshold);
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
