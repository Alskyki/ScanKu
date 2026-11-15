@file:OptIn(ExperimentalMaterial3Api::class)

package com.scanku.app.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.scanku.app.AppViewModel
import com.scanku.app.core.ImageProcessor
import com.scanku.app.data.PageFilter
import com.scanku.app.data.ScanDocument
import com.scanku.app.data.ScanPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun PageEditorScreen(
    vm: AppViewModel,
    docId: String,
    pageId: String,
    onBack: () -> Unit,
    onOcr: () -> Unit
) {
    val docs by vm.docs.collectAsState()
    val loaded by vm.loaded.collectAsState()
    val doc = docs.firstOrNull { it.id == docId }
    val page = doc?.pages?.firstOrNull { it.id == pageId }

    if (doc == null || page == null) {
        LaunchedEffect(loaded) { if (loaded) onBack() }
        Box(Modifier.fillMaxSize())
    } else {
        EditorContent(vm, doc, page, onBack, onOcr)
    }
}

@Composable
private fun EditorContent(
    vm: AppViewModel,
    doc: ScanDocument,
    page: ScanPage,
    onBack: () -> Unit,
    onOcr: () -> Unit
) {
    val busy by vm.busy.collectAsState()
    var filter by remember(page.id) { mutableStateOf(page.filter) }
    var rotation by remember(page.id) { mutableStateOf(page.rotation) }
    var applyAll by remember(page.id) { mutableStateOf(false) }

    // Gambar asli diperkecil sekali saja, lalu filter/rotasi diterapkan untuk pratinjau.
    val source by produceState<Bitmap?>(initialValue = null, page.id) {
        value = withContext(Dispatchers.IO) {
            runCatching { ImageProcessor.decode(vm.repo.origFile(doc.id, page.id), 1400) }.getOrNull()
        }
    }
    val preview by produceState<Bitmap?>(initialValue = null, source, filter, rotation) {
        val s = source
        value = if (s == null) null else withContext(Dispatchers.Default) {
            ImageProcessor.render(s, filter, rotation)
        }
    }

    val changed = filter != page.filter || rotation != page.rotation || applyAll

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Edit halaman") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                        }
                    },
                    actions = {
                        IconButton(onClick = onOcr) {
                            Icon(Icons.Default.TextFields, contentDescription = "Ambil teks (OCR)")
                        }
                        IconButton(
                            onClick = {
                                if (changed) {
                                    vm.updatePage(doc.id, page.id, filter, rotation, applyAll) { onBack() }
                                } else {
                                    onBack()
                                }
                            }
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Simpan")
                        }
                    }
                )
            },
            bottomBar = {
                Surface(tonalElevation = 3.dp) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(PageFilter.entries) { f ->
                                FilterChip(
                                    selected = filter == f,
                                    onClick = { filter = f },
                                    label = { Text(f.label) }
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { rotation = (rotation + 270) % 360 }) {
                                Icon(Icons.Default.RotateLeft, contentDescription = "Putar ke kiri")
                            }
                            IconButton(onClick = { rotation = (rotation + 90) % 360 }) {
                                Icon(Icons.Default.RotateRight, contentDescription = "Putar ke kanan")
                            }
                            Spacer(Modifier.weight(1f))
                            Checkbox(checked = applyAll, onCheckedChange = { applyAll = it })
                            Text("Filter ke semua halaman", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Color(0xFF2B2B2B)),
                contentAlignment = Alignment.Center
            ) {
                val bmp = preview
                if (bmp == null) {
                    CircularProgressIndicator()
                } else {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Pratinjau halaman",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
        if (busy) BusyOverlay()
    }
}
