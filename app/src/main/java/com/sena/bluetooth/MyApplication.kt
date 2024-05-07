package com.sena.bluetooth

import android.app.Application
import android.content.Context


/**
 * FileName: Application
 * Author: JiaoCan
 * Date: 2024/3/25
 */

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {

         private lateinit var instance: MyApplication

        fun getContext(): Context {
            return instance.applicationContext
        }

    }
}

