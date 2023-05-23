package com.nathanchilton.remoteidentifier;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;

import androidx.core.app.ActivityCompat;

import android.view.View;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.app.Activity;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.util.Locale;

import android.widget.Toast;


//public class SoundDetectionActivity extends AppCompatActivity {
public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private boolean permissionToRecordAccepted = false;
    private String[] permissions = {Manifest.permission.RECORD_AUDIO};

    private MediaRecorder mediaRecorder;
    private TextView timestampTextView;

    TextToSpeech tts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sound_detection);

        timestampTextView = findViewById(R.id.timestampTextView);

        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);

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

        tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    tts.setLanguage(Locale.UK);
                }
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
        String voiceMessage = "";


        String fileName = "recording.3gp";
        File outputFile = new File(getExternalFilesDir(null), fileName);
        String filePath = outputFile.getAbsolutePath();

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
                        if (amplitude > 700) {
                            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
                            timestampTextView.setText(timestamp + "\nAmplitude: " + amplitude);

                            // if time since last identification exceeds threshold
                            // set a value for voiceMessage
                        } else {
                            // if it's been (at least) a second or two since we've heard a sound
                            // if there is not a message to announce
                            //   if there has been activity within the past half hour
                            //   AND we have not transmitted an ID for more than one minute
                            //   AND the minute is 00
                            //   set voiceMessage to something which includes both the ID and the date and time
                            // if there is a message to announce

                            // announce the message
                            // https://developer.android.com/reference/android/speech/tts/TextToSpeech#speak(java.lang.CharSequence,%20int,%20android.os.Bundle,%20java.lang.String)
                            tts.speak(voiceMessage, TextToSpeech.QUEUE_FLUSH, null, "utteranceId");

                            // clear the message
                            // note the time of last ID
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
