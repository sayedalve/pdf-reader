package com.aryan.reader.pdf.editor

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import com.tom_roush.pdfbox.pdmodel.PDDocument

interface PdfMerger {
    fun merge(context: Context, sources: List<Uri>, outputUri: Uri): Int
    fun mergePages(context: Context, sources: List<Pair<Uri, List<Int>>>, outputUri: Uri): Int
}

class PdfMergerImpl : PdfMerger {
    override fun merge(context: Context, sources: List<Uri>, outputUri: Uri): Int {
        PdfBoxInit.ensureInitialized(context)
        if (sources.isEmpty()) throw IllegalArgumentException("No PDFs to merge")

        val merger = PDFMergerUtility()
        val outputStream = context.contentResolver.openOutputStream(outputUri)
            ?: throw IllegalStateException("Cannot write merged PDF output")

        var totalPages = 0
        outputStream.use { out ->
            merger.destinationStream = out
            for (src in sources) {
                val input = context.contentResolver.openInputStream(src)
                    ?: throw IllegalStateException("Cannot read src")
                merger.addSource(input)
                totalPages += countPages(context, src)
            }
            merger.mergeDocuments(MemoryUsageSetting.setupTempFileOnly())
        }
        return totalPages
    }

    override fun mergePages(
        context: Context,
        sources: List<Pair<Uri, List<Int>>>,
        outputUri: Uri
    ): Int {
        PdfBoxInit.ensureInitialized(context)

        val output = PDDocument()
        var written = 0
        try {
            for ((src, pages) in sources) {
                val input = context.contentResolver.openInputStream(src)
                    ?: throw IllegalStateException("Cannot read src")
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
            }
            val out = context.contentResolver.openOutputStream(outputUri)
                ?: throw IllegalStateException("Cannot write merged PDF output")
            out.use { output.save(it) }
        } finally {
            output.close()
        }
        return written
    }

    private fun countPages(context: Context, uri: Uri): Int = try {
        context.contentResolver.openFileDescriptor(uri, "r")?.use { fd ->
            android.graphics.pdf.PdfRenderer(fd).use { it.pageCount }
        } ?: 0
    } catch (_: Exception) {
        0
    }
}
