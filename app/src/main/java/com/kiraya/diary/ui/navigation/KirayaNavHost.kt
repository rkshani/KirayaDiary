package com.kiraya.diary.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.kiraya.diary.R
import com.kiraya.diary.ui.home.HomeScreen
import com.kiraya.diary.ui.home.QuickAction
import com.kiraya.diary.ui.properties.PropertiesScreen
import com.kiraya.diary.ui.properties.PropertyDetailScreen
import com.kiraya.diary.ui.properties.PropertyEditScreen
import com.kiraya.diary.ui.properties.UnitEditScreen
import com.kiraya.diary.ui.reports.ReportsScreen
import com.kiraya.diary.ui.settings.SettingsScreen
import com.kiraya.diary.ui.tenants.TenantsScreen

private fun longArg(name: String, default: Long? = null) = navArgument(name) {
    type = NavType.LongType
    if (default != null) defaultValue = default
}

@Composable
fun KirayaNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    val back: () -> Unit = { navController.popBackStack() }
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = modifier,
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onTenantClick = { },
                quickActions = listOf(
                    QuickAction(R.string.quick_properties, R.drawable.ic_property) {
                        navController.navigate(Routes.PROPERTIES)
                    },
                ),
            )
        }
        composable(Routes.TENANTS) { TenantsScreen() }
        composable(Routes.REPORTS) { ReportsScreen() }
        composable(Routes.SETTINGS) { SettingsScreen() }

        // Properties and units
        composable(Routes.PROPERTIES) {
            PropertiesScreen(
                onBack = back,
                onAdd = { navController.navigate(Routes.propertyEdit()) },
                onOpen = { navController.navigate(Routes.propertyDetail(it)) },
            )
        }
        composable(Routes.PROPERTY_DETAIL, listOf(longArg(Routes.ARG_PROPERTY_ID))) {
            PropertyDetailScreen(
                onBack = back,
                onEdit = { navController.navigate(Routes.propertyEdit(it)) },
                onAddUnit = { navController.navigate(Routes.unitEdit(it)) },
                onEditUnit = { p, u -> navController.navigate(Routes.unitEdit(p, u)) },
                onTenantClick = { },
            )
        }
        composable(Routes.PROPERTY_EDIT, listOf(longArg(Routes.ARG_PROPERTY_ID, 0L))) {
            PropertyEditScreen(onDone = back)
        }
        composable(
            Routes.UNIT_EDIT,
            listOf(longArg(Routes.ARG_PROPERTY_ID), longArg(Routes.ARG_UNIT_ID, 0L)),
        ) {
            UnitEditScreen(onDone = back)
        }
    }
}
