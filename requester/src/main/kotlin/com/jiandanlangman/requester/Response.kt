package com.jiandanlangman.requester

data class Response<T>(val requestErrorCode: ErrorCode, val isCache: Boolean, val responseData: String, val parsedData: T, val requestTime: Long, val responseTime: Long, val url: String, val headers: Map<String, String>, val params: Map<String, String>, val error: Throwable? = null)