package com.indiacybercafe.printhub.utils

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.util.Log
import org.apache.poi.hslf.usermodel.HSLFSlideShow
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.hwpf.HWPFDocument
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xslf.usermodel.XMLSlideShow
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.BufferedReader
import java.io.InputStreamReader

object PageCounter {
    private const val TAG = "PAGE_DETECTION"

    @JvmStatic
    fun getPageCount(context: Context, uri: Uri, fileName: String, category: String): Int {
        var finalCategory = category.uppercase()
        
        // If category is generic, try to infer from extension
        if (finalCategory == "OTHER" || finalCategory == "") {
            val ext = fileName.substringAfterLast('.', "").lowercase()
            finalCategory = when (ext) {
                "pdf" -> "PDF"
                "doc", "docx" -> "DOC"
                "xls", "xlsx" -> "XLS"
                "ppt", "pptx" -> "PPT"
                "txt" -> "TXT"
                "jpg", "jpeg", "png", "webp" -> "IMAGE"
                else -> finalCategory
            }
        }

        return try {
            when (finalCategory) {
                "PDF" -> countPdfPages(context, uri)
                "IMAGE" -> 1
                "DOC", "DOCX", "DOCUMENT" -> countDocPages(context, uri, fileName)
                "XLS", "XLSX", "EXCEL" -> countExcelPages(context, uri, fileName)
                "PPT", "PPTX", "POWERPOINT" -> countPptPages(context, uri, fileName)
                "TXT", "TEXT" -> countTxtPages(context, uri)
                else -> 1
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error detecting pages for $fileName: ${e.message}", e)
            -1 // -1 signifies failure
        }
    }

    private fun countPdfPages(context: Context, uri: Uri): Int {
        return try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { descriptor ->
                val renderer = PdfRenderer(descriptor)
                val pageCount = renderer.pageCount
                renderer.close()
                pageCount
            } ?: 0
        } catch (e: Exception) {
            Log.e(TAG, "PDF detection failed", e)
            -1
        }
    }

    private fun countDocPages(context: Context, uri: Uri, fileName: String): Int {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                if (fileName.lowercase().endsWith(".docx")) {
                    val doc = XWPFDocument(inputStream)
                    val props = doc.properties.extendedProperties
                    val pages = props.pages
                    if (pages > 0) pages else {
                        val paragraphs = doc.paragraphs.size
                        Math.max(1, paragraphs / 15)
                    }
                } else if (fileName.lowercase().endsWith(".doc")) {
                    val doc = HWPFDocument(inputStream)
                    val pages = doc.summaryInformation.pageCount
                    if (pages > 0) pages else 1
                } else {
                    1
                }
            } ?: 1
        } catch (e: Exception) {
            Log.e(TAG, "DOC detection failed", e)
            -1
        }
    }

    private fun countExcelPages(context: Context, uri: Uri, fileName: String): Int {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val workbook = WorkbookFactory.create(inputStream)
                var totalPages = 0
                for (i in 0 until workbook.numberOfSheets) {
                    val sheet = workbook.getSheetAt(i)
                    if (sheet.lastRowNum >= 0) {
                        val rows = sheet.lastRowNum + 1
                        totalPages += Math.max(1, (rows + 49) / 50)
                    }
                }
                if (totalPages > 0) totalPages else 1
            } ?: 1
        } catch (e: Exception) {
            Log.e(TAG, "Excel detection failed", e)
            -1
        }
    }

    private fun countPptPages(context: Context, uri: Uri, fileName: String): Int {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                if (fileName.lowercase().endsWith(".pptx")) {
                    val ppt = XMLSlideShow(inputStream)
                    ppt.slides.size
                } else if (fileName.lowercase().endsWith(".ppt")) {
                    val ppt = HSLFSlideShow(inputStream)
                    ppt.slides.size
                } else {
                    1
                }
            } ?: 1
        } catch (e: Exception) {
            Log.e(TAG, "PPT detection failed", e)
            -1
        }
    }

    private fun countTxtPages(context: Context, uri: Uri): Int {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val reader = BufferedReader(InputStreamReader(inputStream))
                var lines = 0
                while (reader.readLine() != null) {
                    lines++
                }
                Math.max(1, (lines + 39) / 40)
            } ?: 1
        } catch (e: Exception) {
            Log.e(TAG, "TXT detection failed", e)
            -1
        }
    }
}
