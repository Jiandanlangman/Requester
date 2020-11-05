package com.jiandanlangman.requester

import java.net.InetAddress

interface DNS {

    fun lookup(hostname: String) : List<InetAddress>

}