package com.reveilpronote.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer
import kotlin.math.max

/** Lit un QR code dans une image (capture d'écran de Pronote). */
object QrDecoder {
    fun decode(ctx: Context, uri: Uri): String? {
        val original = runCatching {
            ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
        }.getOrNull() ?: return null
        // Essai en taille réelle puis réduite (les très grandes images gênent parfois la détection)
        val attempts = buildList {
            add(original)
            val largest = max(original.width, original.height)
            if (largest > 1600) {
                val s = 1600f / largest
                add(Bitmap.createScaledBitmap(original, (original.width * s).toInt(), (original.height * s).toInt(), true))
            }
        }
        for (bmp in attempts) {
            val text = tryDecode(bmp)
            if (text != null) return text
        }
        return null
    }

    private fun tryDecode(bmp: Bitmap): String? {
        val w = bmp.width
        val h = bmp.height
        val pixels = IntArray(w * h)
        bmp.getPixels(pixels, 0, w, 0, 0, w, h)
        val source = RGBLuminanceSource(w, h, pixels)
        val hints = mapOf(
            DecodeHintType.TRY_HARDER to true,
            DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
        )
        for (binarizer in listOf(HybridBinarizer(source), GlobalHistogramBinarizer(source))) {
            val result = runCatching { MultiFormatReader().decode(BinaryBitmap(binarizer), hints) }.getOrNull()
            if (result != null) return result.text
        }
        return null
    }
}
