package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.engine.MenuResponseParser
import java.io.File
import java.io.FileOutputStream

object ExportShareHelper {

    // Share Text or DOC file (.txt or .doc)
    fun shareTextFile(context: Context, fileName: String, extension: String, content: String) {
        try {
            val cacheDir = File(context.cacheDir, "exports")
            if (!cacheDir.exists()) cacheDir.mkdirs()

            val sanitizedFileName = fileName.replace("[\\\\/:*?\"<>|]".toRegex(), "_")
            val file = File(cacheDir, "$sanitizedFileName.$extension")
            
            FileOutputStream(file).use { out ->
                out.write(content.toByteArray(Charsets.UTF_8))
            }

            val fileUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = when (extension.lowercase()) {
                    "doc", "docx" -> "application/msword"
                    else -> "text/plain"
                }
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_SUBJECT, fileName)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(intent, "แบ่งปันสอดไฟล์ $fileName ผ่าน:")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "เกิดข้อผิดพลาดในการส่งออก: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    // Share PDF file using native PdfDocument
    fun sharePdfFile(context: Context, fileName: String, title: String, text: String) {
        try {
            val cacheDir = File(context.cacheDir, "exports")
            if (!cacheDir.exists()) cacheDir.mkdirs()

            val sanitizedFileName = fileName.replace("[\\\\/:*?\"<>|]".toRegex(), "_")
            val file = File(cacheDir, "$sanitizedFileName.pdf")

            val pdfDocument = PdfDocument()
            val textPaint = Paint().apply {
                textSize = 12f
                isAntiAlias = true
                color = android.graphics.Color.BLACK
            }
            val titlePaint = Paint().apply {
                textSize = 18f
                isAntiAlias = true
                color = android.graphics.Color.parseColor("#6750A4")
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            val lines = text.split("\n")
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 width (595), height (842)
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas

            var y = 50f

            // Draw header / title
            canvas.drawText("MengAiRec Smart Document Summary", 40f, y, titlePaint)
            y += 25f
            canvas.drawText("หัวข้อ: $title", 40f, y, textPaint)
            y += 35f

            // Divider line
            val linePaint = Paint().apply {
                color = android.graphics.Color.LTGRAY
                strokeWidth = 1f
            }
            canvas.drawLine(40f, y, 555f, y, linePaint)
            y += 25f

            for (line in lines) {
                if (y > 800f) {
                    pdfDocument.finishPage(page)
                    val newPageInfo = PdfDocument.PageInfo.Builder(595, 842, pdfDocument.pages.size + 1).create()
                    page = pdfDocument.startPage(newPageInfo)
                    canvas = page.canvas
                    y = 50f
                }
                
                // Wrap text manually if too wide
                var textToDraw = line.trim()
                while (textToDraw.length > 70) {
                    val chunk = textToDraw.substring(0, 70)
                    canvas.drawText(chunk, 40f, y, textPaint)
                    y += 18f
                    textToDraw = textToDraw.substring(70)
                    
                    if (y > 800f) {
                        pdfDocument.finishPage(page)
                        val newPageInfo = PdfDocument.PageInfo.Builder(595, 842, pdfDocument.pages.size + 1).create()
                        page = pdfDocument.startPage(newPageInfo)
                        canvas = page.canvas
                        y = 50f
                    }
                }
                
                canvas.drawText(textToDraw, 40f, y, textPaint)
                y += 18f
            }
            pdfDocument.finishPage(page)

            FileOutputStream(file).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()

            val fileUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_SUBJECT, fileName)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(intent, "แบ่งปันสอดไฟล์ PDF ผ่าน:")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "เกิดข้อผิดพลาดในการส่งออก PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    // Share MindMap as PNG Image
    fun shareMindMapPng(context: Context, fileName: String, title: String, json: String) {
        try {
            val cacheDir = File(context.cacheDir, "exports")
            if (!cacheDir.exists()) cacheDir.mkdirs()

            val sanitizedFileName = fileName.replace("[\\\\/:*?\"<>|]".toRegex(), "_")
            val file = File(cacheDir, "$sanitizedFileName.png")

            val data = MenuResponseParser.parseMindMap(json)
            val tree = data?.mind_map

            val width = 1200
            val height = 800
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // Draw background
            canvas.drawColor(android.graphics.Color.parseColor("#F7F2FA"))

            val paint = Paint().apply { isAntiAlias = true }

            // Title banner
            paint.color = android.graphics.Color.parseColor("#6750A4")
            paint.textSize = 28f
            paint.textAlign = Paint.Align.CENTER
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("MengAiRec Mindmap: $title", (width / 2).toFloat(), 65f, paint)

            paint.textSize = 14f
            paint.color = android.graphics.Color.GRAY
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("Generated by Ultimate Engine", (width / 2).toFloat(), 100f, paint)

            val rootText = tree?.root ?: "ผังความคิดแกนสำคัญ"
            val rootX = (width / 2)
            val rootY = 220

            // Draw Central Node Box
            paint.color = android.graphics.Color.parseColor("#D0BCFF")
            paint.textSize = 22f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val rootTextWidth = paint.measureText(rootText)
            val rPad = 25

            canvas.drawRoundRect(
                (rootX - rootTextWidth / 2 - rPad), (rootY - 40).toFloat(),
                (rootX + rootTextWidth / 2 + rPad), (rootY + 30).toFloat(),
                20f, 20f, paint
            )

            paint.color = android.graphics.Color.parseColor("#1D192B")
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText(rootText, rootX.toFloat(), (rootY + 5).toFloat(), paint)

            // Draw Branches
            val branches = tree?.branches ?: emptyList()
            val numBranches = branches.size
            if (numBranches > 0) {
                val branchY = 440
                val startX = 150
                val endX = width - 150
                val spacing = if (numBranches > 1) (endX - startX) / (numBranches - 1) else 0

                val connectorPaint = Paint().apply {
                    color = android.graphics.Color.parseColor("#9070E0")
                    strokeWidth = 3f
                    isAntiAlias = true
                    style = Paint.Style.STROKE
                }

                branches.forEachIndexed { i, branch ->
                    val bX = startX + i * spacing
                    val bY = branchY

                    // Bezier or standard connector lines
                    canvas.drawLine(rootX.toFloat(), (rootY + 30).toFloat(), bX.toFloat(), (bY - 45).toFloat(), connectorPaint)

                    // Branch node
                    paint.color = android.graphics.Color.parseColor("#E8DEF8")
                    paint.textSize = 15f
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    
                    val bText = branch.main_idea
                    val bTextWidth = paint.measureText(bText).coerceAtLeast(140f)

                    canvas.drawRoundRect(
                        (bX - bTextWidth / 2 - 12), (bY - 40).toFloat(),
                        (bX + bTextWidth / 2 + 12), (bY + 20).toFloat(),
                        12f, 12f, paint
                    )

                    paint.color = android.graphics.Color.parseColor("#1D192B")
                    canvas.drawText(bText, bX.toFloat(), (bY - 2).toFloat(), paint)

                    // Sub-node items
                    var subY = bY + 65
                    branch.sub_ideas.take(5).forEach { sub ->
                        // Link line
                        paint.color = android.graphics.Color.parseColor("#CAC4D0")
                        paint.strokeWidth = 2f
                        canvas.drawLine(bX.toFloat(), (bY + 20).toFloat(), bX.toFloat(), (subY - 15).toFloat(), paint)

                        // Sub bubble
                        paint.color = android.graphics.Color.WHITE
                        paint.textSize = 12f
                        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                        val sTextWidth = paint.measureText(sub).coerceAtLeast(110f)

                        canvas.drawRoundRect(
                            (bX - sTextWidth / 2 - 10), (subY - 20).toFloat(),
                            (bX + sTextWidth / 2 + 10), (subY + 14).toFloat(),
                            10f, 10f, paint
                        )

                        paint.color = android.graphics.Color.parseColor("#49454F")
                        
                        // Handle long sub lines wrapping slightly
                        var disp = sub
                        if (disp.length > 25) {
                            disp = disp.substring(0, 22) + "..."
                        }
                        canvas.drawText(disp, bX.toFloat(), (subY - 1).toFloat(), paint)

                        subY += 55
                    }
                }
            } else {
                // If there are no branches, render placeholder circles
                paint.color = android.graphics.Color.parseColor("#E8DEF8")
                canvas.drawCircle((width / 2).toFloat(), 450f, 60f, paint)
                paint.color = android.graphics.Color.parseColor("#1D192B")
                paint.textSize = 14f
                canvas.drawText("ไม่มีสาขาย่อย", (width / 2).toFloat(), 455f, paint)
            }

            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            val fileUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_SUBJECT, fileName)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(intent, "แบ่งปันรูปผังความคิด (PNG) ผ่าน:")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "เกิดข้อผิดพลาดในการส่งออกภาพ: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}
