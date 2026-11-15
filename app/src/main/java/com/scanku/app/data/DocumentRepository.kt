package com.scanku.app.data

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.scanku.app.core.ImageProcessor
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Penyimpanan dokumen di memori internal aplikasi:
 *   files/docs/<docId>/meta.json
 *   files/docs/<docId>/<pageId>_orig.jpg   hasil scan asli (tidak pernah diubah)
 *   files/docs/<docId>/<pageId>.jpg        hasil akhir setelah filter + rotasi
 */
class DocumentRepository(private val context: Context) {

    private val root = File(context.filesDir, "docs").apply { mkdirs() }

    fun docDir(docId: String) = File(root, docId)
    fun origFile(docId: String, pageId: String) = File(docDir(docId), "${pageId}_orig.jpg")
    fun pageFile(docId: String, pageId: String) = File(docDir(docId), "$pageId.jpg")
    private fun metaFile(docId: String) = File(docDir(docId), "meta.json")

    // ---------- Baca / tulis ----------

    fun listDocuments(): List<ScanDocument> =
        root.listFiles()
            ?.filter { it.isDirectory }
            ?.mapNotNull { load(it.name) }
            ?.sortedByDescending { it.createdAt }
            .orEmpty()

    fun load(docId: String): ScanDocument? {
        val f = metaFile(docId)
        if (!f.exists()) return null
        return try {
            val o = JSONObject(f.readText())
            val arr = o.getJSONArray("pages")
            val pages = (0 until arr.length()).map { i ->
                val p = arr.getJSONObject(i)
                ScanPage(
                    id = p.getString("id"),
                    filter = runCatching { PageFilter.valueOf(p.getString("filter")) }
                        .getOrDefault(PageFilter.ORIGINAL),
                    rotation = p.optInt("rotation", 0),
                    version = p.optLong("version", 0L)
                )
            }
            ScanDocument(o.getString("id"), o.getString("name"), o.getLong("createdAt"), pages)
        } catch (e: Exception) {
            null
        }
    }

    private fun save(doc: ScanDocument) {
        docDir(doc.id).mkdirs()
        val pages = JSONArray()
        doc.pages.forEach {
            pages.put(
                JSONObject()
                    .put("id", it.id)
                    .put("filter", it.filter.name)
                    .put("rotation", it.rotation)
                    .put("version", it.version)
            )
        }
        val o = JSONObject()
            .put("id", doc.id)
            .put("name", doc.name)
            .put("createdAt", doc.createdAt)
            .put("pages", pages)
        metaFile(doc.id).writeText(o.toString())
    }

    // ---------- Dokumen ----------

    fun createDocument(name: String, uris: List<Uri>): ScanDocument {
        val doc = ScanDocument(UUID.randomUUID().toString(), name, System.currentTimeMillis(), emptyList())
        docDir(doc.id).mkdirs()
        return appendPages(doc, uris)
    }

    fun addPages(docId: String, uris: List<Uri>): ScanDocument? {
        val doc = load(docId) ?: return null
        return appendPages(doc, uris)
    }

    private fun appendPages(doc: ScanDocument, uris: List<Uri>): ScanDocument {
        val added = uris.map { uri ->
            val page = ScanPage(
                id = UUID.randomUUID().toString().take(8),
                filter = PageFilter.ORIGINAL,
                rotation = 0,
                version = System.currentTimeMillis()
            )
            val input = context.contentResolver.openInputStream(uri)
                ?: error("Tidak bisa membuka hasil scan")
            input.use { ins -> origFile(doc.id, page.id).outputStream().use { ins.copyTo(it) } }
            renderPage(doc.id, page)
            page
        }
        val updated = doc.copy(pages = doc.pages + added)
        save(updated)
        return updated
    }

    fun renameDocument(docId: String, name: String) {
        val doc = load(docId) ?: return
        save(doc.copy(name = name.trim().ifBlank { doc.name }))
    }

    fun deleteDocument(docId: String) {
        docDir(docId).deleteRecursively()
    }

    // ---------- Halaman ----------

    fun deletePage(docId: String, pageId: String) {
        val doc = load(docId) ?: return
        origFile(docId, pageId).delete()
        pageFile(docId, pageId).delete()
        val remaining = doc.pages.filterNot { it.id == pageId }
        if (remaining.isEmpty()) deleteDocument(docId) else save(doc.copy(pages = remaining))
    }

    fun movePage(docId: String, pageId: String, delta: Int) {
        val doc = load(docId) ?: return
        val list = doc.pages.toMutableList()
        val from = list.indexOfFirst { it.id == pageId }
        val to = from + delta
        if (from < 0 || to !in list.indices) return
        val item = list.removeAt(from)
        list.add(to, item)
        save(doc.copy(pages = list))
    }

    /** Simpan filter + rotasi untuk satu halaman (atau filter saja untuk semua halaman). */
    fun updatePage(
        docId: String,
        pageId: String,
        filter: PageFilter,
        rotation: Int,
        applyFilterToAll: Boolean
    ) {
        val doc = load(docId) ?: return
        val now = System.currentTimeMillis()
        val updated = doc.pages.map { p ->
            when {
                p.id == pageId -> p.copy(filter = filter, rotation = rotation, version = now)
                applyFilterToAll -> p.copy(filter = filter, version = now)
                else -> p
            }
        }
        updated.forEachIndexed { i, p ->
            if (p != doc.pages[i]) renderPage(docId, p)
        }
        save(doc.copy(pages = updated))
    }

    /** Membuat ulang berkas hasil akhir dari berkas asli. */
    private fun renderPage(docId: String, page: ScanPage) {
        val src = ImageProcessor.decode(origFile(docId, page.id), MAX_STORE_DIM)
        val out = ImageProcessor.render(src, page.filter, page.rotation)
        FileOutputStream(pageFile(docId, page.id)).use { out.compress(Bitmap.CompressFormat.JPEG, 92, it) }
        if (out !== src) out.recycle()
        src.recycle()
    }

    companion object {
        private const val MAX_STORE_DIM = 3000
    }
}
