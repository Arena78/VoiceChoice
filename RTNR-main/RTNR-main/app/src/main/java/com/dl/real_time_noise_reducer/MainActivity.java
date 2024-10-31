package com.dl.real_time_noise_reducer;
/*
import android.app.Activity;
import android.content.SharedPreferences;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.audiofx.Equalizer;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.jetbrains.annotations.NotNull;
*/

import static android.media.AudioManager.GET_DEVICES_INPUTS;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.media.audiofx.Equalizer;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.dl.rtnr.rtNoiseReducer;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


public class MainActivity extends AppCompatActivity {

    private MainActivity Instance;
    private com.dl.rtnr.rtNoiseReducer rtNoiseReducer;
    private TextView textViewStatus;
    private EditText editTextGainFactor;
    private EditText editMinVolume;
    private EditText editMaxVolume;

    private AudioManager audioManager;
    private AudioRecord audioRecord;
    private AudioTrack audioTrack;
    private AudioDeviceInfo inputDevice;
    private Equalizer mEqualizer;

    private SharedPreferences properties;

    //private lateinit var noiseSuppressor: NoiseSuppressor;

    private int intBufferSize = 0;
    private short[] shortAudioData;

    private float intGain = 1;
    private float minVolume = 0;
    private float maxVolume = 200;

    private boolean isActive = false;

    private Thread thread;
    public PowerManager.WakeLock wakeLock;
    @NotNull
    private final ActivityResultLauncher requestEnableBluetooth = this.registerForActivityResult((ActivityResultContract)(new ActivityResultContracts.StartActivityForResult()), MainActivity::requestEnableBluetooth$lambda$4);
    @NotNull
    private final ActivityResultLauncher requestMultiplePermissions = this.registerForActivityResult((ActivityResultContract)(new ActivityResultContracts.RequestMultiplePermissions()), MainActivity::requestMultiplePermissions$lambda$6);
    public static final int $stable = 8;

    public final PowerManager.WakeLock getWakeLock() {
        PowerManager.WakeLock var1 = this.wakeLock;
        if (var1 != null) {
            return var1;
        } else {
            //throwUninitializedPropertyAccessException("wakeLock");
            return null;
        }
    }

    public final void setWakeLock(@NotNull PowerManager.WakeLock var1) {
        //Intrinsics.checkNotNullParameter(var1, "<set-?>");
        if(var1 != null)
            this.wakeLock = var1;
        else Log.d("WakeLock", "Var1 is Null");
    }

    @RequiresApi(Build.VERSION_CODES.S)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //enableEdgeToEdge();
        setContentView(R.layout.activity_main);

        Instance = this;

        requestBluetooth();

        Activity var10000 = (Activity)this;
        String[] var2 = new String[]{"android.permission.RECORD_AUDIO"};
        ActivityCompat.requestPermissions(var10000, var2, 0);

        var10000 = (Activity)this;
        var2 = new String[]{"android.permission.WAKE_LOCK"};
        ActivityCompat.requestPermissions(var10000, var2, 0);

        initRTNR(this);

        Object var16 = this.getSystemService(Context.AUDIO_SERVICE);
        //checkNotNull(var16, "null cannot be cast to non-null type android.media.AudioManager");
        if(var16 != null) {
            Log.d("AudioManager", "AudioManager is functionall");
            this.audioManager = (AudioManager)var16;
            AudioManager var3 = this.audioManager;
        }

        List<AudioDeviceInfo> allDevices = new LinkedList<AudioDeviceInfo>(Arrays.asList(audioManager.getDevices(GET_DEVICES_INPUTS)));
        List<AudioDeviceInfo> devices = new LinkedList<AudioDeviceInfo>();

