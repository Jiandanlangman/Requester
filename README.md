# Requester
基于Volley的网络请求工具  
为什么还用Volley？因为Volley用起来爽，HttpURLConnection用起来也爽。还有很轻量，Volley
90KB + 本工具10KB = 100KB的代码量  
不过官方已经很久不维护Volley了，使用方式在现在看来也已经非常过时了。本工具就是在Volley上进行二次封装，优化了使用方式和加入了一些小功能，新特性如下：
- 将使用方式改为更加方便直观的链式调用
- 可根据使用情况自动增加或减少最大并发量
- 将数据解析和请求绑在一起，请求直接返回最终mode类(默认使用GSON，可自定义解析器)
- 性能优化，将一些没必要在主线程操作的事件移动到了子线程
- 更加方便的和Activity/Fragment等控件生命周期绑定
- 新增信任所有Https证书和导入指定证书的API
- GZIP支持(需要后端支持，否则打开也无效)
- 更加方便的设置公共参数、公共请求头等
- 开始请求前的统一回调(可用于根据参数计算签名等)
- 请求返回的统一回调(可用于通用错误信息处理等)
- 不用检查null，拿到返回值直接就开干，怎么搞都不会崩
- 支持设置默认路由
- 更加直观的日志，和开启日志的方法
- 其它更多特性

该控件使用Kotlin编写，所以你的项目必须接入Kotlin才能正常使用

一个简单的网络请求:
```
Requester.post("user/list", "tag")
         .addParam("page_size", 20)
         .addParam("page", 1)
         .addParam("created_from", "home")
         .addHeader("device", "test")
         .start(UserList:class.java) { response ->

         }
```
支持同步请求(同步请求不要在主线程做):
```
val response =  Requester.post("user/list", "tag")
         .addParam("page_size", 20)
         .addParam("page", 1)
         .addParam("created_from", "home")
         .sync(UserList:class.java)
```
也支持不指定类型的请求:
```
Requester.post("user/list", "tag")
         .addParam("page_size", 20)
         .addParam("page", 1)
         .addParam("created_from", "home")
         .start { response ->
             //不指定类型的请求可以通过response.responseData取得服务器返回的字符串
         }
```
还支持不取返回值的请求:
```
Requester.post("user/list", "tag")
         .addParam("page_size", 20)
         .addParam("page", 1)
         .addParam("created_from", "home")
         .start()
```
GET方式请求也是一样的方便:
```
Requester.get("user/list", "tag")
         .addParam("page_size", 20)
         .addParam("page", 1)
         .addParam("created_from", "home")
         .start()
```
### 接入方式
- 下载源码，将名为requester的module加入到你的工程，或者将module打包成aar格式
- maven方式接入
    编辑你Project的build.gradle文件
    ```
    allprojects {
        repositories {
            google()
            jcenter()
            mavenCentral()
            maven { url "http://101.132.235.215/repor" } //加入这一行
        }
    }
    ```
    然后编辑你app module的build.gradle文件，在dependencies节点下加入
    ```
    implementation "com.jiandanlangman:requester:1.0.0@aar"  //主依赖
    implementation "com.android.volley:volley:1.1.1"         //Volley，既然是基于Volley的，那肯定需要它
    implementation "com.google.code.gson:gson:2.8.6"         //GSON，json解析工具，必须的
    ```
