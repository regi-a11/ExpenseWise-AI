package com.pennywiseai.tracker.ui.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Centralized category mapping system for consistent categorization
 * across transactions and subscriptions
 */
object CategoryMapping {
    
    data class CategoryInfo(
        val displayName: String,
        val icon: ImageVector,
        val color: Color,
        val fallbackIcon: ImageVector = Icons.Default.Category
    )
    
    // Define all categories with their visual properties
    val categories = mapOf(
        "Food & Dining" to CategoryInfo(
            displayName = "Food & Dining",
            icon = Icons.Default.Restaurant,
            color = Color(0xFFFC8019), // Swiggy orange
            fallbackIcon = Icons.Default.Fastfood
        ),
        "Groceries" to CategoryInfo(
            displayName = "Groceries",
            icon = Icons.Default.ShoppingCart,
            color = Color(0xFF5AC85A), // BigBasket green
            fallbackIcon = Icons.Default.LocalGroceryStore
        ),
        "Transportation" to CategoryInfo(
            displayName = "Transportation",
            icon = Icons.Default.DirectionsCar,
            color = Color(0xFF000000), // Uber black
            fallbackIcon = Icons.Default.Commute
        ),
        "Shopping" to CategoryInfo(
            displayName = "Shopping",
            icon = Icons.Default.ShoppingBag,
            color = Color(0xFFFF9900), // Amazon orange
            fallbackIcon = Icons.Default.Store
        ),
        "Bills & Utilities" to CategoryInfo(
            displayName = "Bills & Utilities",
            icon = Icons.Default.Receipt,
            color = Color(0xFF4CAF50), // Utility green
            fallbackIcon = Icons.Default.Payment
        ),
        "Entertainment" to CategoryInfo(
            displayName = "Entertainment",
            icon = Icons.Default.MovieFilter,
            color = Color(0xFFE50914), // Netflix red
            fallbackIcon = Icons.Default.PlayCircle
        ),
        "Healthcare" to CategoryInfo(
            displayName = "Healthcare",
            icon = Icons.Default.LocalHospital,
            color = Color(0xFF10847E), // PharmEasy teal
            fallbackIcon = Icons.Default.HealthAndSafety
        ),
        "Investments" to CategoryInfo(
            displayName = "Investments",
            icon = Icons.AutoMirrored.Filled.TrendingUp,
            color = Color(0xFF00D09C), // Groww green
            fallbackIcon = Icons.AutoMirrored.Filled.ShowChart
        ),
        "Banking" to CategoryInfo(
            displayName = "Banking",
            icon = Icons.Default.AccountBalance,
            color = Color(0xFF004C8F), // HDFC blue
            fallbackIcon = Icons.Default.AccountBalanceWallet
        ),
        "Personal Care" to CategoryInfo(
            displayName = "Personal Care",
            icon = Icons.Default.Face,
            color = Color(0xFF6A4C93), // Urban Company purple
            fallbackIcon = Icons.Default.Spa
        ),
        "Education" to CategoryInfo(
            displayName = "Education",
            icon = Icons.Default.School,
            color = Color(0xFF673AB7), // Byju's purple
            fallbackIcon = Icons.Default.Book
        ),
        "Mobile" to CategoryInfo(
            displayName = "Mobile & Recharge",
            icon = Icons.Default.Smartphone,
            color = Color(0xFF2A3890), // Jio blue
            fallbackIcon = Icons.Default.PhoneAndroid
        ),
        "Fitness" to CategoryInfo(
            displayName = "Fitness",
            icon = Icons.Default.FitnessCenter,
            color = Color(0xFFFF3278), // Cult.fit pink
            fallbackIcon = Icons.Default.SportsMartialArts
        ),
        "Insurance" to CategoryInfo(
            displayName = "Insurance",
            icon = Icons.Default.Shield,
            color = Color(0xFF0066CC), // LIC blue
            fallbackIcon = Icons.Default.Security
        ),
        "Salary" to CategoryInfo(
            displayName = "Salary",
            icon = Icons.Default.Payments,
            color = Color(0xFF4CAF50), // Income green
            fallbackIcon = Icons.Default.AttachMoney
        ),
        "Income" to CategoryInfo(
            displayName = "Other Income",
            icon = Icons.Default.AddCircle,
            color = Color(0xFF4CAF50), // Income green
            fallbackIcon = Icons.AutoMirrored.Filled.TrendingUp
        ),
        "Others" to CategoryInfo(
            displayName = "Others",
            icon = Icons.Default.Category,
            color = Color(0xFF757575), // Grey
            fallbackIcon = Icons.Default.MoreHoriz
        )
    )
    
