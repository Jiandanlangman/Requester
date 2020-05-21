package com.jiandanlangman.requester

import com.google.gson.GsonBuilder

internal object JSONUtil {

    private val gson = GsonBuilder().create()

    internal fun <T> fromJSON(json: String?, clazz: Class<T>?) = gson.fromJson(json, clazz)


}