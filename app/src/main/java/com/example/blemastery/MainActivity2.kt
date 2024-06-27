package com.example.blemastery

import android.Manifest
import android.bluetooth.BluetoothGattCharacteristic
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.blemastery.ble.BLEManager
import com.example.blemastery.Constants.Companion.readbool

class MainActivity2 : AppCompatActivity() {

    private val REQUEST_CODE_PERMISSIONS = 1
    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT
    )

    private var characteristicToWrite: BluetoothGattCharacteristic? = null
    lateinit var sendBtn : Button
    lateinit var stopBtn : Button
    lateinit var mTextView : TextView
    lateinit var bleManager: BLEManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //enableEdgeToEdge()
        setContentView(R.layout.activity_main2)
        sendBtn = findViewById(R.id.btnSend)
        stopBtn = findViewById(R.id.btnStop)
        mTextView = findViewById(R.id.textviewval)

        bleManager = BLEManager(this)

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this,REQUIRED_PERMISSIONS,REQUEST_CODE_PERMISSIONS)
        }else{
            bleManager.startScanning()
        }

        sendBtn.setOnClickListener{

            readbool = true

            val bArrayList: ByteArray = byteArrayOf(80, 65, 79, 79, 79, 79, 79, 79, 79, 79, 79, 79, 79, 79, 79, 79, 84, 84, 84, 84, 79, 79, 79, 69, 82)

            bleManager.writeRXCharacteristic_Kp(bArrayList)

            val readHandler = Handler(Looper.getMainLooper())
            readHandler.postDelayed(kotlinx.coroutines.Runnable {
                bleManager.read()
            },1000)

        }

        stopBtn.setOnClickListener{

            readbool = false
            val bList : ByteArray = byteArrayOf('M'.code.toByte(), '8'.code.toByte())
            bleManager.writeRXCharacteristic_Kp(bList)

            mTextView.text = getString(R.string.stopped_reading)

        }

    }

    override fun onDestroy() {

        readbool = false
        val bList : ByteArray = byteArrayOf('M'.code.toByte(), '8'.code.toByte())
        bleManager.writeRXCharacteristic_Kp(bList)

        super.onDestroy()
    }

    override fun onPause() {
        super.onPause()
        bleManager.disconnectDevice()
    }

    override fun onRestart() {
        super.onRestart()
        bleManager.startScanning()
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
                bleManager.startScanning()
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}