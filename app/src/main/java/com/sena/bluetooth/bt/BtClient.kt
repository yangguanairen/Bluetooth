package com.sena.bluetooth.bt

import android.bluetooth.BluetoothDevice
import android.content.Context
import com.sena.bluetooth.checkConnectPermission


/**
 * FileName: BtClient
 * Author: JiaoCan
 * Date: 2024/5/8 13:33
 */

class BtClient(context: Context) : BtBase(context) {

    fun connect(device: BluetoothDevice) {
        Thread {
            closeConnect()
            mContext.checkConnectPermission {
                kotlin.runCatching {
                    mListener?.onBtStateChanged(device.address, BtState.CONNECTING)
                    val socket = device.createInsecureRfcommSocketToServiceRecord(SPP_UUID)
                    loopRead(socket)
                }.onFailure { error(it) }
            }
        }.start()
    }

    fun closeConnect() {
        sendClose()
        close()
    }

}

