package com.jiandanlangman.requester

interface DNS {

    fun getIP(hostname:String) : String?

}