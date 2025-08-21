package com.pennywiseai.tracker.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import android.content.Intent
import android.net.Uri
import com.pennywiseai.tracker.R
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pennywiseai.tracker.presentation.home.HomeScreen
import com.pennywiseai.tracker.presentation.subscriptions.SubscriptionsScreen
import com.pennywiseai.tracker.presentation.transactions.TransactionsScreen
import com.pennywiseai.tracker.ui.components.PennyWiseBottomNavigation
import com.pennywiseai.tracker.ui.components.SpotlightTutorial
import com.pennywiseai.tracker.ui.screens.settings.SettingsScreen
import com.pennywiseai.tracker.ui.viewmodel.ThemeViewModel
import com.pennywiseai.tracker.ui.viewmodel.SpotlightViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    rootNavController: NavHostController? = null,
    navController: NavHostController = rememberNavController(),
    themeViewModel: ThemeViewModel = hiltViewModel(),
    spotlightViewModel: SpotlightViewModel = hiltViewModel()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val spotlightState by spotlightViewModel.spotlightState.collectAsState()
    
    // Handle chat screen separately to avoid keyboard issues
    if (currentRoute == "chat") {
        ChatScreenWrapper(
            navController = navController
        )
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
            topBar = {
            val context = LocalContext.current
            PennyWiseTopAppBar(
                title = when (currentRoute) {
                    "home" -> "PennyWise"
                    "transactions" -> "Transactions"
                    "subscriptions" -> "Subscriptions"
                    "analytics" -> "Analytics"
                    "settings" -> "Settings"
                    "categories" -> "Categories"
                    "unrecognized_sms" -> "Unrecognized Messages"
                    else -> "PennyWise"
                },
                showBackButton = currentRoute in listOf("settings", "subscriptions", "transactions", "categories", "unrecognized_sms"),
                showSettingsButton = currentRoute !in listOf("settings", "categories", "unrecognized_sms"),
                showDiscordButton = currentRoute !in listOf("settings", "categories", "unrecognized_sms"), // Hide on settings, categories and unrecognized_sms
                onBackClick = { navController.popBackStack() },
                onSettingsClick = { navController.navigate("settings") },
                onDiscordClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://discord.gg/H3xWeMWjKQ"))
                    context.startActivity(intent)
                }
            )
        },
        bottomBar = {
            // Show bottom navigation only for main screens
            if (currentRoute in listOf("home", "analytics")) {
                PennyWiseBottomNavigation(navController = navController)
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(paddingValues),
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            composable("home") {
                val homeViewModel: com.pennywiseai.tracker.presentation.home.HomeViewModel = hiltViewModel()
                HomeScreen(
                    viewModel = homeViewModel,
                    navController = rootNavController ?: navController,
                    onNavigateToSettings = {
                        navController.navigate("settings")
                    },
                    onNavigateToTransactions = {
                        navController.navigate("transactions")
                    },
                    onNavigateToSubscriptions = {
                        navController.navigate("subscriptions")
                    },
                    onNavigateToChat = {
                        navController.navigate("chat")
                    },
                    onTransactionClick = { transactionId ->
                        rootNavController?.navigate(
                            com.pennywiseai.tracker.navigation.TransactionDetail(transactionId)
                        )
                    },
                    onFabPositioned = { position ->
                        spotlightViewModel.updateFabPosition(position)
                    }
                )
            }
            
            composable(
                route = "transactions?category={category}&merchant={merchant}&period={period}",
                arguments = listOf(
                    navArgument("category") { 
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("merchant") { 
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("period") { 
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val category = backStackEntry.arguments?.getString("category")
                val merchant = backStackEntry.arguments?.getString("merchant")
                val period = backStackEntry.arguments?.getString("period")
                
                TransactionsScreen(
                    initialCategory = category,
                    initialMerchant = merchant,
                    initialPeriod = period,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onTransactionClick = { transactionId ->
                        rootNavController?.navigate(
                            com.pennywiseai.tracker.navigation.TransactionDetail(transactionId)
                        )
                    }
                )
            }
            
            composable("subscriptions") {
                SubscriptionsScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
            
            composable("analytics") {
                com.pennywiseai.tracker.ui.screens.analytics.AnalyticsScreen(
                    onNavigateToChat = { navController.navigate("chat") },
                    onNavigateToTransactions = { category, merchant, period ->
                        val route = buildString {
                            append("transactions")
                            val params = mutableListOf<String>()
                            category?.let { 
                                val encoded = java.net.URLEncoder.encode(it, "UTF-8")
                                params.add("category=$encoded") 
                            }
                            merchant?.let { 
                                val encoded = java.net.URLEncoder.encode(it, "UTF-8")
                                params.add("merchant=$encoded") 
                            }
                            period?.let { 
                                params.add("period=$it") 
                            }
                            if (params.isNotEmpty()) {
                                append("?")
                                append(params.joinToString("&"))
                            }
                        }
                        navController.navigate(route)
                    }
                )
            }
            
            composable("chat") {
                // Empty composable - chat screen is rendered separately in ChatScreenWrapper
                // to avoid keyboard handling issues with edge-to-edge display
            }
            
            composable("settings") {
                SettingsScreen(
                    themeViewModel = themeViewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToCategories = {
                        navController.navigate("categories")
                    },
                    onNavigateToUnrecognizedSms = {
                        navController.navigate("unrecognized_sms")
                    }
                )
            }
            
            composable("categories") {
                com.pennywiseai.tracker.presentation.categories.CategoriesScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
            
            composable("unrecognized_sms") {
                com.pennywiseai.tracker.ui.screens.unrecognized.UnrecognizedSmsScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
            
            // Spotlight Tutorial overlay - outside Scaffold to overlay everything
            if (currentRoute == "home" && spotlightState.showTutorial && spotlightState.fabPosition != null) {
                val homeViewModel: com.pennywiseai.tracker.presentation.home.HomeViewModel? = 
                    navController.currentBackStackEntry?.let { hiltViewModel(it) }
                
                SpotlightTutorial(
                    isVisible = true,
                    targetPosition = spotlightState.fabPosition,
                    message = "Tap here to scan your SMS messages for transactions",
                    onDismiss = {
                        spotlightViewModel.dismissTutorial()
                    },
                    onTargetClick = {
                        homeViewModel?.scanSmsMessages()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatScreenWrapper(
    navController: NavHostController
) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            PennyWiseTopAppBar(
                title = "PennyWise AI",
                showBackButton = true,
                showSettingsButton = true,
                onBackClick = { navController.popBackStack() },
                onSettingsClick = { navController.navigate("settings") },
                onDiscordClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://discord.gg/H3xWeMWjKQ"))
                    context.startActivity(intent)
                }
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
                .imePadding()
        ) {
            com.pennywiseai.tracker.ui.screens.chat.ChatScreen(
                onNavigateToSettings = {
                    navController.navigate("settings")
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PennyWiseTopAppBar(
    title: String,
    showBackButton: Boolean = false,
    showSettingsButton: Boolean = true,
    showDiscordButton: Boolean = true,
    onBackClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onDiscordClick: () -> Unit = {}
) {
    Column {
        TopAppBar(
            title = { Text(title) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
            ),
            navigationIcon = {
                if (showBackButton) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            },
            actions = {
                if (showDiscordButton) {
                    IconButton(onClick = onDiscordClick) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_discord),
                            contentDescription = "Join Discord Community",
                            tint = Color(0xFF5865F2) // Discord brand color
                        )
                    }
                }
                if (showSettingsButton) {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                }
            }
        )
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )
    }
}