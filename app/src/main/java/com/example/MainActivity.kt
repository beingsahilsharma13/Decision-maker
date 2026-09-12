package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.LifeSimViewModel
import com.example.ui.navigation.Screen
import com.example.ui.screens.AdvisorChatScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CloudBackupScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MoodTrackerScreen
import com.example.ui.screens.NewDecisionScreen
import com.example.ui.screens.SimulationResultScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PrimaryIndigo

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                LifeSimApp()
            }
        }
    }
}

@Composable
fun LifeSimApp(viewModel: LifeSimViewModel = viewModel()) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val currentUser by viewModel.currentUser.collectAsState()

    val bottomNavScreens = listOf(
        Screen.Home to (Icons.Default.Psychology to "Simulations"),
        Screen.MoodTracker to (Icons.Default.Favorite to "Well-Being"),
        Screen.AdvisorChat to (Icons.Default.AutoAwesome to "AI Advisor"),
        Screen.CloudBackup to (Icons.Default.CloudSync to "Cloud Vault")
    )

    val showBottomBar = currentRoute in listOf(
        Screen.Home.route,
        Screen.MoodTracker.route,
        Screen.AdvisorChat.route,
        Screen.CloudBackup.route
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar && currentUser != null) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.testTag("bottom_nav_bar")
                ) {
                    bottomNavScreens.forEach { (screen, iconTitle) ->
                        val (icon, label) = iconTitle
                        val isSelected = currentRoute == screen.route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = PrimaryIndigo.copy(alpha = 0.15f),
                                selectedIconColor = PrimaryIndigo,
                                selectedTextColor = PrimaryIndigo
                            ),
                            modifier = Modifier.testTag("nav_item_${screen.route}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Auth.route) {
                AuthScreen(
                    viewModel = viewModel,
                    onAuthSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Auth.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToNewDecision = {
                        navController.navigate(Screen.NewDecision.route)
                    },
                    onNavigateToSimulationResult = { simId ->
                        navController.navigate("${Screen.SimulationResult.route}/$simId")
                    },
                    onNavigateToMood = {
                        navController.navigate(Screen.MoodTracker.route)
                    },
                    onNavigateToChat = {
                        navController.navigate(Screen.AdvisorChat.route)
                    },
                    onNavigateToCloud = {
                        navController.navigate(Screen.CloudBackup.route)
                    },
                    onLogout = {
                        navController.navigate(Screen.Auth.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.NewDecision.route) {
                NewDecisionScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onSimulationFinished = { simId ->
                        navController.navigate("${Screen.SimulationResult.route}/$simId") {
                            popUpTo(Screen.Home.route)
                        }
                    }
                )
            }

            composable(
                route = "${Screen.SimulationResult.route}/{simId}",
                arguments = listOf(navArgument("simId") { type = NavType.StringType })
            ) { backStackEntry ->
                val simId = backStackEntry.arguments?.getString("simId") ?: ""
                viewModel.selectSimulationById(simId)
                SimulationResultScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToChat = { navController.navigate(Screen.AdvisorChat.route) },
                    onNavigateToMood = { navController.navigate(Screen.MoodTracker.route) }
                )
            }

            composable(Screen.MoodTracker.route) {
                MoodTrackerScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.AdvisorChat.route) {
                AdvisorChatScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.CloudBackup.route) {
                CloudBackupScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