### 主要API说明
```
/**
 * 初始化方法，必须调用
 * 虽然请求队列的数量是根据实际使用量自动增加或减少的，但maxRequestQueueCount控制着最大请求队列数和初始请求队列数。请根据实际情况设置，避免资源浪费
 * @param application 应用程序上下文
 * @param maxRequestQueueCount 最多能有多少个请求队列，1个请求队列的最大并发为4，2个请求队列的最大并发则为2*4=8
 * @param certInputStream 自定义证书文件的输入流，不传和传null表示信任所有证书
 */
Requester.init(application: Application, maxRequestQueueCount: Int, certInputStream: InputStream? = null)


//创建一个post请求，tag为标记，可用于取消请求，如果你不需要取消请求，随便传
Requester.post(url: String, tag: Any) : Request


//创建一个get请求，tag为标记，可用于取消请求，如果你不需要取消请求，随便传
Requester.get(url: String, tag: Any) : Request


//设置字符编码，默认为UTF-8，如果你需要的编码也是UTF-8，则无需设置
Requester.setCharset(charset: Charset)


//设置请求默认路由，及URL前缀，如果不设置，则发起网络请求必须使用绝对地址
Requester.setDefaultRouting(url:String)


//是否打印请求日志，推荐Release包不要打印
Requester.showLog(showLog:Boolean)


//设置全局参数，及所有的请求都会默认带上这些参数，这些参数可以被Request的addParam、Requester.setPreRequestCallback替换掉。如果你的请求没有全局参数，可以不调用这个方法
Requester.setGlobalParams(params: Map<String, String>)


//更新单个全局参数，如果value为null，则表示删除这个全局参数
Requester.updateGlobalParam(key: String, value: String?)


//设置全局请求头，使用方法和设置全局参数一样
Requester.setGlobalHeaders(headers: Map<String, String>)


//更新单个请求头，使用方法和更新单个参数一样
Requester.updateGlobalHeader(key: String, value: String?)


//启用GZIP压缩(推荐使用)。此功能需要后端支持，开启后将节省大量的流量并加快响应速度。如果后端不支持，这个功能将失效
Requester.enableGZIP(enable: Boolean)


//设置超时时间，单位毫秒(ms)
Requester.setTimeout(timeoutMs: Int)


//设置自定义数据解析器(json解析)，默认解析器为GSON。如果你的工程里没有接入GSON，则必须在初始化之后立刻调用这个方法设置一个自定义解析器
Requester.setDataParser(dataParser: DataParser)


//当请求即将发送时，会自动回调这个callback，callback中的header, params都已经带上了所有参数。你可以在这个回调中对参数进行修改操作，比如加入新的参数，根据参数计算签名等
Requester.setOnPreRequestCallback(callback: ((url: String, headers: HashMap<String, String>, params: HashMap<String, String>) -> Unit)?)


 /**
  * 所有的请求返回到调用处之前，都会先回调到这个方法。你可以在这个方法里面做一些统一处理，比如判断返回数据的状态等
  * 回调参数说明:
  *   it.requestErrorCode如果等于ErrorCode.NO_ERROR，仅表示请求过程中没有错误发生，即请求正常发送到了后端，后端正常返回了结果。并不能用来表示后端返回数据的正确性
  *   更多错误码请参考ErrorCode枚举
  * 回调返回值说明:
  *  返回true则最终返回到调用者处的ErrorCode不会发生变化
  *  返回false则最终返回到调用者处的ErrorCode会变为ErrorCode.CUSTOM_ERROR，表示全局回调已经处理并确认这个数据是错误的
  */
Requester.setOnResponseListener(listener: ((BaseResponse) -> Boolean)?)


//取消这个tag的所有网络请求，一般用在和Activity/Fragment生命周期绑定
Requester. cancelAll(tag: Any)




//添加请求头
Request.addHeader(field: String, value: Any?): Request


//添加参数
Request.addParam(field: String, value: Any?): Request


//开关单个请求的GZIP压缩
Request.enableGZIP(enable: Boolean): Request


//设置单个请求的超时时间，单位毫秒(ms)
Request.setTimeout(timeoutMs: Int): Request


//同步开始网络请求
Request.sync() : BaseResponseEntity
<T : BaseResponse> Request.sync(type: Class<T>): T


//异步开始网络请求
Request.start()
Request.start(listener: (response: BaseResponse) -> Unit)
<T : BaseResponse> Request.start(type: Class<T>, listener: (response: T) -> Unit) 
```
### 更多
全局扩展、与Activity/Fragment生命周期绑定、详细使用方法及说明请参考Demo