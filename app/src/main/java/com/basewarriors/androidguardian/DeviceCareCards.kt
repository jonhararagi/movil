package com.basewarriors.androidguardian

import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.util.Locale

private data class JunkFile(val uri: Uri, val name: String, val size: Long, val reason: String)

@Composable
fun StorageCleanerCard() {
    val context = LocalContext.current
    var tree by remember { mutableStateOf<Uri?>(null) }
    var status by remember { mutableStateOf("Elige una carpeta para buscar temporales antiguos. No se borra nada sin tu confirmación.") }
    var confirmDelete by remember { mutableStateOf(false) }
    val files = remember { mutableStateListOf<JunkFile>() }
    val selected = remember { mutableStateListOf<String>() }

    fun scan(uri: Uri) {
        files.clear(); selected.clear()
        var visited = 0
        val cutoff = System.currentTimeMillis() - 14L * 24 * 60 * 60 * 1000
        val projection = arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE, DocumentsContract.Document.COLUMN_SIZE, DocumentsContract.Document.COLUMN_LAST_MODIFIED)
        fun walk(id: String, depth: Int) {
            if (visited >= 2500 || depth > 10) return
            var cursor: Cursor? = null
            try {
                cursor = context.contentResolver.query(DocumentsContract.buildChildDocumentsUriUsingTree(uri, id), projection, null, null, null)
                cursor?.use { c ->
                    while (c.moveToNext() && visited < 2500) {
                        visited++
                        val childId = c.getString(0) ?: continue
                        val name = c.getString(1) ?: "archivo-sin-nombre"
                        val mime = c.getString(2) ?: ""
                        val size = if (c.isNull(3)) 0L else c.getLong(3)
                        val modified = if (c.isNull(4)) 0L else c.getLong(4)
                        if (mime == DocumentsContract.Document.MIME_TYPE_DIR) walk(childId, depth + 1)
                        else {
                            val n = name.lowercase(Locale.ROOT)
                            val old = modified > 0 && modified < cutoff
                            val apkOld = modified > 0 && modified < System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
                            val screenshotOld = modified > 0 && modified < System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
                            val screenshotName = n.contains("screenshot") || n.contains("captura") || n.contains("captura_de_pantalla") || n.contains("screen_shot")
                            val reason = when {
                                apkOld && n.endsWith(".apk") -> "Instalador APK antiguo, revisar"
                                old && (n.endsWith(".tmp") || n.endsWith(".temp") || n.endsWith(".part") || n.endsWith(".partial") || n.endsWith(".crdownload") || n.endsWith(".log")) -> "Temporal antiguo"
                                screenshotOld && screenshotName -> "Captura de pantalla antigua, revisar"
                                size >= 100L * 1024 * 1024 -> "Archivo grande (100 MB o más), revisar antes de borrar"
                                size == 0L && old -> "Archivo vacío antiguo"
                                else -> null
                            }
                            if (reason != null) files.add(JunkFile(DocumentsContract.buildDocumentUriUsingTree(uri, childId), name, size, reason))
                        }
                    }
                }
            } catch (_: Exception) { }
        }
        try {
            walk(DocumentsContract.getTreeDocumentId(uri), 0)
            status = if (visited >= 2500) "Límite de seguridad: revisé 2.500 elementos. Elige una carpeta más concreta." else "Revisé " + visited + " elementos. Posibles archivos prescindibles: " + files.size + "."
        } catch (_: Exception) { status = "No pude leer esta carpeta. Prueba con Descargas." }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            try { context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION) } catch (_: Exception) { }
            tree = uri
            scan(uri)
        }
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("🧹 Limpieza cuidadosa", style = MaterialTheme.typography.titleLarge)
            Text("Busca temporales, descargas parciales, logs, APK antiguos, archivos vacíos, capturas antiguas y archivos grandes (100 MB o más) en una carpeta que elijas. Todo se revisa antes de borrar.")
            Text("Los archivos grandes, APK y capturas se marcan para que los revises, no porque sean basura segura. Por las protecciones de Android, solo analiza la carpeta autorizada y no puede limpiar los datos privados de otras apps.")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { picker.launch(tree) }) { Text("Elegir carpeta y analizar") }
                OutlinedButton(onClick = { tree?.let { scan(it) } }, enabled = tree != null) { Text("Reanalizar") }
            }
            Text(status)
            if (files.isNotEmpty()) {
                TextButton(onClick = { if (selected.size == files.size) selected.clear() else { selected.clear(); selected.addAll(files.map { it.uri.toString() }) } }) { Text("Seleccionar todos / ninguno") }
                Text(selected.size.toString() + " seleccionados · " + prettyBytes(files.filter { selected.contains(it.uri.toString()) }.sumOf { it.size }))
                Column(Modifier.heightIn(max = 280.dp).verticalScroll(rememberScrollState())) {
                    files.forEach { f ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Checkbox(checked = selected.contains(f.uri.toString()), onCheckedChange = { yes -> if (yes) { if (!selected.contains(f.uri.toString())) selected.add(f.uri.toString()) } else selected.remove(f.uri.toString()) })
                            Column(Modifier.weight(1f).padding(top = 8.dp)) {
                                Text(f.name)
                                Text(f.reason + " · " + prettyBytes(f.size), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                Button(onClick = { confirmDelete = true }, enabled = selected.isNotEmpty()) { Text("Eliminar seleccionados (" + selected.size + ")") }
            }
        }
    }
    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text("¿Eliminar archivos seleccionados?") },
        text = { Text("Se intentará borrar " + selected.size + " archivos. Puede que el proveedor no tenga papelera. Revisa la lista antes de continuar.") },
        confirmButton = { TextButton(onClick = {
            var deleted = 0
            files.filter { selected.contains(it.uri.toString()) }.forEach { f ->
                try { if (DocumentsContract.deleteDocument(context.contentResolver, f.uri)) deleted++ } catch (_: Exception) { }
            }
            confirmDelete = false
            tree?.let { scan(it) }
            status = "Eliminados: " + deleted + ". Revisa si algún archivo quedó protegido."
        }) { Text("Eliminar") } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") } }
    )
}

