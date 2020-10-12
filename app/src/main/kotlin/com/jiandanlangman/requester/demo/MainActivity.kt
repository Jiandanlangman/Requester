package com.jiandanlangman.requester.demo

import android.os.Bundle
import com.jiandanlangman.requester.DNS
import com.jiandanlangman.requester.Requester
import com.jiandanlangman.requester.get

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
//
//        val u = "https://www.qq.com/uname"
//        val url = URL(u)
//        val ip : String? = "192.168.1.1"q
//
//        Log.d("MainActivity", ip?.let { u.replaceFirst(url.host, it) } ?: u)

        Requester.setDNS(object :DNS {
            override fun lookup(hostname: String) = "z.520hx.vip"

        })
        get("http://z.520hx.vip/gift-log/create&gift_id=47&to_uid=11&number=1&created_in=room&created_in_id=11&room_id=11")
            .start()
    }


    fun getURL()  = "http://xxxxx"

}
