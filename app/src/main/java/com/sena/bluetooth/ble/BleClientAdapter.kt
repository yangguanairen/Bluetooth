package com.sena.bluetooth.ble

import android.bluetooth.le.ScanResult
import android.content.Context
import android.view.ViewGroup
import android.widget.TextView
import com.chad.library.adapter4.BaseQuickAdapter
import com.chad.library.adapter4.viewholder.QuickViewHolder
import com.google.gson.Gson
import com.sena.bluetooth.R
import com.sena.bluetooth.utils.getDeviceName


/**
 * FileName: BleClientAdapter
 * Author: JiaoCan
 * Date: 2024/5/7
 */

class BleClientAdapter : BaseQuickAdapter<ScanResult, QuickViewHolder>() {
    override fun onBindViewHolder(holder: QuickViewHolder, position: Int, item: ScanResult?) {
        if (item == null) return
        val device = item.device
        val sb = StringBuilder().appendLine("Name=${context.getDeviceName(device)}, Address=${device.address}, Rssi=${item.rssi}")

        val scanRecord = item.scanRecord
        val sb2 = StringBuilder()
        scanRecord?.apply {
            sb2.append("advertiseFlags=${this.advertiseFlags}, ")
                .append("serviceUuids=${this.serviceUuids?.joinToString(", ")}, ")
                .append("manufacturerSpecificData=${Gson().toJson(this.manufacturerSpecificData)}, ")
                .append("serviceData = ${Gson().toJson(this.serviceData)}, ")
                .append("txPowerLevel = ${this.txPowerLevel}, ")
                .append("deviceName = ${this.deviceName}")
        }
        sb.append("广播数据: $sb2")

        holder.getView<TextView>(R.id.content).text = sb
    }

    override fun onCreateViewHolder(context: Context, parent: ViewGroup, viewType: Int): QuickViewHolder {
        return QuickViewHolder(R.layout.item_ble, parent)
    }
}

