package com.jiandanlangman.requester

interface DataParser {

    fun <T> parseData(json: String, clazz: Class<T>): T

}