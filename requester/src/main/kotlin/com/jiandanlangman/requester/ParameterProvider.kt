package com.jiandanlangman.requester

import android.os.Handler
import com.android.volley.RetryPolicy

internal interface ParameterProvider {

    fun getCharset(): String

    fun isShowLog(): Boolean

    fun getGZIPEnabled(): Boolean

    fun getRetryPolicy(): RetryPolicy

    fun getGlobalHeaders(): Map<String, Any>

    fun getGlobalParams(): Map<String, Any>

    fun getExecutorDeliveryHandler(): Handler

    fun getMainLooperHandler(): Handler

    fun getPreRequestCallback(): ((url: String, method: Int, headers: HashMap<String, String>, params: HashMap<String, String>) -> Unit)?

    fun getOnResponseListener(): ((response: Response<out ParsedData>) -> Boolean)?

    fun getRequestQueue(): RequestQueue

    fun <T> parseData(json: String, clazz: Class<T>): T

    fun getCacheManager(): CacheManager?

}