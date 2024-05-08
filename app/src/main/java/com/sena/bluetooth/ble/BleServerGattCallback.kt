package com.sena.bluetooth.ble

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothProfile
import android.content.Context
import com.sena.bluetooth.checkConnectPermission
import kotlin.random.Random


/**
 * FileName: BleServerCallback
 * Author: JiaoCan
 * Date: 2024/5/7 17:16
 */


class BleServerGattCallback(context: Context, func: (log: String) -> Unit) : BluetoothGattServerCallback() {

    private val mContext = context
    private lateinit var mGattServer: BluetoothGattServer
    private val mFunc = func

    override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
        super.onConnectionStateChange(device, status, newState)
        if (device == null) return
        val log = "与[${device.address}]" + when {
            BluetoothGatt.GATT_SUCCESS == status && BluetoothProfile.STATE_CONNECTED == newState -> {
                "连接成功"
            }
            newState == BluetoothProfile.STATE_DISCONNECTED -> {
                if (status == 0) "主动断开连接" else "自动断开连接"
            }
            else -> "连接出错, 错误码:$status"
        }
        mFunc.invoke(log)
    }

    override fun onServiceAdded(status: Int, service: BluetoothGattService?) {
        super.onServiceAdded(status, service)
        if (service == null) return
        val log = "添加服务[${service.uuid}]" +
                if (status == BluetoothGatt.GATT_SUCCESS) "成功" else "失败, 错误码:$status"
        mFunc.invoke(log)
    }

    override fun onCharacteristicReadRequest(device: BluetoothDevice?, requestId: Int, offset: Int, characteristic: BluetoothGattCharacteristic?) {
        super.onCharacteristicReadRequest(device, requestId, offset, characteristic)
        if (device == null) return
        val log1 = "[${device.address}]发起特征读取请求: requestId: $requestId, offset: $offset, charUuid: ${characteristic?.uuid}"
        mFunc.invoke(log1)
        val response = "CHAR_" + Random.nextInt()
        mContext.checkConnectPermission {
            mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, response.toByteArray())
        }
        val log2 = "客户端读取Characteristic[${characteristic?.uuid}]:\n$response"
        mFunc.invoke(log2)
    }

    override fun onCharacteristicWriteRequest(device: BluetoothDevice?, requestId: Int, characteristic: BluetoothGattCharacteristic?, preparedWrite: Boolean, responseNeeded: Boolean, offset: Int, value: ByteArray?) {
        super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value)
        if (device == null) return
        val log1 = "[${device.address}]发起特征写入请求: requestId: $requestId, charUuid: ${characteristic?.uuid}, " +
                "preparedWrite: $preparedWrite, responseNeeded: $responseNeeded, offset: $offset, value: ${value?.joinToString(", ")}"
        mFunc.invoke(log1)
        mContext.checkConnectPermission {
            mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
        }
        val log2 = "客户端写入Characteristic[${characteristic?.uuid}]: ${value?.let { String(it) }}"
        mFunc.invoke(log2)
    }

    override fun onDescriptorReadRequest(device: BluetoothDevice?, requestId: Int, offset: Int, descriptor: BluetoothGattDescriptor?) {
        super.onDescriptorReadRequest(device, requestId, offset, descriptor)
        if (device == null) return
        val log1 = "[${device.address}]发起描述读取请求: requestId: $requestId, offset: $offset, descUuid: ${descriptor?.uuid}"
        mFunc.invoke(log1)
        val response = "DESC_" + Random.nextInt()
        mContext.checkConnectPermission {
            mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, response.toByteArray())
        }
        val log2 = "客户端读取Descriptor[${descriptor?.uuid}]:\n$response"
        mFunc.invoke(log2)
    }

    override fun onDescriptorWriteRequest(device: BluetoothDevice?, requestId: Int, descriptor: BluetoothGattDescriptor?, preparedWrite: Boolean, responseNeeded: Boolean, offset: Int, value: ByteArray?) {
        super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value)
        if (device == null || descriptor == null) return
        val valueStr = value?.joinToString(", ")
        val log1 = "[${device.address}]发起描述写入请求: requestId: $requestId, descUuid: ${descriptor.uuid}, " +
                "preparedWrite: $preparedWrite, responseNeeded: $responseNeeded, offset: $offset, value: ${value?.joinToString(", ")}, "
        mFunc.invoke(log1)
        mContext.checkConnectPermission {
            mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
        }
        val log2 = "客户端写入Descriptor[${descriptor.uuid}]:\n $valueStr"
        mFunc.invoke(log2)

        // 简单模拟通知客户端Characteristic变化
        if (BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE.joinToString(", ") == valueStr) {
            val characteristic = descriptor.characteristic
            Thread {
                for (i in 0 until 5) {
                    Thread.sleep(3000)
                    val response = "CHAR_" + Random.nextInt()
                    characteristic.setValue(response)
                    val log3 = "通知客户端改变Characteristic[${characteristic.uuid}]\n$response"
                    mFunc.invoke(log3)
                    mGattServer.notifyCharacteristicChanged(device, characteristic, false)
                }
            }.start()
        }
    }

    override fun onExecuteWrite(device: BluetoothDevice?, requestId: Int, execute: Boolean) {
        super.onExecuteWrite(device, requestId, execute)
        if (device == null) return
        val log = "[${device.address}] onExecuteWrite: requestId: $requestId, execute: $execute"
        mFunc.invoke(log)
    }

    override fun onNotificationSent(device: BluetoothDevice?, status: Int) {
        super.onNotificationSent(device, status)
        if (device == null) return
        val log = "[${device.address}] onNotificationSent: status: $status"
        mFunc.invoke(log)
    }

    override fun onMtuChanged(device: BluetoothDevice?, mtu: Int) {
        super.onMtuChanged(device, mtu)
        if (device == null) return
        val log = "[${device.address}] onMtuChanged: mtu: $mtu"
        mFunc.invoke(log)
    }

    fun setGattServer(gattServer: BluetoothGattServer) {
        mGattServer = gattServer
    }

}

