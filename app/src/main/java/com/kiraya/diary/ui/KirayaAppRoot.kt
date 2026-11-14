package com.kiraya.diary.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kiraya.diary.R
import com.kiraya.diary.ui.navigation.KirayaNavHost
import com.kiraya.diary.ui.navigation.Routes
import com.kiraya.diary.ui.navigation.TopLevelDestination
import com.kiraya.diary.ui.util.Intents
import kotlinx.coroutines.launch

private const val PRIVACY_POLICY_URL = "https://sites.google.com/view/kirayadiary/home"

@Composable
fun KirayaAppRoot(
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showBottomBar = TopLevelDestination.entries.any { top ->
        currentDestination?.hierarchy?.any { it.route == top.route } == true
    }
    val onHome = currentDestination?.hierarchy?.any { it.route == Routes.HOME } == true
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    BackHandler(enabled = drawerState.isOpen) { scope.launch { drawerState.close() } }

    ModalNavigationDrawer(
        drawerState = drawerState,
        // Swipe to open only on Home; the drawer can always be closed by swiping.
        gesturesEnabled = onHome || drawerState.isOpen,
        drawerContent = {
            AppDrawer(
                drawerState = drawerState,
                onHome = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(navController.graph.findStartDestination().id)
                        launchSingleTop = true
                    }
                },
            )
        },
    ) {
        Scaffold(
            // Each screen draws its own top bar and handles the status bar inset.
            contentWindowInsets = WindowInsets(0),
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar {
                        TopLevelDestination.entries.forEach { dest ->
                            val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(dest.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(painterResource(dest.iconRes), contentDescription = null) },
                                label = { Text(stringResource(dest.labelRes)) },
                            )
                        }
                    }
                }
            },
        ) { padding ->
            KirayaNavHost(
                navController = navController,
                onOpenDrawer = { scope.launch { drawerState.open() } },
                modifier = Modifier
                    .padding(padding)
                    .consumeWindowInsets(padding),
            )
        }
    }
}

@Composable
private fun AppDrawer(drawerState: DrawerState, onHome: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val noApp = stringResource(R.string.drawer_no_app)
    fun closeThen(action: () -> Unit) {
        scope.launch {
            drawerState.close()
            action()
        }
    }

    ModalDrawerSheet {
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp),
        )
        HorizontalDivider(Modifier.padding(horizontal = 28.dp))
        Spacer(Modifier.height(8.dp))
        DrawerRow(R.string.nav_home, R.drawable.ic_home, selected = true) { closeThen(onHome) }
        DrawerRow(R.string.drawer_rate_us, R.drawable.ic_star) {
            closeThen { openRateUs(context, noApp) }
        }
        DrawerRow(R.string.drawer_privacy_policy, R.drawable.ic_privacy) {
            closeThen { openUrl(context, PRIVACY_POLICY_URL, noApp) }
        }
        DrawerRow(R.string.drawer_exit, R.drawable.ic_exit) {
            (context as? Activity)?.finishAffinity()
        }
    }
}

@Composable
private fun DrawerRow(
    @StringRes label: Int,
    @DrawableRes icon: Int,
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    NavigationDrawerItem(
        label = { Text(stringResource(label)) },
        icon = { Icon(painterResource(icon), contentDescription = null) },
        selected = selected,
        onClick = onClick,
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
    )
}

/** Opens the Play Store page, or the web page if the Play Store app is missing. */
private fun openRateUs(context: Context, noApp: String) {
    val id = context.packageName
    val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$id"))
    if (!Intents.start(context, market)) {
        openUrl(context, "https://play.google.com/store/apps/details?id=$id", noApp)
    }
}

/** Opens a link in the browser. The app itself stays offline (no INTERNET permission). */
private fun openUrl(context: Context, url: String, noApp: String) {
    if (!Intents.start(context, Intent(Intent.ACTION_VIEW, Uri.parse(url)))) {
        Toast.makeText(context, noApp, Toast.LENGTH_SHORT).show()
    }
}
