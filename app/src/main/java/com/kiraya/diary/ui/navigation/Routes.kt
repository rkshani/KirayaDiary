package com.kiraya.diary.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.kiraya.diary.R

/** All navigation routes. Routes with arguments have a builder function. */
object Routes {
    const val HOME = "home"
    const val TENANTS = "tenants"
    const val REPORTS = "reports"
    const val SETTINGS = "settings"
}

/** The four tabs in the bottom bar. */
enum class TopLevelDestination(
    val route: String,
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
) {
    HOME(Routes.HOME, R.string.nav_home, R.drawable.ic_home),
    TENANTS(Routes.TENANTS, R.string.nav_tenants, R.drawable.ic_tenants),
    REPORTS(Routes.REPORTS, R.string.nav_reports, R.drawable.ic_reports),
    SETTINGS(Routes.SETTINGS, R.string.nav_settings, R.drawable.ic_settings),
}
