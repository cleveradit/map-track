package com.radityodwiki.maptrack.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.radityodwiki.maptrack.R
import com.radityodwiki.maptrack.ui.history.HistoryScreen
import com.radityodwiki.maptrack.ui.home.HomeScreen
import com.radityodwiki.maptrack.ui.tripdetail.TripDetailScreen
import com.radityodwiki.maptrack.ui.tripdetail.TripDetailViewModel

object Routes {
    const val HOME = "home"
    const val HISTORY = "history"
    const val TRIP_DETAIL = "trip/{${TripDetailViewModel.ARG_TRIP_ID}}"

    fun tripDetail(tripId: String) = "trip/$tripId"
}

private enum class TopLevelTab(val route: String, @StringRes val label: Int, val icon: ImageVector) {
    Home(Routes.HOME, R.string.nav_home, Icons.Filled.Home),
    History(Routes.HISTORY, R.string.nav_history, Icons.AutoMirrored.Filled.List),
}

@Composable
fun MapTrackNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                TopLevelTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(stringResource(tab.label)) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.HOME) { HomeScreen() }
            composable(Routes.HISTORY) {
                HistoryScreen(onTripClick = { tripId -> navController.navigate(Routes.tripDetail(tripId)) })
            }
            composable(
                Routes.TRIP_DETAIL,
                arguments = listOf(navArgument(TripDetailViewModel.ARG_TRIP_ID) { type = NavType.StringType }),
            ) {
                TripDetailScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
