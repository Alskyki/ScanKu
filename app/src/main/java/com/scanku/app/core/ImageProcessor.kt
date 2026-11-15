package com.scanku.app.core

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import com.scanku.app.data.PageFilter
import java.io.File
import kotlin.math.max
import kotlin.math.min

/** Pemrosesan gambar: decode hemat memori, rotasi, dan filter dokumen. */
object ImageProcessor {

    /**
     * Membaca gambar dari file. Jika [maxDim] > 0, sisi terpanjang dibatasi sebesar [maxDim] piksel.
     */
    fun decode(file: File, maxDim: Int = 0): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Gambar tidak valid" }

        var sample = 1
        if (maxDim > 0) {
            val longest = max(bounds.outWidth, bounds.outHeight)
            while (longest / (sample * 2) >= maxDim) sample *= 2
        }
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val bmp = BitmapFactory.decodeFile(file.path, opts) ?: error("Gagal membaca gambar")
        return if (maxDim > 0) scaleDown(bmp, maxDim, recycleSrc = true) else bmp
    }

    fun scaleDown(src: Bitmap, maxDim: Int, recycleSrc: Boolean = false): Bitmap {
        val longest = max(src.width, src.height)
        if (longest <= maxDim) return src
        val s = maxDim.toFloat() / longest
        val out = Bitmap.createScaledBitmap(
            src,
            (src.width * s).toInt().coerceAtLeast(1),
            (src.height * s).toInt().coerceAtLeast(1),
            true
        )
        if (recycleSrc && out !== src) src.recycle()
        return out
    }

    /** Menerapkan rotasi lalu filter. Bitmap [src] tidak pernah di-recycle di sini. */
    fun render(src: Bitmap, filter: PageFilter, rotation: Int): Bitmap {
        val rotated = rotate(src, rotation)
        return when (filter) {
            PageFilter.ORIGINAL -> rotated
            PageFilter.GRAYSCALE -> applyMatrix(rotated, grayMatrix())
            PageFilter.MAGIC -> applyMatrix(rotated, magicMatrix())
            PageFilter.LIGHTEN -> applyMatrix(rotated, lightenMatrix())
            PageFilter.BW -> blackWhite(rotated)
        }
    }

    fun rotate(src: Bitmap, degrees: Int): Bitmap {
        val d = ((degrees % 360) + 360) % 360
        if (d == 0) return src
        val m = Matrix().apply { postRotate(d.toFloat()) }
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
    }

    private fun applyMatrix(src: Bitmap, matrix: ColorMatrix): Bitmap {
        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(matrix)
        }
        Canvas(out).drawBitmap(src, 0f, 0f, paint)
        return out
    }

    private fun grayMatrix() = ColorMatrix().apply { setSaturation(0f) }

    /** Kontras naik + warna sedikit lebih hidup, mirip "Magic Color". */
    private fun magicMatrix(): ColorMatrix {
        val c = 1.4f
        val t = 255f * (0.5f - 0.5f * c) + 18f
        val contrast = ColorMatrix(
            floatArrayOf(
                c, 0f, 0f, 0f, t,
                0f, c, 0f, 0f, t,
                0f, 0f, c, 0f, t,
                0f, 0f, 0f, 1f, 0f
            )
        )
        contrast.postConcat(ColorMatrix().apply { setSaturation(1.2f) })
        return contrast
    }

    private fun lightenMatrix(): ColorMatrix {
        val c = 1.1f
        val t = 255f * (0.5f - 0.5f * c) + 35f
        return ColorMatrix(
            floatArrayOf(
                c, 0f, 0f, 0f, t,
                0f, c, 0f, 0f, t,
                0f, 0f, c, 0f, t,
                0f, 0f, 0f, 1f, 0f
            )
        )
    }

    /**
     * Hitam-putih dengan ambang adaptif (rata-rata lokal lewat integral image).
     * Bagus untuk dokumen yang terkena bayangan atau pencahayaan tidak rata.
     */
    private fun blackWhite(src: Bitmap): Bitmap {
        val work = if (max(src.width, src.height) > 2600) scaleDown(src, 2600) else src
        val w = work.width
        val h = work.height
        val px = IntArray(w * h)
        work.getPixels(px, 0, w, 0, 0, w, h)

        val gray = IntArray(w * h)
        for (i in px.indices) {
            val p = px[i]
            gray[i] = (((p shr 16) and 0xFF) * 77 + ((p shr 8) and 0xFF) * 150 + (p and 0xFF) * 29) shr 8
        }

        val iw = w + 1
        val integral = IntArray(iw * (h + 1))
        for (y in 1..h) {
            var rowSum = 0
            for (x in 1..w) {
                rowSum += gray[(y - 1) * w + (x - 1)]
                integral[y * iw + x] = integral[(y - 1) * iw + x] + rowSum
            }
        }

        val r = (max(w, h) / 16).coerceAtLeast(8)
        for (y in 0 until h) {
            val y0 = max(0, y - r)
            val y1 = min(h - 1, y + r)
            for (x in 0 until w) {
                val x0 = max(0, x - r)
                val x1 = min(w - 1, x + r)
                val count = (x1 - x0 + 1) * (y1 - y0 + 1)
                val sum = integral[(y1 + 1) * iw + (x1 + 1)] -
                    integral[y0 * iw + (x1 + 1)] -
                    integral[(y1 + 1) * iw + x0] +
                    integral[y0 * iw + x0]
                px[y * w + x] =
                    if (gray[y * w + x].toLong() * count * 100L < sum.toLong() * 88L) Color.BLACK else Color.WHITE
            }
        }
        return Bitmap.createBitmap(px, w, h, Bitmap.Config.ARGB_8888)
    }
}
