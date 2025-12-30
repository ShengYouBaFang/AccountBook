package com.wangninghao.a202305100111.endtest02_accountbook.network

import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * 百度OCR API接口定义
 */
interface BaiduOCRApi {

    /**
     * 获取AccessToken
     * @param grantType 固定为 client_credentials
     * @param clientId API Key
     * @param clientSecret Secret Key
     */
    @POST("oauth/2.0/token")
    suspend fun getAccessToken(
        @Query("grant_type") grantType: String = "client_credentials",
        @Query("client_id") clientId: String,
        @Query("client_secret") clientSecret: String
    ): AccessTokenResponse

    /**
     * 购物小票识别
     * @param accessToken 访问令牌
     * @param image Base64编码后进行URLEncode的图片数据
     */
    @FormUrlEncoded
    @POST("rest/2.0/ocr/v1/shopping_receipt")
    suspend fun recognizeReceipt(
        @Query("access_token") accessToken: String,
        @Field("image") image: String
    ): ReceiptOCRResponse
}
