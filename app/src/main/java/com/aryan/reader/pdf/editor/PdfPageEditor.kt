package com.aryan.reader.pdf.editor

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.pdmodel.PDDocument

interface PdfPageEditor {
    fun applyPageEdits(
        context: Context,
        sourceUri: Uri,
        order: List<Int>,
        rotationByOriginalIndex: Map<Int, Int> = emptyMap(),
        outputUri: Uri
    ): Int
}

class PdfPageEditorImpl : PdfPageEditor {
    override fun applyPageEdits(
        context: Context,
        sourceUri: Uri,
        order: List<Int>,
        rotationByOriginalIndex: Map<Int, Int>,
        outputUri: Uri
    ): Int {
        PdfBoxInit.ensureInitialized(context)
        if (order.isEmpty()) throw IllegalArgumentException("Result would have no pages")

        val output = PDDocument()
        var written = 0
        try {
            val input = context.contentResolver.openInputStream(sourceUri)
                ?: throw IllegalArgumentException("Cannot open source")
            input.use { stream ->
                PDDocument.load(stream).use { doc ->
                    for (orig in order) {
                        if (orig !in 0 until doc.numberOfPages) continue
                        val srcPage = doc.getPage(orig)
                        val imported = output.importPage(srcPage)
                        val extra = rotationByOriginalIndex[orig] ?: 0
                        if (extra != 0) {
                            imported.rotation = (((srcPage.rotation + extra) % 360) + 360) % 360
                        }
                        written++
                    }
                }
            }
            if (written == 0) throw IllegalArgumentException("Result would have no pages")
            val out = context.contentResolver.openOutputStream(outputUri)
                ?: throw IllegalStateException("Cannot write edited PDF output")
            out.use { output.save(it) }
        } finally {
            output.close()
        }
        return written
    }
}
