package com.example.babymonitor

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import android.os.Handler
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
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
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

    @SuppressLint("MissingPermission")
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

        //var bleInputDevice: AudioDeviceInfo? = null
        var scoInputDevice: AudioDeviceInfo? = null

        val bluetoothManager: BluetoothManager = this.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager;
        val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.getAdapter();

        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices;
        pairedDevices?.forEach { device ->
            val deviceName = device.name
            val deviceHardwareAddress = device.address // MAC address
            Log.d("Bluetooth", "Found connected bl device" + deviceName + ", " + deviceHardwareAddress)
        }
        var handler: Handler = Handler();

        AcceptThread().start();
        var bluetoothServiceGuy = MyBluetoothService(handler);
        //bluetoothServiceGuy.ConnectedThread();

        // Register for broadcasts when a device is discovered.
        //val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        //registerReceiver(receiver, filter)

        for (device in allDevices) {
            //arraySpinner.add(device.getAddress().toString());
            //bleInputDevice = device
            //break
            if (device.type == AudioDeviceInfo.TYPE_BLE_HEADSET) {
                Log.d("DEVICE", "Is BLE HEADSET")
                //bleInputDevice = device

                devices.add(device);
                arraySpinner.add(device.getAddress().toString());
                break;
            }
            else if (device.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO) {
                Log.d("DEVICE", "Is BLUETOOTH SCO")
                scoInputDevice = device
                devices.add(device);
                arraySpinner.add(device.getAddress().toString());
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

        /*if(bleInputDevice != null) {
            inputDevice = bleInputDevice
            Log.d("bluetooth", "Bluetooth device connected (BLE)");
        }
        else */if(scoInputDevice != null) {
            inputDevice = scoInputDevice
            Log.d("bluetooth", "Bluetooth device connected (SCO)");
        }
        else
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


    private inner class AcceptThread : Thread() {
        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter();
        /*private val mmServerSocket: BluetoothServerSocket? by lazy(LazyThreadSafetyMode.NONE) {
            bluetoothAdapter?.listenUsingInsecureRfcommWithServiceRecord("Voice Choice", UUID.fromString("27805505-bfcb-43bc-a048-dd7d933b1b96"))
        }*/









        var handler: Handler = Handler();

        override fun run() {
            var isSecure: Boolean = true;
            var D: Boolean = true;

            //var bt: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter();

            if(bluetoothAdapter == null) {
                //throw new RuntimeException("No bluetooth adapter...");
            }
            var rfcommSocket: BluetoothServerSocket? = null;
            var l2capSocket: BluetoothServerSocket? = null;
            var initSocketOK: Boolean = false;
            // It's possible that create will fail in some cases. retry for 10 times
            for (i in 1..9) {
                initSocketOK = true;
                try {
                    if (rfcommSocket == null) {
                        if (isSecure) {
                            rfcommSocket = bluetoothAdapter.listenUsingRfcommOn(rfcommChannel);
                        } else {
                            rfcommSocket = bluetoothAdapter.listenUsingInsecureRfcommOn(rfcommChannel);
                        }
                    }
                    if (l2capSocket == null) {
                        if (isSecure) {
                            l2capSocket = bluetoothAdapter.listenUsingL2capOn(l2capPsm);
                        } else {
                            l2capSocket = bluetoothAdapter.listenUsingInsecureL2capOn(l2capPsm);
                        }
                    }
                } catch (e: IOException) {
                    Log.e("STAG", "Error create ServerSockets ", e);
                    initSocketOK = false;
                } catch (e: SecurityException) {
                    Log.e("STAG", "Error create ServerSockets ", e);
                    initSocketOK = false;
                    break;
                }
                if (!initSocketOK) {
                    // Need to break out of this loop if BT is being turned off.
                    var state: Int = bluetoothAdapter!!.getState();
                    if ((state != BluetoothAdapter.STATE_TURNING_ON) && (state
                                != BluetoothAdapter.STATE_ON)) {
                        Log.w("STAG", "initServerSockets failed as BT is (being) turned off");
                        break;
                    }
                    try {
                        if (D) {
                            Log.v("STAG", "waiting 300 ms...");
                        }
                        Thread.sleep(300);
                    } catch (e: InterruptedException) {
                        Log.e("STAG", "create() was interrupted");
                    }
                } else {
                    break;
                }
            }
            if (initSocketOK) {
                if (D) {
                    Log.d("STAG", "Succeed to create listening sockets ");
                }
                //ObexServerSockets sockets = new ObexServerSockets(validator, rfcommSocket, l2capSocket);
                sockets.startAccept();
                return sockets;
            } else
            {
                Log.e("STAG", "Error to create listening socket after " + CREATE_RETRY_TIME + " try");
                return null;
            }





            // Keep listening until exception occurs or a socket is returned.
            Log.d("mmSERVERSOCKET", mmServerSocket.toString());
            var socket: BluetoothSocket? = null;

            var shouldLoop = true
            while (shouldLoop) {
                Log.d("Bluetooth", "connecting bl device to socket");
                //socket = mmServerSocket!!.accept();
                Log.d("socket", 5.toString());
                /*try {
                    mmServerSocket?.accept();
                } catch (e: IOException) {
                    Log.e("Bluetooth", "Socket's accept() method failed", e)
                    shouldLoop = false
                    null
                } finally {
                    Log.d("accept", "mmserversocket try accept");
                }*/
                //shouldLoop = false;
                if(socket?.isConnected == true) {
                    socket?.also {
                        //manageMyConnectedSocket(it)


                        Log.d("Bluetooth", "also")
                        mmServerSocket?.close()
                        shouldLoop = false
                    }
                }

                var bluetoothServiceGuy = MyBluetoothService(handler);
                BluetoothDevice.createRfcommSocketToServiceRecord(74:B7:E6:2A:92:4F);

                bluetoothServiceGuy.ConnectedThread();

                shouldLoop = false;
            }


        }

        // Closes the connect socket and causes the thread to finish.
        fun cancel() {
            try {
                mmServerSocket?.close()
            } catch (e: IOException) {
                Log.e("Bluetooth", "Could not close the connect socket", e)
            }
        }
    }


    private val TAG = "MY_APP_DEBUG_TAG"

    // Defines several constants used when transmitting messages between the
// service and the UI.
    val MESSAGE_READ: Int = 0
    val MESSAGE_WRITE: Int = 1
    val MESSAGE_TOAST: Int = 2
// ... (Add other message types here as needed.)

    class MyBluetoothService(
        // handler that gets info from Bluetooth service
        private val handler: Handler
    ) {

        public inner class ConnectedThread(private val mmSocket: BluetoothSocket) : Thread() {

            private val mmInStream: InputStream = mmSocket.inputStream
            private val mmOutStream: OutputStream = mmSocket.outputStream
            private val mmBuffer: ByteArray = ByteArray(1024) // mmBuffer store for the stream

            override fun run() {
                var numBytes: Int // bytes returned from read()

                // Keep listening to the InputStream until an exception occurs.
                while (true) {
                    // Read from the InputStream.
                    numBytes = try {
                        mmInStream.read(mmBuffer)
                    } catch (e: IOException) {
                        Log.d("Bluetooth", "Input stream was disconnected", e)
                        break
                    }

                    // Send the obtained bytes to the UI activity.
                    /*val readMsg = handler.obtainMessage(
                        MESSAGE_READ, numBytes, -1,
                        mmBuffer)
                    readMsg.sendToTarget()*/
                }
            }

            // Call this from the main activity to send data to the remote device.
            fun write(bytes: ByteArray) {
                try {
                    mmOutStream.write(bytes)
                } catch (e: IOException) {
                    Log.e("Bluetooth", "Error occurred when sending data", e)

                    // Send a failure message back to the activity.
                    //val writeErrorMsg = handler.obtainMessage(MESSAGE_TOAST)
                    val bundle = Bundle().apply {
                        putString("toast", "Couldn't send data to the other device")
                    }
                    //writeErrorMsg.data = bundle
                    //handler.sendMessage(writeErrorMsg)
                    return
                }

                // Share the sent message with the UI activity.
                //val writtenMsg = handler.obtainMessage(
                //    MESSAGE_WRITE, -1, -1, mmBuffer)
                //writtenMsg.sendToTarget()
            }

            // Call this method from the main activity to shut down the connection.
            fun cancel() {
                try {
                    mmSocket.close()
                } catch (e: IOException) {
                    Log.e("Bluetooth", "Could not close the connect socket", e)
                }
            }
        }
    }

    // Create a BroadcastReceiver for ACTION_FOUND.
    /*private val receiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val action: String = intent.action
            when(action) {
                BluetoothDevice.ACTION_FOUND -> {
                    // Discovery has found a device. Get the BluetoothDevice
                    // object and its info from the Intent.
                    val device: BluetoothDevice =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    val deviceName = device.name
                    val deviceHardwareAddress = device.address // MAC address
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ...

        // Don't forget to unregister the ACTION_FOUND receiver.
        unregisterReceiver(receiver)
    }*/

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

        var channelMask: Int = AudioFormat.CHANNEL_IN_MONO
        if (inputDevice.channelCounts.size >= 2) {
            channelMask = AudioFormat.CHANNEL_IN_STEREO
        }


        shortAudioData = ShortArray(intBufferSize);


        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        //audioRecord = AudioRecord(MediaRecorder.AudioSource.MIC
        //    , intRecordSampelRate, channelMask
        //    , AudioFormat.ENCODING_PCM_16BIT, intBufferSize);

        audioManager.setMode(AudioManager.MODE_IN_CALL);

        audioRecord = AudioRecord.Builder()
            .setAudioSource(MediaRecorder.AudioSource.MIC)
            .setAudioFormat(AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(intRecordSampelRate)
                .setChannelMask(channelMask)
                .build())
            .setBufferSizeInBytes(intBufferSize)
            .build()


        audioRecord.preferredDevice = inputDevice;

        audioTrack = AudioTrack(AudioManager.STREAM_VOICE_CALL
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
