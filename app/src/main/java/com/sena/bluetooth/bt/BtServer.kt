package com.sena.bluetooth.bt

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.content.Context
import com.sena.bluetooth.checkConnectPermission


/**
 * FileName: BtServer
 * Author: JiaoCan
 * Date: 2024/5/8 13:45
 */

class BtServer(context: Context) : BtBase(context) {
    private var mSSocket: BluetoothServerSocket? = null

    fun listen(bleAdapter: BluetoothAdapter) {
        mContext.checkConnectPermission {
            val sSocket = bleAdapter.listenUsingInsecureRfcommWithServiceRecord("TestBt", SPP_UUID)
            mSSocket = sSocket
            Thread {
                kotlin.runCatching {
                    mListener?.onBtStateChanged("", BtState.CONNECTING)
                    val socket = mSSocket?.accept()
                    mSSocket?.close()
                    socket?.let { loopRead(it) }
                }.onFailure { error(it) }
            }.start()
        }
    }

    fun closeListen() {
        sendClose()
        Thread.sleep(1000)
        close()
        kotlin.runCatching {
            mSSocket?.close()
        }.onFailure { it.printStackTrace() }
    }
}

