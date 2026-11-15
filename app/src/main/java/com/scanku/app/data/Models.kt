package com.scanku.app.data

/** Filter yang bisa diterapkan pada satu halaman. */
enum class PageFilter(val label: String) {
    ORIGINAL("Asli"),
    MAGIC("Magic"),
    LIGHTEN("Terang"),
    GRAYSCALE("Abu-abu"),
    BW("Hitam Putih")
}

/**
 * Satu halaman dokumen.
 * [version] berubah setiap kali halaman diedit, dipakai untuk menyegarkan cache gambar.
 */
data class ScanPage(
    val id: String,
    val filter: PageFilter = PageFilter.ORIGINAL,
    val rotation: Int = 0,
    val version: Long = 0L
)

data class ScanDocument(
    val id: String,
    val name: String,
    val createdAt: Long,
    val pages: List<ScanPage>
)
