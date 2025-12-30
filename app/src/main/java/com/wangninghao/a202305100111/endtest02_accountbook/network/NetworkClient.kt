package com.wangninghao.a202305100111.endtest02_accountbook.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * 网络客户端单例
 */
object NetworkClient {

    private const val BASE_URL = "https://aip.baidubce.com/"

    // API密钥
    const val API_KEY = "Y7CEeHhssMUhvZw2FK92m00k"
    const val SECRET_KEY = "gXmqO65G8x1MyfH2QkRHLcewD4o1Cgyu"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val baiduOCRApi: BaiduOCRApi = retrofit.create(BaiduOCRApi::class.java)
}
