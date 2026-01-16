package com.invoicepro.app.util

import android.content.Context
import android.content.SharedPreferences

class PreferenceManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("InvoiceProPrefs", Context.MODE_PRIVATE)

    fun isProVersion(): Boolean {
        return prefs.getBoolean("is_pro", false)
    }

    fun setProVersion(isPro: Boolean) {
        prefs.edit().putBoolean("is_pro", isPro).apply()
    }

    fun getInvoiceCount(): Int {
        return prefs.getInt("invoice_count", 0)
    }

    fun incrementInvoiceCount() {
        prefs.edit().putInt("invoice_count", getInvoiceCount() + 1).apply()
    }
}