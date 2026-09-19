package com.aryan.reader.pdf.editor

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class MergePdfUiState(
    val selectedFiles: List<String> = emptyList(),
    val selectedUris: List<Uri> = emptyList(),
    val isMerging: Boolean = false,
    val resultMessage: String? = null,
    val errorMessage: String? = null,
    val lastOutputUri: Uri? = null
)

class MergePdfViewModel : ViewModel() {
    private val mergePdf = PdfMergerImpl()
    private val _uiState = MutableStateFlow(MergePdfUiState())
    val uiState: StateFlow<MergePdfUiState> = _uiState.asStateFlow()

    fun addFiles(context: Context, uris: List<Uri>) {
        viewModelScope.launch {
            val newNames = withContext(Dispatchers.IO) {
                uris.forEach { uri ->
                    try {
                        context.contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    } catch (_: Exception) { }
                }
                uris.map { uri -> queryFileName(context, uri) ?: "Unknown.pdf" }
            }
            _uiState.value = _uiState.value.copy(
                selectedFiles = _uiState.value.selectedFiles + newNames,
                selectedUris = _uiState.value.selectedUris + uris,
                resultMessage = null,
                errorMessage = null,
                lastOutputUri = null
            )
        }
    }

    fun moveFile(fromIndex: Int, toIndex: Int) {
        val files = _uiState.value.selectedFiles.toMutableList()
        val uris = _uiState.value.selectedUris.toMutableList()
        if (fromIndex !in files.indices || toIndex !in files.indices) return

        val movedFile = files.removeAt(fromIndex)
        val movedUri = uris.removeAt(fromIndex)
        files.add(toIndex, movedFile)
        uris.add(toIndex, movedUri)

        _uiState.value = _uiState.value.copy(selectedFiles = files, selectedUris = uris)
    }

    fun onRemoveFile(index: Int) {
        val files = _uiState.value.selectedFiles.toMutableList()
        val uris = _uiState.value.selectedUris.toMutableList()
        if (index in files.indices) {
            files.removeAt(index)
            uris.removeAt(index)
            _uiState.value = _uiState.value.copy(selectedFiles = files, selectedUris = uris, lastOutputUri = null)
        }
    }

    fun onMerge(context: Context) {
        val uris = _uiState.value.selectedUris
        if (uris.size < 2) {
            _uiState.value = _uiState.value.copy(errorMessage = "Select at least 2 files to merge")
            return
        }
        _uiState.value = _uiState.value.copy(isMerging = true, errorMessage = null, resultMessage = null, lastOutputUri = null)
        viewModelScope.launch {
            try {
                val outputUri = withContext(Dispatchers.IO) {
                    val outUri = createOutputUri(context, "Merged")
                    mergePdf.merge(context, uris, outUri)
                    outUri
                }
                val outName = queryFileName(context, outputUri) ?: "Merged.pdf"
                _uiState.value = _uiState.value.copy(
                    isMerging = false,
                    lastOutputUri = outputUri,
                    resultMessage = "Successfully merged {uris.size} files into outName"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isMerging = false,
                    errorMessage = "Failed to merge PDF files"
                )
            }
        }
    }

    fun clearFeedback() {
        _uiState.value = _uiState.value.copy(resultMessage = null, errorMessage = null)
    }

    private fun queryFileName(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) cursor.getString(idx) else null
                } else null
            }
        } catch (_: Exception) { null }
    }

    private fun createOutputUri(context: Context, prefix: String): Uri {
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "Episteme_prefix_.pdf"

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val cv = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv)
                ?: throw IllegalStateException("Unable to create output in Downloads")
        } else {
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
            if (!dir.exists()) dir.mkdirs()
            val file = java.io.File(dir, fileName)
            if (!file.exists()) file.createNewFile()
            FileProvider.getUriForFile(context, "{context.packageName}.provider", file)
        }
    }
}
