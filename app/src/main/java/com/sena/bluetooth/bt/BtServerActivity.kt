package com.sena.bluetooth.bt

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.sena.bluetooth.databinding.ActivityBtServerBinding
import com.sena.bluetooth.getOrNull
import com.sena.bluetooth.toast
import com.sena.bluetooth.utils.FileUtil
import java.text.SimpleDateFormat

class BtServerActivity : AppCompatActivity(), BtBase.BtListener {

    private val binding by lazy { ActivityBtServerBinding.inflate(layoutInflater) }
    private val mServer = BtServer(this).apply {
        setBtListener(this@BtServerActivity)
    }
    private var mBleAdapter: BluetoothAdapter? = null
    private var curLog = ""
    @SuppressLint("SimpleDateFormat")
    private val dataFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    private lateinit var requestFile: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        val bleManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        mBleAdapter = bleManager.adapter

        initLaunch()
        initView()

        mBleAdapter?.let { startListen(it) }
    }

    override fun onDestroy() {
        super.onDestroy()
        mServer.setBtListener(null)
        mServer.closeListen()
    }

    private fun initLaunch() {
        requestFile = registerForActivityResult(ActivityResultContracts.GetContent()) {
            if (it == null) {
                toast("未选择文件...")
                return@registerForActivityResult
            }
            getOrNull { FileUtil.uriToPath(this, it) }?.let { path ->
                println("测试: $path")
                mServer.sendFile(path)
            }
        }
    }

    private fun initView() {
        binding.sendText.setOnClickListener { sendText() }
        binding.sendFile.setOnClickListener { sendFile() }
        binding.disconnect.setOnClickListener { disconnect() }
    }

    private fun startListen(bleAdapter: BluetoothAdapter) {
        mServer.listen(bleAdapter)
    }

    private fun sendText() {
        if (!mServer.isConnected()) {
            toast("未连接...")
            return
        }
        val text = binding.input.text.toString()
        if (text.isEmpty()) {
            toast("发送文本不得为空...")
            return
        }
        mServer.sendMsg(text)
    }

    private fun sendFile() {
        if (!mServer.isConnected()) {
            toast("未连接...")
            return
        }
        requestFile.launch("*/*")
    }

    private fun disconnect() {
        if (!mServer.isConnected()) {
            toast("未连接...")
            return
        }
        mServer.closeListen()
        mBleAdapter?.let { mServer.listen(it) }
    }

    private fun logPrint(text: String) {
        println(text)
        curLog = curLog + "\n" + dataFormat.format(System.currentTimeMillis()) + "\n" + text + "\n"
        runOnUiThread {
            binding.log.text = curLog
            val scrollY = binding.log.measuredHeight - binding.logLayout.height
            binding.logLayout.smoothScrollTo(0, scrollY)
        }
    }

    override fun onBtStateChanged(address: String, state: BtBase.BtState) {
        when (state) {
            BtBase.BtState.CONNECTING -> logPrint("等待连接中...")
            BtBase.BtState.CONNECTED -> logPrint("与[$address]连接成功")
            BtBase.BtState.DISCONNECT -> {
                logPrint( "与[$address]断开连接")
                mBleAdapter?.let { startListen(it) }
            }
            BtBase.BtState.ERROR -> {
                logPrint("与[$address]连接出错")
                mBleAdapter?.let { startListen(it) }
            }
            else -> {}
        }
    }

    override fun onMsgOperated(address: String, msg: String, isSender: Boolean) {
        if (isSender) {
            logPrint("向[$address]发送消息: $msg")
        } else {
            logPrint("从[$address]接收到消息: $msg")
        }
    }

    override fun onFileOperated(address: String, fileName: String, progress: Int, isSender: Boolean, savePath: String?) {
        if (progress == 0) {
            if (isSender) {
                logPrint("向[$address]发送文件($fileName)中...")
            } else {
                logPrint("从[$address]接收文件($fileName)中...")
            }
        } else if (progress == 100) {
            if (isSender) {
                logPrint("向[$address]发送文件($fileName)完成")
            } else {
                logPrint("从[$address]接收文件($fileName)完成\n存储路径:$savePath")
            }
        }
    }
}