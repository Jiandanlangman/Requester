package com.jiandanlangman.requester

interface CacheManager {


    fun get(url: String, headers: HashMap<String, String>, params: HashMap<String, String>): String?

    fun put(url: String, headers: HashMap<String, String>, params: HashMap<String, String>, responseData: String)

}