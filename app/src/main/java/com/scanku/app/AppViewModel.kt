package com.scanku.app

import android.app.Application
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scanku.app.core.ExportResult
import com.scanku.app.core.PdfExporter
import com.scanku.app.core.SizePreset
import com.scanku.app.data.DocumentRepository
import com.scanku.app.data.PageFilter
import com.scanku.app.data.ScanDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AppViewModel(app: Application) : AndroidViewModel(app) {

    val repo = DocumentRepository(app)

    private val _docs = MutableStateFlow<List<ScanDocument>>(emptyList())
    val docs: StateFlow<List<ScanDocument>> = _docs.asStateFlow()

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    init {
        viewModelScope.launch { reload() }
    }

    private suspend fun reload() {
        _docs.value = withContext(Dispatchers.IO) { repo.listDocuments() }
        _loaded.value = true
    }

    private fun toast(msg: String) =
        Toast.makeText(getApplication(), msg, Toast.LENGTH_LONG).show()

    private fun runBusy(block: suspend () -> Unit) {
        viewModelScope.launch {
            _busy.value = true
            try {
                block()
            } catch (e: Exception) {
                toast("Terjadi kesalahan: ${e.message}")
                reload()
            } finally {
                _busy.value = false
            }
        }
    }

    private fun defaultName(): String =
        "Scan " + SimpleDateFormat("d MMM yyyy HH.mm", Locale("id", "ID")).format(Date())

    // ---------- Dokumen ----------

    fun createDocument(uris: List<Uri>, onCreated: (String) -> Unit) = runBusy {
        val doc = withContext(Dispatchers.IO) { repo.createDocument(defaultName(), uris) }
        reload()
        onCreated(doc.id)
    }

    fun addPages(docId: String, uris: List<Uri>) = runBusy {
        withContext(Dispatchers.IO) { repo.addPages(docId, uris) }
        reload()
    }

    fun renameDocument(docId: String, name: String) = runBusy {
        withContext(Dispatchers.IO) { repo.renameDocument(docId, name) }
        reload()
    }

    fun deleteDocument(docId: String) = runBusy {
        withContext(Dispatchers.IO) { repo.deleteDocument(docId) }
        reload()
    }

    // ---------- Halaman ----------

    fun deletePage(docId: String, pageId: String) = runBusy {
        withContext(Dispatchers.IO) { repo.deletePage(docId, pageId) }
        reload()
    }

    fun movePage(docId: String, pageId: String, delta: Int) = runBusy {
        withContext(Dispatchers.IO) { repo.movePage(docId, pageId, delta) }
        reload()
    }

    fun updatePage(
        docId: String,
        pageId: String,
        filter: PageFilter,
        rotation: Int,
        applyAll: Boolean,
        onDone: () -> Unit
    ) = runBusy {
        withContext(Dispatchers.Default) { repo.updatePage(docId, pageId, filter, rotation, applyAll) }
        reload()
        onDone()
    }

    // ---------- Ekspor PDF ----------

    /** Jika [preset] null maka dipakai [targetKb] sebagai target ukuran file. */
    fun export(
        doc: ScanDocument,
        preset: SizePreset?,
        targetKb: Int?,
        onDone: (Result<ExportResult>) -> Unit
    ) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.Default) {
                runCatching {
                    val files = doc.pages.map { repo.pageFile(doc.id, it.id) }
                    val dir = File(getApplication<Application>().cacheDir, "exports").apply {
                        mkdirs()
                        listFiles()?.forEach { it.delete() }
                    }
                    val out = File(dir, safeFileName(doc.name) + ".pdf")
                    if (preset != null) {
                        PdfExporter.exportPreset(files, out, preset)
                    } else {
                        PdfExporter.exportTarget(files, out, (targetKb ?: 1000) * 1024L)
                    }
                }
            }
            onDone(result)
        }
    }

    fun saveCopy(source: File, dest: Uri, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    getApplication<Application>().contentResolver.openOutputStream(dest)!!.use { out ->
                        source.inputStream().use { it.copyTo(out) }
                    }
                }.isSuccess
            }
            onDone(ok)
        }
    }

    private fun safeFileName(name: String): String =
        name.replace(Regex("[^A-Za-z0-9._ -]"), "_").trim().ifBlank { "Dokumen" }
}
