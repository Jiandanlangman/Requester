package com.jiandanlangman.requester

import com.google.gson.GsonBuilder

internal class GSONDataParser : DataParser {

    private val gson = GsonBuilder().create()

    override fun <T> parseData(json: String, clazz: Class<T>) = gson.fromJson(json, clazz)

}