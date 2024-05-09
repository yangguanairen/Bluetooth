package com.sena.bluetooth.bt

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import com.sena.bluetooth.checkConnectPermission
import com.sena.bluetooth.databinding.ActivityBtClientBinding
import com.sena.bluetooth.getOrNull
import com.sena.bluetooth.toast
import com.sena.bluetooth.utils.FileUtil
import java.text.SimpleDateFormat

class BtClientActivity : AppCompatActivity(), BtBase.BtListener {

    private val binding by lazy { ActivityBtClientBinding.inflate(layoutInflater) }
    private var mBleAdapter: BluetoothAdapter? = null
    private val mListAdapter = BtClientAdapter()
    private val mClient: BtClient = BtClient(this).apply {
        setBtListener(this@BtClientActivity)
    }
    private var curLog = ""
    @SuppressLint("SimpleDateFormat")
    private val dataFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    private lateinit var requestFile: ActivityResultLauncher<String>

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null || intent.action != BluetoothDevice.ACTION_FOUND) return
            val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            if (device == null) return
            val notHas = mListAdapter.items.none { it.address == device.address}
            if (notHas) mListAdapter.add(device)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val status = intent.getBooleanExtra("status", false)
        if (status) {
            val bleManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            mBleAdapter = bleManager.adapter
        }
        initLaunch()
        initView()
        registerReceiver(receiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
        mBleAdapter?.let { startScan() }
    }

    override fun onDestroy() {
        super.onDestroy()
        mClient.setBtListener(null)
        mClient.closeConnect()
        unregisterReceiver(receiver)
        checkConnectPermission {
            mBleAdapter?.cancelDiscovery()
        }
    }

    private fun initLaunch() {
        requestFile = registerForActivityResult(ActivityResultContracts.GetContent()) {
            if (it == null) {
                toast("未选择文件...")
                return@registerForActivityResult
            }
            getOrNull { FileUtil.uriToPath(this, it) }?.let { path ->
                println("测试: $path")
                mClient.sendClientFile(path)
            }
        }
    }


    private fun initView() {
        mListAdapter.setOnItemClickListener { a, _, p ->
            a.getItem(p)?.let { connect(it) }
        }
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@BtClientActivity)
            adapter = mListAdapter
        }
        binding.scan.setOnClickListener { startScan() }
        binding.sendText.setOnClickListener { sendText() }
        binding.sendFile.setOnClickListener { sendFile() }
        binding.disconnect.setOnClickListener { disconnect() }
    }

    private fun startScan() {
        mBleAdapter?: toast("无法使用, 检查权限和蓝牙...")
        mBleAdapter?.let {
            checkConnectPermission {
                mListAdapter.submitList(emptyList())
                mListAdapter.addAll(it.bondedDevices.toList())
                it.cancelDiscovery()
                it.startDiscovery()
            }
        }
    }

    private fun connect(device: BluetoothDevice) {
        if (mClient.isConnectedWithDevice(device)) {
            toast("设备已连接")
            return
        }
        mClient.connect(device)
    }

    private fun disconnect() {
        if (!mClient.isConnected()) {
            toast("未连接...")
            return
        }
        mClient.closeConnect()
    }

    private fun sendText() {
        if (!mClient.isConnected()) {
            toast("未连接...")
            return
        }
        val text = binding.input.text.toString()
        if (text.isEmpty()) {
            toast("发送文本不得为空...")
            return
        }
        mClient.sendClientMsg(text)
    }

    private fun sendFile() {
        if (!mClient.isConnected()) {
            toast("未连接...")
            return
        }
        requestFile.launch("*/*")
    }

    override fun onBtStateChanged(address: String, state: BtBase.BtState) {
        when (state) {
            BtBase.BtState.CONNECTING -> logPrint("与[$address]连接中...")
            BtBase.BtState.CONNECTED -> logPrint("与[$address]连接成功")
            BtBase.BtState.DISCONNECT -> logPrint("与[$address]断开连接")
            BtBase.BtState.BUSY -> logPrint("正在发送其它数据, 请稍后再发...")
            BtBase.BtState.ERROR -> logPrint("与[$address]连接出错")
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

    override fun onOperateErrorLog(log: String) {
        logPrint(log)
    }

    private fun logPrint(text: String) {
        println(text)
        curLog = curLog + "\n" + dataFormat.format(System.currentTimeMillis()) + "\n" + text + "\n"
        binding.log.text = curLog
        val scrollY = binding.log.measuredHeight - binding.logLayout.height
        binding.logLayout.smoothScrollTo(0, scrollY)
    }
}