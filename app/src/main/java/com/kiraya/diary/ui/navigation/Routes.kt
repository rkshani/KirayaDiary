package com.kiraya.diary.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.kiraya.diary.R

/** All navigation routes. Routes with arguments have a builder function. */
object Routes {
    const val ARG_PROPERTY_ID = "propertyId"
    const val ARG_UNIT_ID = "unitId"
    const val ARG_TENANT_ID = "tenantId"
    const val ARG_PAYMENT_ID = "paymentId"
    const val ARG_BILL_ID = "billId"
    const val ARG_EXPENSE_ID = "expenseId"
    const val ARG_ENTRY_ID = "entryId"

    // Bottom bar tabs
    const val HOME = "home"
    const val TENANTS = "tenants"
    const val REPORTS = "reports"
    const val SETTINGS = "settings"

    // Properties and units
    const val PROPERTIES = "properties"
    const val PROPERTY_DETAIL = "property/{$ARG_PROPERTY_ID}"
    const val PROPERTY_EDIT = "property_edit?$ARG_PROPERTY_ID={$ARG_PROPERTY_ID}"
    const val UNIT_EDIT = "unit_edit/{$ARG_PROPERTY_ID}?$ARG_UNIT_ID={$ARG_UNIT_ID}"

    fun propertyDetail(id: Long) = "property/$id"
    fun propertyEdit(id: Long = 0) = "property_edit?$ARG_PROPERTY_ID=$id"
    fun unitEdit(propertyId: Long, unitId: Long = 0) = "unit_edit/$propertyId?$ARG_UNIT_ID=$unitId"

    // Tenants
    const val TENANT_DETAIL = "tenant/{$ARG_TENANT_ID}"
    const val TENANT_EDIT = "tenant_edit?$ARG_TENANT_ID={$ARG_TENANT_ID}&$ARG_UNIT_ID={$ARG_UNIT_ID}"
    const val LEASE_EDIT = "lease_edit/{$ARG_TENANT_ID}"
    const val MOVE_OUT = "move_out/{$ARG_TENANT_ID}"

    fun tenantDetail(id: Long) = "tenant/$id"
    fun tenantEdit(tenantId: Long = 0, unitId: Long = 0) = "tenant_edit?$ARG_TENANT_ID=$tenantId&$ARG_UNIT_ID=$unitId"
    fun leaseEdit(tenantId: Long) = "lease_edit/$tenantId"
    fun moveOut(tenantId: Long) = "move_out/$tenantId"

    // Rent and payments
    const val MONTHLY_RENT = "monthly_rent"
    const val RENT_ENTRY_EDIT = "rent_entry_edit/{$ARG_ENTRY_ID}"
    const val PAYMENT_EDIT = "payment_edit?$ARG_TENANT_ID={$ARG_TENANT_ID}&$ARG_PAYMENT_ID={$ARG_PAYMENT_ID}"

    fun rentEntryEdit(entryId: Long) = "rent_entry_edit/$entryId"
    fun paymentEdit(tenantId: Long = 0, paymentId: Long = 0) =
        "payment_edit?$ARG_TENANT_ID=$tenantId&$ARG_PAYMENT_ID=$paymentId"

    // Bills
    const val BILLS = "bills"
    const val BILL_EDIT = "bill_edit?$ARG_BILL_ID={$ARG_BILL_ID}"

    fun billEdit(id: Long = 0) = "bill_edit?$ARG_BILL_ID=$id"

    // Expenses
    const val EXPENSES = "expenses"
    const val EXPENSE_EDIT = "expense_edit?$ARG_EXPENSE_ID={$ARG_EXPENSE_ID}"

    fun expenseEdit(id: Long = 0) = "expense_edit?$ARG_EXPENSE_ID=$id"

    // Settings screens
    const val REMINDERS = "reminders"
    const val BACKUP = "backup"
    const val APP_LOCK = "app_lock"
    const val ONBOARDING = "onboarding"
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
