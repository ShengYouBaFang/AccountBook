package com.wangninghao.a202305100111.endtest02_accountbook.util

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.wangninghao.a202305100111.endtest02_accountbook.data.entity.Record
import com.wangninghao.a202305100111.endtest02_accountbook.data.entity.RecordType
import org.apache.poi.hssf.usermodel.HSSFCellStyle
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.hssf.util.HSSFColor
import org.apache.poi.ss.usermodel.BorderStyle
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.HorizontalAlignment
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Excel导出工具类
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
            val workbook = HSSFWorkbook()

            // 创建样式
            val headerStyle = createHeaderStyle(workbook)
            val incomeStyle = createIncomeStyle(workbook)
            val expenseStyle = createExpenseStyle(workbook)
            val normalStyle = createNormalStyle(workbook)

            // 创建工作表
            val sheet = workbook.createSheet("账单记录")

            // 设置列宽
            sheet.setColumnWidth(0, 5000)  // 日期
            sheet.setColumnWidth(1, 3000)  // 类型
            sheet.setColumnWidth(2, 4000)  // 分类
            sheet.setColumnWidth(3, 4000)  // 金额
            sheet.setColumnWidth(4, 8000)  // 备注

            // 创建表头
            val headerRow = sheet.createRow(0)
            val headers = arrayOf("日期", "类型", "分类", "金额", "备注")
            headers.forEachIndexed { index, header ->
                val cell = headerRow.createCell(index)
                cell.setCellValue(header)
                cell.setCellStyle(headerStyle)
            }

            // 填充数据
            records.forEachIndexed { index, record ->
                val row = sheet.createRow(index + 1)

                // 日期
                row.createCell(0).apply {
                    setCellValue(dateFormat.format(Date(record.timestamp)))
                    setCellStyle(normalStyle)
                }

                // 类型
                row.createCell(1).apply {
                    setCellValue(if (record.type == RecordType.INCOME) "收入" else "支出")
                    setCellStyle(if (record.type == RecordType.INCOME) incomeStyle else expenseStyle)
                }

                // 分类
                row.createCell(2).apply {
                    setCellValue(record.category)
                    setCellStyle(normalStyle)
                }

                // 金额
                row.createCell(3).apply {
                    val prefix = if (record.type == RecordType.INCOME) "+" else "-"
                    setCellValue("$prefix${String.format("%.2f", record.amount)}")
                    setCellStyle(if (record.type == RecordType.INCOME) incomeStyle else expenseStyle)
                }

                // 备注
                row.createCell(4).apply {
                    setCellValue(record.note)
                    setCellStyle(normalStyle)
                }
            }

            // 添加统计行
            val summaryRowIndex = records.size + 2
            val totalIncome = records.filter { it.type == RecordType.INCOME }.sumOf { it.amount }
            val totalExpense = records.filter { it.type == RecordType.EXPENSE }.sumOf { it.amount }

            sheet.createRow(summaryRowIndex).apply {
                createCell(0).apply {
                    setCellValue("总计")
                    setCellStyle(headerStyle)
                }
                createCell(1).apply {
                    setCellValue("收入")
                    setCellStyle(incomeStyle)
                }
                createCell(2).apply {
                    setCellValue(String.format("+%.2f", totalIncome))
                    setCellStyle(incomeStyle)
                }
                createCell(3).apply {
                    setCellValue("支出")
                    setCellStyle(expenseStyle)
                }
                createCell(4).apply {
                    setCellValue(String.format("-%.2f", totalExpense))
                    setCellStyle(expenseStyle)
                }
            }

            // 保存文件
            val fileName = "账单_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.xls"
            val filePath = saveToDownloads(context, workbook, fileName)

            workbook.close()

            Result.success(filePath)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * 保存文件到Downloads目录
     */
    private fun saveToDownloads(context: Context, workbook: HSSFWorkbook, fileName: String): String {
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
                workbook.write(outputStream)
            }

            "Downloads/$fileName"
        } else {
            // Android 9及以下
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, fileName)
            FileOutputStream(file).use { outputStream ->
                workbook.write(outputStream)
            }
            file.absolutePath
        }
    }

    /**
     * 创建表头样式
     */
    private fun createHeaderStyle(workbook: HSSFWorkbook): HSSFCellStyle {
        return workbook.createCellStyle().apply {
            fillForegroundColor = HSSFColor.HSSFColorPredefined.GREY_25_PERCENT.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
            setAlignment(HorizontalAlignment.CENTER)
            borderTop = BorderStyle.THIN
            borderBottom = BorderStyle.THIN
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN

            val font = workbook.createFont().apply {
                bold = true
            }
            setFont(font)
        }
    }

    /**
     * 创建收入样式（绿色）
     */
    private fun createIncomeStyle(workbook: HSSFWorkbook): HSSFCellStyle {
        return workbook.createCellStyle().apply {
            setAlignment(HorizontalAlignment.CENTER)
            borderTop = BorderStyle.THIN
            borderBottom = BorderStyle.THIN
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN

            val font = workbook.createFont().apply {
                color = HSSFColor.HSSFColorPredefined.GREEN.index
            }
            setFont(font)
        }
    }

    /**
     * 创建支出样式（红色）
     */
    private fun createExpenseStyle(workbook: HSSFWorkbook): HSSFCellStyle {
        return workbook.createCellStyle().apply {
            setAlignment(HorizontalAlignment.CENTER)
            borderTop = BorderStyle.THIN
            borderBottom = BorderStyle.THIN
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN

            val font = workbook.createFont().apply {
                color = HSSFColor.HSSFColorPredefined.RED.index
            }
            setFont(font)
        }
    }

    /**
     * 创建普通样式
     */
    private fun createNormalStyle(workbook: HSSFWorkbook): HSSFCellStyle {
        return workbook.createCellStyle().apply {
            setAlignment(HorizontalAlignment.CENTER)
            borderTop = BorderStyle.THIN
            borderBottom = BorderStyle.THIN
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN
        }
    }
}
