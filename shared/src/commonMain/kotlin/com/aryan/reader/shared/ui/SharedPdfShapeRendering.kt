package com.aryan.reader.shared.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.IntSize
import com.aryan.reader.shared.pdf.PdfAnnotationKind
import com.aryan.reader.shared.pdf.PdfInkTool
import com.aryan.reader.shared.pdf.PdfPagePoint
import com.aryan.reader.shared.pdf.SharedPdfAnnotation
import com.aryan.reader.shared.pdf.PdfPageBounds

internal fun DrawScope.drawSharedPdfShapeAnnotation(
    annotation: SharedPdfAnnotation,
    canvasSize: IntSize
) {
    if (annotation.points.size < 2) return
    val start = annotation.points.first()
    val end = annotation.points.last()
    
    val startX = start.x * canvasSize.width
    val startY = start.y * canvasSize.height
    val endX = end.x * canvasSize.width
    val endY = end.y * canvasSize.height
    
    val color = Color(annotation.colorArgb)
    val strokeWidth = (annotation.strokeWidth * canvasSize.width).coerceAtLeast(1f)
    
    when (annotation.tool) {
        PdfInkTool.RECTANGLE -> {
            val left = minOf(startX, endX)
            val top = minOf(startY, endY)
            val width = kotlin.math.abs(endX - startX)
            val height = kotlin.math.abs(endY - startY)
            drawRect(
                color = color,
                topLeft = Offset(left, top),
                size = Size(width, height),
                style = Stroke(width = strokeWidth)
            )
        }
        PdfInkTool.ELLIPSE -> {
            val left = minOf(startX, endX)
            val top = minOf(startY, endY)
            val width = kotlin.math.abs(endX - startX)
            val height = kotlin.math.abs(endY - startY)
            drawOval(
                color = color,
                topLeft = Offset(left, top),
                size = Size(width, height),
                style = Stroke(width = strokeWidth)
            )
        }
        PdfInkTool.LINE -> {
            drawLine(
                color = color,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = strokeWidth
            )
        }
        PdfInkTool.ARROW -> {
            drawLine(
                color = color,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = strokeWidth
            )
            // Draw arrow head
            val angle = kotlin.math.atan2(endY - startY, endX - startX)
            val arrowLength = 20f + strokeWidth * 2
            val arrowAngle = kotlin.math.PI / 6 // 30 degrees
            
            val pt1X = endX - arrowLength * kotlin.math.cos(angle - arrowAngle).toFloat()
            val pt1Y = endY - arrowLength * kotlin.math.sin(angle - arrowAngle).toFloat()
            val pt2X = endX - arrowLength * kotlin.math.cos(angle + arrowAngle).toFloat()
            val pt2Y = endY - arrowLength * kotlin.math.sin(angle + arrowAngle).toFloat()
            
            drawLine(
                color = color,
                start = Offset(endX, endY),
                end = Offset(pt1X, pt1Y),
                strokeWidth = strokeWidth
            )
            drawLine(
                color = color,
                start = Offset(endX, endY),
                end = Offset(pt2X, pt2Y),
                strokeWidth = strokeWidth
            )
        }
        else -> Unit
    }
}
