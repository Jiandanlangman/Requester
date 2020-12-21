package com.jiandanlangman.requester.demo

import android.app.Application
import android.content.Context
import android.os.Build
import com.jiandanlangman.requester.DNS
import com.jiandanlangman.requester.ErrorCode
import com.jiandanlangman.requester.Requester
import org.json.JSONObject
import java.lang.reflect.Method
import java.nio.charset.Charset

class DemoApplication : Application() {


    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        exemptionAllHiddenApis()
    }


    private fun exemptionAllHiddenApis() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val getDeclaredMethodMethod = Class::class.java.getDeclaredMethod("getDeclaredMethod", String::class.java, arrayOf(Class::class.java)::class.java)
                val vmRuntimeClass = Class::class.java.getDeclaredMethod("forName", String::class.java).invoke(null, "dalvik.system.VMRuntime")
                (getDeclaredMethodMethod.invoke(vmRuntimeClass, "setHiddenApiExemptions", arrayOf(Array<String>::class.java)) as Method).invoke((getDeclaredMethodMethod.invoke(vmRuntimeClass, "getRuntime", null) as Method).invoke(null), arrayOf("L"))
            } catch (ignore: Throwable) {

            }
        }
    }


    override fun onCreate() {
        super.onCreate()

        /**
         * 初始化方法，必须调用
         * 虽然请求队列的数量是根据实际使用量自动增加或减少的，但maxRequestQueueCount控制着最大请求队列数和初始请求队列数。请根据实际情况设置，避免资源浪费
         * @param application 应用程序上下文
         * @param maxRequestQueueCount 最多能有多少个请求队列，1个请求队列的最大并发为4，2个请求队列的最大并发则为2*4=8
         * @param certInputStream 自定义证书文件的输入流，不传和传null表示信任所有证书
         */
        Requester.init(this, 2)

        //初始化Requester并指定Https证书
//        val inputStream = assets.open("cert.cer")
//        Requester.init(this, 2, inputStream)
//        inputStream.close()

        //字符编码，默认为UTF-8，如果你需要的编码也是UTF-8，则无需设置
        Requester.setCharset("UTF-8")

        //设置请求默认路由，及URL前缀，如果不设置，则发起网络请求必须使用绝对地址
        //如果你设置的是: "https://www.baidu.com/"，后续请求时只需传入接口名称即可。比如: "test?a=b"，则实际请求的地址是: "https://www.baidu.com/test?a=b"
        Requester.setDefaultRouting("https://www.baidu.com/")

        //是否打印请求日志，推荐Release包不要打印
        Requester.showLog(BuildConfig.DEBUG)

        val globalParams = HashMap<String, String>()
        globalParams["p"] = "android"
        globalParams["_lang"] = "CN_zh-Hans"
        globalParams["c"] = "google"
        globalParams["v"] = "1015"
        globalParams["n"] = "xxx"
        globalParams["_uid"] = "14043"
        globalParams["token"] = "12595d2aX9eccY4bcaY8d78Ya64218aa2f91"
        //设置全局参数，及所有的请求都会默认带上这些参数，这些参数可以被Request的addParam、Requester.setPreRequestCallback替换掉
        //如果你的请求没有全局参数，可以不调用这个方法
        Requester.setGlobalParams(globalParams)

        //更新单个全局参数，如果value为null，则表示删除这个全局参数
//        Requester.updateGlobalParam("uid", "45678")

        //设置和更新全局请求头，使用方法和设置全局参数一模一样
        //如果你的请求没有全局请求头，可以不调用这个方法
//        val globalHeaders = HashMap<String, String>()
//        globalHeaders["device"] = "moniqi"
//        Requester.setGlobalHeaders(globalHeaders)
//        Requester.updateGlobalHeader("device", "aaaaa")

        //启用GZIP压缩(推荐使用)
        //这个功能需要后端支持，如果后端支持，开启这个功能后将节省大量的流量(节省10倍)并加快响应速度。如果后端不支持，前端开启这个功能也没什么大碍，不会影响使用
        Requester.enableGZIP(true)

        //当请求即将发送时，会自动回调这个方法，这个方法中的header, params都已经带上了所有参数。你可以在这个回调中对参数进行修改操作，比如加入新的参数，校验签名等
        Requester.setOnPreRequestCallback { url, method, headers, params ->
            params["t"] = System.currentTimeMillis().toString()
            val sign = (params["t"] + params["uid"] + params["token"]).hashCode().toString()
            params["sign"] = sign
        }

        //所有的请求返回到调用处之前，都会先回调到这个方法。你可以在这个方法里面做一些统一处理，比如判断返回数据的状态等
        Requester.setOnResponseListener {
            /**
             * 回调返回值说明:
             *  返回true则最终返回到调用者处的ErrorCode不会发生变化
             *  返回false则最终返回到调用者处的ErrorCode会变为ErrorCode.CUSTOM_ERROR，表示全局回调已经处理并确认这个数据是错误的
             */

            //ErrorCode.NO_ERROR表示请求过程中没有错误发生，即请求正常发送到了后端，后端正常返回了结果。并不能用来表示后端返回数据的正确性
            //更多错误码请参考ErrorCode枚举
            if (it.requestErrorCode == ErrorCode.NO_ERROR) {
                //responseData表示服务器返回的原始数据
                val jsonObject = JSONObject(it.responseData)
                when (jsonObject.optString("status")) {
                    "ok" -> {
                        //请求正常，数据正常
                        return@setOnResponseListener true
                    }
                    "no_coins" -> {
                        //弹出充值框
                        return@setOnResponseListener false
                    }
                    "no_login" -> {
                        //弹出登录框
                        return@setOnResponseListener false
                    }
                    else -> {
                        //弹出错误提示
//                        showToast(jsonObject.optString("err_msg"))
                        return@setOnResponseListener false
                    }
                }
            }
            true
        }

        //取消这个tag的所有网络请求
        Requester.cancelAll("aaaaa")
    }



}