package com.jiandanlangman.requester

import com.android.volley.toolbox.HurlStack
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLSocketFactory

class HostnameVerifierHurlStack(private val hostnameVerifier: HostnameVerifier, urlRewriter: UrlRewriter?, sSLSocketFactory: SSLSocketFactory?) : HurlStack(urlRewriter, sSLSocketFactory) {

    override fun createConnection(url: URL?): HttpURLConnection {
        val httpURLConnection = super.createConnection(url)
        if (httpURLConnection is HttpsURLConnection)
            httpURLConnection.hostnameVerifier = hostnameVerifier
        return httpURLConnection
    }

}