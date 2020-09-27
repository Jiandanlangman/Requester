package com.jiandanlangman.requester.demo

import android.os.Build
import android.os.Bundle
import android.util.Log
import com.jiandanlangman.requester.post
import java.net.InetAddress
import java.net.URL

class MainActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //一个简单的请求案例
//        post("user/list")
//            .addParam("page_index", 1)
//            .addParam("page_size", 20)
//            .start(UserList::class.java) {
//
//            }
        val u = "https://www.qq.com/uname"
        val url = URL(u)
        val ip : String? = "192.168.1.1"

        Log.d("MainActivity", ip?.let { u.replaceFirst(url.host, it) } ?: u)
    }

}
