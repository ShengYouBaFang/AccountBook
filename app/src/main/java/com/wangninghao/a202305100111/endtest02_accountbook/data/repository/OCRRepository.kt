package com.wangninghao.a202305100111.endtest02_accountbook.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.wangninghao.a202305100111.endtest02_accountbook.network.NetworkClient
import com.wangninghao.a202305100111.endtest02_accountbook.network.OCRItem
import com.wangninghao.a202305100111.endtest02_accountbook.network.OCRParsedResult

/**
 * OCR识别仓库
 */
class OCRRepository(context: Context) {

    private val api = NetworkClient.baiduOCRApi
    private val prefs: SharedPreferences = context.getSharedPreferences("ocr_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_TOKEN_EXPIRE_TIME = "token_expire_time"
    }

    /**
     * 获取AccessToken（带缓存）
     */
    suspend fun getAccessToken(): String? {
        // 检查缓存的token是否有效
        val cachedToken = prefs.getString(KEY_ACCESS_TOKEN, null)
        val expireTime = prefs.getLong(KEY_TOKEN_EXPIRE_TIME, 0)

        if (cachedToken != null && System.currentTimeMillis() < expireTime) {
            return cachedToken
        }

        // 重新获取token
        return try {
            val response = api.getAccessToken(
                clientId = NetworkClient.API_KEY,
                clientSecret = NetworkClient.SECRET_KEY
            )

            if (response.accessToken != null) {
                // 缓存token，提前1小时过期以确保安全
                val newExpireTime = System.currentTimeMillis() + (response.expiresIn ?: 0) * 1000 - 3600000
                prefs.edit()
                    .putString(KEY_ACCESS_TOKEN, response.accessToken)
                    .putLong(KEY_TOKEN_EXPIRE_TIME, newExpireTime)
                    .apply()
                response.accessToken
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 识别购物小票
     * @param base64Image Base64编码的图片（已进行URLEncode）
     */
    suspend fun recognizeReceipt(base64Image: String): OCRParsedResult {
        return try {
            val token = getAccessToken()
            if (token == null) {
                return OCRParsedResult(
                    totalAmount = null,
                    paidAmount = null,
                    shopName = null,
                    date = null,
                    consumptionTime = null,
                    discount = null,
                    receiptNum = null,
                    change = null,
                    currency = null,
                    items = emptyList(),
                    success = false,
                    errorMessage = "获取AccessToken失败"
                )
            }

            val response = api.recognizeReceipt(
                accessToken = token,
                image = base64Image
            )

            // 检查错误
            if (response.errorCode != null) {
                return OCRParsedResult(
                    totalAmount = null,
                    paidAmount = null,
                    shopName = null,
                    date = null,
                    consumptionTime = null,
                    discount = null,
                    receiptNum = null,
                    change = null,
                    currency = null,
                    items = emptyList(),
                    success = false,
                    errorMessage = "OCR识别失败: ${response.errorMsg}"
                )
            }

            // 解析结果
            val result = response.wordsResult?.firstOrNull()
            if (result == null) {
                return OCRParsedResult(
                    totalAmount = null,
                    paidAmount = null,
                    shopName = null,
                    date = null,
                    consumptionTime = null,
                    discount = null,
                    receiptNum = null,
                    change = null,
                    currency = null,
                    items = emptyList(),
                    success = false,
                    errorMessage = "未识别到小票信息"
                )
            }

            // 提取金额（优先使用total_amount和paid_amount中数值较大的）
            val totalAmountStr = result.totalAmount?.firstOrNull()?.word
            val paidAmountStr = result.paidAmount?.firstOrNull()?.word
            val totalAmountValue = totalAmountStr?.toDoubleOrNull()
            val paidAmountValue = paidAmountStr?.toDoubleOrNull()

            // 取两个金额中较大的值，防止某个字段为空导致识别失败
            val totalAmount = when {
                totalAmountValue != null && paidAmountValue != null -> maxOf(totalAmountValue, paidAmountValue)
                totalAmountValue != null -> totalAmountValue
                paidAmountValue != null -> paidAmountValue
                else -> null
            }

            // 提取店名
            val shopName = result.shopName?.firstOrNull()?.word

            // 提取日期
            val date = result.consumptionDate?.firstOrNull()?.word

            // 提取消费时间
            val consumptionTime = result.consumptionTime?.firstOrNull()?.word

            // 提取其他字段
            val discount = result.discount?.firstOrNull()?.word
            val receiptNum = result.receiptNum?.firstOrNull()?.word
            val change = result.change?.firstOrNull()?.word
            val currency = result.currency?.firstOrNull()?.word

            // 提取商品明细
            val items = result.table?.mapNotNull { item ->
                val productName = item.product?.word ?: return@mapNotNull null
                OCRItem(
                    name = productName,
                    quantity = item.quantity?.word ?: "",
                    unitPrice = item.unitPrice?.word ?: "",
                    subtotal = item.subtotalAmount?.word ?: ""
                )
            } ?: emptyList()

            OCRParsedResult(
                totalAmount = totalAmount,
                paidAmount = paidAmountValue,
                shopName = shopName,
                date = date,
                consumptionTime = consumptionTime,
                discount = discount,
                receiptNum = receiptNum,
                change = change,
                currency = currency,
                items = items,
                success = true
            )

        } catch (e: Exception) {
            e.printStackTrace()
            OCRParsedResult(
                totalAmount = null,
                paidAmount = null,
                shopName = null,
                date = null,
                consumptionTime = null,
                discount = null,
                receiptNum = null,
                change = null,
                currency = null,
                items = emptyList(),
                success = false,
                errorMessage = "网络请求失败: ${e.message}"
            )
        }
    }
}
