package com.example.babymonitor;

import static android.app.PendingIntent.getActivity;

import android.app.Activity;

import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.net.wifi.WifiManager;
import android.nfc.Tag;
import android.os.PowerManager;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Button;

import androidx.core.content.ContextCompat;

import com.example.babymonitor.ForegroundService;
//import com.getcapacitor.JSObject;
//import com.getcapacitor.NativePlugin;
//import com.getcapacitor.Plugin;
//import com.getcapacitor.PluginCall;
//import com.getcapacitor.PluginMethod;
//import com.getcapacitor.PluginResult;

import java.net.DatagramSocket;

//@NativePlugin
public class MainActivity {


    private static String TAG = "V.General";
    //@PluginMethod
    public void StartRecorder() {//PluginCall call) {
        Log.i(TAG, "Starting the foreground-thread");

        Intent serviceIntent = new Intent(getActivity().getApplicationContext(), ForegroundService.class);
        serviceIntent.putExtra("inputExtra", "Foreground Service Example in Android");
        ContextCompat.startForegroundService(getActivity(), serviceIntent);

        //call.resolve();
    }
    //@PluginMethod
    public void StopRecorder() {//PluginCall call) {
        Log.i(TAG, "Stopping the foreground-thread");

        Intent serviceIntent = new Intent(getActivity().getApplicationContext(), ForegroundService.class);
        getActivity().getApplicationContext().stopService(serviceIntent);

        //call.resolve();
    }




    // From what I've seen you don't need the wake-lock or wifi-lock below for the audio-recorder to persist through screen-off.
    // However, to be on the safe side you might want to activate them anyway. (and/or if you have other functions that need them)



    private PowerManager.WakeLock wakeLock_partial = null;
    public void StartPartialWakeLock() {
        if (wakeLock_partial != null && wakeLock_partial.isHeld()) return;
        Log.i("vmain", "Starting partial wake-lock.");
        final PowerManager pm = (PowerManager) getActivity().getApplicationContext().getSystemService(Context.POWER_SERVICE);
        wakeLock_partial = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "com.myapp:partial_wake_lock");
        wakeLock_partial.acquire();
    }
    public void StopPartialWakeLock() {
        if (wakeLock_partial != null && wakeLock_partial.isHeld()) {
            Log.i("vmain", "Stopping partial wake-lock.");
            wakeLock_partial.release();
        }
    }

    private WifiManager.WifiLock wifiLock = null;
    public void StartWifiLock() {
        WifiManager wifiManager = (WifiManager) getActivity().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL, "LockTag");
        wifiLock.acquire();
    }
    public void StopWifiLock() {
        wifiLock.release();
    }
}