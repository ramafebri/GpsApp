package com.rama.gpsapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.rama.gpsapp.ui.deadreckoning.DeadReckoningScreen
import com.rama.gpsapp.ui.deadreckoning.DeadReckoningViewModel
import com.rama.gpsapp.ui.gestures.GestureSettingsScreen
import com.rama.gpsapp.ui.gestures.GestureSettingsViewModel
import com.rama.gpsapp.ui.level.LevelScreen
import com.rama.gpsapp.ui.level.LevelViewModel
import com.rama.gpsapp.ui.theme.GpsAppTheme
import com.rama.gpsapp.ui.theft.AntiTheftSettingsScreen
import com.rama.gpsapp.ui.theft.AntiTheftViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GpsAppTheme {
                val gestureViewModel: GestureSettingsViewModel = viewModel()
                val antiTheftViewModel: AntiTheftViewModel = viewModel()
                val deadReckoningViewModel: DeadReckoningViewModel = viewModel()
                val levelViewModel: LevelViewModel = viewModel()
                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            gestureViewModel.refreshPermissionState()
                            antiTheftViewModel.refreshPermissionState()
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                val navController = rememberNavController()
                val destinations = listOf(
                    AppDestination.Gestures,
                    AppDestination.AntiTheft,
                    AppDestination.DeadReckoning,
                    AppDestination.Level
                )
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = backStackEntry?.destination

                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            destinations.forEach { destination ->
                                val selected = currentDestination
                                    ?.hierarchy
                                    ?.any { it.route == destination.route } == true
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = {
                                        navController.navigate(destination.route) {
                                            popUpTo(navController.graph.startDestinationId) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = { Text(destination.shortLabel) },
                                    label = { Text(destination.label) }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = AppDestination.Gestures.route
                    ) {
                        composable(AppDestination.Gestures.route) {
                            GestureSettingsScreen(
                                viewModel = gestureViewModel,
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                        composable(AppDestination.AntiTheft.route) {
                            AntiTheftSettingsScreen(
                                viewModel = antiTheftViewModel,
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                        composable(AppDestination.DeadReckoning.route) {
                            DeadReckoningScreen(
                                viewModel = deadReckoningViewModel,
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                        composable(AppDestination.Level.route) {
                            LevelScreen(
                                viewModel = levelViewModel,
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                    }
                }
            }
        }
    }
}

private sealed class AppDestination(
    val route: String,
    val label: String,
    val shortLabel: String
) {
    object Gestures : AppDestination(
        route = "gestures",
        label = "Gestures",
        shortLabel = "G"
    )

    object AntiTheft : AppDestination(
        route = "anti_theft",
        label = "Anti-Theft",
        shortLabel = "A"
    )

    object DeadReckoning : AppDestination(
        route = "dead_reckoning",
        label = "Dead Reckoning",
        shortLabel = "D"
    )

    object Level : AppDestination(
        route = "level",
        label = "Level",
        shortLabel = "L"
    )
}