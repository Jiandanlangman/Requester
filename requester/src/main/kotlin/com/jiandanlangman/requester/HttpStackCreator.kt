package com.jiandanlangman.requester

import com.android.volley.toolbox.BaseHttpStack
import javax.net.ssl.SSLSocketFactory

interface HttpStackCreator {

    fun create(sslSocketFactory:SSLSocketFactory, dns: DNS?) : BaseHttpStack

}