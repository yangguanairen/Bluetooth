package com.sena.bluetooth.bt

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import com.sena.bluetooth.checkConnectPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch


/**
 * FileName: BtClient
 * Author: JiaoCan
 * Date: 2024/5/8 13:33
 */

class BtClient(context: Context) : BtBase(context) {

    private var lastJob: Job? = null
    private var mJob: Job? = null

    fun connect(device: BluetoothDevice) {
        val newJob = CoroutineScope(Dispatchers.Main).launch {
            // 关闭上一次连接
            kotlin.runCatching {
                if (isConnected()) sendClose()
            }.onFailure {
                it.printStackTrace()
                mListener?.onOperateErrorLog("发送(关闭)失败")
            }
            close()
            lastJob?.cancel()
            var socket: BluetoothSocket? = null
            mContext.checkConnectPermission {
                kotlin.runCatching {
                    mListener?.onBtStateChanged(device.address, BtState.CONNECTING)
                    socket = device.createInsecureRfcommSocketToServiceRecord(SPP_UUID)
                }.onFailure { error(it) }
            }
            kotlin.runCatching {
                socket?.let { loopRead(it) }
            }.onFailure { error(it) }
        }
        lastJob = mJob
        mJob = newJob
    }

    fun closeConnect() {
        CoroutineScope(Dispatchers.Main).launch {
            kotlin.runCatching {
                if (isConnected()) sendClose()
            }.onFailure {
                it.printStackTrace()
                mListener?.onOperateErrorLog("发送(关闭)失败")
            }
            close()
            mJob?.cancel()
        }
    }

    fun sendClientMsg(msg: String) {
        CoroutineScope(Dispatchers.Main).launch {
            kotlin.runCatching {
                sendMsg(msg)
            }.onFailure {
                it.printStackTrace()
                mListener?.onOperateErrorLog("发送($msg)失败")
            }
        }
    }

    fun sendClientFile(filePath: String) {
        CoroutineScope(Dispatchers.Main).launch {
            kotlin.runCatching {
                sendFile(filePath)
            }.onFailure {
                it.printStackTrace()
                mListener?.onOperateErrorLog("发送($filePath)失败")
            }
        }
    }

}

