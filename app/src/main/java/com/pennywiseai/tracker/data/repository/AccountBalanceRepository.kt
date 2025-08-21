package com.pennywiseai.tracker.data.repository

import com.pennywiseai.tracker.data.database.dao.AccountBalanceDao
import com.pennywiseai.tracker.data.database.entity.AccountBalanceEntity
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountBalanceRepository @Inject constructor(
    private val accountBalanceDao: AccountBalanceDao
) {
    
    suspend fun insertBalance(balance: AccountBalanceEntity): Long {
        return accountBalanceDao.insertBalance(balance)
    }
    
    suspend fun getLatestBalance(bankName: String, accountLast4: String): AccountBalanceEntity? {
        return accountBalanceDao.getLatestBalance(bankName, accountLast4)
    }
    
    fun getLatestBalanceFlow(bankName: String, accountLast4: String): Flow<AccountBalanceEntity?> {
        return accountBalanceDao.getLatestBalanceFlow(bankName, accountLast4)
    }
    
    fun getAllLatestBalances(): Flow<List<AccountBalanceEntity>> {
        return accountBalanceDao.getAllLatestBalances()
    }
    
    fun getTotalBalance(): Flow<BigDecimal?> {
        return accountBalanceDao.getTotalBalance()
    }
    
    fun getBalanceHistory(
        bankName: String,
        accountLast4: String,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): Flow<List<AccountBalanceEntity>> {
        return accountBalanceDao.getBalanceHistory(bankName, accountLast4, startDate, endDate)
    }
    
    fun getAccountCount(): Flow<Int> {
        return accountBalanceDao.getAccountCount()
    }
    
    suspend fun deleteOldBalances(beforeDate: LocalDateTime): Int {
        return accountBalanceDao.deleteOldBalances(beforeDate)
    }
    
    suspend fun updateBalance(balance: AccountBalanceEntity) {
        accountBalanceDao.updateBalance(balance)
    }
    
    suspend fun deleteBalance(balance: AccountBalanceEntity) {
        accountBalanceDao.deleteBalance(balance)
    }
    
    /**
     * Inserts a balance record from a transaction if it has balance information
     */
    suspend fun insertBalanceFromTransaction(
        bankName: String?,
        accountLast4: String?,
        balance: BigDecimal?,
        timestamp: LocalDateTime,
        transactionId: Long
    ) {
        if (bankName != null && accountLast4 != null && balance != null) {
            val balanceEntity = AccountBalanceEntity(
                bankName = bankName,
                accountLast4 = accountLast4,
                balance = balance,
                timestamp = timestamp,
                transactionId = transactionId
            )
            insertBalance(balanceEntity)
        }
    }
}