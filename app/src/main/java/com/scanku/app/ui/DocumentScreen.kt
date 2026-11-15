@file:OptIn(ExperimentalMaterial3Api::class)

package com.scanku.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.scanku.app.AppViewModel
import com.scanku.app.data.ScanDocument
import com.scanku.app.data.ScanPage

@Composable
fun DocumentScreen(
    vm: AppViewModel,
    docId: String,
    onBack: () -> Unit,
    onEditPage: (String) -> Unit,
    onOcr: (String) -> Unit
) {
    val docs by vm.docs.collectAsState()
    val loaded by vm.loaded.collectAsState()
    val doc = docs.firstOrNull { it.id == docId }

    if (doc == null) {
        // Dokumen sudah dihapus (atau belum selesai dimuat)
        LaunchedEffect(loaded) { if (loaded) onBack() }
        Box(Modifier.fillMaxSize())
    } else {
        DocumentContent(vm, doc, onBack, onEditPage, onOcr)
    }
}

@Composable
private fun DocumentContent(
    vm: AppViewModel,
    doc: ScanDocument,
    onBack: () -> Unit,
    onEditPage: (String) -> Unit,
    onOcr: (String) -> Unit
) {
    val busy by vm.busy.collectAsState()
    var showExport by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var showDeleteDoc by remember { mutableStateOf(false) }
    var pageToDelete by remember { mutableStateOf<ScanPage?>(null) }
    var topMenu by remember { mutableStateOf(false) }

    val addScan = rememberScannerLauncher { uris -> vm.addPages(doc.id, uris) }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(doc.name, maxLines = 1) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                        }
                    },
                    actions = {
                        IconButton(onClick = { onOcr("all") }) {
                            Icon(Icons.Default.TextFields, contentDescription = "Ambil teks (OCR)")
                        }
                        Box {
                            IconButton(onClick = { topMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                            }
                            DropdownMenu(expanded = topMenu, onDismissRequest = { topMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text("Ubah nama") },
                                    onClick = { topMenu = false; showRename = true }
                                )
                                DropdownMenuItem(
                                    text = { Text("Hapus dokumen") },
                                    onClick = { topMenu = false; showDeleteDoc = true }
                                )
                            }
                        }
                    }
                )
            },
            bottomBar = {
                Surface(tonalElevation = 3.dp) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(onClick = { onOcr("all") }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.TextFields, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("  Teks (OCR)")
                        }
                        Button(onClick = { showExport = true }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("  Ekspor PDF")
                        }
                    }
                }
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { addScan(30) }) {
                    Icon(Icons.Default.Add, contentDescription = "Tambah halaman")
                }
            }
        ) { padding ->
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(12.dp, 12.dp, 12.dp, 88.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(doc.pages, key = { _, p -> p.id }) { index, page ->
                    PageCard(
                        index = index,
                        total = doc.pages.size,
                        doc = doc,
                        page = page,
                        vm = vm,
                        onOpen = { onEditPage(page.id) },
                        onOcr = { onOcr(page.id) },
                        onMove = { delta -> vm.movePage(doc.id, page.id, delta) },
                        onDelete = { pageToDelete = page }
                    )
                }
            }
        }
        if (busy) BusyOverlay()
    }

    if (showExport) {
        ExportDialog(vm = vm, doc = doc, onDismiss = { showExport = false })
    }

    if (showRename) {
        var name by remember { mutableStateOf(doc.name) }
        AlertDialog(
            onDismissRequest = { showRename = false },
            title = { Text("Ubah nama dokumen") },
            text = {
                OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true)
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.renameDocument(doc.id, name)
                    showRename = false
                }) { Text("Simpan") }
            },
            dismissButton = { TextButton(onClick = { showRename = false }) { Text("Batal") } }
        )
    }

    if (showDeleteDoc) {
        AlertDialog(
            onDismissRequest = { showDeleteDoc = false },
            title = { Text("Hapus dokumen?") },
            text = { Text("Semua halaman dalam dokumen ini akan dihapus permanen.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDoc = false
                    vm.deleteDocument(doc.id)
                }) { Text("Hapus") }
            },
            dismissButton = { TextButton(onClick = { showDeleteDoc = false }) { Text("Batal") } }
        )
    }

    pageToDelete?.let { page ->
        AlertDialog(
            onDismissRequest = { pageToDelete = null },
            title = { Text("Hapus halaman?") },
            text = {
                Text(
                    if (doc.pages.size == 1) "Ini halaman terakhir, dokumen akan ikut terhapus."
                    else "Halaman ini akan dihapus permanen."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pageToDelete = null
                    vm.deletePage(doc.id, page.id)
                }) { Text("Hapus") }
            },
            dismissButton = { TextButton(onClick = { pageToDelete = null }) { Text("Batal") } }
        )
    }
}

@Composable
private fun PageCard(
    index: Int,
    total: Int,
    doc: ScanDocument,
    page: ScanPage,
    vm: AppViewModel,
    onOpen: () -> Unit,
    onOcr: () -> Unit,
    onMove: (Int) -> Unit,
    onDelete: () -> Unit
) {
    var menu by remember { mutableStateOf(false) }

    Card(onClick = onOpen, modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(0.72f)) {
        Box(Modifier.fillMaxSize()) {
            PageImage(
                file = vm.repo.pageFile(doc.id, page.id),
                version = page.version,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFEDEDED)),
                contentScale = ContentScale.Fit
            )
            Surface(
                color = Color(0x99000000),
                shape = RoundedCornerShape(topEnd = 8.dp),
                modifier = Modifier.align(Alignment.BottomStart)
            ) {
                Text(
                    "${index + 1}",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
            Box(Modifier.align(Alignment.TopEnd)) {
                IconButton(
                    onClick = { menu = true },
                    modifier = Modifier
                        .padding(4.dp)
                        .size(32.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color(0x99000000),
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Menu halaman", modifier = Modifier.size(18.dp))
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Edit / filter") }, onClick = { menu = false; onOpen() })
                    DropdownMenuItem(text = { Text("Ambil teks (OCR)") }, onClick = { menu = false; onOcr() })
                    if (index > 0) {
                        DropdownMenuItem(text = { Text("Pindah ke depan") }, onClick = { menu = false; onMove(-1) })
                    }
                    if (index < total - 1) {
                        DropdownMenuItem(text = { Text("Pindah ke belakang") }, onClick = { menu = false; onMove(1) })
                    }
                    DropdownMenuItem(text = { Text("Hapus") }, onClick = { menu = false; onDelete() })
                }
            }
        }
    }
}
