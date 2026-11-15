package com.scanku.app.core

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream
import java.util.Locale

/** Pilihan kualitas / ukuran file PDF. */
enum class SizePreset(val label: String, val hint: String, val maxDim: Int, val quality: Int) {
    SMALL("Kecil", "Hemat kuota, cocok dikirim lewat chat", 1100, 55),
    MEDIUM("Sedang", "Seimbang antara ukuran dan ketajaman", 1700, 72),
    HIGH("Tinggi", "Tajam, cocok untuk dicetak", 2400, 88),
    ORIGINAL("Asli", "Tanpa kompresi ulang", 0, 100)
}

data class ExportResult(val file: File, val sizeBytes: Long, val note: String? = null)

/**
 * Penulis PDF sederhana: tiap halaman adalah satu gambar JPEG (DCTDecode) yang disisipkan apa adanya.
 * Dengan cara ini ukuran file benar-benar bisa dikontrol lewat resolusi dan kualitas JPEG.
 */
object PdfExporter {

    private const val PAGE_WIDTH = 595.28 // lebar A4 dalam point; tinggi menyesuaikan rasio gambar

    private class Jpeg(val bytes: ByteArray, val width: Int, val height: Int)

    /** Tangga kompresi untuk mode "target ukuran": (sisi terpanjang px, kualitas JPEG). */
    private val LADDER = listOf(
        2400 to 90, 2000 to 82, 1700 to 74, 1400 to 66,
        1100 to 58, 900 to 48, 700 to 40, 500 to 30
    )

    fun exportPreset(files: List<File>, out: File, preset: SizePreset): ExportResult {
        val pages = if (preset == SizePreset.ORIGINAL) original(files) else encode(files, preset.maxDim, preset.quality)
        write(pages, out)
        return ExportResult(out, out.length())
    }

    fun exportTarget(files: List<File>, out: File, targetBytes: Long): ExportResult {
        if (files.sumOf { it.length() } <= targetBytes) {
            write(original(files), out)
            return ExportResult(out, out.length(), "Ukuran asli sudah di bawah target, jadi tidak dikompresi.")
        }
        var last: List<Jpeg> = emptyList()
        for ((dim, quality) in LADDER) {
            val pages = encode(files, dim, quality)
            last = pages
            if (pages.sumOf { it.bytes.size.toLong() } <= targetBytes) {
                write(pages, out)
                return ExportResult(out, out.length())
            }
        }
        write(last, out)
        return ExportResult(
            out, out.length(),
            "Target belum tercapai. Ini ukuran terkecil yang masih aman agar teks tetap terbaca."
        )
    }

    // ---------- Persiapan gambar ----------

    private fun original(files: List<File>): List<Jpeg> = files.map { f ->
        val b = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(f.path, b)
        Jpeg(f.readBytes(), b.outWidth, b.outHeight)
    }

    private fun encode(files: List<File>, maxDim: Int, quality: Int): List<Jpeg> = files.map { f ->
        val bmp = ImageProcessor.decode(f, maxDim)
        val bos = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, quality, bos)
        val j = Jpeg(bos.toByteArray(), bmp.width, bmp.height)
        bmp.recycle()
        j
    }

    // ---------- Penulisan PDF ----------

    private fun write(pages: List<Jpeg>, out: File) {
        out.parentFile?.mkdirs()
        out.outputStream().buffered().use { writePdf(pages, it) }
    }

    private class CountingStream(private val out: OutputStream) : OutputStream() {
        var count = 0L
            private set

        override fun write(b: Int) {
            out.write(b); count++
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            out.write(b, off, len); count += len
        }

        override fun flush() = out.flush()
    }

    private fun writePdf(pages: List<Jpeg>, target: OutputStream) {
        val s = CountingStream(target)
        val n = pages.size
        val totalObjects = 3 + 3 * n // nomor 0 (kosong) + katalog + daftar halaman + 3 objek per halaman
        val offsets = LongArray(totalObjects)

        fun str(t: String) = s.write(t.toByteArray(Charsets.ISO_8859_1))
        fun begin(num: Int) {
            offsets[num] = s.count
            str("$num 0 obj\n")
        }

        str("%PDF-1.4\n%âãÏÓ\n")

        begin(1)
        str("<< /Type /Catalog /Pages 2 0 R >>\nendobj\n")

        val kids = (0 until n).joinToString(" ") { "${5 + 3 * it} 0 R" }
        begin(2)
        str("<< /Type /Pages /Count $n /Kids [$kids] >>\nendobj\n")

        for ((i, p) in pages.withIndex()) {
            val img = 3 + 3 * i
            val content = 4 + 3 * i
            val page = 5 + 3 * i
            val pw = PAGE_WIDTH
            val ph = pw * p.height / p.width

            begin(img)
            str(
                "<< /Type /XObject /Subtype /Image /Width ${p.width} /Height ${p.height} " +
                    "/ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode /Length ${p.bytes.size} >>\nstream\n"
            )
            s.write(p.bytes)
            str("\nendstream\nendobj\n")

            val cs = String.format(Locale.US, "q %.2f 0 0 %.2f 0 0 cm /Im0 Do Q", pw, ph)
            begin(content)
            str("<< /Length ${cs.length} >>\nstream\n$cs\nendstream\nendobj\n")

            begin(page)
            str(
                String.format(
                    Locale.US,
                    "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 %.2f %.2f] " +
                        "/Resources << /XObject << /Im0 $img 0 R >> >> /Contents $content 0 R >>\nendobj\n",
                    pw, ph
                )
            )
        }

        val xrefPos = s.count
        str("xref\n0 $totalObjects\n")
        str("0000000000 65535 f \n")
        for (i in 1 until totalObjects) {
            str(String.format(Locale.US, "%010d 00000 n \n", offsets[i]))
        }
        str("trailer\n<< /Size $totalObjects /Root 1 0 R >>\nstartxref\n$xrefPos\n%%EOF\n")
        s.flush()
    }
}
