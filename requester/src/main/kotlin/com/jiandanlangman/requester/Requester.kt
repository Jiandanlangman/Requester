package com.jiandanlangman.requester

import android.app.Activity
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

    private val onResponseCallback: (BaseResponse) -> Boolean = { onResponseListener?.invoke(it) ?: true }
    private val requestQueues = ArrayList<RequestQueue>()
    private val requestQueueBurdens = HashMap<RequestQueue, Int>()

    private var charset = Charset.forName("UTF-8")
    private var showLog = true
    private var init = false
    private var maxRequestQueueCount = 1
    private var enableGZIP = false
    private var routing = ""

    private lateinit var sslSocketFactory: SSLSocketFactory
    private lateinit var cacheDir: File
    private lateinit var mainLooperHandler: Handler

    private var executorDeliveryHandler: Handler? = null
    private var preRequestCallback: ((String, HashMap<String, String>, HashMap<String, String>) -> Unit)? = null
    private var onResponseListener: ((BaseResponse) -> Boolean)? = null


    fun init(application: Application, maxRequestQueueCount: Int, certInputStream: InputStream? = null) {
        if (init)
            return
        sslSocketFactory = HTTPSManager.buildSSLSocketFactory(certInputStream)
        cacheDir = File(application.externalCacheDir, "volley")
        mainLooperHandler = Handler(Looper.getMainLooper())
        HttpsURLConnection.setDefaultHostnameVerifier { _, _ -> true }
        this.maxRequestQueueCount = if (maxRequestQueueCount > 0) maxRequestQueueCount else 1
        createRequestQueues()
        listeningActivityLifecycle(application)
        init = true
    }


    fun setOnResponseListener(listener: ((BaseResponse) -> Boolean)?) {
        onResponseListener = listener
    }

    fun setOnPreRequestCallback(callback: ((url: String, headers: HashMap<String, String>, params: HashMap<String, String>) -> Unit)?) {
        preRequestCallback = callback
    }

    fun setGlobalHeaders(headers: Map<String, String>) {
        globalHeaders.clear()
        globalHeaders.putAll(headers)
    }

    fun updateGlobalHeader(key: String, value: String?) {
        if (value == null)
            globalHeaders.remove(key)
        else
            globalHeaders[key] = value
    }

    fun setGlobalParams(params: Map<String, String>) {
        globalParams.clear()
        globalParams.putAll(params)
    }

    fun updateGlobalParam(key: String, value: String?) {
        if (value == null)
            globalParams.remove(key)
        else
            globalParams[key] = value
    }

    fun enableGZIP(enable: Boolean) {
        enableGZIP = enable
    }

    fun setDefaultRouting(url: String?) {
        routing = url ?: ""
    }

    fun setCharset(charset: Charset) {
        this.charset = charset
    }

    fun showLog(showLog:Boolean) {
        this.showLog = showLog
    }

    fun get(url: String, tag: Any) = request(com.android.volley.Request.Method.GET, url, tag)

    fun post(url: String, tag: Any) = request(com.android.volley.Request.Method.POST, url, tag)

    fun cancelAll(tag: Any) = requestQueues.forEach { it.cancelAll(tag) }

    internal fun getCharset() = charset

    internal fun isShowLog() = showLog

    private fun request(method: Int, url: String, tag: Any): Request {
        val headers = HashMap<String, Any>()
        headers.putAll(globalHeaders)
        val params = HashMap<String, Any>()
        params.putAll(globalParams)
        val requestQueue = getRequestQueue()
        return Request(requestQueue, executorDeliveryHandler!!, mainLooperHandler, tag, if (url.startsWith("http", true)) url else "$routing$url", method, headers, params, enableGZIP, preRequestCallback, onResponseCallback)
    }

    private fun listeningActivityLifecycle(application: Application) = application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {

        override fun onActivityDestroyed(activity: Activity) = cancelAll(activity)

        override fun onActivityStopped(activity: Activity) = Unit

        override fun onActivityPaused(activity: Activity) = Unit

        override fun onActivityStarted(activity: Activity) = Unit

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

        override fun onActivityResumed(activity: Activity) = Unit

    })

    private fun createRequestQueues() {
        val lock = Object()
        val thread = Thread {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
            Looper.prepare()
            executorDeliveryHandler = Handler(Looper.myLooper()!!)
            synchronized(lock) { lock.notifyAll() }
            Looper.loop()
        }
        thread.isDaemon = true
        thread.name = "VolleyResponseDeliveryThread"
        thread.start()
        while (true) {
            synchronized(lock) {
                if (executorDeliveryHandler != null) {
                    val requestQueueCount = (maxRequestQueueCount + 1) / 2
                    for (i in 0 until requestQueueCount)
                        createRequestQueue()
                    return
                }
                lock.wait(16)
            }
        }
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