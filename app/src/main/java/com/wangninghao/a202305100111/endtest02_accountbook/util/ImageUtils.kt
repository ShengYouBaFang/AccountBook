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
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * 图片处理工具类
 * 优化OCR识别：保持图片完整性，不裁剪，仅按比例缩放
 */
object ImageUtils {

    private const val TAG = "ImageUtils"

    // 百度OCR API限制：最长边4096px，base64不超过4MB
    private const val MAX_DIMENSION = 4096
    private const val MAX_BASE64_SIZE = 4 * 1024 * 1024 // 4MB
    private const val TARGET_BASE64_SIZE = 3 * 1024 * 1024 // 3MB，留有余量

    /**
     * 从Uri加载图片，保持完整性，仅在必要时按比例缩放
     * 不裁剪任何区域，确保图片内容完整
     *
     * @param context 上下文
     * @param uri 图片Uri
     * @param maxDimension 最长边最大尺寸（默认4096，百度OCR API限制）
     * @return 处理后的Bitmap
     */
    fun loadAndCompressBitmap(
        context: Context,
        uri: Uri,
        maxDimension: Int = MAX_DIMENSION
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

            val originalWidth = options.outWidth
            val originalHeight = options.outHeight
            Log.d(TAG, "Original image size: ${originalWidth}x${originalHeight}")

            // 计算初始采样率（用于内存优化，避免OOM）
            // 使用较小的采样率以保留更多细节
            val sampleSize = calculateOptimalSampleSize(originalWidth, originalHeight, maxDimension)
            Log.d(TAG, "Initial sample size: $sampleSize")

            // 加载图片
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
            var finalBitmap = if (rotation != 0) {
                Log.d(TAG, "Rotating bitmap by $rotation degrees")
                val rotated = rotateBitmap(bitmap!!, rotation)
                if (rotated != bitmap) {
                    bitmap!!.recycle()
                }
                rotated
            } else {
                bitmap!!
            }

            // 如果旋转后的图片仍然超过最大尺寸限制，进行精确缩放
            val maxSide = max(finalBitmap.width, finalBitmap.height)
            if (maxSide > maxDimension) {
                Log.d(TAG, "Image still too large ($maxSide), scaling to fit $maxDimension")
                val scaledBitmap = scaleToFit(finalBitmap, maxDimension)
                if (scaledBitmap != finalBitmap) {
                    finalBitmap.recycle()
                }
                finalBitmap = scaledBitmap
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
     * 计算最优采样率
     * 目标：在不超过内存限制的情况下，尽可能保留图片细节
     */
    private fun calculateOptimalSampleSize(width: Int, height: Int, maxDimension: Int): Int {
        var sampleSize = 1
        val maxSide = max(width, height)

        // 只有当图片非常大时才使用采样
        // 目标是让采样后的图片最长边接近但不小于 maxDimension
        while (maxSide / sampleSize > maxDimension * 2) {
            sampleSize *= 2
        }

        return sampleSize
    }

    /**
     * 精确缩放图片到指定的最大尺寸
     * 保持宽高比，不裁剪
     */
    private fun scaleToFit(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val maxSide = max(width, height)

        if (maxSide <= maxDimension) {
            return bitmap
        }

        val scale = maxDimension.toFloat() / maxSide.toFloat()
        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()

        Log.d(TAG, "Scaling from ${width}x${height} to ${newWidth}x${newHeight}")

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
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
     * 自动调整压缩质量以确保不超过大小限制
     *
     * @param bitmap 图片
     * @param initialQuality 初始压缩质量 (0-100)
     * @return Base64编码的字符串
     */
    fun bitmapToBase64(bitmap: Bitmap, initialQuality: Int = 95): String {
        var quality = initialQuality
        var bytes: ByteArray

        // 尝试不同的压缩质量，确保不超过大小限制
        do {
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            bytes = outputStream.toByteArray()

            Log.d(TAG, "Compressed with quality $quality: ${bytes.size} bytes")

            // 如果大小超过限制且质量还可以降低，继续压缩
            if (bytes.size > TARGET_BASE64_SIZE && quality > 50) {
                quality -= 10
                Log.d(TAG, "Image too large, reducing quality to $quality")
            } else {
                break
            }
        } while (quality >= 50)

        // 如果还是太大，尝试更低的质量
        if (bytes.size > MAX_BASE64_SIZE && quality > 30) {
            quality = 30
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            bytes = outputStream.toByteArray()
            Log.d(TAG, "Final compression with quality $quality: ${bytes.size} bytes")
        }

        Log.d(TAG, "Final compressed image size: ${bytes.size} bytes (${bytes.size / 1024}KB)")
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    /**
     * 将Bitmap转换为URLEncode后的Base64字符串（用于百度OCR API）
     * 注意：百度OCR API要求image参数进行URL编码
     */
    fun bitmapToUrlEncodedBase64(bitmap: Bitmap, quality: Int = 95): String {
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
     * 优化版本：保持图片完整，自动调整压缩质量
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
        Log.d(TAG, "Final base64 length: ${result.length} chars")
        return result
    }

    /**
     * 计算图片文件大小（压缩后）
     */
    fun getCompressedSize(bitmap: Bitmap, quality: Int = 95): Long {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        return outputStream.size().toLong()
    }
}
