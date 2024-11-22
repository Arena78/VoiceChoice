package com.example.babymonitor;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.IBinder;
//import android.support.annotation.Nullable;
//import android.support.v4.app.NotificationCompat;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.example.babymonitor.MainActivity;
//import com.getcapacitor.PluginCall;
//import com.getcapacitor.PluginMethod;

public class ForegroundService extends Service {
    public static final String CHANNEL_ID = "ForegroundServiceChannel";

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String input = intent.getStringExtra("inputExtra");
        createNotificationChannel();
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Foreground Service")
                .setContentText(input)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(1, notification);

        // do heavy work on a background thread
        StartRecorder();

        //stopSelf();

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    //@Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }


    private static String TAG = "ForegroundService";

    // the audio recording options
    private static final int RECORDING_RATE = 44100;
    private static final int CHANNEL = AudioFormat.CHANNEL_IN_MONO;
    private static final int FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    // the audio recorder
    private AudioRecord recorder;
    // the minimum buffer size needed for audio recording
    private static int BUFFER_SIZE = AudioRecord.getMinBufferSize(RECORDING_RATE, CHANNEL, FORMAT);
    // are we currently sending audio data
    private boolean currentlySendingAudio = false;

    public void StartRecorder() {
        Log.i(TAG, "Starting the audio stream");
        currentlySendingAudio = true;
        startStreaming();
    }

    public void StopRecorder() {
        Log.i(TAG, "Stopping the audio stream");
        currentlySendingAudio = false;
        recorder.release();
    }

    private void startStreaming() {
        Log.i(TAG, "Starting the background thread (in this foreground service) to read the audio data");

        Thread streamThread = new Thread(() -> {
            try {
                Log.d(TAG, "Creating the buffer of size " + BUFFER_SIZE);
                //byte[] buffer = new byte[BUFFER_SIZE];
                int rate = AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_SYSTEM);
                int bufferSize = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
                short[] buffer = new short[bufferSize];

                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);

                Log.d(TAG, "Creating the AudioRecord");
                //recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, RECORDING_RATE, CHANNEL, FORMAT, BUFFER_SIZE * 10);
                if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);

                Log.d(TAG, "AudioRecord recording...");
                recorder.startRecording();

                while (currentlySendingAudio == true) {
                    // read the data into the buffer
                    int readSize = recorder.read(buffer, 0, buffer.length);

                    double maxAmplitude = 0;
                    for (int i = 0; i < readSize; i++) {
                        if (Math.abs(buffer[i]) > maxAmplitude) {
                            maxAmplitude = Math.abs(buffer[i]);
                        }
                    }

                    double db = 0;
                    if (maxAmplitude != 0) {
                        db = 20.0 * Math.log10(maxAmplitude / 32767.0) + 90;
                    }

                    Log.d(TAG, "Max amplitude: " + maxAmplitude + " ; DB: " + db);
                }

                Log.d(TAG, "AudioRecord finished recording");
            } catch (Exception e) {
                Log.e(TAG, "Exception: " + e);
            }
        });

        // start the thread
        streamThread.start();
    }
}