@Composable
fun ReminderManagerCard() {
    val context = LocalContext.current
    var reminders by remember { mutableStateOf(ReminderStore.load(context).filter { it.triggerAtMillis > System.currentTimeMillis() }) }
    var pending by remember { mutableStateOf<GuardianReminder?>(null) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("⏰ Alarmas y recordatorios", style = MaterialTheme.typography.titleLarge)
            Text("Puedes revisar los avisos pendientes y cancelar los que ya no necesites.")
            if (reminders.isEmpty()) Text("No hay recordatorios pendientes.")
            reminders.sortedBy { it.triggerAtMillis }.forEach { r ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(r.body)
                        Text(java.text.DateFormat.getDateTimeInstance().format(java.util.Date(r.triggerAtMillis)), style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = { pending = r }) { Text("Cancelar") }
                }
            }
            OutlinedButton(onClick = { ReminderStore.restorePending(context); reminders = ReminderStore.load(context).filter { it.triggerAtMillis > System.currentTimeMillis() } }) { Text("Actualizar lista") }
        }
    }
    pending?.let { r -> AlertDialog(
        onDismissRequest = { pending = null },
        title = { Text("Cancelar recordatorio") },
        text = { Text("¿Cancelar «" + r.body + "»?") },
        confirmButton = { TextButton(onClick = { ReminderStore.cancel(context, r.id); reminders = ReminderStore.load(context).filter { it.triggerAtMillis > System.currentTimeMillis() }; pending = null }) { Text("Cancelar aviso") } },
        dismissButton = { TextButton(onClick = { pending = null }) { Text("Conservar") } }
    ) }
}

private fun prettyBytes(bytes: Long): String = when {
    bytes < 1024 -> bytes.toString() + " B"
    bytes < 1024L * 1024 -> String.format(Locale.getDefault(), "%.1f KB", bytes / 1024.0)
    bytes < 1024L * 1024 * 1024 -> String.format(Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024))
    else -> String.format(Locale.getDefault(), "%.2f GB", bytes / (1024.0 * 1024 * 1024))
}
