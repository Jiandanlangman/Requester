package com.jiandanlangman.requester.demo

import com.jiandanlangman.requester.ParsedData


data class UserList(val userList:List<User>) : ParsedData {
    data class User(val uid:Int, val avatar:String, val nickname:String, val gender:Int, val age:Int)
}