        //devices = Collections.emptyList();
        //devices += audioManager.getDevices(GET_DEVICES_OUTPUTS)
        List<String> arraySpinner = new LinkedList<String>();
        //arraySpinner = Collections.emptyList();
        /*
        for (int i = 0; i < allDevices.size(); i++) {
            switch(allDevices.get(i).getType()) {
                case AudioDeviceInfo.TYPE_BLE_HEADSET:
                    Log.d("DEVICE", "Is BLE HEADSET");
                    devices.add(allDevices.get(i));
                    arraySpinner.add(allDevices.get(i).getAddress().toString());
                    break;

                case AudioDeviceInfo.TYPE_BUILTIN_MIC:
                    Log.d("DEVICE", "Is BUILT INT MIC");
                    //devices.add(allDevices.get(i));
                    //arraySpinner.add(allDevices.get(i).getAddress().toString());
                    break;

                case AudioDeviceInfo.TYPE_HEARING_AID:
                    Log.d("DEVICE", "Is HEARING AID");
                    devices.add(allDevices.get(i));
                    arraySpinner.add(allDevices.get(i).getAddress().toString());
                    break;

                case AudioDeviceInfo.TYPE_USB_HEADSET:
                    Log.d("DEVICE", "Is USB HEADSET");
                    devices.add(allDevices.get(i));
                    arraySpinner.add(allDevices.get(i).getAddress().toString());
                    break;

                case AudioDeviceInfo.TYPE_WIRED_HEADPHONES:
                    Log.d("DEVICE", "Is WIRED HEADPHONES");
                    devices.add(allDevices.get(i));
                    arraySpinner.add(allDevices.get(i).getAddress().toString());
                    break;

                case AudioDeviceInfo.TYPE_WIRED_HEADSET:
                    Log.d("DEVICE", "Is WIRED HEADSET");
                    devices.add(allDevices.get(i));
                    arraySpinner.add(allDevices.get(i).getAddress().toString());
                    break;

                case AudioDeviceInfo.TYPE_UNKNOWN:
                    Log.d("DEVICE", "Is UNKNOWN");
                    break;

                default:
                    Log.d("DEVICE", "Is" + allDevices.get(i).getAddress().toString());
                    break;
            }
        }

        */
        allDevices.forEach(device -> {
            //arraySpinner.add(device.getAddress().toString());
            //bleInputDevice = device
            //break
            if (device.getType() == AudioDeviceInfo.TYPE_BLE_HEADSET) {
                Log.d("DEVICE", "Is BLE HEADSET");
                devices.add(device);
                arraySpinner.add(device.getAddress().toString());
            }
            else if (device.getType() == AudioDeviceInfo.TYPE_BLUETOOTH_SCO) {
                Log.d("DEVICE", "Is BLUETOOTH SCO");
                //devices.add(device);
                //arraySpinner.add(device.getAddress().toString());
            }
            else if (device.getType() == AudioDeviceInfo.TYPE_BUILTIN_MIC) {
                Log.d("DEVICE", "Is BUILT IN MIC");
                try {
                    devices.add(device);
                    arraySpinner.add(device.getAddress().toString());
                } catch(Exception e) {
                    Log.d("ERROR", "Error: " + e);
                }
            }
            /*else if (device.type == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE) {
                Log.d("DEVICE", "Is BUILT IN EARPIECE")
                arraySpinner.add(device.getAddress().toString());
            }*/
            else if (device.getType() == AudioDeviceInfo.TYPE_HEARING_AID) {
                Log.d("DEVICE", "Is HEARING AID");
                devices.add(device);
                arraySpinner.add(device.getAddress().toString());
            }
            else if (device.getType() == AudioDeviceInfo.TYPE_USB_HEADSET) {
                Log.d("DEVICE", "Is USB HEADSET");
                devices.add(device);
                arraySpinner.add(device.getAddress().toString());
            }
            else if (device.getType() == AudioDeviceInfo.TYPE_WIRED_HEADPHONES) {
                Log.d("DEVICE", "Is WIRED HEADPHONES");
                devices.add(device);
                arraySpinner.add(device.getAddress().toString());
            }
            else if (device.getType() == AudioDeviceInfo.TYPE_WIRED_HEADSET) {
                Log.d("DEVICE", "Is WIRED HEADSET");
                devices.add(device);
                arraySpinner.add(device.getAddress().toString());
            }
            else if (device.getType() == AudioDeviceInfo.TYPE_UNKNOWN) {
                Log.d("DEVICE", "Is UNKNOWN DEVICE");
            }
            else {

                Log.d("DEVICE", "Is " + device.getType());
            }
        });

