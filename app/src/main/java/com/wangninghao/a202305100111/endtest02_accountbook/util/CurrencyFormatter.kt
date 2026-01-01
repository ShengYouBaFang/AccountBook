package com.wangninghao.a202305100111.endtest02_accountbook.util

import java.text.DecimalFormat

/**
 * 金额格式化工具类
 */
object CurrencyFormatter {

    private val decimalFormat = DecimalFormat("#,##0.00")
    private val simpleFormat = DecimalFormat("0.00")

    /**
     * 格式化金额（带千分位）
     * @return 格式: "1,234.56"
     */
    fun format(amount: Double): String {
        return decimalFormat.format(amount)
    }

    /**
     * 格式化金额（简单格式）
     * @return 格式: "1234.56"
     */
    fun formatSimple(amount: Double): String {
        return simpleFormat.format(amount)
    }

    /**
     * 格式化金额带符号
     * @param isExpense 是否为支出
     * @return 格式: "-1,234.56" 或 "+1,234.56"
     */
    fun formatWithSign(amount: Double, isExpense: Boolean): String {
        val sign = if (isExpense) "-" else "+"
        return "$sign${format(amount)}"
    }

    /**
     * 格式化金额带货币符号
     * @return 格式: "¥1,234.56"
     */
    fun formatWithCurrency(amount: Double): String {
        return "¥${format(amount)}"
    }

    /**
     * 格式化金额带货币符号和正负号
     */
    fun formatWithCurrencyAndSign(amount: Double, isExpense: Boolean): String {
        val sign = if (isExpense) "-" else "+"
        return "$sign¥${format(amount)}"
    }

    /**
     * 解析金额字符串
     */
    fun parse(amountStr: String): Double? {
        return try {
            amountStr.replace(",", "").replace("¥", "").replace("+", "").replace("-", "").toDouble()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 验证金额字符串格式
     */
    fun isValidAmount(amountStr: String): Boolean {
        if (amountStr.isBlank()) return false
        return try {
            val amount = amountStr.toDouble()
            amount > 0
        } catch (e: Exception) {
            false
        }
    }
}
