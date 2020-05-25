package com.jiandanlangman.requester

import android.os.SystemClock
import android.util.Log
import com.android.volley.Response
import com.android.volley.VolleyError


class Request internal constructor(private val parameterProvider: ParameterProvider, private val tag: Any, private val url: String, private val method: Int) {

    private companion object {

        const val EMPTY_JSON = "{}"

    }

    private val headers = HashMap<String, String>()
    private val params = HashMap<String, String>()

    private var gzipEnabled = parameterProvider.getGZIPEnabled()
    private var retryPolicy = parameterProvider.getRetryPolicy()
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

    fun sync() = sync(BaseParsedData::class.java)

    fun <T : ParsedData> sync(type: Class<T>): com.jiandanlangman.requester.Response<T> {
        val lock = Object()
        var response: com.jiandanlangman.requester.Response<T>? = null
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

    fun start(listener: (response: com.jiandanlangman.requester.Response<BaseParsedData>) -> Unit) = start(BaseParsedData::class.java, listener)

    fun <T : ParsedData> start(type: Class<T>, listener: (response: com.jiandanlangman.requester.Response<T>) -> Unit) {
        parameterProvider.getExecutorDeliveryHandler().post {
            val globalHeaders = parameterProvider.getGlobalHeaders()
            globalHeaders.keys.filter { !headers.containsKey(it) }.forEach { headers[it] = globalHeaders[it].toString() }
            val globalParams = parameterProvider.getGlobalParams()
            globalParams.keys.filter { !params.containsKey(it) }.forEach { params[it] = globalParams[it].toString() }
            parameterProvider.getPreRequestCallback()?.invoke(url, headers, params)
            val requestUrl = generateFullUrl()
            if (parameterProvider.isShowLog()) {
                startRequestTime = SystemClock.elapsedRealtime()
                Log.d("StartRequest", "request: $requestUrl")
            }
            val request = StringRequest(parameterProvider.getCharset(), method, gzipEnabled, if (method == com.android.volley.Request.Method.GET) generateFullUrl() else url, headers, params, Response.Listener {
                handleResponse(requestUrl, it ?: "", type, listener)
            }, Response.ErrorListener {
                handleError(requestUrl, it, type, listener)
            }).setRetryPolicy(retryPolicy).setShouldCache(false).setTag(tag)
            parameterProvider.getRequestQueue().add(request)
        }
    }

    private fun <T : ParsedData> handleResponse(requestUrl: String, response: String, type: Class<T>, listener: (response: com.jiandanlangman.requester.Response<T>) -> Unit) {
        if (response.isNotEmpty()) {
            if (parameterProvider.isShowLog())
                Log.d("OnResponse", " \nrequest   :  $requestUrl\ntimeconsum:  ${SystemClock.elapsedRealtime() - startRequestTime}ms\nresponse  :  $response\n\n  ")
            var responseIsJSON = true
            val parsedData = try {
                parameterProvider.parseData(response, type)
            } catch (ignore: Throwable) {
                responseIsJSON = false
                parameterProvider.parseData(EMPTY_JSON, type)
            }
            var resp = Response(if (responseIsJSON) ErrorCode.NO_ERROR else ErrorCode.PARSE_DATA_ERROR, response, parsedData)
            if (parameterProvider.getOnResponseListener()?.invoke(resp) == false)
                resp = Response(ErrorCode.CUSTOM_ERROR, response, parsedData)
            parameterProvider.getMainLooperHandler().post { listener.invoke(resp) }
        } else {
            val parsedData = parameterProvider.parseData(EMPTY_JSON, type)
            var resp = Response(ErrorCode.NO_RESPONSE_DATA, EMPTY_JSON, parsedData)
            if (parameterProvider.getOnResponseListener()?.invoke(resp) == false)
                resp = Response(ErrorCode.CUSTOM_ERROR, EMPTY_JSON, parsedData)
            parameterProvider.getMainLooperHandler().post { listener.invoke(resp) }
        }
    }

    private fun <T : ParsedData> handleError(requestUrl: String, error: VolleyError, type: Class<T>, listener: (response: com.jiandanlangman.requester.Response<T>) -> Unit) {
        if (parameterProvider.isShowLog()) {
            Log.d("OnRequestError", " \n request: $requestUrl")
            error.printStackTrace()
        }
        val parsedData = parameterProvider.parseData(EMPTY_JSON, type)
        var resp = Response(ErrorCode.REQUEST_FAILED, EMPTY_JSON, parsedData)
        if (parameterProvider.getOnResponseListener()?.invoke(resp) == false)
            resp = Response(ErrorCode.CUSTOM_ERROR, EMPTY_JSON, parsedData)
        parameterProvider.getMainLooperHandler().post { listener.invoke(resp) }
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