        inputDevice = devices.get(0);
        Log.d("Device", String.valueOf(arraySpinner.size()));

        //Spinner s = findViewById<View>(R.id.InputMic
        //View var20 = this.findViewById(R.id.InputMic);
        //Intrinsics.checkNotNull(var20, "null cannot be cast to non-null type android.widget.Spinner");
        Spinner s = (Spinner)this.findViewById(R.id.InputMic);;
        ArrayAdapter adapter = new ArrayAdapter((Context)this, android.R.layout.simple_spinner_item, arraySpinner);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        s.setAdapter((SpinnerAdapter)adapter);

        //Object var23 = this.getSystemService(Context.POWER_SERVICE);
        //Intrinsics.checkNotNull(var23, "null cannot be cast to non-null type android.os.PowerManager");
        PowerManager var24 = (PowerManager)this.getSystemService(Context.POWER_SERVICE);
        PowerManager $this$onCreate_u24lambda_u241 = var24;
        //int var10 = false;
        PowerManager.WakeLock var11 = $this$onCreate_u24lambda_u241.newWakeLock(1, "MyApp::MyWakelockTag");
        PowerManager.WakeLock $this$onCreate_u24lambda_u241_u24lambda_u240 = var11;
        //int var13 = false;
        $this$onCreate_u24lambda_u241_u24lambda_u240.acquire(10*60*1000L /*10 minutes*/);
        //Intrinsics.checkNotNullExpressionValue(var11, "run(...)");
        this.setWakeLock(var11);


        this.textViewStatus = (TextView)this.findViewById(R.id.textViewStatus);
        this.editTextGainFactor = (EditText)this.findViewById(R.id.editTextGainFactor);
        this.editMinVolume = (EditText)this.findViewById(R.id.editMinVolume);
        this.editMaxVolume = (EditText)this.findViewById(R.id.editMaxVolume);

        this.thread = new Thread(this::threadLoop);

        EditText var25 = this.editTextGainFactor;
        EditText var28;
        if (var25 != null) {
            var28 = var25;
        }
        else {
            //Intrinsics.throwUninitializedPropertyAccessException("editTextGainFactor");
            var28 = null;
        }
        var28.addTextChangedListener((TextWatcher)(new TextWatcher() {
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //Intrinsics.checkNotNullParameter(s, "s");
                //String tempGain = s.toString();
                //Log.d("Gain", tempGain.toString());

                try {
                    String tempGain = s.toString();
                    Log.d("Gain", tempGain.toString());

                    MainActivity.this.intGain = Integer.parseInt(tempGain);
                } catch (Exception var7) {
                    Exception error = var7;
                    Log.d("Gain", "Gain isnt int " + error);
                }

            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void afterTextChanged(Editable s) {
            }
        }));

        var25 = this.editMinVolume;
        if (var25 != null) {
            var28 = var25;
        }
        else {
            //Intrinsics.throwUninitializedPropertyAccessException("editMinVolume");
            var28 = null;
        }
        var28.addTextChangedListener((TextWatcher)(new TextWatcher() {
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //Intrinsics.checkNotNullParameter(s, "s");
                //String tempGain = s.toString();
                //Log.d("GainMin", tempGain.toString());

                try {
                    String tempGain = s.toString();
                    Log.d("GainMin", tempGain.toString());

                    MainActivity.this.minVolume = Integer.parseInt(tempGain);
                } catch (Exception var7) {
                    Exception error = var7;
                    Log.d("GainMin", "GainMin Error: " + error);
                }

            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void afterTextChanged(Editable s) {
            }
        }));

