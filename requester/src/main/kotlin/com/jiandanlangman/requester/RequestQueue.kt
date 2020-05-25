package com.jiandanlangman.requester

import com.android.volley.Cache
import com.android.volley.Network
import com.android.volley.Request
import com.android.volley.ResponseDelivery

internal class RequestQueue(cache: Cache, network: Network, threadPoolSize: Int, delivery: ResponseDelivery) : com.android.volley.RequestQueue(cache, network, threadPoolSize, delivery) {

    private var requestAddListener: (Request<*>) -> Unit = {}

    override fun <T> add(request: Request<T>): Request<T> {
        super.add(request)
        requestAddListener.invoke(request)
        return request
    }

    fun setRequestAddListener(listener: (request: Request<*>) -> Unit) {
        this.requestAddListener = listener
    }
}