package com.sena.bluetooth.ble

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.sena.bluetooth.databinding.ActivityBleClientBinding
import com.sena.bluetooth.toast

class BleClientActivity : AppCompatActivity() {

    private val binding by lazy { ActivityBleClientBinding.inflate(layoutInflater) }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            if (result == null) return
            val notHas = mListAdapter.items.none { it.device.address == result.device.address}
            if (notHas) mListAdapter.add(result)
        }
    }

    private var mBleAdapter: BluetoothAdapter? = null
    private val mListAdapter = BleClientAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val status = intent.getBooleanExtra("status", false)
        if (status) {
            val bleManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            mBleAdapter = bleManager.adapter
        }

        initView()
    }

    private fun initView() {
        mListAdapter.setOnItemClickListener { a, v, p ->

        }
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@BleClientActivity)
            adapter = mListAdapter
        }
        binding.scan.setOnClickListener {
            mBleAdapter?: toast("无法使用, 检查权限和蓝牙...")
            mBleAdapter?.let {
                val scanner = it.bluetoothLeScanner
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                    mListAdapter.submitList(emptyList())
                    scanner.stopScan(scanCallback)
                    scanner.startScan(scanCallback)
                }
            }
        }
    }
}