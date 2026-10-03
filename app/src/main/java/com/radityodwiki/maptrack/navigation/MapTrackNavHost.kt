package com.radityodwiki.maptrack.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Place
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
import com.radityodwiki.maptrack.ui.acceleration.AccelerationScreen
import com.radityodwiki.maptrack.ui.history.HistoryScreen
import com.radityodwiki.maptrack.ui.home.HomeScreen
import com.radityodwiki.maptrack.ui.places.PlaceDetailScreen
import com.radityodwiki.maptrack.ui.places.PlaceDetailViewModel
import com.radityodwiki.maptrack.ui.places.PlaceEditorScreen
import com.radityodwiki.maptrack.ui.places.PlaceEditorViewModel
import com.radityodwiki.maptrack.ui.places.PlacesScreen
import com.radityodwiki.maptrack.ui.settings.SettingsScreen
import com.radityodwiki.maptrack.ui.tripdetail.TripDetailScreen
import com.radityodwiki.maptrack.ui.tripdetail.TripDetailViewModel

object Routes {
    const val HOME = "home"
    const val HISTORY = "history"
    const val PLACES = "places"
    const val SETTINGS = "settings"
    const val ACCELERATION = "acceleration"
    const val TRIP_DETAIL = "trip/{${TripDetailViewModel.ARG_TRIP_ID}}"

    const val PLACE_DETAIL = "place/{${PlaceDetailViewModel.ARG_PLACE_ID}}"

    const val PLACE_EDITOR = "place-editor?" +
        "${PlaceEditorViewModel.ARG_PLACE_ID}={${PlaceEditorViewModel.ARG_PLACE_ID}}&" +
        "${PlaceEditorViewModel.ARG_LATITUDE}={${PlaceEditorViewModel.ARG_LATITUDE}}&" +
        "${PlaceEditorViewModel.ARG_LONGITUDE}={${PlaceEditorViewModel.ARG_LONGITUDE}}"

    fun tripDetail(tripId: String) = "trip/$tripId"

    fun placeDetail(placeId: String) = "place/$placeId"

    fun newPlace() = "place-editor"

    /** Coordinates travel as strings so they keep full Double precision. */
    fun newPlaceAt(latitude: Double, longitude: Double) =
        "place-editor?${PlaceEditorViewModel.ARG_LATITUDE}=$latitude&${PlaceEditorViewModel.ARG_LONGITUDE}=$longitude"

    fun editPlace(placeId: String) = "place-editor?${PlaceEditorViewModel.ARG_PLACE_ID}=$placeId"
}

private enum class TopLevelTab(val route: String, @StringRes val label: Int, val icon: ImageVector) {
    Home(Routes.HOME, R.string.nav_home, Icons.Filled.Home),
    History(Routes.HISTORY, R.string.nav_history, Icons.AutoMirrored.Filled.List),
    Places(Routes.PLACES, R.string.nav_places, Icons.Filled.Place),
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
            composable(Routes.HOME) {
                HomeScreen(
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    onOpenAcceleration = { navController.navigate(Routes.ACCELERATION) },
                )
            }
            composable(Routes.ACCELERATION) { AccelerationScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.SETTINGS) { SettingsScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.HISTORY) {
                HistoryScreen(onTripClick = { tripId -> navController.navigate(Routes.tripDetail(tripId)) })
            }
            composable(Routes.PLACES) {
                PlacesScreen(
                    onPlaceClick = { placeId -> navController.navigate(Routes.placeDetail(placeId)) },
                    onAddPlace = { navController.navigate(Routes.newPlace()) },
                )
            }
            composable(
                Routes.PLACE_DETAIL,
                arguments = listOf(navArgument(PlaceDetailViewModel.ARG_PLACE_ID) { type = NavType.StringType }),
            ) {
                PlaceDetailScreen(
                    onBack = { navController.popBackStack() },
                    onEdit = { placeId -> navController.navigate(Routes.editPlace(placeId)) },
                    onTripClick = { tripId -> navController.navigate(Routes.tripDetail(tripId)) },
                )
            }
            composable(
                Routes.PLACE_EDITOR,
                arguments = listOf(
                    PlaceEditorViewModel.ARG_PLACE_ID,
                    PlaceEditorViewModel.ARG_LATITUDE,
                    PlaceEditorViewModel.ARG_LONGITUDE,
                ).map { name ->
                    navArgument(name) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                },
            ) {
                PlaceEditorScreen(onDone = { navController.popBackStack() })
            }
            composable(
                Routes.TRIP_DETAIL,
                arguments = listOf(navArgument(TripDetailViewModel.ARG_TRIP_ID) { type = NavType.StringType }),
            ) {
                TripDetailScreen(
                    onBack = { navController.popBackStack() },
                    onSaveVisitAsPlace = { lat, lng -> navController.navigate(Routes.newPlaceAt(lat, lng)) },
                )
            }
        }
    }
}
