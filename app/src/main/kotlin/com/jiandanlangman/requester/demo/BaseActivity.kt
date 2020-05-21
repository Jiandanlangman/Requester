package com.jiandanlangman.requester.demo

import androidx.appcompat.app.AppCompatActivity
import com.jiandanlangman.requester.Requester

abstract class BaseActivity : AppCompatActivity() {

    override fun onDestroy() {
        Requester.cancelAll(this)
        super.onDestroy()
    }

}