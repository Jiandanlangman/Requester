package com.jiandanlangman.requester

import com.android.volley.Cache
import com.android.volley.Network
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.ResponseDelivery

class RequestQueue(cache: Cache, network: Network, threadPoolSize: Int, delivery: ResponseDelivery) : RequestQueue(cache, network, threadPoolSize, delivery) {

    private var requestAddListener: (Request<*>) -> Unit = {}

    override fun <T> add(request: Request<T>): Request<T> {
        val req = super.add(request)
        requestAddListener.invoke(req)
        return req
    }

    fun setRequestAddListener(listener: (request: Request<*>) -> Unit) {
        this.requestAddListener = listener
    }
}