package com.aryan.reader.pdf.editor

import android.content.ContentValues
import android.content.Context
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

data class SplitPdfUiState(
    val sourceFileName: String = "",
    val sourceUri: Uri? = null,
    val isSplitting: Boolean = false,
    val resultMessage: String? = null,
    val errorMessage: String? = null,
    val outputUris: List<Uri> = emptyList()
)

class SplitPdfViewModel : ViewModel() {
    private val splitPdf = PdfSplitterImpl()
    private val _uiState = MutableStateFlow(SplitPdfUiState())
    val uiState: StateFlow<SplitPdfUiState> = _uiState.asStateFlow()

    fun onSelectFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            val name = withContext(Dispatchers.IO) { queryFileName(context, uri) ?: "Unknown.pdf" }
            _uiState.value = _uiState.value.copy(
                sourceFileName = name,
                sourceUri = uri,
                resultMessage = null,
                errorMessage = null,
                outputUris = emptyList()
            )
        }
    }

    fun splitAllPages(context: Context) {
        val src = _uiState.value.sourceUri ?: return
        val name = _uiState.value.sourceFileName
        _uiState.value = _uiState.value.copy(isSplitting = true, errorMessage = null, resultMessage = null, outputUris = emptyList())
        viewModelScope.launch {
            try {
                val results = withContext(Dispatchers.IO) { splitPdf.splitAll(context, src, name) }
                _uiState.value = _uiState.value.copy(
                    isSplitting = false,
                    outputUris = results,
                    resultMessage = "Successfully split PDF into {results.size} separate pages in cache."
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSplitting = false,
                    errorMessage = "Failed to split PDF"
                )
            }
        }
    }
    
    fun extractRange(context: Context, rangeString: String) {
        val src = _uiState.value.sourceUri ?: return
        _uiState.value = _uiState.value.copy(isSplitting = true, errorMessage = null, resultMessage = null, outputUris = emptyList())
        viewModelScope.launch {
            try {
                val pages = parseRange(rangeString)
                if (pages.isEmpty()) {
                    _uiState.value = _uiState.value.copy(isSplitting = false, errorMessage = "Invalid range format")
                    return@launch
                }
                
                val outUri = withContext(Dispatchers.IO) {
                    val uri = createOutputUri(context, "Extracted")
                    splitPdf.extractPages(context, src, pages, uri)
                    uri
                }
                _uiState.value = _uiState.value.copy(
                    isSplitting = false,
                    outputUris = listOf(outUri),
                    resultMessage = "Successfully extracted pages into new PDF"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSplitting = false,
                    errorMessage = "Failed to extract PDF pages"
                )
            }
        }
    }

    fun clearFeedback() {
        _uiState.value = _uiState.value.copy(resultMessage = null, errorMessage = null)
    }

    private fun parseRange(rangeString: String): List<Int> {
        val result = mutableSetOf<Int>()
        val parts = rangeString.split(",")
        for (part in parts) {
            val p = part.trim()
            if (p.isEmpty()) continue
            if (p.contains("-")) {
                val bounds = p.split("-")
                if (bounds.size == 2) {
                    val start = bounds[0].trim().toIntOrNull()
                    val end = bounds[1].trim().toIntOrNull()
                    if (start != null && end != null && start <= end) {
                        for (i in start..end) {
                            result.add(i - 1) // 0-based
                        }
                    }
                }
            } else {
                val page = p.toIntOrNull()
                if (page != null) {
                    result.add(page - 1)
                }
            }
        }
        return result.sorted()
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
