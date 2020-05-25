package com.jiandanlangman.requester

import java.io.InputStream
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

internal object HTTPSManager {

    fun buildSSLSocketFactory(inputStream: InputStream?): SSLSocketFactory {
        val sslContext = SSLContext.getInstance("TLS")
        if (inputStream != null) {
            val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
            keyStore.load(null, null)
            val certificate = CertificateFactory.getInstance("X.509").generateCertificate(inputStream)
            keyStore.setCertificateEntry("ca", certificate)
            val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            tmf.init(keyStore)
            sslContext.init(null, tmf.trustManagers, null)
        }
        sslContext.init(null, arrayOf(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
            override fun getAcceptedIssuers() = null
        }), null)
        return sslContext.socketFactory
    }

}