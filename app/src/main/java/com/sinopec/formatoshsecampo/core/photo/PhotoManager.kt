package com.sinopec.formatoshsecampo.core.photo

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

/**
 * Centraliza la lectura y compresión de fotos.
 * La idea es que todos los formatos usen exactamente la misma política de fotos.
 */
object PhotoManager {
    fun loadBitmap(activity: Activity, uri: Uri): Bitmap {
        return MediaStore.Images.Media.getBitmap(activity.contentResolver, uri)
    }

    fun compressForReport(activity: Activity, uri: Uri, outDir: File): File {
        outDir.mkdirs()
        val original = loadBitmap(activity, uri)
        val resized = resizeIfNeeded(original, PhotoQuality.MAX_IMAGE_SIDE_PX)
        val out = File(outDir, "HSE_FOTO_${System.currentTimeMillis()}.jpg")
        FileOutputStream(out).use { fos ->
            // ESTE ES EL PUNTO EXACTO DONDE SE APLICA LA COMPRESIÓN MANUAL.
            resized.compress(Bitmap.CompressFormat.JPEG, PhotoQuality.JPEG_QUALITY, fos)
        }
        return out
    }

    private fun resizeIfNeeded(bitmap: Bitmap, maxSide: Int): Bitmap {
        val largestSide = max(bitmap.width, bitmap.height)
        if (largestSide <= maxSide) return bitmap
        val scale = maxSide.toFloat() / largestSide.toFloat()
        val newW = (bitmap.width * scale).toInt()
        val newH = (bitmap.height * scale).toInt()
        return Bitmap.createScaledBitmap(bitmap, newW, newH, true)
    }
}
