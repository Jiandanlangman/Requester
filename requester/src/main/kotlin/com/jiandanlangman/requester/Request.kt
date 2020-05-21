package com.jiandanlangman.requester

import android.os.Handler
import android.os.SystemClock
import android.util.Log
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.RetryPolicy
import com.android.volley.VolleyError


class Request internal constructor(private val requestQueue: RequestQueue, private val executorDeliveryHandler: Handler, private val mainLooperHandler: Handler, private val tag: Any, private val url: String, private val method: Int, initHeaders: Map<String, Any>, initParams: Map<String, Any>, gzipEnabled: Boolean, rp: RetryPolicy, private val preRequestCallback: ((String, HashMap<String, String>, HashMap<String, String>) -> Unit)?, private val responseCallback: (response: BaseResponse) -> Boolean) {

    private companion object {

        const val EMPTY_JSON = "{}"

    }

    private val headers = HashMap<String, String>()
    private val params = HashMap<String, String>()

    private var enableGZIP = gzipEnabled
    private var retryPolicy = rp
    private var startRequestTime = 0L

    init {
        initHeaders.keys.forEach { headers[it] = initHeaders[it].toString() }
        initParams.keys.forEach { params[it] = initParams[it].toString() }
    }

    fun addHeader(field: String, value: Any?): Request {
        if (value != null)
            headers[field] = value.toString()
        return this
    }

    fun addParam(field: String, value: Any?): Request {
        if (value != null)
            params[field] = value.toString()
        return this
    }

    fun enableGZIP(enable: Boolean): Request {
        enableGZIP = enable
        return this
    }

    fun setTimeout(timeoutMs: Int): Request {
        if (timeoutMs != retryPolicy.currentTimeout)
            retryPolicy = DisableRetryPolicy(timeoutMs)
        return this
    }

    fun sync() = sync(BaseResponse::class.java)

    fun <T : BaseResponse> sync(type: Class<T>): T {
        val lock = Object()
        var response: T? = null
        synchronized(lock) {
            start(type) {
                synchronized(lock) {
                    response = it
                    lock.notifyAll()
                }
            }
            lock.wait()
        }
        return response!!
    }

    fun start() = start { }

    fun start(listener: (response: BaseResponse) -> Unit) = start(BaseResponse::class.java, listener)

    fun <T : BaseResponse> start(type: Class<T>, listener: (response: T) -> Unit) {
        preRequestCallback?.invoke(url, headers, params)
        executorDeliveryHandler.post {
            val requestUrl = generateFullUrl()
            if (Requester.isShowLog()) {
                startRequestTime = SystemClock.elapsedRealtime()
                Log.d("StartRequest", "request: $requestUrl")
            }
            val request = StringRequest(method, enableGZIP, if (method == com.android.volley.Request.Method.GET) generateFullUrl() else url, headers, params, Response.Listener {
                handleResponse(requestUrl, it ?: "", type, listener)
            }, Response.ErrorListener {
                handleError(requestUrl, it, type, listener)
            }).setRetryPolicy(retryPolicy).setShouldCache(false).setTag(tag)
            requestQueue.add(request)
        }
    }

    private fun <T : BaseResponse> handleResponse(requestUrl: String, response: String, type: Class<T>, listener: (response: T) -> Unit) {
        if (response.isNotEmpty()) {
            if (Requester.isShowLog())
                Log.d("OnResponse", " \nrequest   :  $requestUrl\ntimeconsum:  ${SystemClock.elapsedRealtime() - startRequestTime}ms\nresponse  :  $response\n\n  ")
            var responseIsJSON = true
            val responseEntity = try {
                Requester.parseData(response, type)
            } catch (ignore: Throwable) {
                responseIsJSON = false
                Requester.parseData(EMPTY_JSON, type)
            }
            responseEntity.requestErrorCode = if (responseIsJSON) ErrorCode.NO_ERROR else ErrorCode.PARSE_DATA_ERROR
            responseEntity.responseData = response
            responseEntity.requestErrorCode = if (!responseCallback.invoke(responseEntity)) ErrorCode.CUSTOM_ERROR else responseEntity.requestErrorCode
            mainLooperHandler.post { listener.invoke(responseEntity) }
        } else {
            val responseEntity = Requester.parseData(EMPTY_JSON, type)
            responseEntity.requestErrorCode = ErrorCode.NO_RESPONSE_DATA
            mainLooperHandler.post { listener.invoke(responseEntity) }
        }
    }

    private fun <T : BaseResponse> handleError(requestUrl: String, error: VolleyError, type: Class<T>, listener: (response: T) -> Unit) {
        if (Requester.isShowLog()) {
            Log.d("OnRequestError", " \n request: $requestUrl")
            error.printStackTrace()
        }
        val responseEntity = Requester.parseData(EMPTY_JSON, type)
        responseEntity.requestErrorCode = ErrorCode.REQUEST_FAILED
        responseEntity.requestErrorCode = if (!responseCallback.invoke(responseEntity)) ErrorCode.CUSTOM_ERROR else ErrorCode.REQUEST_FAILED
        mainLooperHandler.post { listener.invoke(responseEntity) }
    }


    private fun generateFullUrl(): String {
        val sb = StringBuilder()
        sb.append(url)
        if (params.isNotEmpty()) {
            sb.append(if (sb.contains("?")) "&" else "?")
            params.forEach {
                sb.append(it.key)
                sb.append("=")
                sb.append(it.value)
                sb.append("&")
            }
        } else
            sb.append("&")
        return sb.substring(0, sb.length - 1)
    }


}