    /**
     * Get category for a merchant name (unified logic)
     */
    fun getCategory(merchantName: String): String {
        val merchantLower = merchantName.lowercase()
        
        return when {
            // Food & Dining
            isFoodMerchant(merchantLower) -> "Food & Dining"
            
            // Groceries
            isGroceryMerchant(merchantLower) -> "Groceries"
            
            // Transportation
            isTransportMerchant(merchantLower) -> "Transportation"
            
            // Shopping
            isShoppingMerchant(merchantLower) -> "Shopping"
            
            // Bills & Utilities
            isUtilityMerchant(merchantLower) -> "Bills & Utilities"
            
            // Entertainment
            isEntertainmentMerchant(merchantLower) -> "Entertainment"
            
            // Healthcare
            isHealthcareMerchant(merchantLower) -> "Healthcare"
            
            // Investment
            isInvestmentMerchant(merchantLower) -> "Investments"
            
            // Banking
            isBankingMerchant(merchantLower) -> "Banking"
            
            // Personal Care
            isPersonalCareMerchant(merchantLower) -> "Personal Care"
            
            // Education
            isEducationMerchant(merchantLower) -> "Education"
            
            // Mobile
            isMobileMerchant(merchantLower) -> "Mobile"
            
            // Fitness
            isFitnessMerchant(merchantLower) -> "Fitness"
            
            // Insurance
            isInsuranceMerchant(merchantLower) -> "Insurance"
            
            else -> "Others"
        }
    }
    
    // Merchant detection functions
    private fun isFoodMerchant(merchant: String) = 
        merchant.contains("swiggy") || merchant.contains("zomato") || 
        merchant.contains("dominos") || merchant.contains("pizza") ||
        merchant.contains("burger") || merchant.contains("kfc") ||
        merchant.contains("mcdonalds") || merchant.contains("restaurant") ||
        merchant.contains("cafe") || merchant.contains("food") ||
        merchant.contains("starbucks") || merchant.contains("haldiram") ||
        merchant.contains("barbeque")
    
    private fun isGroceryMerchant(merchant: String) = 
        merchant.contains("bigbasket") || merchant.contains("blinkit") ||
        merchant.contains("zepto") || merchant.contains("grofers") ||
        merchant.contains("jiomart") || merchant.contains("dmart") ||
        merchant.contains("reliance fresh") || merchant.contains("more") ||
        merchant.contains("grocery") || merchant.contains("dunzo")
    
    private fun isTransportMerchant(merchant: String) = 
        merchant.contains("uber") || merchant.contains("ola") ||
        merchant.contains("rapido") || merchant.contains("metro") ||
        merchant.contains("irctc") || merchant.contains("redbus") ||
        merchant.contains("makemytrip") || merchant.contains("goibibo") ||
        merchant.contains("petrol") || merchant.contains("fuel") ||
        merchant.contains("parking") || merchant.contains("toll") ||
        merchant.contains("fastag") || merchant.contains("indigo") ||
        merchant.contains("air india") || merchant.contains("spicejet") ||
        merchant.contains("vistara") || merchant.contains("cleartrip")
    
    private fun isShoppingMerchant(merchant: String) = 
        merchant.contains("amazon") || merchant.contains("flipkart") ||
        merchant.contains("myntra") || merchant.contains("ajio") ||
        merchant.contains("nykaa") || merchant.contains("meesho") ||
        merchant.contains("snapdeal") || merchant.contains("shopclues") ||
        merchant.contains("firstcry") || merchant.contains("pepperfry") ||
        merchant.contains("urban ladder") || merchant.contains("store") ||
        merchant.contains("mart") && !merchant.contains("jiomart") && !merchant.contains("dmart")
    
    private fun isUtilityMerchant(merchant: String) = 
        merchant.contains("electricity") || merchant.contains("water") ||
        merchant.contains("gas") || merchant.contains("broadband") ||
        merchant.contains("wifi") || merchant.contains("internet") ||
        merchant.contains("tata sky") || merchant.contains("dish") ||
        merchant.contains("d2h") || merchant.contains("bill") ||
        merchant.contains("tata power") || merchant.contains("adani") ||
        merchant.contains("bses") || merchant.contains("act fibernet")
    
    private fun isEntertainmentMerchant(merchant: String) = 
        merchant.contains("netflix") || merchant.contains("spotify") ||
        merchant.contains("prime") || merchant.contains("hotstar") ||
        merchant.contains("sony liv") || merchant.contains("zee5") ||
        merchant.contains("voot") || merchant.contains("youtube") ||
        merchant.contains("cinema") || merchant.contains("pvr") ||
        merchant.contains("inox") || merchant.contains("bookmyshow") ||
        merchant.contains("gaana") || merchant.contains("jiosaavn") ||
        merchant.contains("apple music") || merchant.contains("wynk")
    
