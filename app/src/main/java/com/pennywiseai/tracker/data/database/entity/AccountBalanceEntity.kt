package com.pennywiseai.tracker.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity(
    tableName = "account_balances",
    indices = [
        Index(value = ["bank_name", "account_last4", "timestamp"], unique = true),
        Index(value = ["bank_name", "account_last4"]),
        Index(value = ["timestamp"])
    ]
)
data class AccountBalanceEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    
    @ColumnInfo(name = "bank_name")
    val bankName: String,
    
    @ColumnInfo(name = "account_last4")
    val accountLast4: String,
    
    @ColumnInfo(name = "balance")
    val balance: BigDecimal,
    
    @ColumnInfo(name = "timestamp")
    val timestamp: LocalDateTime,
    
    @ColumnInfo(name = "transaction_id")
    val transactionId: Long? = null,
    
    @ColumnInfo(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now()
)