        var25 = this.editMaxVolume;
        if (var25 != null) {
            var28 = var25;
        }
        else {
            //Intrinsics.throwUninitializedPropertyAccessException("editMaxVolume");
            var28 = null;
        }
        var28.addTextChangedListener((TextWatcher)(new TextWatcher() {
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //Intrinsics.checkNotNullParameter(s, "s");
                try {
                    String tempGain = s.toString();
                    Log.d("GainMax", tempGain.toString());

                    MainActivity.this.maxVolume = Integer.parseInt(tempGain);
                } catch (Exception var7) {
                    Exception error = var7;
                    Log.d("GainMax", "GainMax Error: " + error);
                }

            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void afterTextChanged(Editable s) {
            }
        }));

        s.setOnItemSelectedListener((AdapterView.OnItemSelectedListener)(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView parentView, View selectedItemView, int position, long id) {
                //Intrinsics.checkNotNullParameter(selectedItemView, "selectedItemView");
                MainActivity.this.inputDevice = (AudioDeviceInfo)((List)devices).get(position);
                Log.d("DEVICE", ((AudioDeviceInfo)((List)devices).get(position)).getAddress());
            }

            public void onNothingSelected(AdapterView parentView) {
            }
        }));

    }


    void initRTNR(Activity context) {
        try {
            rtNoiseReducer = new rtNoiseReducer(context);
        } catch (java.io.IOException e) {
            Log.d("class", "Failed to create noise reduction");
        }
    }

    public void buttonStart(View view) {
        Log.d("Start", "Start button clicked");
        float tempGain = intGain;
        try {
            tempGain = Integer.parseInt(editTextGainFactor.getText().toString());
            intGain = tempGain;
            Log.d("Gain", Float.toString(tempGain));
        } catch(Exception e) {
            tempGain = intGain;
            Log.d("ERROR", "Int Gain not of type Integer");
        }
        //if (tempGain instanceof int) {
        intGain = tempGain;
        //}

        textViewStatus.setText("Active");

        if (!isActive) {
            thread.start();
        }
        // else
        //     thread.resume();

        isActive = true;
    }

    @RequiresApi(Build.VERSION_CODES.S)
    public void buttonStop(View view) {
        Log.d("Stop", "Stop button clicked");
        isActive = false;
        if (audioTrack != null) {
            audioTrack.stop();
        }
        if (audioRecord != null) {
            audioRecord.stop();
        }

        textViewStatus.setText("Inactive");

        thread = new Thread(() -> {
            threadLoop();
        });
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private final void threadLoop() {
        int intRecordSampelRate = AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_MUSIC);
        this.intBufferSize = 512;//AudioRecord.getMinBufferSize(intRecordSampelRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        this.shortAudioData = new short[this.intBufferSize];
        if (ActivityCompat.checkSelfPermission((Context)this, "android.permission.RECORD_AUDIO") == 0) {
            this.audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, intRecordSampelRate, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT, this.intBufferSize);
            AudioRecord var2 = this.audioRecord;
            AudioRecord var10000;
            if (var2 != null) {
                var10000 = var2;
            } else {
                //Intrinsics.throwUninitializedPropertyAccessException("audioRecord");
                var10000 = null;
            }

            AudioDeviceInfo var24 = this.inputDevice;
            AudioDeviceInfo var10001;
            if (var24 != null) {
                var10001 = var24;
            } else {
                //Intrinsics.throwUninitializedPropertyAccessException("inputDevice");
                var10001 = null;
            }

            var10000.setPreferredDevice(var10001);
            this.audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, intRecordSampelRate, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT, this.intBufferSize, AudioTrack.MODE_STREAM);
            AudioTrack var25 = this.audioTrack;
            AudioTrack var27;
            if (var25 != null) {
                var27 = var25;
            } else {
                //Intrinsics.throwUninitializedPropertyAccessException("audioTrack");
                var27 = null;
            }

            var27.setPlaybackRate(intRecordSampelRate);
            var2 = this.audioRecord;
            if (var2 != null) {
                var10000 = var2;
            } else {
                //Intrinsics.throwUninitializedPropertyAccessException("audioRecord");
                var10000 = null;
            }

            var10000.startRecording();
            var25 = this.audioTrack;
            if (var25 != null) {
                var27 = var25;
            } else {
                //Intrinsics.throwUninitializedPropertyAccessException("audioTrack");
                var27 = null;
            }

            var27.play();/*
            float f = 1.0F;
            float z = 1.0F;
            float r = 0.0F;
            float x = 0.0F;
            float xp = 0.0F;
            float y = 0.0F;
            float yd = 0.0F;
            float var30 = (float)((double)z / (Math.PI * (double)f));
            float k2 = (float)((double)1 / Math.pow(2 * Math.PI * (double)f, (double)2));
            var30 = (float)((double)(r * z) / (2 * Math.PI * (double)f));*/
            float posVolume = 0.0F;
            /*float velVolume = 0.0F;
            long startTime = System.nanoTime();
            int minVolDelay = 1000000;
            boolean isQuiet = false;
*/
            short[] var10003;
            short[] var28;
            label156:
            for(float lastAvrVol = 0.0F; this.isActive;
                )
            {
                AudioRecord var19 = this.audioRecord;
                if (var19 != null) {
                    var10000 = var19;
                }
                else {
                    //Intrinsics.throwUninitializedPropertyAccessException("audioRecord");
                    var10000 = null;
                }

                short[] var29 = this.shortAudioData;
                if (var29 != null) {
                    var28 = var29;
                }
                else {
                    //Intrinsics.throwUninitializedPropertyAccessException("shortAudioData");
                    var28 = null;
                }

                var29 = this.shortAudioData;
                if (var29 != null) {
                    var10003 = var29;
                }
                else {
                    //Intrinsics.throwUninitializedPropertyAccessException("shortAudioData");
                    var10003 = null;
                }

                var10000.read(var28, 0, var10003.length);
                int i = 0;
                float averageVolume = 0.0F;
                short[] var22 = this.shortAudioData;
                short[] var32;
                if (var22 != null) {
                    var32 = var22;
                }
                else {
                    //Intrinsics.throwUninitializedPropertyAccessException("shortAudioData");
                    var32 = null;
                }

                short[] tempShortAudioData = this.shortAudioData;


                boolean llop = true;
                i = 0;
                while(i < shortAudioData.length) {

                    short[] var23 = this.shortAudioData;
                    if (var23 != null) {
                        var32 = var23;
                    } else {
                        //Intrinsics.throwUninitializedPropertyAccessException("shortAudioData");
                        var32 = null;
                    }

                    //Log.d("Gain", String.valueOf(this.intGain) + ", " + String.valueOf(this.minVolume) + ", " + String.valueOf(this.maxVolume));
                    float s = var32[i] * this.intGain;
                    if (s > Short.MAX_VALUE || s < Short.MIN_VALUE) {
                        if (s > Short.MAX_VALUE) {
                            s = Short.MAX_VALUE;
                        } else {
                            s = Short.MIN_VALUE;
                        }

                        Log.d("VOLUME", "HIT MAX VOLUME");
                    }
                    if(i % 2 == 0)
                        averageVolume += s;
                    //Log.d("shortData", i.toString() + ", " + s.toString());
                    tempShortAudioData[i] = (short)s;
                    i += 1;

                }
                averageVolume /= (i / 2);

                averageVolume /= (float)(i / 2);

                posVolume = averageVolume;

                Log.d("real", String.valueOf(averageVolume));
                Log.d("pos", String.valueOf(posVolume));
                //Log.d("vel", String.valueOf(velVolume));

                i = 0;
                while(i < shortAudioData.length) {
                    if (Math.abs(posVolume) < (float)this.minVolume) {
                        //if (startTime + (long)minVolDelay < System.nanoTime()) {
                        var22 = this.shortAudioData;
                        if (var22 != null) {
                            var32 = var22;
                        } else {
                            //Intrinsics.throwUninitializedPropertyAccessException("shortAudioData");
                            var32 = null;
                        }

                        this.shortAudioData[i] = 0;
                        //} else if (!isQuiet) {
                        //    startTime = System.nanoTime();
                        //    isQuiet = true;
                        //}
                    }
                    else if (Math.abs(posVolume) > (float)this.maxVolume) {
                        var22 = this.shortAudioData;
                        if (var22 != null) {
                            var32 = var22;
                        } else {
                            //Intrinsics.throwUninitializedPropertyAccessException("shortAudioData");
                            var32 = null;
                        }

                        this.shortAudioData[i] = 0;
                        //isQuiet = false;
                    }
                    else {
                        var22 = this.shortAudioData;
                        if (var22 != null) {
                            var32 = var22;
                        } else {
                            //Intrinsics.throwUninitializedPropertyAccessException("shortAudioData");
                            var32 = null;
                        }

                        this.shortAudioData[i] = tempShortAudioData[i];
                        //isQuiet = false;
                    }

                    ++i;
                }

                short[] shortData = (var28); // 512 -> 256

                //double[] doubleData = shortArrayToDoubleArray(shortData); // 256 -> 256
                double[] doubleData = new double[var28.length];
                Log.d("Var28", String.valueOf(var28.length));
                for (int j = 0; j < var28.length; j++) {
                    doubleData[j] = (double)var28[j];
                }
                Log.d("double", String.valueOf(doubleData.length));
                double[] se_out = rtNoiseReducer.audioSE(doubleData);
                //shortData = doubleArrayToShortArray(se_out);
                for (int j = 0; j < se_out.length; j++) {
                    var28[j] = (short)se_out[j];
                }

                //var28 = shortData;

                var27.write(shortData, 0, var10003.length);


            }

            Log.d("RUNNING", "Thread is done running");
            if (this.getWakeLock().isHeld()) {
                this.getWakeLock().release();
            }

        }
    }
