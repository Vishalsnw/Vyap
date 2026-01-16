package com.invoicepro.app.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.invoicepro.app.model.BusinessProfile
import com.invoicepro.app.model.Customer
import com.invoicepro.app.model.Invoice
import com.invoicepro.app.model.InvoiceItem
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PdfGenerator(private val context: Context) {

    fun generateInvoicePdf(
        business: BusinessProfile,
        customer: Customer,
        invoice: Invoice,
        items: List<InvoiceItem>
    ): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        var y = 50f

        // Business Header
        paint.textSize = 20f
        paint.isFakeBoldText = true
        canvas.drawText(business.name, 50f, y, paint)
        y += 25f

        paint.textSize = 12f
        paint.isFakeBoldText = false
        canvas.drawText(business.address, 50f, y, paint)
        y += 15f
        canvas.drawText("Phone: ${business.phone}", 50f, y, paint)
        y += 15f
        business.gstin?.let {
            canvas.drawText("GSTIN: $it", 50f, y, paint)
            y += 15f
        }

        y += 20f
        paint.strokeWidth = 1f
        canvas.drawLine(50f, y, 545f, y, paint)
        y += 30f

        // Invoice Details
        paint.textSize = 14f
        paint.isFakeBoldText = true
        canvas.drawText("INVOICE", 50f, y, paint)
        paint.isFakeBoldText = false
        paint.textSize = 12f
        canvas.drawText("No: ${invoice.invoiceNumber}", 400f, y, paint)
        y += 20f
        canvas.drawText("Date: ${dateFormat.format(Date(invoice.date))}", 400f, y, paint)
        y += 30f

        // Bill To
        paint.isFakeBoldText = true
        canvas.drawText("Bill To:", 50f, y, paint)
        y += 15f
        paint.isFakeBoldText = false
        canvas.drawText(customer.name, 50f, y, paint)
        y += 15f
        canvas.drawText(customer.address, 50f, y, paint)
        y += 15f
        canvas.drawText("Phone: ${customer.phone}", 50f, y, paint)
        y += 40f

        // Table Header
        paint.isFakeBoldText = true
        canvas.drawText("Item", 50f, y, paint)
        canvas.drawText("Qty", 300f, y, paint)
        canvas.drawText("Rate", 380f, y, paint)
        canvas.drawText("Total", 480f, y, paint)
        y += 10f
        canvas.drawLine(50f, y, 545f, y, paint)
        y += 20f

        // Table Items
        paint.isFakeBoldText = false
        items.forEach { item ->
            canvas.drawText(item.productName, 50f, y, paint)
            canvas.drawText(item.quantity.toString(), 300f, y, paint)
            canvas.drawText("%.2f".format(item.rate), 380f, y, paint)
            canvas.drawText("%.2f".format(item.amount), 480f, y, paint)
            y += 20f
        }

        y += 20f
        canvas.drawLine(350f, y, 545f, y, paint)
        y += 30f

        // Totals
        paint.isFakeBoldText = true
        canvas.drawText("Subtotal:", 380f, y, paint)
        canvas.drawText("₹%.2f".format(invoice.subtotal), 480f, y, paint)
        y += 20f
        canvas.drawText("GST:", 380f, y, paint)
        canvas.drawText("₹%.2f".format(invoice.cgst + invoice.sgst + invoice.igst), 480f, y, paint)
        y += 25f
        paint.textSize = 16f
        canvas.drawText("Grand Total:", 380f, y, paint)
        canvas.drawText("₹%.2f".format(invoice.total), 480f, y, paint)

        pdfDocument.finishPage(page)

        val file = File(context.getExternalFilesDir(null), "${invoice.invoiceNumber}.pdf")
        return try {
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            file
        } catch (e: Exception) {
            pdfDocument.close()
            null
        }
    }
}