package com.jiandanlangman.requester

interface DNS {

    fun lookup(hostname:String) : String?

}