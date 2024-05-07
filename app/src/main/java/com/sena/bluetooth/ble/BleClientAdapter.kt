package com.sena.bluetooth.ble

import android.bluetooth.le.ScanResult
import android.content.Context
import android.view.ViewGroup
import android.widget.TextView
import com.chad.library.adapter4.BaseQuickAdapter
import com.chad.library.adapter4.viewholder.QuickViewHolder
import com.google.gson.Gson
import com.sena.bluetooth.R


/**
 * FileName: BleClientAdapter
 * Author: JiaoCan
 * Date: 2024/5/7
 */

class BleClientAdapter : BaseQuickAdapter<ScanResult, QuickViewHolder>() {
    override fun onBindViewHolder(holder: QuickViewHolder, position: Int, item: ScanResult?) {
        if (item == null) return
        val gsonStr = Gson().toJson(item)
        println("测试: $gsonStr")
        holder.getView<TextView>(R.id.content).text = gsonStr
    }

    override fun onCreateViewHolder(context: Context, parent: ViewGroup, viewType: Int): QuickViewHolder {
        return QuickViewHolder(R.layout.item_ble, parent)
    }
}

