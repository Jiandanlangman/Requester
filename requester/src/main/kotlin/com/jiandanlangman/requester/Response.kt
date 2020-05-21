package com.jiandanlangman.requester

data class Response<T> (val requestErrorCode : ErrorCode, val responseData: String, val parsedData: T)