/*
    @RequiresApi(Build.VERSION_CODES.S)
    private static void onCreate$lambda$2(@NonNull MainActivity activity) {
        //Intrinsics.checkNotNullParameter(this$0, "this$0");
        activity.threadLoop();
    }
*/
    public void requestBluetooth() {
        Log.d("SDK", String.valueOf(Build.VERSION.SDK_INT));
        if (Build.VERSION.SDK_INT >= 31) {
            ActivityResultLauncher var10000 = this.requestMultiplePermissions;
            String[] var1 = new String[]{"android.permission.BLUETOOTH_SCAN", "android.permission.BLUETOOTH_CONNECT"};
            var10000.launch(var1);
        } else {
            //Intent enableBtIntent = new Intent("android.bluetooth.adapter.action.REQUEST_ENABLE");
            //this.requestEnableBluetooth.launch(enableBtIntent);
        }
    }
    private static final void requestEnableBluetooth$lambda$4(ActivityResult result) {
        //Intrinsics.checkNotNullParameter(result, "result");
        if (result.getResultCode() == -1) {
            Log.d("BLUETOOTH", "ACCESS GRANTED");
        } else {
            Log.d("BLUETOOTH", "ACCESS DENIED");
        }

    }

    private static final void requestMultiplePermissions$lambda$6(Map permissions) {
        //Intrinsics.checkNotNullParameter(permissions, "permissions");
        Iterable $this$forEach$iv = (Iterable)permissions.entrySet();
        Iterator var3 = $this$forEach$iv.iterator();

        while(var3.hasNext()) {
            Object element$iv = var3.next();
            Map.Entry it = (Map.Entry)element$iv;
            Log.d("MyTag", (String)it.getKey() + " = " + (Boolean)it.getValue());
        }

    }


 /*
    short[] shortData = byteArrayToShortArray(writeData); // 512 -> 256
    double[] doubleData = shortArrayToDoubleArray(shortData); // 256 -> 256
    double[] se_out = rtNoiseReducer.audioSE(doubleData);
    shortData = doubleArrayToShortArray(se_out);
    writeData = shortArrayToByteArray(shortData);*/

}
