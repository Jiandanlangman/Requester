package com.jiandanlangman.requester

enum class ErrorCode {
    NO_ERROR,  //成功
    NO_RESPONSE_DATA, //数据为空
    REQUEST_FAILED,  //请求失败
    PARSE_DATA_ERROR, //解析数据失败
    CUSTOM_ERROR       //自定义失败
}
