package com.gandhasiri.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.gandhasiri.app.ui.screens.LegalScreen
import com.gandhasiri.app.ui.screens.OnboardingScreen
import com.gandhasiri.app.ui.screens.PinLoginScreen
import com.gandhasiri.app.ui.screens.ProfileScreen
import com.gandhasiri.app.ui.screens.RegisterScreen
import com.gandhasiri.app.ui.screens.SettingsScreen
import com.gandhasiri.app.ui.screens.addtree.AddTreeScreen
import com.gandhasiri.app.ui.screens.addtree.ConfirmationScreen
import com.gandhasiri.app.ui.screens.MapScreen
import com.gandhasiri.app.ui.screens.SecurityScreen
import com.gandhasiri.app.ui.screens.TreesScreen
import com.gandhasiri.app.ui.screens.TreeDetailScreen
import com.gandhasiri.app.ui.theme.GandhaSiriTheme

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Trees : Screen("trees", "Trees", Icons.Filled.List)
    object Map : Screen("map", "Map", Icons.Filled.LocationOn)
    object Security : Screen("security", "Security", Icons.Filled.Lock)
    object Legal : Screen("legal", "Legal", Icons.Filled.Info)
}

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { GandhaSiriTheme { MainApp() } }
    }
}

@Composable
fun MainApp() {
    val navController = rememberNavController()
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val navItems = listOf(Screen.Trees, Screen.Map, Screen.Security, Screen.Legal)
    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            val showBottomBar = navItems.any { 
                currentDestination?.route?.startsWith(it.route) == true 
            }
            
            if (showBottomBar) {
                NavigationBar(
                    containerColor = com.gandhasiri.app.ui.theme.DarkWood,
                    contentColor = com.gandhasiri.app.ui.theme.LightGold
                ) {
                    navItems.forEach { screen ->
                        val selected = currentDestination?.route?.startsWith(screen.route) == true
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = null) },
                            label = { Text(screen.title) },
                            selected = selected,
                            onClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                                selectedIconColor = com.gandhasiri.app.ui.theme.LightGold,
                                selectedTextColor = com.gandhasiri.app.ui.theme.LightGold,
                                unselectedIconColor = com.gandhasiri.app.ui.theme.MutedSandalwood,
                                unselectedTextColor = com.gandhasiri.app.ui.theme.MutedSandalwood,
                                indicatorColor = com.gandhasiri.app.ui.theme.DarkWood.copy(alpha = 0.5f)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "splash",
            modifier = Modifier.padding(innerPadding),
            enterTransition = { fadeIn(animationSpec = tween(500)) },
            exitTransition = { fadeOut(animationSpec = tween(500)) },
            popEnterTransition = { fadeIn(animationSpec = tween(500)) },
            popExitTransition = { fadeOut(animationSpec = tween(500)) }
        ) {
            composable("splash") {
                com.gandhasiri.app.ui.screens.SplashScreen(onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo("splash") { inclusive = true }
                    }
                })
            }
            composable("language_selection") {
                com.gandhasiri.app.ui.screens.LanguageSelectionScreen(onNavigateNext = {
                    navController.navigate("onboarding") {
                        popUpTo("language_selection") { inclusive = true }
                    }
                })
            }
            composable("onboarding") {
                OnboardingScreen(onFinish = {
                    navController.navigate("register") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                })
            }
            composable("register") {
                RegisterScreen(onRegisterSuccess = {
                    navController.navigate("pin_login") {
                        popUpTo("register") { inclusive = true }
                    }
                })
            }
            composable("pin_login") {
                PinLoginScreen(onLoginSuccess = {
                    navController.navigate(Screen.Trees.route) {
                        popUpTo("pin_login") { inclusive = true }
                    }
                })
            }
            composable(Screen.Trees.route) { 
                TreesScreen(
                    onAddTreeClick = { navController.navigate("add_tree") },
                    onTreeClick = { treeId -> navController.navigate("tree_detail/$treeId") },
                    onProfileClick = { navController.navigate("profile") }
                ) 
            }
            composable("profile") {
                ProfileScreen(
                    onBackClick = { navController.navigateUp() },
                    onSettingsClick = { navController.navigate("settings") }
                )
            }
            composable("settings") {
                SettingsScreen(
                    onBackClick = { navController.navigateUp() },
                    onLegalClick = { navController.navigate(Screen.Legal.route) },
                    onLogOut = {
                        navController.navigate("pin_login") {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    }
                )
            }
            composable(
                route = "${Screen.Map.route}?targetTreeId={targetTreeId}",
                arguments = listOf(navArgument("targetTreeId") { 
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                })
            ) { backStackEntry ->
                val targetTreeId = backStackEntry.arguments?.getString("targetTreeId")
                MapScreen(
                    targetTreeId = targetTreeId,
                    onTreeClick = { treeId ->
                        navController.navigate("tree_detail/$treeId")
                    },
                    onProfileClick = { navController.navigate("profile") }
                )
            }
            composable(Screen.Security.route) {
                SecurityScreen(
                    onProfileClick = { navController.navigate("profile") }
                )
            }
            composable(Screen.Legal.route) {
                LegalScreen(
                    onProfileClick = { navController.navigate("profile") }
                )
            }
            composable("add_tree") {
                AddTreeScreen(onTreeSaved = { treeId ->
                    navController.navigate("confirmation/$treeId") {
                        popUpTo("add_tree") { inclusive = true }
                    }
                })
            }
            composable(
                "tree_detail/{treeId}",
                arguments = listOf(navArgument("treeId") { type = NavType.StringType })
            ) { backStackEntry ->
                val treeId = backStackEntry.arguments?.getString("treeId") ?: ""
                TreeDetailScreen(
                    treeId = treeId,
                    onBackClick = { navController.popBackStack() },
                    onAddMeasurementClick = { navController.navigate("add_measurement/$treeId") },
                    onViewOnMap = { id ->
                        navController.navigate("${Screen.Map.route}?targetTreeId=$id") {
                            popUpTo(Screen.Trees.route)
                        }
                    }
                )
            }
            composable(
                "add_measurement/{treeId}",
                arguments = listOf(navArgument("treeId") { type = NavType.StringType })
            ) { backStackEntry ->
                val treeId = backStackEntry.arguments?.getString("treeId") ?: ""
                com.gandhasiri.app.ui.screens.AddMeasurementScreen(
                    treeId = treeId,
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable(
                "confirmation/{treeId}",
                arguments = listOf(navArgument("treeId") { type = NavType.StringType })
            ) { backStackEntry ->
                val treeId = backStackEntry.arguments?.getString("treeId") ?: ""
                ConfirmationScreen(
                    treeId = treeId,
                    onFinish = {
                        navController.navigate(Screen.Trees.route) {
                            popUpTo(Screen.Trees.route) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}
