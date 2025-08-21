package com.pennywiseai.tracker.data.repository

import com.pennywiseai.tracker.data.database.dao.CategoryDao
import com.pennywiseai.tracker.data.database.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao
) {
    
    fun getAllCategories(): Flow<List<CategoryEntity>> {
        return categoryDao.getAllCategories()
    }
    
    fun getExpenseCategories(): Flow<List<CategoryEntity>> {
        return categoryDao.getExpenseCategories()
    }
    
    fun getIncomeCategories(): Flow<List<CategoryEntity>> {
        return categoryDao.getIncomeCategories()
    }
    
    suspend fun getCategoryById(categoryId: Long): CategoryEntity? {
        return categoryDao.getCategoryById(categoryId)
    }
    
    suspend fun getCategoryByName(categoryName: String): CategoryEntity? {
        return categoryDao.getCategoryByName(categoryName)
    }
    
    suspend fun createCategory(
        name: String,
        color: String,
        isIncome: Boolean = false
    ): Long {
        val category = CategoryEntity(
            name = name,
            color = color,
            isSystem = false,
            isIncome = isIncome,
            displayOrder = 999
        )
        return categoryDao.insertCategory(category)
    }
    
    suspend fun updateCategory(category: CategoryEntity) {
        categoryDao.updateCategory(
            category.copy(updatedAt = LocalDateTime.now())
        )
    }
    
    suspend fun deleteCategory(categoryId: Long): Boolean {
        // Only delete non-system categories
        val category = categoryDao.getCategoryById(categoryId)
        if (category != null && !category.isSystem) {
            categoryDao.deleteCategory(categoryId)
            return true
        }
        return false
    }
    
    suspend fun categoryExists(categoryName: String): Boolean {
        return categoryDao.categoryExists(categoryName)
    }
    
    suspend fun initializeDefaultCategories() {
        // Only initialize if no categories exist
        if (categoryDao.getCategoryCount() == 0) {
            val defaultCategories = listOf(
                CategoryEntity(name = "Food & Dining", color = "#FC8019", isSystem = true, isIncome = false, displayOrder = 1),
                CategoryEntity(name = "Groceries", color = "#5AC85A", isSystem = true, isIncome = false, displayOrder = 2),
                CategoryEntity(name = "Transportation", color = "#000000", isSystem = true, isIncome = false, displayOrder = 3),
                CategoryEntity(name = "Shopping", color = "#FF9900", isSystem = true, isIncome = false, displayOrder = 4),
                CategoryEntity(name = "Bills & Utilities", color = "#4CAF50", isSystem = true, isIncome = false, displayOrder = 5),
                CategoryEntity(name = "Entertainment", color = "#E50914", isSystem = true, isIncome = false, displayOrder = 6),
                CategoryEntity(name = "Healthcare", color = "#10847E", isSystem = true, isIncome = false, displayOrder = 7),
                CategoryEntity(name = "Investments", color = "#00D09C", isSystem = true, isIncome = false, displayOrder = 8),
                CategoryEntity(name = "Banking", color = "#004C8F", isSystem = true, isIncome = false, displayOrder = 9),
                CategoryEntity(name = "Personal Care", color = "#6A4C93", isSystem = true, isIncome = false, displayOrder = 10),
                CategoryEntity(name = "Education", color = "#673AB7", isSystem = true, isIncome = false, displayOrder = 11),
                CategoryEntity(name = "Mobile", color = "#2A3890", isSystem = true, isIncome = false, displayOrder = 12),
                CategoryEntity(name = "Fitness", color = "#FF3278", isSystem = true, isIncome = false, displayOrder = 13),
                CategoryEntity(name = "Insurance", color = "#0066CC", isSystem = true, isIncome = false, displayOrder = 14),
                CategoryEntity(name = "Travel", color = "#00BCD4", isSystem = true, isIncome = false, displayOrder = 15),
                CategoryEntity(name = "Salary", color = "#4CAF50", isSystem = true, isIncome = true, displayOrder = 16),
                CategoryEntity(name = "Income", color = "#4CAF50", isSystem = true, isIncome = true, displayOrder = 17),
                CategoryEntity(name = "Others", color = "#757575", isSystem = true, isIncome = false, displayOrder = 18)
            )
            categoryDao.insertCategories(defaultCategories)
        }
    }
}