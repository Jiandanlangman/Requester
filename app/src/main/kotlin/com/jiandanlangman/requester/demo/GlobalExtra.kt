package com.jiandanlangman.requester.demo

import com.jiandanlangman.requester.Requester

fun BaseActivity.post(url:String) = Requester.post(url, this)

fun BaseActivity.get(url:String) = Requester.get(url, this)