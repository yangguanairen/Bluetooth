package com.sena.bluetooth.ble

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import com.sena.bluetooth.R
import com.sena.bluetooth.utils.checkAdvertisePermission
import com.sena.bluetooth.utils.checkConnectPermission
import com.sena.bluetooth.databinding.ActivityBleServerBinding
import java.util.UUID

val UUID_SERVICE = UUID.fromString("10000000-0000-0000-0000-000000000000")
val UUID_CHA_READ_NOTIFY = UUID.fromString("11000000-0000-0000-0000-000000000000")
val UUID_DESC_NOTIFY = UUID.fromString("11100000-0000-0000-0000-000000000000")
val UUID_CAHR_WRITE = UUID.fromString("12000000-0000-0000-0000-000000000000")

class BleServerActivity : AppCompatActivity() {

    private val binding by lazy { ActivityBleServerBinding.inflate(layoutInflater) }
    private var mGattServer: BluetoothGattServer? = null
    private var mBleAdvertiser: BluetoothLeAdvertiser? = null
    private val mGattCallback = BleServerGattCallback(this) { logPrint(it) }
    private val mAdvertiseCallback = object : AdvertiseCallback() {
        override fun onStartFailure(errorCode: Int) {
            logPrint("BLE广播开启失败, 错误码:$errorCode")
        }

        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            logPrint("BLE广播开启成功")
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.toolbar.setTitle(R.string.ble_server)
        initServer()
    }

    override fun onDestroy() {
        super.onDestroy()
        checkAdvertisePermission { mBleAdvertiser?.stopAdvertising(mAdvertiseCallback) }
        checkConnectPermission { mGattServer?.close() }
    }

    private fun initServer() {
        val bleManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bleAdapter = bleManager.adapter
        // 启动BLE蓝牙广播
        // 广播设置(必须)
        val settings = AdvertiseSettings.Builder()
            // 广播模式: 低功耗, 平衡, 低延迟
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            // 发射功率级别: 极低, 低, 中, 高
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            // 能否连接,广播分为可连接广播和不可连接广播
            .setConnectable(true)
            .build()
        // 广播数据(必须)
        val advertiseData = AdvertiseData.Builder()
            // 包含蓝牙名称
            .setIncludeDeviceName(true)
            // 包含发射功率级别
            .setIncludeTxPowerLevel(false)
            // 设备厂商数据, 自定义
            .addManufacturerData(1, byteArrayOf(22, 33))
            .build()
        // 扫描响应数据(可选, 当客户端扫描时才发送)
        val scanResp = AdvertiseData.Builder()
            // 设备厂商数据, 自定义
            .addManufacturerData(2, byteArrayOf(66, 66))
            // 服务UUID
            .addServiceUuid(ParcelUuid(UUID_SERVICE))
            // 服务数据, 自定义
//            .addServiceData(ParcelUuid(UUID_SERVICE), byteArrayOf(2))
            .build()
        mBleAdvertiser = bleAdapter.bluetoothLeAdvertiser
        checkAdvertisePermission {
            mBleAdvertiser?.startAdvertising(settings, advertiseData, scanResp, mAdvertiseCallback)
        }
        // 启动BLE蓝牙服务端
        // 注意：必须要开启可连接的BLE广播，其它设备才能发现并连接BLE服务端!
        val service = BluetoothGattService(UUID_SERVICE, BluetoothGattService.SERVICE_TYPE_PRIMARY)
        // 添加可读+通知characteristic
        val characteristicRead = BluetoothGattCharacteristic(UUID_CHA_READ_NOTIFY,
        BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_NOTIFY, BluetoothGattCharacteristic.PERMISSION_READ)
        characteristicRead.addDescriptor(BluetoothGattDescriptor(UUID_DESC_NOTIFY, BluetoothGattCharacteristic.PERMISSION_WRITE))
        service.addCharacteristic(characteristicRead)
        // 添加可写characteristic
        val characteristicWrite = BluetoothGattCharacteristic(UUID_CAHR_WRITE,
            BluetoothGattCharacteristic.PROPERTY_WRITE, BluetoothGattCharacteristic.PERMISSION_WRITE)
        service.addCharacteristic(characteristicWrite)
        mGattServer = bleManager.openGattServer(this, mGattCallback)
        mGattServer?.apply {
            mGattCallback.setGattServer(this)
            addService(service)
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