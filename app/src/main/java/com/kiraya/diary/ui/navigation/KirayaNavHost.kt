package com.kiraya.diary.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.kiraya.diary.ui.home.HomeScreen
import com.kiraya.diary.ui.reports.ReportsScreen
import com.kiraya.diary.ui.settings.SettingsScreen
import com.kiraya.diary.ui.tenants.TenantsScreen

@Composable
fun KirayaNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = modifier,
    ) {
        composable(Routes.HOME) { HomeScreen() }
        composable(Routes.TENANTS) { TenantsScreen() }
        composable(Routes.REPORTS) { ReportsScreen() }
        composable(Routes.SETTINGS) { SettingsScreen() }
    }
}
