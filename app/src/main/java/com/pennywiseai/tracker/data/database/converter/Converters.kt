package com.pennywiseai.tracker.data.database.converter

import androidx.room.TypeConverter
import com.pennywiseai.tracker.data.database.entity.SubscriptionState
import com.pennywiseai.tracker.data.database.entity.TransactionType
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class Converters {
    // Handle both ISO format (2025-08-01T18:17:16) and space format (2025-08-01 18:17:16)
    private val dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    private val dateTimeFormatterWithSpace = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    
    @TypeConverter
    fun fromBigDecimal(value: BigDecimal?): String? {
        return value?.toString()
    }
    
    @TypeConverter
    fun toBigDecimal(value: String?): BigDecimal? {
        return value?.let { BigDecimal(it) }
    }
    
    @TypeConverter
    fun fromLocalDateTime(value: LocalDateTime?): String? {
        return value?.format(dateTimeFormatter)
    }
    
    @TypeConverter
    fun toLocalDateTime(value: String?): LocalDateTime? {
        return value?.let { dateStr ->
            try {
                // Try ISO format first (with 'T')
                LocalDateTime.parse(dateStr, dateTimeFormatter)
            } catch (e: Exception) {
                // Fall back to space format
                LocalDateTime.parse(dateStr, dateTimeFormatterWithSpace)
            }
        }
    }
    
    @TypeConverter
    fun fromLocalDate(value: LocalDate?): String? {
        return value?.format(dateFormatter)
    }
    
    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? {
        return value?.let { LocalDate.parse(it, dateFormatter) }
    }
    
    @TypeConverter
    fun fromTransactionType(value: TransactionType): String {
        return value.name
    }
    
    @TypeConverter
    fun toTransactionType(value: String): TransactionType {
        return TransactionType.valueOf(value)
    }
    
    @TypeConverter
    fun fromSubscriptionState(value: SubscriptionState): String {
        return value.name
    }
    
    @TypeConverter
    fun toSubscriptionState(value: String): SubscriptionState {
        return SubscriptionState.valueOf(value)
    }
}