    private fun isHealthcareMerchant(merchant: String) = 
        merchant.contains("1mg") || merchant.contains("pharmeasy") ||
        merchant.contains("netmeds") || merchant.contains("apollo") ||
        merchant.contains("pharmacy") || merchant.contains("medical") ||
        merchant.contains("hospital") || merchant.contains("clinic") ||
        merchant.contains("doctor") || merchant.contains("practo") ||
        merchant.contains("healthkart") || merchant.contains("truemeds")
    
    private fun isInvestmentMerchant(merchant: String) = 
        merchant.contains("groww") || merchant.contains("zerodha") ||
        merchant.contains("upstox") || merchant.contains("kuvera") ||
        merchant.contains("paytm money") || merchant.contains("coin") ||
        merchant.contains("smallcase") || merchant.contains("mutual fund") ||
        merchant.contains("sip") || merchant.contains("angel") ||
        merchant.contains("5paisa") || merchant.contains("etmoney")
    
    private fun isBankingMerchant(merchant: String) = 
        merchant.contains("hdfc") || merchant.contains("icici") ||
        merchant.contains("axis") || merchant.contains("sbi") ||
        merchant.contains("kotak") || merchant.contains("bank") ||
        merchant.contains("loan") || merchant.contains("emi") ||
        merchant.contains("credit card") || merchant.contains("yes bank") ||
        merchant.contains("idfc") || merchant.contains("indusind") ||
        merchant.contains("pnb") || merchant.contains("canara") ||
        merchant.contains("union bank") || merchant.contains("rbl")
    
    private fun isPersonalCareMerchant(merchant: String) = 
        merchant.contains("urban company") || merchant.contains("salon") ||
        merchant.contains("spa") || merchant.contains("barber") ||
        merchant.contains("beauty") || merchant.contains("grooming") ||
        merchant.contains("housejoy")
    
    private fun isEducationMerchant(merchant: String) = 
        merchant.contains("byju") || merchant.contains("unacademy") ||
        merchant.contains("vedantu") || merchant.contains("coursera") ||
        merchant.contains("udemy") || merchant.contains("upgrade") ||
        merchant.contains("school") || merchant.contains("college") ||
        merchant.contains("university") || merchant.contains("toppr") ||
        merchant.contains("udacity") || merchant.contains("simplilearn") ||
        merchant.contains("whitehat") || merchant.contains("great learning")
    
    private fun isMobileMerchant(merchant: String) = 
        merchant.contains("airtel") || merchant.contains("jio") ||
        merchant.contains("vodafone") || merchant.contains("idea") ||
        merchant.contains("bsnl") || merchant.contains("recharge") ||
        merchant.contains("prepaid") || merchant.contains("postpaid") ||
        merchant.contains("mobile")
    
    private fun isFitnessMerchant(merchant: String) = 
        merchant.contains("cult") || merchant.contains("gym") ||
        merchant.contains("fitness") || merchant.contains("yoga") ||
        merchant.contains("healthifyme") || merchant.contains("fitternity") ||
        merchant.contains("gold's gym") || merchant.contains("anytime fitness")
    
    private fun isInsuranceMerchant(merchant: String) = 
        merchant.contains("insurance") || merchant.contains("lic") ||
        merchant.contains("policy") || merchant.contains("hdfc life") ||
        merchant.contains("icici pru") || merchant.contains("sbi life") ||
        merchant.contains("max life") || merchant.contains("bajaj allianz") ||
        merchant.contains("policybazaar") || merchant.contains("acko") ||
        merchant.contains("digit")
}

/**
 * Icon provider with fallback mechanism
 */
object IconProvider {
    
    /**
     * Get icon for a merchant with fallback logic
     * 1. Try to get brand-specific icon
     * 2. If not found, use category icon
     * 3. If category not found, use default icon
     */
    fun getIconForMerchant(merchantName: String): IconResource {
        // Try brand icon first
        BrandIcons.getIconResource(merchantName)?.let { iconRes ->
            return IconResource.DrawableResource(iconRes)
        }
        
        // Fall back to category icon
        val category = CategoryMapping.getCategory(merchantName)
        val categoryInfo = CategoryMapping.categories[category] 
            ?: CategoryMapping.categories["Others"]!!
        
        return IconResource.VectorIcon(
            icon = categoryInfo.icon,
            tint = categoryInfo.color
        )
    }
    
    /**
     * Get category info including icon and color
     */
    fun getCategoryInfo(merchantName: String): CategoryMapping.CategoryInfo {
        val category = CategoryMapping.getCategory(merchantName)
        return CategoryMapping.categories[category] 
            ?: CategoryMapping.categories["Others"]!!
    }
}

/**
 * Sealed class for different icon types
 */
sealed class IconResource {
    data class DrawableResource(val resId: Int) : IconResource()
    data class VectorIcon(val icon: ImageVector, val tint: Color) : IconResource()
}