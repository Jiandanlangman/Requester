package com.jiandanlangman.requester

import android.os.SystemClock
import android.util.Log
import com.android.volley.VolleyError
import java.net.URL


class Request internal constructor(private val parameterProvider: ParameterProvider, private val tag: Any, private val url: String, private val method: Int) {

    private companion object {

        const val EMPTY_JSON = "{}"

    }

    private val headers = HashMap<String, String>()
    private val params = HashMap<String, String>()

    private var gzipEnabled = parameterProvider.getGZIPEnabled()
    private var retryPolicy = parameterProvider.getRetryPolicy()
    private var disableCache = false
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

    fun disableCache(): Request {
        disableCache = true
        return this
    }

    fun sync() = sync(BaseParsedData::class.java)

    fun <T : ParsedData> sync(type: Class<T>): Response<T> {
        val lock = Object()
        var response: Response<T>? = null
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

    fun start(listener: (response: Response<BaseParsedData>) -> Unit) = start(BaseParsedData::class.java, listener)

    fun <T : ParsedData> start(type: Class<T>, listener: (response: Response<T>) -> Unit) {
        parameterProvider.getExecutorDeliveryHandler().post {
            val globalHeaders = parameterProvider.getGlobalHeaders()
            globalHeaders.keys.filter { !headers.containsKey(it) }.forEach { headers[it] = globalHeaders[it].toString() }
            val host = URL(url).protocol
            headers["Host"] = host
            val globalParams = parameterProvider.getGlobalParams()
            globalParams.keys.filter { !params.containsKey(it) }.forEach { params[it] = globalParams[it].toString() }
            parameterProvider.getPreRequestCallback()?.invoke(url, headers, params)
            val fullUrl = generateFullUrl()
            if (!disableCache)
                parameterProvider.getCacheManager()?.get(url, headers, params)?.let {
                    //TODO 在网络请求特别快的情况下，是否会出现网络比缓存先返回的情况？
                    handleResponse(fullUrl, it, type, true, listener)
                }
            if (parameterProvider.isShowLog()) {
                startRequestTime = SystemClock.elapsedRealtime()
                Log.d("StartRequest", "request: $fullUrl")
            }
            val requestUrl = if (method == com.android.volley.Request.Method.GET)
                parameterProvider.getDNS()?.lookup(host)?.let { fullUrl.replaceFirst(host, it) } ?: fullUrl
            else
                parameterProvider.getDNS()?.lookup(host)?.let { url.replaceFirst(host, it) } ?: url
            val request = StringRequest(parameterProvider.getCharset(), method, gzipEnabled, requestUrl, headers, params, {
                if (!handleResponse(fullUrl, it ?: "", type, false, listener) && !disableCache)
                    parameterProvider.getCacheManager()?.put(url, params, headers, it)
            }, {
                handleError(fullUrl, it, type, listener)
            }).setRetryPolicy(retryPolicy).setShouldCache(false).setTag(tag)
            parameterProvider.getRequestQueue().add(request)
        }
    }

    private fun <T : ParsedData> handleResponse(requestUrl: String, response: String, type: Class<T>, isCache: Boolean, listener: (response: Response<T>) -> Unit): Boolean {
        var hasError = true
        if (response.isNotEmpty()) {
            if (parameterProvider.isShowLog() && !isCache)
                Log.d("OnResponse", " \nrequest   :  $requestUrl\ntimeconsum:  ${SystemClock.elapsedRealtime() - startRequestTime}ms\nresponse  :  $response\n\n  ")
            var parseDataSuccess = true
            val parsedData = try {
                parameterProvider.parseData(response, type)
            } catch (ignore: Throwable) {
                parseDataSuccess = false
                parameterProvider.parseData(EMPTY_JSON, type)
            }
            var resp = Response(if (parseDataSuccess) ErrorCode.NO_ERROR else ErrorCode.PARSE_DATA_ERROR, isCache, response, parsedData)
            if (parameterProvider.getOnResponseListener()?.invoke(resp) == false)
                resp = Response(ErrorCode.CUSTOM_ERROR, isCache, response, parsedData)
            else {
                hasError = false
                if (parameterProvider.isShowLog() && isCache)
                    Log.d("OnCache", " \nrequest   :  $requestUrl\ntimeconsum:  ${SystemClock.elapsedRealtime() - startRequestTime}ms\nresponse  :  $response\n\n  ")
            }
            parameterProvider.getMainLooperHandler().post { listener.invoke(resp) }
        } else {
            val parsedData = parameterProvider.parseData(EMPTY_JSON, type)
            var resp = Response(ErrorCode.NO_RESPONSE_DATA, isCache, EMPTY_JSON, parsedData)
            if (parameterProvider.getOnResponseListener()?.invoke(resp) == false)
                resp = Response(ErrorCode.CUSTOM_ERROR, isCache, EMPTY_JSON, parsedData)
            parameterProvider.getMainLooperHandler().post { listener.invoke(resp) }
        }
        return hasError
    }

    private fun <T : ParsedData> handleError(requestUrl: String, error: VolleyError, type: Class<T>, listener: (response: Response<T>) -> Unit) {
        if (parameterProvider.isShowLog()) {
            Log.d("OnRequestError", " \n request: $requestUrl")
            error.printStackTrace()
        }
        val parsedData = parameterProvider.parseData(EMPTY_JSON, type)
        var resp = Response(ErrorCode.REQUEST_FAILED, false, EMPTY_JSON, parsedData)
        if (parameterProvider.getOnResponseListener()?.invoke(resp) == false)
            resp = Response(ErrorCode.CUSTOM_ERROR, false, EMPTY_JSON, parsedData)
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
