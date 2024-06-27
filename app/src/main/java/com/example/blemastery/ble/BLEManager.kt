package com.example.blemastery.ble

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.blemastery.Constants
import java.util.UUID
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import com.example.blemastery.Constants.Companion.readbool
import com.example.blemastery.MainActivity2


class BLEManager(private val mContext: Context) {

    private var characteristicToWrite: BluetoothGattCharacteristic? = null
    var RX_SERVICE_UUID = UUID.fromString("00000077-0000-1000-8000-00805f9b34fb")
    var RX_CHAR_UUID = UUID.fromString("0000ff01-0000-1000-8000-00805f9b34fb")
    private lateinit var mBuffer: ByteArray
    private var mBufferPointer = 0
    private var bluetoothGatt: BluetoothGatt? = null
    private var activity: Activity? = mContext as Activity?

    private var bleManager : BluetoothManager = mContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private var bluetoothAdapter: BluetoothAdapter = bleManager.adapter
    private var bluetoothLeScanner: BluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner

    private val REQUEST_CODE_PERMISSIONS = 1
    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT
    )

    val bluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatt?.discoverServices()
                gatt?.requestMtu(100)
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                // Handle disconnection
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
            val service = gatt?.getService(RX_SERVICE_UUID) // Replace with your service UUID
            characteristicToWrite = service?.getCharacteristic(RX_CHAR_UUID) // Replace with your characteristic UUID

            broadcastUpdate(Constants.BLUETOOTH_EVENT_SERVICES_DISCOVERED)
            //gatt?.readCharacteristic(characteristicToWrite)
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
                    val dataString = it.contentToString() // Convert byte array to string
                    Log.d("praveen data str", "Received data str: $dataString")
                    Log.d("praveen", "Received data length: ${data.size}")

                    if (readbool) {

                        activity?.runOnUiThread(kotlinx.coroutines.Runnable {
                            (mContext as? MainActivity2)?.mTextView?.text = data.contentToString()
                        })

                        Handler(Looper.getMainLooper()).postDelayed({read()},250)
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
                if (mBuffer != null) {
                    if (mBufferPointer < mBuffer!!.size) {
                        mBufferPointer += characteristic!!.value.size
                        val percent = (mBufferPointer.toDouble() / mBuffer!!.size.toDouble()) * 100
                        broadcastUpdate(Constants.BLUETOOTH_WRITE_PROGRESS, percent)
                        Handler(Looper.getMainLooper()).postDelayed({
                            if (gatt != null) {
                                writeDataToCharacteristic(characteristic, gatt)
                            }
                        }, 30)

                    }
                }
            } else {
                Log.e("praveen", "Characteristic write failed: $status")
            }
        }
    }

    val scanCallback = object : ScanCallback(){
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            result?.device?.let {
                val deviceName = it.name ?: "Unknown"
                Log.d("devname",deviceName)

                if (deviceName.equals("PLAYCOMPUTER")) {
                    connectToDevice(it)

                    (mContext as? MainActivity2)?.mTextView?.setText("CONNECTED TO "+deviceName)

                    bluetoothLeScanner.stopScan(this)
                }
            }
        }
    }

    fun startScanning() {
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        bluetoothLeScanner.startScan(null, scanSettings, scanCallback)
    }

    fun connectToDevice(device: BluetoothDevice) {
        if (ActivityCompat.checkSelfPermission(
                mContext,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            (mContext as? MainActivity2)?.let { ActivityCompat.requestPermissions(it, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS) }
        }else
            bluetoothGatt = device.connectGatt(activity, false, bluetoothGattCallback)
    }

    fun disconnectDevice() {
        bluetoothGatt?.let {
            it.disconnect()
            it.close()
        }
    }

    private fun writeDataToCharacteristic(iChar:BluetoothGattCharacteristic ,iGatt: BluetoothGatt ) {

        if(mBuffer != null) {
            //this.write_Kp = true
            if (mBuffer.size > mBufferPointer) {
                var arr : ByteArray
                if (mBuffer.size - mBufferPointer > 20){
                    arr = mBuffer.copyOfRange(mBufferPointer, mBufferPointer + 20)
                }else{
                    arr = mBuffer.copyOfRange(mBufferPointer, mBuffer.size)
                }
                //            iChar.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                iChar.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                iChar.setValue(arr)
                Log.d("writeByte",""+arr.contentToString())
                iGatt.writeCharacteristic(iChar, arr, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
            }else{
                broadcastUpdate(Constants.BLUETOOTH_EVENT_SENDING_COMPLETE)
                bluetoothGatt!!.readCharacteristic(iChar)

                //            AppConstants.ble_ok_read = true;
                mBuffer = ByteArray(0)
                mBufferPointer = 0
            }
        }
    }



    fun writeRXCharacteristic_Kp(value : ByteArray) {
        var RxService : BluetoothGattService = bluetoothGatt!!.getService(RX_SERVICE_UUID)
        val RxChar : BluetoothGattCharacteristic = RxService.getCharacteristic(RX_CHAR_UUID)
        mBuffer = value
        writeDataToCharacteristic(RxChar, bluetoothGatt!!)
    }

    fun read(){
        bluetoothGatt?.readCharacteristic(characteristicToWrite)
    }

    private fun broadcastUpdate(action: String){
        val intent = Intent(action)
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent)
    }

    private fun broadcastUpdate(action: String,iValue: Double){
        val intent = Intent(action)
        intent.putExtra(Constants.BLUETOOTH_EXTRA_DATA,iValue)
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent)
    }

//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<out String>,
//        grantResults: IntArray
//    ) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        if (requestCode == REQUEST_CODE_PERMISSIONS) {
//            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
//                // Permissions are granted, proceed with your functionality
//                startScanning()
//            } else {
//                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show()
//                finish()
//            }
//        }
//    }

}