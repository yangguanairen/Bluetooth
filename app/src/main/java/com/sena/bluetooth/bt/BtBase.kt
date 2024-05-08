package com.sena.bluetooth.bt

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import com.sena.bluetooth.checkConnectPermission
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID


/**
 * FileName: BtBase
 * Author: JiaoCan
 * Date: 2024/5/8 11:20
 */

open class BtBase(context: Context) {
    protected val mContext = context

    protected val SPP_UUID =  UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private val FLAG_MSG = 0
    private val FLAG_FILE = 1
    private val FLAG_CLOSE = -1

    private var mSocket: BluetoothSocket? = null
    private var remoteAddress: String = ""
    private var isSending = false
    private var isReading = false
    private var mDos: DataOutputStream? = null


    fun loopRead(socket: BluetoothSocket) {
        mSocket = socket
        remoteAddress = socket.remoteDevice.address
        kotlin.runCatching {
            if (!socket.isConnected) mContext.checkConnectPermission { socket.connect() }
            mListener?.onBtStateChanged(remoteAddress, BtState.CONNECTED)
            mDos = DataOutputStream(socket.outputStream)
            val dis = DataInputStream(socket.inputStream)
            isReading = true
            while (isReading) {
                when (dis.readInt()) {
                    FLAG_MSG -> readMsg(dis)
                    FLAG_FILE -> readFile(dis)
                    FLAG_CLOSE -> {
                        break
                    }
                }
            }
            close()
        }.onFailure { error(it) }
    }

    private fun readMsg(dis: DataInputStream) {
        val msg = dis.readUTF()
        mListener?.onMsgOperated(remoteAddress, msg)
    }

    private fun readFile(dis: DataInputStream) {
        val fileName = dis.readUTF()
        val fileLen = dis.readLong()
        val targetFile = File(mContext.externalCacheDir, fileName)
        val fos = FileOutputStream(targetFile)
        var len: Int
        val buffer = ByteArray(4 * 1024)
        mListener?.onFileOperated(remoteAddress, fileName, 0)
        while (dis.read(buffer).also { len = it } > 0) {
            fos.write(buffer, 0, len)
            if (len >= fileLen) break
        }
        fos.close()
        mListener?.onFileOperated(remoteAddress, fileName, 100)
    }


    fun sendMsg(msg: String) {
        if (checkSending()) return
        isSending = true
        kotlin.runCatching {
            mDos?.apply {
                writeInt(FLAG_MSG)
                writeUTF(msg)
                flush()
                mListener?.onMsgOperated(remoteAddress, msg)
            }
        }.onFailure { error(it) }
        isSending = false
    }

    fun sendFile(path: String) {
        if (checkSending()) return
        isSending = true
        Thread {
            kotlin.runCatching {
                val file = File(path)
                val fileName = file.name
                val fis = FileInputStream(file)
                mDos?.apply {
                    writeInt(FLAG_FILE)
                    writeUTF(file.name)
                    writeLong(file.length())
                    var len: Int
                    val buffer = ByteArray(4 * 1024)
                    mListener?.onFileOperated(remoteAddress, fileName, 0)
                    while (fis.read(buffer).also { len = it  } > 0) {
                        write(buffer, 0, len)
                    }
                    flush()
                    mListener?.onFileOperated(remoteAddress, fileName, 100)
                }
            }.onFailure {
                it.printStackTrace()
                error(it)
            }
            isSending = false
        }.start()
    }

    protected fun sendClose() {
        kotlin.runCatching { mDos?.writeInt(FLAG_CLOSE) }
    }

    protected fun error(throwable: Throwable) {
        throwable.printStackTrace()
        kotlin.runCatching { mSocket?.close() }
        if (mSocket != null) {
            mListener?.onBtStateChanged(remoteAddress, BtState.ERROR)
        }
        isReading = false
        isSending = false
        mSocket = null
        remoteAddress = ""
    }

    protected fun close() {
        kotlin.runCatching { mSocket?.close() }
        if (mSocket != null) {
            mListener?.onBtStateChanged(remoteAddress, BtState.DISCONNECT)
        }
        isReading = false
        isSending = false
        mSocket = null
        remoteAddress = ""
    }

    private fun checkSending(): Boolean {
        if (isSending) {
            mListener?.onBtStateChanged(remoteAddress, BtState.BUSY)
        }
        return isSending
    }

    fun isConnected(device: BluetoothDevice): Boolean {
        return mSocket?.isConnected == true && mSocket?.remoteDevice == device
    }

    protected var mListener: BtListener? = null

    fun setBtListener(btListener: BtListener?) {
        mListener = btListener
    }

    interface BtListener {
        fun onBtStateChanged(address: String, state: BtState)
        fun onMsgOperated(address: String, msg: String)
        fun onFileOperated(address: String, fileName: String, progress: Int)
    }

    enum class BtState {
        CONNECTING,
        CONNECTED,
        DISCONNECT,
        BUSY,
        ERROR
    }

}

