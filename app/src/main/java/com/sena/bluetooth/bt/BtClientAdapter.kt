package com.sena.bluetooth.bt

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.view.ViewGroup
import android.widget.TextView
import com.chad.library.adapter4.BaseQuickAdapter
import com.chad.library.adapter4.viewholder.QuickViewHolder
import com.sena.bluetooth.R
import com.sena.bluetooth.getDeviceBond
import com.sena.bluetooth.getDeviceName


/**
 * FileName: BtClientAdapter
 * Author: JiaoCan
 * Date: 2024/5/8 10:44
 */

class BtClientAdapter : BaseQuickAdapter<BluetoothDevice, QuickViewHolder>() {
    override fun onBindViewHolder(holder: QuickViewHolder, position: Int, item: BluetoothDevice?) {
        if (item == null) return
        val sb = context.getDeviceName(item) + "\n" + item.address +
                if (context.getDeviceBond(item) == BluetoothDevice.BOND_BONDED) " (已配对) " else " (未配对) "
        holder.getView<TextView>(R.id.content).text = sb
    }

    override fun onCreateViewHolder(context: Context, parent: ViewGroup, viewType: Int): QuickViewHolder {
        return QuickViewHolder(R.layout.item_ble, parent)
    }
}

