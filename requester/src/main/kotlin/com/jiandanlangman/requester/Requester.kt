package com.jiandanlangman.requester

import android.app.Application
import android.os.*
import com.android.volley.ExecutorDelivery
import com.android.volley.toolbox.BasicNetwork
import com.android.volley.toolbox.DiskBasedCache
import com.android.volley.toolbox.HurlStack
import java.io.File
import java.io.InputStream
import java.nio.charset.Charset
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLSocketFactory

object Requester {

    private val globalHeaders = HashMap<String, Any>()
    private val globalParams = HashMap<String, Any>()

    private val requestQueues = ArrayList<RequestQueue>()
    private val requestQueueBurdens = HashMap<RequestQueue, Int>()

    private  val parameterProvider = object : ParameterProvider {

        override fun getCharset() = charset

        override fun isShowLog() = showLog

        override fun getGZIPEnabled() = gzipEnabled

        override fun getRetryPolicy() = retryPolicy

        override fun getGlobalHeaders() = globalHeaders

        override fun getGlobalParams() = globalParams

        override fun getExecutorDeliveryHandler() = executorDeliveryHandler

        override fun getMainLooperHandler() = mainLooperHandler

        override fun getPreRequestCallback() = preRequestCallback

        override fun getOnResponseListener() = onResponseListener

        override fun getRequestQueue() = this@Requester.getRequestQueue()

        override fun <T> parseData(json: String, clazz: Class<T>): T {
            if (dataParser == null)
                setDataParser(GSONDataParser())
            return dataParser!!.parseData(json, clazz)
        }

    }

    private var charset = Charset.forName("UTF-8")
    private var showLog = true
    private var init = false
    private var maxRequestQueueCount = 1
    private var gzipEnabled = false
    private var routing = ""
    private var retryPolicy = DisableRetryPolicy(20 * 1000)

    private lateinit var sslSocketFactory: SSLSocketFactory
    private lateinit var cacheDir: File
    private lateinit var mainLooperHandler: Handler
    private lateinit var executorDeliveryHandler: Handler




    private var dataParser: DataParser? = null
    private var preRequestCallback: ((String, HashMap<String, String>, HashMap<String, String>) -> Unit)? = null
    private var onResponseListener: ((Response<out ParsedData>) -> Boolean)? = null


    fun init(application: Application, maxRequestQueueCount: Int, certInputStream: InputStream? = null) {
        if (init)
            return
        init = true
        sslSocketFactory = HTTPSManager.buildSSLSocketFactory(certInputStream)
        cacheDir = File(application.externalCacheDir, "requester")
        mainLooperHandler = Handler(Looper.getMainLooper())
        HttpsURLConnection.setDefaultHostnameVerifier { _, _ -> true }
        this.maxRequestQueueCount = if (maxRequestQueueCount > 0) maxRequestQueueCount else 1
        val thread = HandlerThread("RequesterDeliveryThread", Process.THREAD_PRIORITY_BACKGROUND)
        thread.start()
        executorDeliveryHandler = Handler(thread.looper)
        executorDeliveryHandler.post {
            val initRequestQueueCount = (maxRequestQueueCount + 1) / 2
            for (i in 0 until initRequestQueueCount)
                createRequestQueue()
        }
    }


    fun setOnResponseListener(listener: ((Response<out ParsedData>) -> Boolean)?) {
        onResponseListener = listener
    }

    fun setOnPreRequestCallback(callback: ((url: String, headers: HashMap<String, String>, params: HashMap<String, String>) -> Unit)?) {
        preRequestCallback = callback
    }

    fun setGlobalHeaders(headers: Map<String, Any>) {
        globalHeaders.clear()
        globalHeaders.putAll(headers)
    }

    fun updateGlobalHeader(key: String, value: Any?) {
        if (value == null)
            globalHeaders.remove(key)
        else
            globalHeaders[key] = value
    }

    fun setGlobalParams(params: Map<String, Any>) {
        globalParams.clear()
        globalParams.putAll(params)
    }

    fun updateGlobalParam(key: String, value: Any?) {
        if (value == null)
            globalParams.remove(key)
        else
            globalParams[key] = value
    }

    fun enableGZIP(enable: Boolean) {
        gzipEnabled = enable
    }

    fun setDefaultRouting(url: String?) {
        routing = url ?: ""
    }

    fun setCharset(charset: Charset) {
        this.charset = charset
    }

    fun showLog(showLog: Boolean) {
        this.showLog = showLog
    }

    fun setTimeout(timeoutMs: Int) {
        if (timeoutMs != retryPolicy.currentTimeout)
            retryPolicy = DisableRetryPolicy(timeoutMs)
    }

    fun setDataParser(dataParser: DataParser) {
        this.dataParser = dataParser
    }

    fun get(url: String, tag: Any) = request(com.android.volley.Request.Method.GET, url, tag)

    fun post(url: String, tag: Any) = request(com.android.volley.Request.Method.POST, url, tag)

    fun request(method: Int, url: String, tag: Any) = Request(parameterProvider, tag, if (url.startsWith("http", true)) url else "$routing$url", method)

    fun cancelAll(tag: Any) = requestQueues.forEach { it.cancelAll(tag) }


    private fun getRequestQueue(): RequestQueue {
        var burdens = Int.MAX_VALUE
        var requestQueue = requestQueues[0]
        requestQueues.forEach {
            val b = requestQueueBurdens[it] ?: 0
            if (b < 4) {
                stopExcessRequestQueue()
                return it
            }
            if (b < burdens) {
                burdens = b
                requestQueue = it
            }
        }
        if (requestQueues.size < maxRequestQueueCount)
            return createRequestQueue()
        return requestQueue
    }

    private fun createRequestQueue(): RequestQueue {
        val cache = DiskBasedCache(cacheDir)
        val network = BasicNetwork(HurlStack(null, sslSocketFactory))
        val requestQueue = RequestQueue(cache, network, 4, ExecutorDelivery(executorDeliveryHandler))
        requestQueue.setRequestAddListener { requestQueueBurdens[requestQueue] = (requestQueueBurdens[requestQueue] ?: 0) + 1 }
        requestQueue.addRequestFinishedListener<StringRequest> { requestQueueBurdens[requestQueue] = (requestQueueBurdens[requestQueue] ?: 0) - 1 }
        requestQueues.add(requestQueue)
        requestQueue.start()
        return requestQueue
    }


    private fun stopExcessRequestQueue() {
        val excessRequestQueues = requestQueues.filter { requestQueueBurdens[it] == null || requestQueueBurdens[it] == 0 }
        val count = (maxRequestQueueCount + 1) / 2
        if (excessRequestQueues.size > count) {
            for (i in count until excessRequestQueues.size) {
                val requestQueue = excessRequestQueues[i]
                requestQueues.remove(requestQueue)
                requestQueueBurdens.remove(requestQueue)
                requestQueue.stop()
            }
        }
    }

}