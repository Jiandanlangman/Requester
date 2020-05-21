package com.jiandanlangman.requester.demo


data class UserList(val userList:List<User>)  {
    data class User(val uid:Int, val avatar:String, val nickname:String, val gender:Int, val age:Int)
}