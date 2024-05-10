package com.sena.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.sena.bluetooth.ble.BleClientActivity
import com.sena.bluetooth.ble.BleServerActivity
import com.sena.bluetooth.bt.BtClientActivity
import com.sena.bluetooth.bt.BtServerActivity
import com.sena.bluetooth.databinding.ActivityMainBinding
import com.sena.bluetooth.utils.toast


const val INTENT_STATUS = "status"

class MainActivity : AppCompatActivity() {


    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    private lateinit var requestPermissions: ActivityResultLauncher<Array<String>>
    private lateinit var requestActivityResult: ActivityResultLauncher<Intent>

    private var haveAllCondition = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)


        initLaunch()
        checkPermission()
        initView()
    }

    private fun initLaunch() {
        requestPermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val isSuccess = result.none { it.key != Manifest.permission.READ_EXTERNAL_STORAGE && !it.value }
            if (isSuccess) {
                checkBle()
            } else {
                toast("无蓝牙权限...")
            }
        }
        requestActivityResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val bleManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val bleAdapter = bleManager.adapter
            val isEnable = bleAdapter.isEnabled
            if (isEnable) {
                haveAllCondition = true
            } else {
                haveAllCondition = false
                toast("蓝牙未开启...")
            }
        }
    }

    private fun initView() {
       binding.toolbar.setTitle(R.string.app_name)
       binding.bleClient.setOnClickListener {
           val intent = Intent(this, BleClientActivity::class.java)
           intent.putExtra(INTENT_STATUS, haveAllCondition)
           startActivity(intent)
       }
        binding.bleServer.setOnClickListener {
            val intent = Intent(this, BleServerActivity::class.java)
            intent.putExtra(INTENT_STATUS, haveAllCondition)
            startActivity(intent)
        }
        binding.btClient.setOnClickListener {
            val intent = Intent(this, BtClientActivity::class.java)
            intent.putExtra(INTENT_STATUS, haveAllCondition)
            startActivity(intent)
        }
        binding.bteServer.setOnClickListener {
            val intent = Intent(this, BtServerActivity::class.java)
            intent.putExtra(INTENT_STATUS, haveAllCondition)
            startActivity(intent)
        }
    }

    private fun checkPermission() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        } else {
            listOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }
        val notGrantedList = arrayListOf<String>()
        permissions.forEach {
            val isGranted = ActivityCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
            if (!isGranted) notGrantedList.add(it)
        }
       requestPermissions.launch(notGrantedList.toTypedArray())
    }

    private fun checkBle() {
        val bleManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bleAdapter = bleManager.adapter
        val isEnable = bleAdapter.isEnabled
        if (!isEnable) {
            requestActivityResult.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        } else {
            haveAllCondition = true
        }
    }
}