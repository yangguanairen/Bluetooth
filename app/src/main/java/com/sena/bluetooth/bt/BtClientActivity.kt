package com.sena.bluetooth.bt

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.recyclerview.widget.LinearLayoutManager
import com.sena.bluetooth.checkConnectPermission
import com.sena.bluetooth.databinding.ActivityBtClientBinding
import com.sena.bluetooth.toast

class BtClientActivity : AppCompatActivity(), BtBase.BtListener {

    private val binding by lazy { ActivityBtClientBinding.inflate(layoutInflater) }
    private var mBleAdapter: BluetoothAdapter? = null
    private val mListAdapter = BtClientAdapter()
    private val mClient: BtClient = BtClient(this).apply {
        setBtListener(this@BtClientActivity)
    }

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
        if (mClient.isConnected(device)) return
        mClient.connect(device)
    }

    private fun sendText() {
        val text = binding.input.text.toString()
        mClient.sendMsg(text)
    }

    private fun sendFile() {

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

    override fun onMsgOperated(address: String, msg: String) {
        logPrint("向[$address]发送消息: $msg")
    }

    override fun onFileOperated(address: String, fileName: String, progress: Int) {
        if (progress == 0) {
            logPrint("向[$address]发送文件($fileName)中...")
        } else if (progress == 100) {
            logPrint("向[$address]发送文件($fileName)完成")
        }
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
}