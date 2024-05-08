package com.sena.bluetooth.ble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.recyclerview.widget.LinearLayoutManager
import com.sena.bluetooth.checkConnectPermission
import com.sena.bluetooth.databinding.ActivityBleClientBinding
import com.sena.bluetooth.getDeviceName
import com.sena.bluetooth.toast

class BleClientActivity : AppCompatActivity() {

    private val binding by lazy { ActivityBleClientBinding.inflate(layoutInflater) }
    private var mBleAdapter: BluetoothAdapter? = null
    private val mListAdapter = BleClientAdapter()

    private var mGatt: BluetoothGatt? = null
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            if (result == null) return
            if (getDeviceName(result.device).isNullOrEmpty()) return
            val notHas = mListAdapter.items.none { it.device.address == result.device.address}
            if (notHas) mListAdapter.add(result)
        }
    }
    private val gattCallback = BleClientGattCallback(this) { logPrint(it) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val status = intent.getBooleanExtra("status", false)
        if (status) {
            val bleManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            mBleAdapter = bleManager.adapter
        }
        initView()
        mBleAdapter?.let { checkConnectPermission { it.bluetoothLeScanner.startScan(scanCallback) } }
    }

    override fun onDestroy() {
        super.onDestroy()
        closeConnect()
        checkConnectPermission {
            mBleAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
        }
    }

    private fun initView() {
        mListAdapter.setOnItemClickListener { a, _, p ->
            a.getItem(p)?.device?.let { connect(it) }
        }
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@BleClientActivity)
            adapter = mListAdapter
        }
        binding.scan.setOnClickListener { startScan() }
        binding.setNotify.setOnClickListener { mGatt?.let { setNotify(it) } }
        binding.read.setOnClickListener { mGatt?.let { read(it) } }
        binding.write.setOnClickListener { mGatt?.let { write(it) } }
    }

    private fun startScan() {
        mBleAdapter?: toast("无法使用, 检查权限和蓝牙...")
        mBleAdapter?.let {
            checkConnectPermission {
                mGatt?.disconnect()
                mGatt?.close()
                val scanner = it.bluetoothLeScanner
                mListAdapter.submitList(emptyList())
                scanner.stopScan(scanCallback)
                scanner.startScan(scanCallback)
            }
        }
    }

    private fun connect(device: BluetoothDevice) {
        checkConnectPermission {
            closeConnect()
            val address = device.address
            logPrint("与[${address}]开始连接......")
            mGatt = device.connectGatt(this, false, gattCallback)
        }
    }

    private fun setNotify(gatt: BluetoothGatt) {
        val service = gatt.getService(UUID_SERVICE) ?: return
        val characteristic = service.getCharacteristic(UUID_CHA_READ_NOTIFY)
        checkConnectPermission {
            /// 设置Characteristic通知
            val setNotifyResult = gatt.setCharacteristicNotification(characteristic, true)
            val descriptor = characteristic.getDescriptor(UUID_DESC_NOTIFY)
            // 服务端不主动发数据, 只通知客户端去读取数据
            // descriptor.value = BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
            // 向Characteristic的Descriptor属性写入通知开关, 使蓝牙设备主动向手机发送数据
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            val writeDescriptorResult = gatt.writeDescriptor(descriptor)
            val log = "与[${gatt.device.address}]设置特征通知: $setNotifyResult, 写入描述: $writeDescriptorResult"
            logPrint(log)
        }
    }

    private fun read(gatt: BluetoothGatt) {
        val service = gatt.getService(UUID_SERVICE) ?: return
        val characteristic = service.getCharacteristic(UUID_CHA_READ_NOTIFY)
        checkConnectPermission { gatt.readCharacteristic(characteristic) }
    }

    private fun write(gatt: BluetoothGatt) {
        val service = gatt.getService(UUID_SERVICE) ?: return
        val characteristic = service.getCharacteristic(UUID_CAHR_WRITE)
        characteristic.setValue(binding.input.text.toString())
        checkConnectPermission { gatt.writeCharacteristic(characteristic) }
    }

    private fun closeConnect() {
        mGatt?.let {
            checkConnectPermission {
                it.disconnect()
                it.close()
            }
        }
    }

    private fun logPrint(text: String) {
        println(text)
        val tvText = binding.log.text.toString() + "\n" + text + "\n"
        Handler(Looper.getMainLooper()).post {
            binding.log.text = tvText
            val scrollY = binding.log.measuredHeight - binding.logLayout.height
            binding.logLayout.smoothScrollTo(0, scrollY)
        }
    }
}