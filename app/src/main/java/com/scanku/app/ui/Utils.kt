package com.scanku.app.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Context.findActivity(): Activity {
    var c: Context = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    error("Activity tidak ditemukan")
}

fun formatSize(bytes: Long): String =
    if (bytes >= 1024L * 1024L) String.format(Locale.US, "%.1f MB", bytes / 1048576.0)
    else "${(bytes / 1024).coerceAtLeast(1)} KB"

fun formatDate(ms: Long): String =
    SimpleDateFormat("d MMM yyyy, HH.mm", Locale("id", "ID")).format(Date(ms))

fun sharePdf(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Bagikan PDF"))
}

fun shareText(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Bagikan teks"))
}

/** Menampilkan gambar halaman; [version] menyegarkan cache setelah halaman diedit. */
@Composable
fun PageImage(
    file: File,
    version: Long,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current
    val request = remember(file.path, version) {
        ImageRequest.Builder(context)
            .data(file)
            .memoryCacheKey("${file.path}#$version")
            .crossfade(true)
            .build()
    }
    AsyncImage(model = request, contentDescription = null, modifier = modifier, contentScale = contentScale)
}

/** Lapisan loading yang memblokir sentuhan selama proses berjalan. */
@Composable
fun BusyOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x66000000))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = {}
            ),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}
