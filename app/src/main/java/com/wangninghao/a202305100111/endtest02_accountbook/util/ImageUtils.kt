package com.wangninghao.a202305100111.endtest02_accountbook.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.net.URLEncoder

/**
 * 图片处理工具类
 */
object ImageUtils {

    private const val TAG = "ImageUtils"

    /**
     * 从Uri加载并压缩图片
     * @param context 上下文
     * @param uri 图片Uri
     * @param maxWidth 最大宽度
     * @param maxHeight 最大高度
     * @return 压缩后的Bitmap
     */
    fun loadAndCompressBitmap(
        context: Context,
        uri: Uri,
        maxWidth: Int = 1024,
        maxHeight: Int = 1024
    ): Bitmap? {
        return try {
            Log.d(TAG, "Loading image from uri: $uri")

            // 首先获取图片尺寸
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }

            context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, options)
            }

            if (options.outWidth <= 0 || options.outHeight <= 0) {
                Log.e(TAG, "Failed to get image dimensions: ${options.outWidth}x${options.outHeight}")
                return null
            }

            Log.d(TAG, "Original image size: ${options.outWidth}x${options.outHeight}")

            // 计算缩放比例
            val width = options.outWidth
            val height = options.outHeight
            var sampleSize = 1
            while (width / sampleSize > maxWidth || height / sampleSize > maxHeight) {
                sampleSize *= 2
            }

            Log.d(TAG, "Sample size: $sampleSize")

            // 加载压缩后的图片
            val loadOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            var bitmap: Bitmap? = null
            context.contentResolver.openInputStream(uri)?.use { input ->
                bitmap = BitmapFactory.decodeStream(input, null, loadOptions)
            }

            if (bitmap == null) {
                Log.e(TAG, "Failed to decode bitmap")
                return null
            }

            Log.d(TAG, "Loaded bitmap size: ${bitmap!!.width}x${bitmap!!.height}")

            // 处理图片旋转
            val rotation = getRotation(context, uri)
            val finalBitmap = if (rotation != 0) {
                Log.d(TAG, "Rotating bitmap by $rotation degrees")
                rotateBitmap(bitmap!!, rotation)
            } else {
                bitmap!!
            }

            Log.d(TAG, "Final bitmap size: ${finalBitmap.width}x${finalBitmap.height}")
            finalBitmap

        } catch (e: Exception) {
            Log.e(TAG, "Error loading image", e)
            e.printStackTrace()
            null
        }
    }

    /**
     * 获取图片旋转角度
     */
    private fun getRotation(context: Context, uri: Uri): Int {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val exif = ExifInterface(input)
                when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            } ?: 0
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get EXIF rotation", e)
            0
        }
    }

    /**
     * 旋转Bitmap
     */
    private fun rotateBitmap(bitmap: Bitmap, degrees: Int): Bitmap {
        val matrix = Matrix().apply {
            postRotate(degrees.toFloat())
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * 将Bitmap转换为Base64字符串
     * @param bitmap 图片
     * @param quality 压缩质量 (0-100)
     * @return Base64编码的字符串
     */
    fun bitmapToBase64(bitmap: Bitmap, quality: Int = 85): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        val bytes = outputStream.toByteArray()
        Log.d(TAG, "Compressed image size: ${bytes.size} bytes")
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    /**
     * 将Bitmap转换为URLEncode后的Base64字符串（用于百度OCR API）
     * 注意：百度OCR API要求image参数进行URL编码
     */
    fun bitmapToUrlEncodedBase64(bitmap: Bitmap, quality: Int = 85): String {
        val base64 = bitmapToBase64(bitmap, quality)
        Log.d(TAG, "Base64 string length: ${base64.length}")
        return URLEncoder.encode(base64, "UTF-8")
    }

    /**
     * 从Uri直接获取URLEncode后的Base64字符串
     */
    fun uriToUrlEncodedBase64(context: Context, uri: Uri): String? {
        Log.d(TAG, "Converting uri to base64: $uri")
        val bitmap = loadAndCompressBitmap(context, uri)
        if (bitmap == null) {
            Log.e(TAG, "Failed to load bitmap from uri")
            return null
        }
        val result = bitmapToUrlEncodedBase64(bitmap)
        // 回收bitmap
        if (!bitmap.isRecycled) {
            bitmap.recycle()
        }
        return result
    }

    /**
     * 从Uri直接获取Base64字符串（不进行URL编码）
     */
    fun uriToBase64(context: Context, uri: Uri): String? {
        Log.d(TAG, "Converting uri to base64 (no url encode): $uri")
        val bitmap = loadAndCompressBitmap(context, uri)
        if (bitmap == null) {
            Log.e(TAG, "Failed to load bitmap from uri")
            return null
        }
        val result = bitmapToBase64(bitmap)
        // 回收bitmap
        if (!bitmap.isRecycled) {
            bitmap.recycle()
        }
        return result
    }

    /**
     * 计算图片文件大小（压缩后）
     */
    fun getCompressedSize(bitmap: Bitmap, quality: Int = 85): Long {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        return outputStream.size().toLong()
    }
}
