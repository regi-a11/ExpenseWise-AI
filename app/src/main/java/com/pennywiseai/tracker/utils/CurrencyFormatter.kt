package com.pennywiseai.tracker.utils

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

/**
 * Utility object for formatting currency values
 */
object CurrencyFormatter {
    
    private val INDIAN_LOCALE = Locale.Builder().setLanguage("en").setRegion("IN").build()
    
    /**
     * Formats a BigDecimal amount as currency using Indian locale
     */
    fun formatCurrency(amount: BigDecimal): String {
        val formatter = NumberFormat.getCurrencyInstance(INDIAN_LOCALE)
        // Show decimals only if they exist
        formatter.minimumFractionDigits = 0
        formatter.maximumFractionDigits = 2
        return formatter.format(amount)
    }
    
    /**
     * Formats a Double amount as currency using Indian locale
     */
    fun formatCurrency(amount: Double): String {
        return formatCurrency(amount.toBigDecimal())
    }
}