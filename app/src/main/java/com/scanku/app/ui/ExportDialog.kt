package com.scanku.app.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.scanku.app.AppViewModel
import com.scanku.app.core.ExportResult
import com.scanku.app.core.SizePreset
import com.scanku.app.data.ScanDocument

/** Dialog ekspor PDF: pilih kualitas atau target ukuran file, lalu bagikan / simpan. */
@Composable
fun ExportDialog(vm: AppViewModel, doc: ScanDocument, onDismiss: () -> Unit) {
    val context = LocalContext.current

    var selected by remember { mutableStateOf(SizePreset.MEDIUM) }
    var useTarget by remember { mutableStateOf(false) }
    var targetKb by remember { mutableFloatStateOf(1000f) }
    var working by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<ExportResult?>(null) }

    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        val r = result
        if (uri != null && r != null) {
            vm.saveCopy(r.file, uri) { ok ->
                Toast.makeText(context, if (ok) "PDF disimpan" else "Gagal menyimpan PDF", Toast.LENGTH_SHORT).show()
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!working) onDismiss() },
        title = { Text("Ekspor PDF") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val r = result
                when {
                    r != null -> {
                        Text("PDF siap: ${doc.pages.size} halaman, ${formatSize(r.sizeBytes)}",
                            style = MaterialTheme.typography.titleSmall)
                        r.note?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { sharePdf(context, r.file) }) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text("  Bagikan")
                            }
                            OutlinedButton(onClick = { saveLauncher.launch(r.file.name) }) {
                                Text("Simpan")
                            }
                        }
                        TextButton(onClick = { result = null }) { Text("Ubah ukuran") }
                    }

                    working -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
                            Text("Membuat PDF...")
                        }
                    }

                    else -> {
                        Text("Ukuran file", style = MaterialTheme.typography.labelLarge)
                        SizePreset.entries.forEach { p ->
                            OptionRow(
                                selected = !useTarget && selected == p,
                                title = p.label,
                                subtitle = p.hint
                            ) {
                                useTarget = false
                                selected = p
                            }
                        }
                        OptionRow(
                            selected = useTarget,
                            title = "Atur target ukuran",
                            subtitle = "Kompres otomatis hingga di bawah batas yang kamu tentukan"
                        ) { useTarget = true }
                        if (useTarget) {
                            val kb = targetKb.toInt()
                            Text(
                                "Maksimal ${if (kb >= 1024) String.format("%.1f MB", kb / 1024.0) else "$kb KB"}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Slider(
                                value = targetKb,
                                onValueChange = { targetKb = it },
                                valueRange = 100f..5000f
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (result == null) {
                TextButton(
                    enabled = !working,
                    onClick = {
                        working = true
                        vm.export(
                            doc = doc,
                            preset = if (useTarget) null else selected,
                            targetKb = if (useTarget) targetKb.toInt() else null
                        ) { res ->
                            working = false
                            res.onSuccess { result = it }
                                .onFailure {
                                    Toast.makeText(context, "Gagal membuat PDF: ${it.message}", Toast.LENGTH_LONG).show()
                                }
                        }
                    }
                ) { Text("Buat PDF") }
            } else {
                TextButton(onClick = onDismiss) { Text("Selesai") }
            }
        },
        dismissButton = {
            if (result == null && !working) {
                TextButton(onClick = onDismiss) { Text("Batal") }
            }
        }
    )
}

@Composable
private fun OptionRow(selected: Boolean, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
