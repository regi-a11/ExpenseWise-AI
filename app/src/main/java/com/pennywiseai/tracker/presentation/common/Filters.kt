package com.pennywiseai.tracker.presentation.common

import java.time.LocalDate
import java.time.YearMonth

enum class TimePeriod(val label: String) {
    THIS_MONTH("This Month"),
    LAST_MONTH("Last Month"),
    LAST_3_MONTHS("Last 3 Months"),
    ALL("All Time")
}

enum class TransactionTypeFilter(val label: String) {
    ALL("All"),
    INCOME("Income"),
    EXPENSE("Expense"),
    CREDIT("Credit"),
    TRANSFER("Transfer"),
    INVESTMENT("Investment")
}

fun getDateRangeForPeriod(period: TimePeriod): Pair<LocalDate, LocalDate> {
    val today = LocalDate.now()
    return when (period) {
        TimePeriod.THIS_MONTH -> {
            val start = YearMonth.now().atDay(1)
            start to today
        }
        TimePeriod.LAST_MONTH -> {
            val lastMonth = YearMonth.now().minusMonths(1)
            val start = lastMonth.atDay(1)
            val end = lastMonth.atEndOfMonth()
            start to end
        }
        TimePeriod.LAST_3_MONTHS -> {
            val start = today.minusMonths(3)
            start to today
        }
        TimePeriod.ALL -> {
            // Use a reasonable date range for "All Time" - 10 years back to today
            val start = today.minusYears(10)
            start to today
        }
    }
}