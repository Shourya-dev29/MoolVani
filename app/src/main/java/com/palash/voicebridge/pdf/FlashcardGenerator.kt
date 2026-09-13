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
 * Offline visual flashcard generator.
 * Creates printable flashcard sheets (4 to 8 cards per A4 page)
 * formatted with Hindi, Santhali, pronunciation, and graphic frame.
 */
class FlashcardGenerator(private val context: Context) {

    companion object {
        const val PAGE_WIDTH = 595
        const val PAGE_HEIGHT = 842
        const val MARGIN = 30f
    }

    suspend fun generate(
        phrases: List<CurriculumEntity>,
        category: String,
        cardsPerPage: Int = 6
    ): File = withContext(Dispatchers.IO) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        drawFlashcards(canvas, phrases, category, cardsPerPage)

        pdfDocument.finishPage(page)

        val outputDir = File(context.cacheDir, "flashcards").apply { mkdirs() }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val sanitizedCat = category.replace(" ", "_").lowercase()
        val outputFile = File(outputDir, "flashcards_${sanitizedCat}_$timestamp.pdf")

        FileOutputStream(outputFile).use { outputStream ->
            pdfDocument.writeTo(outputStream)
        }
        pdfDocument.close()

        outputFile
    }

    private fun drawFlashcards(
        canvas: Canvas,
        phrases: List<CurriculumEntity>,
        category: String,
        cardsPerPage: Int
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Header Title
        paint.color = Color.rgb(27, 94, 32)
        paint.textSize = 16f
        paint.isFakeBoldText = true
        canvas.drawText("PALASH VoiceBridge — Bilingual Flashcards: $category", MARGIN, MARGIN + 10f, paint)

        paint.color = Color.rgb(100, 100, 100)
        paint.textSize = 10f
        paint.isFakeBoldText = false
        canvas.drawText("Cut along dashed lines to create classroom flashcards (Hindi / Santhali)", MARGIN, MARGIN + 26f, paint)

        // Calculate Grid
        val columns = 2
        val rows = if (cardsPerPage <= 4) 2 else if (cardsPerPage <= 6) 3 else 4
        val count = minOf(phrases.size, columns * rows)

        val topOffset = MARGIN + 40f
        val bottomOffset = MARGIN + 25f
        val usableWidth = PAGE_WIDTH - (MARGIN * 2)
        val usableHeight = PAGE_HEIGHT - topOffset - bottomOffset

        val cardGap = 12f
        val cardWidth = (usableWidth - (cardGap * (columns - 1))) / columns
        val cardHeight = (usableHeight - (cardGap * (rows - 1))) / rows

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.2f
            color = Color.rgb(180, 200, 180)
        }

        val cutGuidePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
            color = Color.rgb(200, 200, 200)
        }

        for (i in 0 until count) {
            val item = phrases[i]
            val col = i % columns
            val row = i / columns

            val left = MARGIN + col * (cardWidth + cardGap)
            val top = topOffset + row * (cardHeight + cardGap)
            val right = left + cardWidth
            val bottom = top + cardHeight
            val cardRect = RectF(left, top, right, bottom)

            // Card background & Border
            paint.color = Color.rgb(254, 255, 254)
            canvas.drawRoundRect(cardRect, 8f, 8f, paint)
            canvas.drawRoundRect(cardRect, 8f, 8f, strokePaint)

            // Top Decorative Header Strip
            val stripRect = RectF(left, top, right, top + 22f)
            paint.color = Color.rgb(235, 245, 235)
            canvas.drawRoundRect(stripRect, 8f, 8f, paint)
            canvas.drawRect(left, top + 10f, right, top + 22f, paint) // square off bottom corners of strip

            paint.color = Color.rgb(27, 94, 32)
            paint.textSize = 9f
            paint.isFakeBoldText = true
            canvas.drawText(category.uppercase(), left + 10f, top + 15f, paint)

            val cardNumber = "#${i + 1}"
            canvas.drawText(cardNumber, right - 28f, top + 15f, paint)

            // Center Illustration Frame / Visual placeholder
            val imgFrameTop = top + 32f
            val imgFrameHeight = cardHeight * 0.45f
            val imgRect = RectF(left + 16f, imgFrameTop, right - 16f, imgFrameTop + imgFrameHeight)

            paint.color = Color.rgb(245, 248, 245)
            canvas.drawRoundRect(imgRect, 6f, 6f, paint)
            canvas.drawRoundRect(imgRect, 6f, 6f, cutGuidePaint)

            // Visual glyph placeholder icon
            paint.color = Color.rgb(180, 200, 180)
            canvas.drawCircle(imgRect.centerX(), imgRect.centerY() - 6f, 18f, paint)
            paint.color = Color.rgb(100, 140, 100)
            paint.textSize = 10f
            paint.isFakeBoldText = true
            val iconLabel = item.topic.take(10)
            canvas.drawText("VISUAL", imgRect.centerX() - 18f, imgRect.centerY() + 18f, paint)

            // Hindi Text (Large bold)
            var textY = imgFrameTop + imgFrameHeight + 24f
            paint.color = Color.rgb(20, 20, 20)
            paint.textSize = 15f
            paint.isFakeBoldText = true
            canvas.drawText(item.hindiPhrase, left + 14f, textY, paint)

            // Santhali Translation
            textY += 20f
            paint.color = Color.rgb(27, 94, 32)
            paint.textSize = 13f
            paint.isFakeBoldText = true
            val santhaliText = item.santhaliTranslation ?: "Santhali demo"
            canvas.drawText("Santhali: $santhaliText", left + 14f, textY, paint)

            // Pronunciation
            if (!item.pronunciation.isNullOrBlank()) {
                textY += 16f
                paint.color = Color.rgb(100, 100, 100)
                paint.textSize = 9.5f
                paint.isFakeBoldText = false
                canvas.drawText("Pronunciation: ${item.pronunciation}", left + 14f, textY, paint)
            }
        }

        // Footer note
        paint.color = Color.rgb(120, 120, 120)
        paint.textSize = 8.5f
        paint.isFakeBoldText = false
        canvas.drawText("PALASH VoiceBridge Flashcard System | Mother Tongue-Based Primary Education (Jharkhand)", MARGIN, PAGE_HEIGHT - 12f, paint)
    }
}
