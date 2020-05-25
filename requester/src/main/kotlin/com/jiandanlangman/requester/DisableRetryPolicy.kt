package com.jiandanlangman.requester

import com.android.volley.DefaultRetryPolicy

internal class DisableRetryPolicy(private val initialTimeoutMs: Int) : DefaultRetryPolicy(initialTimeoutMs, DEFAULT_MAX_RETRIES, DEFAULT_BACKOFF_MULT) {

    override fun getCurrentRetryCount() = 0

    override fun hasAttemptRemaining() = false

    override fun getCurrentTimeout() = initialTimeoutMs

}