package com.jiandanlangman.requester

import android.os.SystemClock
import android.util.Log
import com.android.volley.Response
import com.android.volley.VolleyError


class Request internal constructor(private val requestQueue: RequestQueue, private val tag: Any, private val url: String, private val method: Int) {

    private companion object {

        const val EMPTY_JSON = "{}"

    }

    private val headers = HashMap<String, String>()
    private val params = HashMap<String, String>()

    private var gzipEnabled = Requester.getGZIPEnabled()
    private var retryPolicy = Requester.getRetryPolicy()
    private var startRequestTime = 0L


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
        gzipEnabled = enable
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
        Requester.postOnExecutorDeliveryHandler(Runnable {
            val globalHeaders = Requester.getGlobalHeaders()
            globalHeaders.keys.filter { !headers.containsKey(it) }.forEach { headers[it] = globalHeaders[it].toString() }
            val globalParams = Requester.getGlobalParams()
            globalParams.keys.filter { !params.containsKey(it) }.forEach { params[it] = globalParams[it].toString() }
            Requester.onPreRequest(url, headers, params)
            val requestUrl = generateFullUrl()
            if (Requester.isShowLog()) {
                startRequestTime = SystemClock.elapsedRealtime()
                Log.d("StartRequest", "request: $requestUrl")
            }
            val request = StringRequest(method, gzipEnabled, if (method == com.android.volley.Request.Method.GET) generateFullUrl() else url, headers, params, Response.Listener {
                handleResponse(requestUrl, it ?: "", type, listener)
            }, Response.ErrorListener {
                handleError(requestUrl, it, type, listener)
            }).setRetryPolicy(retryPolicy).setShouldCache(false).setTag(tag)
            requestQueue.add(request)
        })
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
            responseEntity.requestErrorCode = if (!Requester.onResponse(responseEntity)) ErrorCode.CUSTOM_ERROR else responseEntity.requestErrorCode
            Requester.postOnMainLooperHandler(Runnable { listener.invoke(responseEntity) })
        } else {
            val responseEntity = Requester.parseData(EMPTY_JSON, type)
            responseEntity.requestErrorCode = ErrorCode.NO_RESPONSE_DATA
            Requester.postOnMainLooperHandler(Runnable { listener.invoke(responseEntity) })
        }
    }

    private fun <T : BaseResponse> handleError(requestUrl: String, error: VolleyError, type: Class<T>, listener: (response: T) -> Unit) {
        if (Requester.isShowLog()) {
            Log.d("OnRequestError", " \n request: $requestUrl")
            error.printStackTrace()
        }
        val responseEntity = Requester.parseData(EMPTY_JSON, type)
        responseEntity.requestErrorCode = ErrorCode.REQUEST_FAILED
        responseEntity.requestErrorCode = if (!Requester.onResponse(responseEntity)) ErrorCode.CUSTOM_ERROR else ErrorCode.REQUEST_FAILED
        Requester.postOnMainLooperHandler(Runnable { listener.invoke(responseEntity) })
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
