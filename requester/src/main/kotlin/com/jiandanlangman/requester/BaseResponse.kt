package com.jiandanlangman.requester

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

open class BaseResponse {

    @Expose
    var requestErrorCode = ErrorCode.NO_ERROR

    @Expose
    var responseData: String = "{}"


    override fun toString() = "BaseResponseEntity(requestErrorCode=$requestErrorCode, value='$responseData')"


}