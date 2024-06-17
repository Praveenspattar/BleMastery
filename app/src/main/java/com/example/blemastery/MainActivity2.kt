package com.example.blemastery

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.util.Arrays
import java.util.UUID

class MainActivity2 : AppCompatActivity() {

    private val REQUEST_CODE_PERMISSIONS = 1
    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT
    )

    private lateinit var bleManager : BluetoothManager
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothLeScanner: BluetoothLeScanner

    private var bluetoothGatt: BluetoothGatt? = null
    private var characteristicToWrite: BluetoothGattCharacteristic? = null
    lateinit var sendBtn : Button
    lateinit var stopBtn : Button
    lateinit var valTv : TextView
    var readbool = false

    val scanCallback = object : ScanCallback(){
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            result?.device?.let {
                val deviceName = it.name ?: "Unknown"
                Log.d("devname",deviceName)

                if (deviceName.equals("PLAYCOMPUTER")) {
                    connectToDevice(it)

                    valTv.setText(deviceName)

                    bluetoothLeScanner.stopScan(this)
                }
            }
        }
    }

    private val bluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatt?.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                // Handle disconnection
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
            val service = gatt?.getService(UUID.fromString("00000077-0000-1000-8000-00805f9b34fb")) // Replace with your service UUID
            characteristicToWrite = service?.getCharacteristic(UUID.fromString("0000ff01-0000-1000-8000-00805f9b34fb")) // Replace with your characteristic UUID
            gatt?.readCharacteristic(characteristicToWrite)
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            super.onCharacteristicRead(gatt, characteristic, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val data = characteristic?.value
                data?.let {
                    val dataString = String(data, Charsets.UTF_8) // Convert byte array to string
                    Log.d("praveen", "Received data: $dataString")

                    if (readbool) {
                        bluetoothGatt?.readCharacteristic(characteristicToWrite)

                        valTv.setText(Arrays.toString(data))
                    }

                }
            } else {
                Log.e("praveen", "Failed to read characteristic: $status")
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("praveen", "Characteristic write successful")
            } else {
                Log.e("praveen", "Characteristic write failed: $status")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main2)

        bleManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bleManager.adapter
        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
        sendBtn = findViewById(R.id.btnSend)
        stopBtn = findViewById(R.id.btnStop)
        valTv = findViewById(R.id.textviewval)

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this,REQUIRED_PERMISSIONS,REQUEST_CODE_PERMISSIONS)
        }else{
            startScanning()
        }

        sendBtn.setOnClickListener{

            //[80, 65, 79, 79, 79, 79, 79, 79, 79, 79, 79, 79, 79, 79, 79, 79, 84, 84, 84, 84, 79, 79, 79, 69, 82]

            readbool = true

            val bArrayList: ByteArray = byteArrayOf(80, 65, 79, 79, 79, 79, 79, 79, 79, 79, 79, 79, 79, 79, 79, 79, 84, 84, 84, 84, 79, 79, 79, 69, 82)

            writeDataToCharacteristic(bArrayList)

        }

        stopBtn.setOnClickListener{

            readbool = false
            val bList : ByteArray = byteArrayOf('M'.code.toByte(), '8'.code.toByte())

            writeDataToCharacteristic(bList)

        }

    }

    private fun startScanning() {
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        bluetoothLeScanner.startScan(null, scanSettings, scanCallback)
    }

    private fun connectToDevice(device: BluetoothDevice) {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }else
            bluetoothGatt = device.connectGatt(this, false, bluetoothGattCallback)
    }

    private fun writeDataToCharacteristic(data: ByteArray) {

        Log.d("Bytestosend",data.joinToString(", "))

        characteristicToWrite?.let {
            it.value = data
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            //            iChar.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
            it.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            bluetoothGatt?.writeCharacteristic(it)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                // Permissions are granted, proceed with your functionality
                startScanning()
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}