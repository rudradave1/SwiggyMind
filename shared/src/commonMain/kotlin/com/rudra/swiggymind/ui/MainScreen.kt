package com.rudra.swiggymind.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.rudra.swiggymind.di.sharedComponent
import com.rudra.swiggymind.ui.chat.ChatScreen
import com.rudra.swiggymind.ui.chat.ChatViewModel
import com.rudra.swiggymind.ui.history.HistoryScreen
import com.rudra.swiggymind.ui.history.HistoryViewModel
import com.rudra.swiggymind.ui.theme.SwiggyColors

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Assistant : Screen("chat", "Chat", Icons.AutoMirrored.Filled.Chat)
    object History : Screen("history", "History", Icons.Default.History)
    object FoodDna : Screen("food_dna", "Food DNA", Icons.Default.Analytics)
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            Column {
                HorizontalDivider(thickness = 1.dp, color = SwiggyColors.Border)
                NavigationBar(
                    containerColor = SwiggyColors.Surface,
                    tonalElevation = 0.dp
                ) {
                val screens = listOf(
                    Screen.Assistant,
                    Screen.History
                )
                screens.forEach { screen ->
                    val selected = currentDestination?.hierarchy?.any { it.route?.startsWith(screen.route) == true } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().route ?: Screen.Assistant.route) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = {
                            Text(
                                screen.label,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = SwiggyColors.Primary,
                            selectedTextColor = SwiggyColors.Primary,
                            unselectedIconColor = SwiggyColors.Subtle,
                            unselectedTextColor = SwiggyColors.Subtle,
                            indicatorColor = SwiggyColors.Surface
                        )
                    )
                }
            }
        }
    }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Assistant.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Assistant.route + "?convId={convId}") { backStackEntry ->
                val convId = backStackEntry.arguments?.getString("convId")
                val sc = sharedComponent!!
                val chatViewModel: ChatViewModel = viewModel {
                    ChatViewModel(
                        sc.responseOrchestrator,
                        sc.chatHistoryDao,
                        sc.restaurantRepository,
                        sc.settingsRepository,
                        sc.isMcpEnabled
                    )
                }
                LaunchedEffect(convId) {
                    convId?.let { chatViewModel.loadConversation(it) }
                }
                ChatScreen(
                    viewModel = chatViewModel,
                    onViewDnaClick = {
                        navController.navigate(Screen.FoodDna.route)
                    }
                )
            }
            composable(Screen.History.route) {
                val sc = sharedComponent!!
                val historyViewModel: HistoryViewModel = viewModel {
                    HistoryViewModel(sc.chatHistoryDao, sc.shouldSeedDefaults)
                }
                HistoryScreen(
                    viewModel = historyViewModel,
                    onConversationClick = { convId ->
                        navController.navigate(Screen.Assistant.route + "?convId=$convId") {
                            popUpTo(navController.graph.findStartDestination().route ?: Screen.Assistant.route)
                            launchSingleTop = true
                        }
                    },
                    onExploreClick = {
                        navController.navigate(Screen.Assistant.route) {
                            popUpTo(navController.graph.findStartDestination().route ?: Screen.Assistant.route)
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Screen.FoodDna.route) {
                val sc = sharedComponent!!
                val dnaViewModel: com.rudra.swiggymind.ui.dna.FoodDnaViewModel = viewModel {
                    com.rudra.swiggymind.ui.dna.FoodDnaViewModel(sc.chatHistoryDao, sc.llmClient)
                }
                com.rudra.swiggymind.ui.dna.FoodDnaScreen(
                    viewModel = dnaViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
