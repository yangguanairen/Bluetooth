package com.sena.bluetooth.bt

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import com.sena.bluetooth.utils.checkConnectPermission
import com.sena.bluetooth.utils.getOrNull
import com.sena.bluetooth.utils.FileUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
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
    protected var remoteAddress: String = ""
    private var isSending = false
    private var isReading = false
    private var mDos: DataOutputStream? = null


    protected suspend fun loopRead(socket: BluetoothSocket) {
        mSocket = socket
        remoteAddress = socket.remoteDevice.address
        withContext(Dispatchers.IO) {
            if (!socket.isConnected) mContext.checkConnectPermission { socket.connect() }
        }
        mListener?.onBtStateChanged(remoteAddress, BtState.CONNECTED)
        mDos = DataOutputStream(socket.outputStream)
        val dis = DataInputStream(socket.inputStream)
        isReading = true
        while (isReading) {
            val flag = withContext(Dispatchers.IO) {
                getOrNull { dis.readInt() }
            }
            when (flag) {
                FLAG_MSG -> readMsg(dis)
                FLAG_FILE -> readFile(dis)
                FLAG_CLOSE -> close()
            }
        }
    }

    private suspend fun readMsg(dis: DataInputStream) {
        val msg = withContext(Dispatchers.IO) { dis.readUTF() }
        mListener?.onMsgOperated(remoteAddress, msg, false)
    }

    private suspend fun readFile(dis: DataInputStream) {
        val fileName = withContext(Dispatchers.IO) { dis.readUTF() }
        val fileLen = withContext(Dispatchers.IO) { dis.readLong() }
        val targetFileUri = FileUtil.createFileInDownload(mContext, fileName, null) ?: return
        val fos = mContext.contentResolver.openOutputStream(targetFileUri) ?: return
        var len: Int
        var readLen = 0
        val buffer = ByteArray(4 * 1024)
        mListener?.onFileOperated(remoteAddress, fileName, 0, false, null)
        withContext(Dispatchers.IO) {
            while (dis.read(buffer).also { len = it } > 0) {
                fos.write(buffer, 0, len)
                readLen += len
                if (readLen >= fileLen) break
            }
            fos.flush()
            fos.close()
        }

        mListener?.onFileOperated(remoteAddress, fileName, 100, false, FileUtil.uriToPath(mContext, targetFileUri))
    }


    protected suspend fun sendMsg(msg: String) {
        if (checkSending()) return
        isSending = true
        withContext(Dispatchers.IO) {
            mDos?.apply {
                writeInt(FLAG_MSG)
                writeUTF(msg)
                flush()
            }
        }
        mListener?.onMsgOperated(remoteAddress, msg, true)
        isSending = false
    }

    protected suspend fun sendFile(path: String) {
        if (checkSending()) return
        isSending = true
        val file = File(path)
        val fileName = file.name
        mListener?.onFileOperated(remoteAddress, fileName, 0, true, null)
        withContext(Dispatchers.IO) {
            val fis = FileInputStream(file)
            mDos?.apply {
                writeInt(FLAG_FILE)
                writeUTF(file.name)
                writeLong(file.length())
                var len: Int
                val buffer = ByteArray(4 * 1024)
                while (fis.read(buffer).also { len = it  } > 0) {
                    write(buffer, 0, len)
                }
                flush()
            }
        }
        mListener?.onFileOperated(remoteAddress, fileName, 100, true, null)
        isSending = false
    }

    protected suspend fun sendClose() {
        if (checkSending()) return
        isSending = true
        withContext(Dispatchers.IO) {
            mDos?.writeInt(FLAG_CLOSE)
        }
        mDos?.let {
            mListener?.onMsgOperated(remoteAddress, "关闭", true)
        }
        isSending = false
    }

    protected fun error(throwable: Throwable) {
        throwable.printStackTrace()
        kotlin.runCatching { mSocket?.close() }
        if (mSocket != null) {
            mListener?.onBtStateChanged(remoteAddress, BtState.ERROR)
        }
        isReading = false
        isSending = false
        mDos?.close()
        mDos = null
        mSocket = null
        remoteAddress = ""
    }

    protected fun close() {
        isReading = false
        isSending = false
        mDos?.close()
        mDos = null
        kotlin.runCatching { mSocket?.close() }
        if (mSocket != null) {
            mListener?.onBtStateChanged(remoteAddress, BtState.DISCONNECT)
        }
        mSocket = null
        remoteAddress = ""
    }

    private fun checkSending(): Boolean {
        if (isSending) {
            mListener?.onBtStateChanged(remoteAddress, BtState.BUSY)
        }
        return isSending
    }

    fun isConnected(): Boolean {
        return mSocket?.isConnected == true
    }

    fun isConnectedWithDevice(device: BluetoothDevice): Boolean {
        return mSocket?.isConnected == true && mSocket?.remoteDevice == device
    }

    protected var mListener: BtListener? = null

    fun setBtListener(btListener: BtListener?) {
        mListener = btListener
    }

    interface BtListener {
        fun onBtStateChanged(address: String, state: BtState)
        fun onMsgOperated(address: String, msg: String, isSender: Boolean)
        fun onFileOperated(address: String, fileName: String, progress: Int, isSender: Boolean, savePath: String?)
        fun onOperateErrorLog(log: String)
    }

    enum class BtState {
        CONNECTING,
        CONNECTED,
        DISCONNECT,
        BUSY,
        ERROR,
    }

}

