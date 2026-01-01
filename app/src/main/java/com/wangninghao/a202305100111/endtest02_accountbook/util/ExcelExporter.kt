package com.wangninghao.a202305100111.endtest02_accountbook.util

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.wangninghao.a202305100111.endtest02_accountbook.data.entity.Record
import com.wangninghao.a202305100111.endtest02_accountbook.data.entity.RecordType
import jxl.Workbook
import jxl.format.Alignment
import jxl.format.Border
import jxl.format.BorderLineStyle
import jxl.format.Colour
import jxl.write.Label
import jxl.write.WritableCellFormat
import jxl.write.WritableFont
import jxl.write.WritableWorkbook
import java.io.File
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Excel导出工具类
 * 使用JXL库（Android兼容）
 */
object ExcelExporter {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    /**
     * 导出记账数据到Excel文件
     * @param context 上下文
     * @param records 记账记录列表
     * @return 导出结果（成功返回文件路径，失败返回null）
     */
    fun exportRecords(context: Context, records: List<Record>): Result<String> {
        return try {
            val fileName = "账单_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.xls"
            val filePath = saveToDownloads(context, records, fileName)
            Result.success(filePath)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * 保存文件到Downloads目录
     */
    private fun saveToDownloads(context: Context, records: List<Record>, fileName: String): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ 使用MediaStore
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.ms-excel")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }

            val uri = context.contentResolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                contentValues
            ) ?: throw Exception("无法创建文件")

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                writeExcelToStream(outputStream, records)
            } ?: throw Exception("无法打开文件流")

            "Downloads/$fileName"
        } else {
            // Android 9及以下
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, fileName)
            writeExcelToFile(file, records)
            file.absolutePath
        }
    }

    /**
     * 写入Excel到输出流（Android 10+）
     */
    private fun writeExcelToStream(outputStream: OutputStream, records: List<Record>) {
        val workbook: WritableWorkbook = Workbook.createWorkbook(outputStream)
        try {
            writeWorkbookContent(workbook, records)
            workbook.write()
        } finally {
            workbook.close()
        }
    }

    /**
     * 写入Excel到文件（Android 9及以下）
     */
    private fun writeExcelToFile(file: File, records: List<Record>) {
        val workbook: WritableWorkbook = Workbook.createWorkbook(file)
        try {
            writeWorkbookContent(workbook, records)
            workbook.write()
        } finally {
            workbook.close()
        }
    }

    /**
     * 写入工作簿内容
     */
    private fun writeWorkbookContent(workbook: WritableWorkbook, records: List<Record>) {
        val sheet = workbook.createSheet("账单记录", 0)

        // 设置列宽
        sheet.setColumnView(0, 18)  // 日期
        sheet.setColumnView(1, 8)   // 类型
        sheet.setColumnView(2, 12)  // 分类
        sheet.setColumnView(3, 12)  // 金额
        sheet.setColumnView(4, 25)  // 备注

        // 创建样式
        val headerFormat = createHeaderFormat()
        val incomeFormat = createIncomeFormat()
        val expenseFormat = createExpenseFormat()
        val normalFormat = createNormalFormat()

        // 创建表头
        val headers = arrayOf("日期", "类型", "分类", "金额", "备注")
        headers.forEachIndexed { index, header ->
            sheet.addCell(Label(index, 0, header, headerFormat))
        }

        // 填充数据
        records.forEachIndexed { index, record ->
            val row = index + 1

            // 日期
            sheet.addCell(Label(0, row, dateFormat.format(Date(record.timestamp)), normalFormat))

            // 类型
            val typeText = if (record.type == RecordType.INCOME) "收入" else "支出"
            val typeFormat = if (record.type == RecordType.INCOME) incomeFormat else expenseFormat
            sheet.addCell(Label(1, row, typeText, typeFormat))

            // 分类
            sheet.addCell(Label(2, row, record.category, normalFormat))

            // 金额
            val prefix = if (record.type == RecordType.INCOME) "+" else "-"
            val amountText = "$prefix${String.format("%.2f", record.amount)}"
            val amountFormat = if (record.type == RecordType.INCOME) incomeFormat else expenseFormat
            sheet.addCell(Label(3, row, amountText, amountFormat))

            // 备注
            sheet.addCell(Label(4, row, record.note, normalFormat))
        }

        // 添加统计行
        val summaryRow = records.size + 2
        val totalIncome = records.filter { it.type == RecordType.INCOME }.sumOf { it.amount }
        val totalExpense = records.filter { it.type == RecordType.EXPENSE }.sumOf { it.amount }

        sheet.addCell(Label(0, summaryRow, "总计", headerFormat))
        sheet.addCell(Label(1, summaryRow, "收入", incomeFormat))
        sheet.addCell(Label(2, summaryRow, String.format("+%.2f", totalIncome), incomeFormat))
        sheet.addCell(Label(3, summaryRow, "支出", expenseFormat))
        sheet.addCell(Label(4, summaryRow, String.format("-%.2f", totalExpense), expenseFormat))
    }

    /**
     * 创建表头样式
     */
    private fun createHeaderFormat(): WritableCellFormat {
        val font = WritableFont(WritableFont.ARIAL, 10, WritableFont.BOLD)
        return WritableCellFormat(font).apply {
            setAlignment(Alignment.CENTRE)
            setBackground(Colour.GREY_25_PERCENT)
            setBorder(Border.ALL, BorderLineStyle.THIN)
        }
    }

    /**
     * 创建收入样式（绿色）
     */
    private fun createIncomeFormat(): WritableCellFormat {
        val font = WritableFont(WritableFont.ARIAL, 10)
        font.colour = Colour.GREEN
        return WritableCellFormat(font).apply {
            setAlignment(Alignment.CENTRE)
            setBorder(Border.ALL, BorderLineStyle.THIN)
        }
    }

    /**
     * 创建支出样式（红色）
     */
    private fun createExpenseFormat(): WritableCellFormat {
        val font = WritableFont(WritableFont.ARIAL, 10)
        font.colour = Colour.RED
        return WritableCellFormat(font).apply {
            setAlignment(Alignment.CENTRE)
            setBorder(Border.ALL, BorderLineStyle.THIN)
        }
    }

    /**
     * 创建普通样式
     */
    private fun createNormalFormat(): WritableCellFormat {
        val font = WritableFont(WritableFont.ARIAL, 10)
        return WritableCellFormat(font).apply {
            setAlignment(Alignment.CENTRE)
            setBorder(Border.ALL, BorderLineStyle.THIN)
        }
    }
}
