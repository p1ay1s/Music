package com.niki.base.util

import com.niki.base.baseUrl
import com.niki.base.log.logE
import com.niki.base.log.logI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.util.concurrent.TimeUnit

const val ON_FAILURE_CODE = -1

/**
 * 摘自招新系统，略有修改
 */
object ServiceBuilder {
    interface ConnectivityService {
        @GET("/")
        suspend fun checkConnectivity(): Response<Unit>
    }

    private const val TIMEOUT_SET = 15L

    private val client = OkHttpClient.Builder()
        .readTimeout(TIMEOUT_SET, TimeUnit.SECONDS)
        .connectTimeout(TIMEOUT_SET, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client) //通过 client 将超时设置为 15s 以防止服务器响应较慢获取不了数据的情况
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    fun retrofitBuilder(baseUrl: String) = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    /**
     * 使用 ServiceBuilder.create(XXXService::class.java) 返回一个 Service 代理对象
     */
    fun <T> create(serviceClass: Class<T>): T = retrofit.create(serviceClass)

    /**
     * 使用 ServiceBuilder.create<XXXService> 返回一个 Service 代理对象
     */
    inline fun <reified T> create(): T = create(T::class.java)

    /**
     * 检测连通性
     */
    fun ping(url: String, callback: (Boolean) -> Unit) = CoroutineScope(Dispatchers.Main).launch {
        try {
            val connectivityService = retrofitBuilder(url).create(ConnectivityService::class.java)
            val response = connectivityService.checkConnectivity()
            callback(response.isSuccessful)
        } catch (_: Exception) {
            callback(false)
        }
    }

    /**
     * 在 model 层确定 T 的类型，进一步回调给 viewModel 层
     *
     * @param onSuccess 包含返回体
     * @param onError 包含状态码以及信息
     */
    inline fun <reified T> makeRequest(
        call: Call<T>,
        crossinline onSuccess: (data: T) -> Unit,
        crossinline onError: ((code: Int, msg: String) -> Unit)
    ) = call.enqueue(object : Callback<T> {
        override fun onResponse(call: Call<T>, response: Response<T>) {
            val url = call.request().url().toString()
            response.apply {
                logI("ServiceBuilder", message() + "\n at: " + url)
                when {
                    isSuccessful && body() != null -> onSuccess(body()!!)
                    isSuccessful && body() == null -> onError(ON_FAILURE_CODE, "请求超时")
                    else -> onError(code(), message())
                }
            }
        }

        override fun onFailure(call: Call<T>, t: Throwable) {
            val url = call.request().url().toString()
            logE("ServiceBuilder", t.message.toString() + "\n at: " + url)
            onError(ON_FAILURE_CODE, t.message.toString())
        }
    })
}