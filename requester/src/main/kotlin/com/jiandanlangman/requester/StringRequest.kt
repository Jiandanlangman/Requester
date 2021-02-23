package com.jiandanlangman.requester

import androidx.core.util.Pools
import com.android.volley.NetworkResponse
import com.android.volley.Response
import com.android.volley.toolbox.HttpHeaderParser
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.lang.ref.SoftReference
import java.nio.charset.Charset
import java.util.zip.GZIPInputStream

internal open class StringRequest(private val charset: String, method: Int, enableGZIP: Boolean, url: String, headers: Map<String, String>, private val params: Map<String, String>, private val body: ByteArray?, listener: Response.Listener<String>?, errorListener: Response.ErrorListener) : com.android.volley.toolbox.StringRequest(method, url, listener, errorListener) {

    private companion object {
        val bufferPool = Pools.SynchronizedPool<SoftReference<ByteArray>>(8)
    }

    private val headers = HashMap<String, String>()

    init {
        this.headers.putAll(headers)
        this.headers["Accept-Charset"] = charset
        if (enableGZIP)
            this.headers["Accept-Encoding"] = "gzip"
    }

    override fun getParams() = params

    override fun getHeaders() = headers

    override fun getBody(): ByteArray = body ?: super.getBody()

    override fun parseNetworkResponse(response: NetworkResponse): Response<String> {
        if ("gzip".equals(response.headers["Content-Encoding"], true))
            return Response.success(String(uncompress(response.data), Charset.forName(HttpHeaderParser.parseCharset(response.headers))), HttpHeaderParser.parseCacheHeaders(response))
        return super.parseNetworkResponse(response)
    }

    override fun getParamsEncoding() = charset

    private fun uncompress(src: ByteArray): ByteArray {
        val baos = ByteArrayOutputStream()
        val gzis = GZIPInputStream(ByteArrayInputStream(src))
        val buffer = getBuffer()
        var readLength: Int
        while (gzis.read(buffer).also { readLength = it } != -1)
            baos.write(buffer, 0, readLength)
        gzis.close()
        releaseBuffer(buffer)
        return baos.toByteArray()
    }

    private fun getBuffer(): ByteArray {
        var buffer: ByteArray? = null
        while (true) {
            val sr = bufferPool.acquire() ?: break
            buffer = sr.get()
            if (buffer != null)
                break
        }
        return buffer ?: ByteArray(2048)
    }

    private fun releaseBuffer(buffer: ByteArray) = bufferPool.release(SoftReference(buffer))

}