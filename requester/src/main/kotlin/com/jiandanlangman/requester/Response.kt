package com.jiandanlangman.requester

data class Response<T> (val requestErrorCode : ErrorCode, val isCache:Boolean,  val responseData: String, val parsedData: T)