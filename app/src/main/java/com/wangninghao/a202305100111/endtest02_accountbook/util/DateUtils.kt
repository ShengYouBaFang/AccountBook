package com.wangninghao.a202305100111.endtest02_accountbook.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 日期工具类
 */
object DateUtils {

    private val monthFormat = SimpleDateFormat("yyyy-MM", Locale.CHINA)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.CHINA)
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
    private val fullFormat = SimpleDateFormat("yyyy年MM月dd日 HH:mm", Locale.CHINA)

    /**
     * 获取当前月份字符串
     * @return 格式: "2025-01"
     */
    fun getCurrentMonth(): String {
        return monthFormat.format(Date())
    }

    /**
     * 获取月份的起止时间戳范围
     * @param month 格式: "2025-01"
     * @return Pair(开始时间戳, 结束时间戳)
     */
    fun getMonthRange(month: String): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        try {
            monthFormat.parse(month)?.let {
                calendar.time = it
            }
        } catch (e: Exception) {
            // 解析失败使用当前月份
        }

        // 设置为月份第一天的0点
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis

        // 设置为下个月第一天的0点
        calendar.add(Calendar.MONTH, 1)
        val endTime = calendar.timeInMillis

        return Pair(startTime, endTime)
    }

    /**
     * 格式化时间戳为月份字符串
     */
    fun formatMonth(timestamp: Long): String {
        return monthFormat.format(Date(timestamp))
    }

    /**
     * 格式化时间戳为日期字符串
     */
    fun formatDate(timestamp: Long): String {
        return dateFormat.format(Date(timestamp))
    }

    /**
     * 格式化时间戳为时间字符串
     */
    fun formatTime(timestamp: Long): String {
        return timeFormat.format(Date(timestamp))
    }

    /**
     * 格式化时间戳为日期时间字符串
     */
    fun formatDateTime(timestamp: Long): String {
        return dateTimeFormat.format(Date(timestamp))
    }

    /**
     * 格式化时间戳为完整中文格式
     */
    fun formatFull(timestamp: Long): String {
        return fullFormat.format(Date(timestamp))
    }

    /**
     * 获取时间戳对应的日期（天）
     */
    fun getDayOfMonth(timestamp: Long): Int {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        return calendar.get(Calendar.DAY_OF_MONTH)
    }

    /**
     * 获取某天的起始时间戳（0点）
     */
    fun getDayStartTime(timestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    /**
     * 判断两个时间戳是否是同一天
     */
    fun isSameDay(timestamp1: Long, timestamp2: Long): Boolean {
        return formatDate(timestamp1) == formatDate(timestamp2)
    }

    /**
     * 判断时间戳是否是今天
     */
    fun isToday(timestamp: Long): Boolean {
        return isSameDay(timestamp, System.currentTimeMillis())
    }

    /**
     * 判断时间戳是否是昨天
     */
    fun isYesterday(timestamp: Long): Boolean {
        val yesterday = Calendar.getInstance()
        yesterday.add(Calendar.DAY_OF_YEAR, -1)
        return isSameDay(timestamp, yesterday.timeInMillis)
    }

    /**
     * 获取友好的日期显示
     */
    fun getFriendlyDate(timestamp: Long): String {
        return when {
            isToday(timestamp) -> "今天"
            isYesterday(timestamp) -> "昨天"
            else -> {
                val sdf = SimpleDateFormat("MM月dd日", Locale.CHINA)
                sdf.format(Date(timestamp))
            }
        }
    }

    /**
     * 获取星期几
     */
    fun getWeekDay(timestamp: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SUNDAY -> "周日"
            Calendar.MONDAY -> "周一"
            Calendar.TUESDAY -> "周二"
            Calendar.WEDNESDAY -> "周三"
            Calendar.THURSDAY -> "周四"
            Calendar.FRIDAY -> "周五"
            Calendar.SATURDAY -> "周六"
            else -> ""
        }
    }

    /**
     * 解析日期字符串为时间戳
     * @param dateStr 格式: "2025-01-15" 或 "2025-1-15"
     */
    fun parseDate(dateStr: String): Long? {
        return try {
            dateFormat.parse(dateStr)?.time
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 获取月份的天数
     */
    fun getDaysInMonth(month: String): Int {
        val calendar = Calendar.getInstance()
        try {
            monthFormat.parse(month)?.let {
                calendar.time = it
            }
        } catch (e: Exception) {
            // 使用当前月份
        }
        return calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    /**
     * 获取上一个月
     */
    fun getPreviousMonth(month: String): String {
        val calendar = Calendar.getInstance()
        try {
            monthFormat.parse(month)?.let {
                calendar.time = it
            }
        } catch (e: Exception) {
            // 使用当前月份
        }
        calendar.add(Calendar.MONTH, -1)
        return monthFormat.format(calendar.time)
    }

    /**
     * 获取下一个月
     */
    fun getNextMonth(month: String): String {
        val calendar = Calendar.getInstance()
        try {
            monthFormat.parse(month)?.let {
                calendar.time = it
            }
        } catch (e: Exception) {
            // 使用当前月份
        }
        calendar.add(Calendar.MONTH, 1)
        return monthFormat.format(calendar.time)
    }

    /**
     * 获取月份的显示名称
     * @return 格式: "2025年1月"
     */
    fun getMonthDisplayName(month: String): String {
        val calendar = Calendar.getInstance()
        try {
            monthFormat.parse(month)?.let {
                calendar.time = it
            }
        } catch (e: Exception) {
            // 使用当前月份
        }
        val year = calendar.get(Calendar.YEAR)
        val monthNum = calendar.get(Calendar.MONTH) + 1
        return "${year}年${monthNum}月"
    }
}
