package com.jiandanlangman.requester

import com.android.volley.Request
import com.android.volley.toolbox.HttpResponse
import com.android.volley.toolbox.HurlStack
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLSocketFactory

internal object HostnameVerifierHurlStackCreator : HttpStackCreator {

    private val hostnameVerifier = HostnameVerifier { _, _ -> true }

    override fun create(sslSocketFactory: SSLSocketFactory, dns: DNS?) = HostnameVerifierHurlStack(hostnameVerifier, null, sslSocketFactory, dns)

    internal class HostnameVerifierHurlStack(private val hostnameVerifier: HostnameVerifier, urlRewriter: UrlRewriter?, sSLSocketFactory: SSLSocketFactory?, private val dns: DNS?) : HurlStack(urlRewriter, sSLSocketFactory) {

        override fun executeRequest(request: Request<*>, additionalHeaders: MutableMap<String, String>): HttpResponse {
            request.headers["Host"] = URL(request.url).host
            return super.executeRequest(request, additionalHeaders)
        }

        override fun createConnection(url: URL): HttpURLConnection {
            val host = url.host
            val iNetAddressList = dns?.lookup(host)
            val ip = if (iNetAddressList.isNullOrEmpty()) null else iNetAddressList[0].hostAddress
            val requestUrl = ip?.let { URL(url.toString().replace(host, ip)) } ?: url
            val httpURLConnection = super.createConnection(requestUrl)
            if (httpURLConnection is HttpsURLConnection)
                httpURLConnection.hostnameVerifier = hostnameVerifier
            return httpURLConnection
        }

    }
}