package com.kiraya.diary.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.kiraya.diary.R
import com.kiraya.diary.ui.backup.BackupScreen
import com.kiraya.diary.ui.bills.BillEditScreen
import com.kiraya.diary.ui.bills.BillsScreen
import com.kiraya.diary.ui.expenses.ExpenseEditScreen
import com.kiraya.diary.ui.expenses.ExpensesScreen
import com.kiraya.diary.ui.home.HomeScreen
import com.kiraya.diary.ui.home.QuickAction
import com.kiraya.diary.ui.lease.LeaseEditScreen
import com.kiraya.diary.ui.moveout.MoveOutScreen
import com.kiraya.diary.ui.payments.PaymentEditScreen
import com.kiraya.diary.ui.properties.PropertiesScreen
import com.kiraya.diary.ui.properties.PropertyDetailScreen
import com.kiraya.diary.ui.properties.PropertyEditScreen
import com.kiraya.diary.ui.properties.UnitEditScreen
import com.kiraya.diary.ui.receipts.ReceiptScreen
import com.kiraya.diary.ui.reminders.RemindersScreen
import com.kiraya.diary.ui.rent.MonthlyRentScreen
import com.kiraya.diary.ui.rent.RentEntryEditScreen
import com.kiraya.diary.ui.reports.ReportsScreen
import com.kiraya.diary.ui.settings.SettingsScreen
import com.kiraya.diary.ui.tenants.TenantDetailActions
import com.kiraya.diary.ui.tenants.TenantDetailScreen
import com.kiraya.diary.ui.tenants.TenantEditScreen
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
                onTenantClick = { navController.navigate(Routes.tenantDetail(it)) },
                quickActions = listOf(
                    QuickAction(R.string.payment_add, R.drawable.ic_payment) {
                        navController.navigate(Routes.paymentEdit())
                    },
                    QuickAction(R.string.quick_monthly_rent, R.drawable.ic_calendar) {
                        navController.navigate(Routes.MONTHLY_RENT)
                    },
                    QuickAction(R.string.quick_bills, R.drawable.ic_bill) {
                        navController.navigate(Routes.BILLS)
                    },
                    QuickAction(R.string.reminders_title, R.drawable.ic_notification) {
                        navController.navigate(Routes.REMINDERS)
                    },
                    QuickAction(R.string.expenses_title, R.drawable.ic_expense) {
                        navController.navigate(Routes.EXPENSES)
                    },
                    QuickAction(R.string.quick_add_tenant, R.drawable.ic_person) {
                        navController.navigate(Routes.tenantEdit())
                    },
                    QuickAction(R.string.quick_properties, R.drawable.ic_property) {
                        navController.navigate(Routes.PROPERTIES)
                    },
                ),
            )
        }
        composable(Routes.TENANTS) {
            TenantsScreen(
                onAdd = { navController.navigate(Routes.tenantEdit()) },
                onOpen = { navController.navigate(Routes.tenantDetail(it)) },
            )
        }
        composable(Routes.REPORTS) { ReportsScreen() }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onReminders = { navController.navigate(Routes.REMINDERS) },
                onBackup = { navController.navigate(Routes.BACKUP) },
            )
        }
        composable(Routes.BACKUP) { BackupScreen(onBack = back) }

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
                onTenantClick = { navController.navigate(Routes.tenantDetail(it)) },
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

        // Tenants
        composable(Routes.TENANT_DETAIL, listOf(longArg(Routes.ARG_TENANT_ID))) {
            TenantDetailScreen(
                actions = TenantDetailActions(
                    onBack = back,
                    onEdit = { navController.navigate(Routes.tenantEdit(it)) },
                    onEditLease = { navController.navigate(Routes.leaseEdit(it)) },
                    onEditRentEntry = { navController.navigate(Routes.rentEntryEdit(it)) },
                    onOpenBill = { navController.navigate(Routes.billEdit(it)) },
                    onRecordPayment = { navController.navigate(Routes.paymentEdit(tenantId = it)) },
                    onEditPayment = { navController.navigate(Routes.paymentEdit(paymentId = it)) },
                    onReceipt = { navController.navigate(Routes.receipt(it)) },
                    onMoveOut = { navController.navigate(Routes.moveOut(it)) },
                ),
            )
        }
        composable(Routes.MONTHLY_RENT) {
            MonthlyRentScreen(
                onBack = back,
                onTenantClick = { navController.navigate(Routes.tenantDetail(it)) },
            )
        }
        composable(Routes.RENT_ENTRY_EDIT, listOf(longArg(Routes.ARG_ENTRY_ID))) {
            RentEntryEditScreen(onDone = back)
        }
        composable(
            Routes.PAYMENT_EDIT,
            listOf(longArg(Routes.ARG_TENANT_ID, 0L), longArg(Routes.ARG_PAYMENT_ID, 0L)),
        ) { entry ->
            val isNew = (entry.arguments?.getLong(Routes.ARG_PAYMENT_ID) ?: 0L) == 0L
            PaymentEditScreen(
                onDone = { savedId ->
                    // A new payment opens its receipt, ready to share.
                    if (isNew && savedId != null) {
                        navController.navigate(Routes.receipt(savedId)) {
                            popUpTo(Routes.PAYMENT_EDIT) { inclusive = true }
                        }
                    } else {
                        back()
                    }
                },
            )
        }
        composable(Routes.RECEIPT, listOf(longArg(Routes.ARG_PAYMENT_ID))) {
            ReceiptScreen(onBack = back)
        }
        // Bills
        composable(Routes.BILLS) {
            BillsScreen(
                onBack = back,
                onAdd = { navController.navigate(Routes.billEdit()) },
                onOpen = { navController.navigate(Routes.billEdit(it)) },
            )
        }
        composable(Routes.BILL_EDIT, listOf(longArg(Routes.ARG_BILL_ID, 0L))) {
            BillEditScreen(onDone = back)
        }
        composable(Routes.REMINDERS) {
            RemindersScreen(
                onBack = back,
                onTenantClick = { navController.navigate(Routes.tenantDetail(it)) },
            )
        }
        // Expenses
        composable(Routes.EXPENSES) {
            ExpensesScreen(
                onBack = back,
                onAdd = { navController.navigate(Routes.expenseEdit()) },
                onOpen = { navController.navigate(Routes.expenseEdit(it)) },
            )
        }
        composable(Routes.EXPENSE_EDIT, listOf(longArg(Routes.ARG_EXPENSE_ID, 0L))) {
            ExpenseEditScreen(onDone = back)
        }
        composable(Routes.MOVE_OUT, listOf(longArg(Routes.ARG_TENANT_ID))) {
            MoveOutScreen(onDone = back)
        }
        composable(Routes.LEASE_EDIT, listOf(longArg(Routes.ARG_TENANT_ID))) {
            LeaseEditScreen(onDone = back)
        }
        composable(
            Routes.TENANT_EDIT,
            listOf(longArg(Routes.ARG_TENANT_ID, 0L), longArg(Routes.ARG_UNIT_ID, 0L)),
        ) { entry ->
            val isNew = (entry.arguments?.getLong(Routes.ARG_TENANT_ID) ?: 0L) == 0L
            TenantEditScreen(
                onDone = { savedId ->
                    if (isNew && savedId != null && savedId > 0) {
                        navController.navigate(Routes.tenantDetail(savedId)) {
                            popUpTo(Routes.TENANT_EDIT) { inclusive = true }
                        }
                    } else {
                        back()
                    }
                },
            )
        }
    }
}
