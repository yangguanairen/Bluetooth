package com.sena.bluetooth

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast


/**
 * FileName: ExpandFunc
 * Author: JiaoCan
 * Date: 2024/5/7
 */


fun toast(text: String) {
    MyApplication.getContext().apply {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
        }
    }
}

