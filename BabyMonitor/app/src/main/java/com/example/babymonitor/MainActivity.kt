package com.example.babymonitor

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioManager.GET_DEVICES_INPUTS
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.audiofx.Equalizer
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import java.io.IOException
import kotlin.math.abs
import kotlin.math.pow


class MainActivity : ComponentActivity() {

    private lateinit var textViewStatus: TextView
    private lateinit var editTextGainFactor: EditText
    private lateinit var editMinVolume: EditText
    private lateinit var editMaxVolume: EditText

    private lateinit var audioManager: AudioManager;
    private lateinit var audioRecord: AudioRecord;
    private lateinit var audioTrack: AudioTrack;
    private lateinit var inputDevice: AudioDeviceInfo;
    private lateinit var mEqualizer: Equalizer;

    private lateinit var properties: SharedPreferences;

    //private lateinit var noiseSuppressor: NoiseSuppressor;

    private var intBufferSize: Int = 0;
    private lateinit var shortAudioData: ShortArray;

    private var intGain: Int = 1;
    private var minVolume = 0;
    private var maxVolume = 200;

    private var isActive: Boolean = false;

    private lateinit var thread: Thread;
    public lateinit var wakeLock: PowerManager.WakeLock

    @RequiresApi(Build.VERSION_CODES.S)
    @Override
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.linearlayout)

        requestBluetooth()

        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), PackageManager.PERMISSION_GRANTED)
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WAKE_LOCK), PackageManager.PERMISSION_GRANTED)

        audioManager = this.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        var allDevices = audioManager.getDevices(GET_DEVICES_INPUTS)
        var devices: MutableList<AudioDeviceInfo> = ArrayList();
        //devices += audioManager.getDevices(GET_DEVICES_OUTPUTS)
        val arraySpinner: MutableList<String> = ArrayList()

        for (device in allDevices) {
            //arraySpinner.add(device.getAddress().toString());
            //bleInputDevice = device
            //break
            if (device.type == AudioDeviceInfo.TYPE_BLE_HEADSET) {
                Log.d("DEVICE", "Is BLE HEADSET")
                devices.add(device);
                arraySpinner.add(device.getAddress().toString());
            }
            else if (device.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO) {
                Log.d("DEVICE", "Is BLUETOOTH SCO")
                //devices.add(device);
                //arraySpinner.add(device.getAddress().toString());
            }
            else if (device.type == AudioDeviceInfo.TYPE_BUILTIN_MIC) {
                Log.d("DEVICE", "Is BUILT IN MIC")
                devices.add(device);
                arraySpinner.add(device.getAddress().toString());
            }
            /*else if (device.type == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE) {
                Log.d("DEVICE", "Is BUILT IN EARPIECE")
                arraySpinner.add(device.getAddress().toString());
            }*/
            else if (device.type == AudioDeviceInfo.TYPE_HEARING_AID) {
                Log.d("DEVICE", "Is HEARING AID")
                devices.add(device);
                arraySpinner.add(device.getAddress().toString());
            }
            else if (device.type == AudioDeviceInfo.TYPE_USB_HEADSET) {
                Log.d("DEVICE", "Is USB HEADSET")
                devices.add(device);
                arraySpinner.add(device.getAddress().toString());
            }
            else if (device.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES) {
                Log.d("DEVICE", "Is WIRED HEADPHONES")
                devices.add(device);
                arraySpinner.add(device.getAddress().toString());
            }
            else if (device.type == AudioDeviceInfo.TYPE_WIRED_HEADSET) {
                Log.d("DEVICE", "Is WIRED HEADSET")
                devices.add(device);
                arraySpinner.add(device.getAddress().toString());
            }
            else if (device.type == AudioDeviceInfo.TYPE_UNKNOWN) {
                Log.d("DEVICE", "Is UNKNOWN DEVICE")
            }
            else {

                Log.d("DEVICE", "Is " + device.type);
            }
        }
        inputDevice = devices[0];
        Log.d("Device", arraySpinner.size.toString());

        val s = findViewById<View>(R.id.InputMic) as Spinner
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item, arraySpinner
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        s.adapter = adapter


        //val mContext = applicationContext
        //val powerManager = mContext.getSystemService(POWER_SERVICE) as PowerManager
        //wakeLock = powerManager.newWakeLock(PARTIAL_WAKE_LOCK, ACQUIRE_CAUSES_WAKEUP.toString())
        //wakeLock.acquire()
        wakeLock =
            (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
                newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyApp::MyWakelockTag").apply {
                    acquire()
                }
            }


        textViewStatus = findViewById(R.id.textViewStatus)
        editTextGainFactor = findViewById(R.id.editTextGainFactor)
        editMinVolume = findViewById(R.id.editMinVolume)
        editMaxVolume = findViewById(R.id.editMaxVolume)

        thread = Thread {
            threadLoop();
        }

        editTextGainFactor.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(
                s: CharSequence, start: Int, before: Int,
                count: Int
            ) {
                var tempGain = s.toString()
                Log.d("Gain", tempGain.toString())
                try {
                    intGain = tempGain.toInt()
                }
                catch(error : Exception) {
                    Log.d("Gain", "Gain isnt int " + error)
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
            }
        })

        editMinVolume.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(
                s: CharSequence, start: Int, before: Int,
                count: Int
            ) {
                var tempGain = s.toString()
                Log.d("GainMin", tempGain.toString())
                try {
                    minVolume = tempGain.toInt()
                }
                catch(error : Exception) {
                    Log.d("GainMin", "GainMin Error: " + error)
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
            }
        })

        editMaxVolume.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(
                s: CharSequence, start: Int, before: Int,
                count: Int
            ) {
                var tempGain = s.toString()
                Log.d("GainMax", tempGain.toString())
                try {
                    maxVolume = tempGain.toInt()
                }
                catch(error : Exception) {
                    Log.d("GainMax", "GainMax Error: " + error)
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
            }
        })

        s.setOnItemSelectedListener(object : OnItemSelectedListener {
            override fun onItemSelected(
                parentView: AdapterView<*>?,
                selectedItemView: View,
                position: Int,
                id: Long
            ) {
                inputDevice = devices[position];
                Log.d("DEVICE", devices[position].address);
            }

            override fun onNothingSelected(parentView: AdapterView<*>?) {
                // your code here
            }
        })
    }

    public fun buttonStart(view: View) {
        Log.d("Start", "Start button clicked")

        var tempGain = editTextGainFactor.text.toString().toInt()
        Log.d("Gain", tempGain.toString())
        if(tempGain is Int)
            intGain = tempGain

        textViewStatus.text = "Active";

        if(!isActive)
            thread.start();
        //else
        //    thread.resume();

        isActive = true;
    }

    public fun buttonStop(view: View) {
        Log.d("Stop", "Stop button clicked")
        isActive = false;
        if(this::audioTrack.isInitialized)
            audioTrack.stop();
        if(this::audioRecord.isInitialized)
            audioRecord.stop();

        textViewStatus.text = "Inactive";

        thread = Thread {
            threadLoop();
        }
    }

    private fun threadLoop() {
        var intRecordSampelRate = AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_MUSIC);

        intBufferSize = AudioRecord.getMinBufferSize(intRecordSampelRate, AudioFormat.CHANNEL_IN_MONO
            , AudioFormat.ENCODING_PCM_16BIT);

        shortAudioData = ShortArray(intBufferSize);


        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        audioRecord = AudioRecord(MediaRecorder.AudioSource.MIC
            , intRecordSampelRate, AudioFormat.CHANNEL_IN_STEREO
            , AudioFormat.ENCODING_PCM_16BIT, intBufferSize);

        audioRecord.preferredDevice = inputDevice;

        audioTrack = AudioTrack(AudioManager.STREAM_MUSIC
            , intRecordSampelRate, AudioFormat.CHANNEL_IN_STEREO
            , AudioFormat.ENCODING_PCM_16BIT, intBufferSize, AudioTrack.MODE_STREAM);

        audioTrack.setPlaybackRate(intRecordSampelRate);
        //audioTrack.setWakeMode(PARTIAL_WAKE_LOCK);

        audioRecord.startRecording();
        audioTrack.play();

        //equalizer = Equalizer(1000000, audioTrack.audioSessionId)
        //equalizer.setEnabled(true);

        //wakeLock.acquire(9099999999999999999.toLong())




        // As described in https://www.youtube.com/watch?v=KPoeNZZ6H4s
        var f = 1f; // F
        var z = 1f; // Zeta
        var r = 0f; // R

        var x = 0f;
        var xp = 0f;
        var y = 0f;
        var yd = 0f;

        var k1 = (z / (Math.PI * f)).toFloat();
        var k2 = (1 / ((2 * Math.PI * f).pow(2))).toFloat();
        var k3 = (r * z / (2 * Math.PI * f)).toFloat();

        var posVolume = 0f; // current playback volume
        var velVolume = 0f; // delta volume (how much it changes right now)

        var startTime: Long = System.nanoTime();
        var minVolDelay = 1000000
        var isQuiet = false

        var lastAvrVol = 0f; // The average volume last frame

        while(isActive) {
            audioRecord.read(shortAudioData, 0, shortAudioData.size);

            var i = 0;
            var averageVolume = 0f; // Calculated actuall average volume this frame

            var tempShortAudioData = shortAudioData;

            while(i < shortAudioData.size) {
                var s = shortAudioData[i] * intGain;
                if(s > Short.MAX_VALUE || s < Short.MIN_VALUE) {
                    if(s > Short.MAX_VALUE)
                        s = Short.MAX_VALUE.toInt();
                    else
                        s = Short.MIN_VALUE.toInt();
                    Log.d("VOLUME", "HIT MAX VOLUME");
                }
                if(i % 2 == 0)
                    averageVolume += s;
                //Log.d("shortData", i.toString() + ", " + s.toString());
                tempShortAudioData[i] = s.toShort();
                i += 1;
            }
            averageVolume /= (i / 2);
            //Log.d("Avg", averageVolume.toString())

            //var volumeDeriv = (averageVolume - lastAvrVol) / delta;

            posVolume = averageVolume;
            //lastAvrVol = averageVolume;
            //posVolume += delta * velVolume;
            //velVolume += delta * (averageVolume + k3*volumeDeriv - posVolume - k1*velVolume) / k2;
/*
            var averageVel = (averageVolume - lastAvrVol) / delta;

            posVolume += (delta * velVolume).toFloat();
            velVolume += (delta * (averageVolume + k3*averageVel - posVolume - k1*velVolume) /k2).toFloat();
            if(velVolume == Float.POSITIVE_INFINITY) {
                velVolume = Float.MAX_VALUE;
            } else if(velVolume == Float.NEGATIVE_INFINITY)
                velVolume = -Float.MAX_VALUE;
            if(posVolume == Float.POSITIVE_INFINITY) {
                posVolume = Float.MAX_VALUE;
            } else if(posVolume == Float.NEGATIVE_INFINITY)
                posVolume = -Float.MAX_VALUE;
            //accVolume = (averageVolume + k3*averageVel - posVolume - k1*velVolume) / k2;
*/
            Log.d("real", averageVolume.toString())
            Log.d("pos", posVolume.toString())
            Log.d("vel", velVolume.toString())

            i = 0;
            while(i < shortAudioData.size) {
                if(abs(posVolume) < minVolume) {
                    if(startTime + minVolDelay < System.nanoTime())
                        shortAudioData[i] = 0;
                    else if(!isQuiet) {
                        startTime = System.nanoTime();
                        isQuiet = true
                    }
                }
                else if(abs(posVolume) > maxVolume) {
                    shortAudioData[i] = 0;
                    isQuiet = false
                }
                else {
                    shortAudioData[i] = tempShortAudioData[i];
                    isQuiet = false
                }
                i += 1;
            }

            //Log.d("AvrVolume", (averageVolume / shortAudioData.size).toString())

            audioTrack.write(shortAudioData, 0, shortAudioData.size);

            lastAvrVol = averageVolume;
            //Log.d("RUNNING", "Thread is running");

        }

        Log.d("RUNNING", "Thread is done running");

        if(wakeLock.isHeld)
            wakeLock.release();

    }


    fun requestBluetooth() {
        // check android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestMultiplePermissions.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                )
            )
        } else {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            requestEnableBluetooth.launch(enableBtIntent)
        }
    }
/*
    fun initRTNR() {
        try {
            rtNoiseReducer = rtNoiseReducer(getActivity())
        } catch (e: IOException) {
            Log.d("class", "Failed to create noise reduction")
        }
    }
*/
    private val requestEnableBluetooth =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                Log.d("BLUETOOTH", "ACCESS GRANTED")
            } else {
                Log.d("BLUETOOTH", "ACCESS DENIED")
            }
        }

    private val requestMultiplePermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach {
                Log.d("MyTag", "${it.key} = ${it.value}")
            }
        }
}
