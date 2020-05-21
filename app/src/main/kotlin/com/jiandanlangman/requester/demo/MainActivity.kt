package com.jiandanlangman.requester.demo

import android.os.Bundle

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

    }
}
