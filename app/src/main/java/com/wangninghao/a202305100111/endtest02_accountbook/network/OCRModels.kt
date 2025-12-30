package com.wangninghao.a202305100111.endtest02_accountbook.network

import com.google.gson.annotations.SerializedName

/**
 * AccessToken响应
 */
data class AccessTokenResponse(
    @SerializedName("access_token")
    val accessToken: String?,
    @SerializedName("expires_in")
    val expiresIn: Long?,
    @SerializedName("error")
    val error: String?,
    @SerializedName("error_description")
    val errorDescription: String?
)

/**
 * 购物小票OCR响应
 */
data class ReceiptOCRResponse(
    @SerializedName("log_id")
    val logId: Long?,
    @SerializedName("words_result")
    val wordsResult: List<ReceiptResult>?,
    @SerializedName("error_code")
    val errorCode: Int?,
    @SerializedName("error_msg")
    val errorMsg: String?
)

/**
 * 小票识别结果
 */
data class ReceiptResult(
    @SerializedName("shop_name")
    val shopName: List<WordItem>?,
    @SerializedName("total_amount")
    val totalAmount: List<WordItem>?,
    @SerializedName("paid_amount")
    val paidAmount: List<WordItem>?,
    @SerializedName("consumption_date")
    val consumptionDate: List<WordItem>?,
    @SerializedName("consumption_time")
    val consumptionTime: List<WordItem>?,
    @SerializedName("discount")
    val discount: List<WordItem>?,
    @SerializedName("receipt_num")
    val receiptNum: List<WordItem>?,
    @SerializedName("machine_num")
    val machineNum: List<WordItem>?,
    @SerializedName("employee_num")
    val employeeNum: List<WordItem>?,
    @SerializedName("change")
    val change: List<WordItem>?,
    @SerializedName("currency")
    val currency: List<WordItem>?,
    @SerializedName("print_date")
    val printDate: List<WordItem>?,
    @SerializedName("print_time")
    val printTime: List<WordItem>?,
    @SerializedName("table")
    val table: List<TableItem>?,
    @SerializedName("table_row_num")
    val tableRowNum: Int?
)

/**
 * 单词项
 */
data class WordItem(
    @SerializedName("word")
    val word: String?
)

/**
 * 商品明细项
 */
data class TableItem(
    @SerializedName("product")
    val product: WordItem?,
    @SerializedName("quantity")
    val quantity: WordItem?,
    @SerializedName("unit_price")
    val unitPrice: WordItem?,
    @SerializedName("subtotal_amount")
    val subtotalAmount: WordItem?
)

/**
 * OCR解析后的简化结果
 */
data class OCRParsedResult(
    val totalAmount: Double?,
    val paidAmount: Double?,           // 实收金额
    val shopName: String?,
    val date: String?,                 // 消费日期
    val consumptionTime: String?,      // 消费时间
    val discount: String?,             // 折扣/优惠
    val receiptNum: String?,           // 小票号
    val change: String?,               // 找零
    val currency: String?,             // 币种
    val items: List<OCRItem>,
    val success: Boolean,
    val errorMessage: String? = null
)

/**
 * OCR商品项
 */
data class OCRItem(
    val name: String,
    val quantity: String,
    val unitPrice: String,
    val subtotal: String
)
