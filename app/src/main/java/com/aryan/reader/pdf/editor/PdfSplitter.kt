package com.aryan.reader.pdf.editor

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.pdmodel.PDDocument
import java.io.File
import androidx.core.content.FileProvider

interface PdfSplitter {
    fun splitAll(context: Context, sourceUri: Uri, baseName: String): List<Uri>
    fun extractPages(context: Context, sourceUri: Uri, pages: List<Int>, outputUri: Uri): Int
}

class PdfSplitterImpl : PdfSplitter {
    override fun splitAll(context: Context, sourceUri: Uri, baseName: String): List<Uri> {
        PdfBoxInit.ensureInitialized(context)
        val results = mutableListOf<Uri>()
        val cleanBaseName = baseName.removeSuffix(".pdf").ifBlank { "Document" }
        val splitDir = File(context.cacheDir, "split").apply { mkdirs() }

        val input = context.contentResolver.openInputStream(sourceUri)
            ?: throw IllegalArgumentException("Cannot open source")
        input.use { stream ->
            PDDocument.load(stream).use { doc ->
                for (i in 0 until doc.numberOfPages) {
                    PDDocument().use { single ->
                        single.importPage(doc.getPage(i))
                        val fileName = "cleanBaseName_page{i + 1}.pdf"
                        val outFile = File(splitDir, fileName)
                        outFile.outputStream().use { single.save(it) }
                        results.add(FileProvider.getUriForFile(context, "{context.packageName}.provider", outFile))
                    }
                }
            }
        }
        return results
    }

    override fun extractPages(
        context: Context,
        sourceUri: Uri,
        pages: List<Int>,
        outputUri: Uri
    ): Int {
        PdfBoxInit.ensureInitialized(context)
        var written = 0
        val output = PDDocument()
        try {
            val input = context.contentResolver.openInputStream(sourceUri)
                ?: throw IllegalArgumentException("Cannot open source")
            input.use { stream ->
                PDDocument.load(stream).use { doc ->
                    for (i in pages) {
                        if (i in 0 until doc.numberOfPages) {
                            output.importPage(doc.getPage(i))
                            written++
                        }
                    }
                }
            }
            val out = context.contentResolver.openOutputStream(outputUri)
                ?: throw IllegalStateException("Cannot write split PDF output")
            out.use { output.save(it) }
        } finally {
            output.close()
        }
        return written
    }
}
