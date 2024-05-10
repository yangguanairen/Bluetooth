package com.sena.bluetooth.ble

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.content.Context
import com.sena.bluetooth.utils.checkConnectPermission


/**
 * FileName: BleGattCallback
 * Author: JiaoCan
 * Date: 2024/5/7 16:15
 */

class BleClientGattCallback(context: Context, func: (log: String) -> Unit) : BluetoothGattCallback() {

    private val mContext = context
    private val mFunc = func

    override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
        super.onServicesDiscovered(gatt, status)
        if (gatt == null) return
        gatt.services.forEach { s ->
            val log = StringBuilder().appendLine("[${gatt.device.address}]发现服务:")
                .appendLine("S=${s.uuid}")
            s.characteristics.forEach { c ->
                log.appendLine("C=${c.uuid}")
                c.descriptors.forEach { d ->
                    log.appendLine("D=${d.uuid}")
                }
            }
            mFunc.invoke(log.toString())
        }
    }

    override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
        super.onConnectionStateChange(gatt, status, newState)
        if (gatt == null) return
        val address = gatt.device.address
        val isConnected = BluetoothGatt.GATT_SUCCESS == status && BluetoothProfile.STATE_CONNECTED == newState
        if (isConnected) {
            mContext.checkConnectPermission { gatt.discoverServices() }
        } else {
            mContext.checkConnectPermission {
                gatt.disconnect()
                gatt.close()
            }
        }
        val log = "与[${address}]" + when {
            isConnected -> "连接成功"
            newState == BluetoothProfile.STATE_DISCONNECTED -> {
                if (status == 0) "主动断开连接" else "自动断开连接"
            }
            else -> "连接出错, 错误码:$status"
        }
        mFunc.invoke(log)
    }

    override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
        super.onCharacteristicChanged(gatt, characteristic, value)
        val charValue = characteristic.value
        val log = "[${gatt.device.address}]读取数据(十进制)\n${charValue.joinToString(", ")}\nUTF-8:${String(charValue)}"
        mFunc.invoke(log)
    }

    override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray, status: Int) {
        super.onCharacteristicRead(gatt, characteristic, value, status)
        val charValue = characteristic.value
        val log = "[${gatt.device.address}]读取数据(十进制)\n${charValue.joinToString(", ")}\nUTF-8:${String(charValue)}"
        mFunc.invoke(log)
    }

    override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
        super.onCharacteristicWrite(gatt, characteristic, status)
        if (gatt == null || characteristic == null) return
        val charValue = characteristic.value
        val log = "[${gatt.device.address}]写入数据(十进制)\n${charValue.joinToString(", ")}\nUTF-8:${String(charValue)}"
        mFunc.invoke(log)
    }


}

