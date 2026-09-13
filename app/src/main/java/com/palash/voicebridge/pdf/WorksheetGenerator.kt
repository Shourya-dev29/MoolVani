package com.palash.voicebridge.pdf

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import com.palash.voicebridge.data.local.CurriculumEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Offline bilingual worksheet generator using Android's native PdfDocument.
 * Standard A4 Page dimensions: 595 x 842 points (72 dpi).
 * Generates worksheets adhering to NIPUN Bharat FLN learning outcomes.
 */
class WorksheetGenerator(private val context: Context) {

    companion object {
        const val PAGE_WIDTH = 595
        const val PAGE_HEIGHT = 842
        const val MARGIN = 40f
    }

    suspend fun generate(
        phrases: List<CurriculumEntity>,
        topic: String,
        className: String = "Grade 1"
    ): File = withContext(Dispatchers.IO) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        drawWorksheet(canvas, phrases, topic, className)

        pdfDocument.finishPage(page)

        val outputDir = File(context.cacheDir, "worksheets").apply { mkdirs() }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val sanitizedTopic = topic.replace(" ", "_").lowercase()
        val outputFile = File(outputDir, "worksheet_${sanitizedTopic}_$timestamp.pdf")

        FileOutputStream(outputFile).use { outputStream ->
            pdfDocument.writeTo(outputStream)
        }
        pdfDocument.close()

        outputFile
    }

    private fun drawWorksheet(
        canvas: Canvas,
        phrases: List<CurriculumEntity>,
        topic: String,
        className: String
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // 1. Header Banner
        paint.color = Color.rgb(27, 94, 32) // Forest Green
        canvas.drawRect(MARGIN, MARGIN, PAGE_WIDTH - MARGIN, MARGIN + 60f, paint)

        paint.color = Color.WHITE
        paint.textSize = 18f
        paint.isFakeBoldText = true
        canvas.drawText("PALASH VoiceBridge — Bilingual Learning Worksheet", MARGIN + 14f, MARGIN + 28f, paint)

        paint.textSize = 11f
        paint.isFakeBoldText = false
        canvas.drawText("NIPUN Bharat FLN Aligned | Mother Tongue-Based Early Primary Education", MARGIN + 14f, MARGIN + 48f, paint)

        // 2. Metadata Box
        var currentY = MARGIN + 80f
        paint.color = Color.rgb(240, 245, 240)
        canvas.drawRoundRect(RectF(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY + 36f), 6f, 6f, paint)

        paint.color = Color.rgb(30, 30, 30)
        paint.textSize = 11f
        val currentDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
        canvas.drawText("Topic: $topic", MARGIN + 12f, currentY + 22f, paint)
        canvas.drawText("Class: $className", MARGIN + 180f, currentY + 22f, paint)
        canvas.drawText("Date: $currentDate", MARGIN + 310f, currentY + 22f, paint)
        canvas.drawText("Student Name: ________________", MARGIN + 400f, currentY + 22f, paint)

        // 3. Instruction Box
        currentY += 50f
        paint.color = Color.rgb(230, 240, 230)
        canvas.drawRoundRect(RectF(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY + 40f), 4f, 4f, paint)
        paint.color = Color.rgb(20, 80, 20)
        paint.textSize = 11f
        paint.isFakeBoldText = true
        canvas.drawText("निर्देश (Instructions): चित्र/शब्द देखकर संथाली में बोलें और अभ्यास स्थान में लिखें।", MARGIN + 12f, currentY + 18f, paint)
        paint.textSize = 10f
        paint.isFakeBoldText = false
        canvas.drawText("Read the Hindi prompt, recite in Santhali, and complete the exercise below.", MARGIN + 12f, currentY + 32f, paint)

        // 4. Questions & Exercises
        currentY += 56f
        val itemsToDraw = phrases.take(6)
        val itemHeight = 90f

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1f
            color = Color.rgb(200, 210, 200)
        }

        val dotLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1f
            color = Color.rgb(180, 180, 180)
        }

        for ((index, item) in itemsToDraw.withIndex()) {
            val itemTop = currentY + (index * itemHeight)
            val cardRect = RectF(MARGIN, itemTop, PAGE_WIDTH - MARGIN, itemTop + itemHeight - 8f)

            // Card background
            paint.color = if (index % 2 == 0) Color.WHITE else Color.rgb(248, 250, 248)
            canvas.drawRoundRect(cardRect, 4f, 4f, paint)
            canvas.drawRoundRect(cardRect, 4f, 4f, strokePaint)

            // Number badge
            paint.color = Color.rgb(27, 94, 32)
            canvas.drawCircle(MARGIN + 20f, itemTop + 24f, 12f, paint)
            paint.color = Color.WHITE
            paint.textSize = 11f
            paint.isFakeBoldText = true
            val numStr = (index + 1).toString()
            canvas.drawText(numStr, MARGIN + 17f, itemTop + 28f, paint)

            // Hindi Text (Teacher Prompt)
            paint.color = Color.rgb(30, 30, 30)
            paint.textSize = 14f
            paint.isFakeBoldText = true
            canvas.drawText(item.hindiPhrase, MARGIN + 42f, itemTop + 24f, paint)

            // Santhali Translation (Target Prompt)
            paint.color = Color.rgb(27, 94, 32)
            paint.textSize = 13f
            paint.isFakeBoldText = true
            val santhaliText = item.santhaliTranslation ?: "Santhali translation demo"
            canvas.drawText("Santhali: $santhaliText", MARGIN + 42f, itemTop + 44f, paint)

            // Pronunciation hint
            if (!item.pronunciation.isNullOrBlank()) {
                paint.color = Color.rgb(100, 100, 100)
                paint.textSize = 10f
                paint.isFakeBoldText = false
                canvas.drawText("(${item.pronunciation})", MARGIN + 42f, itemTop + 62f, paint)
            }

            // Tracing / Student Writing Area
            paint.color = Color.rgb(120, 120, 120)
            paint.textSize = 9f
            canvas.drawText("अभ्यास (Trace / Write):", PAGE_WIDTH - MARGIN - 180f, itemTop + 22f, paint)

            // Dotted response lines
            canvas.drawLine(PAGE_WIDTH - MARGIN - 180f, itemTop + 40f, PAGE_WIDTH - MARGIN - 14f, itemTop + 40f, dotLinePaint)
            canvas.drawLine(PAGE_WIDTH - MARGIN - 180f, itemTop + 62f, PAGE_WIDTH - MARGIN - 14f, itemTop + 62f, dotLinePaint)
        }

        // 5. Footer
        paint.color = Color.rgb(100, 100, 100)
        paint.textSize = 9f
        paint.isFakeBoldText = false
        val footerY = PAGE_HEIGHT - MARGIN + 15f
        canvas.drawText("PALASH VoiceBridge — 100% Offline Educational Tool | Govt. of Jharkhand Initiative", MARGIN, footerY, paint)
        canvas.drawText("Page 1 of 1", PAGE_WIDTH - MARGIN - 50f, footerY, paint)
    }
}
