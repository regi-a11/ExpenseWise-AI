package com.pennywiseai.tracker.utils

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * Utility functions for currency formatting
 */
object CurrencyUtils {
    
    private val indianLocale = Locale("en", "IN")
    private val indianCurrencyFormat = NumberFormat.getCurrencyInstance(indianLocale).apply {
        currency = Currency.getInstance("INR")
        maximumFractionDigits = 0 // No decimal places for whole amounts
    }
    
    /**
     * Formats a BigDecimal amount as Indian Rupees
     * @param amount The amount to format
     * @return Formatted string like "₹1,234" or "₹1,23,456"
     */
    fun formatCurrency(amount: BigDecimal): String {
        // For amounts with decimals, show them
        return if (amount.stripTrailingZeros().scale() > 0) {
            val formatter = NumberFormat.getCurrencyInstance(indianLocale).apply {
                currency = Currency.getInstance("INR")
                maximumFractionDigits = 2
                minimumFractionDigits = 1
            }
            formatter.format(amount)
        } else {
            indianCurrencyFormat.format(amount)
        }
    }
    
    /**
     * Formats a Double amount as Indian Rupees
     */
    fun formatCurrency(amount: Double): String {
        return formatCurrency(BigDecimal.valueOf(amount))
    }
    
    /**
     * Formats an Int amount as Indian Rupees
     */
    fun formatCurrency(amount: Int): String {
        return formatCurrency(BigDecimal(amount))
    }
    
    /**
     * Formats an amount with a custom number of decimal places
     */
    fun formatCurrency(amount: BigDecimal, decimalPlaces: Int): String {
        val formatter = NumberFormat.getCurrencyInstance(indianLocale).apply {
            currency = Currency.getInstance("INR")
            maximumFractionDigits = decimalPlaces
            minimumFractionDigits = decimalPlaces
        }
        return formatter.format(amount)
    }
}