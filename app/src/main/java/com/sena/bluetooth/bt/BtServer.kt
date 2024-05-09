package com.sena.bluetooth.bt

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import com.sena.bluetooth.checkConnectPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


/**
 * FileName: BtServer
 * Author: JiaoCan
 * Date: 2024/5/8 13:45
 */

class BtServer(context: Context) : BtBase(context) {

    private var mJob: Job? = null
    private var mSSocket: BluetoothServerSocket? = null

    fun listen(bleAdapter: BluetoothAdapter) {
        mJob = CoroutineScope(Dispatchers.Main).launch {
            mContext.checkConnectPermission {
                kotlin.runCatching {
                    val sSocket = bleAdapter.listenUsingInsecureRfcommWithServiceRecord("TestBt", SPP_UUID)
                    mSSocket = sSocket
                }.onFailure { error(it) }
            }
            mListener?.onBtStateChanged("", BtState.CONNECTING)
            kotlin.runCatching {
                var socket: BluetoothSocket?
                withContext(Dispatchers.IO) {
                    socket = mSSocket?.accept()
                }
                // 关闭监听, 单次连接单个设备
                mSSocket?.close()
                socket?.let { loopRead(it) }
            }.onFailure { error(it) }
        }
    }

    fun closeListen() {
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

    fun sendServerMsg(msg: String) {
        CoroutineScope(Dispatchers.Main).launch {
            kotlin.runCatching {
                sendMsg(msg)
            }.onFailure {
                it.printStackTrace()
                mListener?.onOperateErrorLog("发送($msg)失败")
            }
        }
    }

    fun sendServerFile(filePath: String) {
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

