package com.sena.bluetooth.bt

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.sena.bluetooth.databinding.ActivityBtServerBinding

class BtServerActivity : AppCompatActivity(), BtBase.BtListener {

    private val binding by lazy { ActivityBtServerBinding.inflate(layoutInflater) }
    private val mServer = BtServer(this).apply {
        setBtListener(this@BtServerActivity)
    }

    private var mBleAdapter: BluetoothAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        val bleManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        mBleAdapter = bleManager.adapter
        mBleAdapter?.let { startListen(it) }
    }

    override fun onDestroy() {
        super.onDestroy()
        mServer.setBtListener(null)
        mServer.closeListen()
    }

    private fun startListen(bleAdapter: BluetoothAdapter) {
        mServer.listen(bleAdapter)
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

    override fun onBtStateChanged(address: String, state: BtBase.BtState) {
        when (state) {
            BtBase.BtState.CONNECTING -> logPrint("等待连接中...")
            BtBase.BtState.CONNECTED -> logPrint("与[$address]连接成功")
            BtBase.BtState.DISCONNECT -> {
                logPrint("与[$address]断开连接")
                mBleAdapter?.let { startListen(it) }
            }
            BtBase.BtState.ERROR -> {
                logPrint("与[$address]连接出错")
                mBleAdapter?.let { startListen(it) }
            }
            else -> {}
        }
    }

    override fun onMsgOperated(address: String, msg: String) {
        logPrint("从[$address]接收到消息: $msg")
    }

    override fun onFileOperated(address: String, fileName: String, progress: Int) {
        if (progress == 0) {
            logPrint("从[$address]接收文件($fileName)中...")
        } else if (progress == 100) {
            logPrint("从[$address]接收文件($fileName)完成\n存储路径:null")
        }
    }
}