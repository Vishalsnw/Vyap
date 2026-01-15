package com.invoicepro.app.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.invoicepro.app.model.Invoice
import java.io.File
import java.io.FileOutputStream

object PdfGenerator {
    fun generateInvoicePdf(context: Context, invoice: Invoice, fileName: String): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        val paint = Paint()

        paint.textSize = 24f
        canvas.drawText("Invoice: ${invoice.invoiceNumber}", 50f, 50f, paint)
        
        paint.textSize = 16f
        canvas.drawText("Total: ₹${invoice.total}", 50f, 100f, paint)

        pdfDocument.finishPage(page)

        val file = File(context.getExternalFilesDir(null), "$fileName.pdf")
        return try {
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
