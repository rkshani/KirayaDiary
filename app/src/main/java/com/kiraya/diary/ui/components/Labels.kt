package com.kiraya.diary.ui.components

import androidx.annotation.StringRes
import com.kiraya.diary.R
import com.kiraya.diary.domain.model.BillType
import com.kiraya.diary.domain.model.ExpenseCategory
import com.kiraya.diary.domain.model.PaymentMethod
import com.kiraya.diary.domain.model.RentStatus
import com.kiraya.diary.domain.model.SplitMode
import com.kiraya.diary.domain.model.UnitType

@get:StringRes
val UnitType.labelRes: Int
    get() = when (this) {
        UnitType.FLAT -> R.string.unit_type_flat
        UnitType.SHOP -> R.string.unit_type_shop
        UnitType.ROOM -> R.string.unit_type_room
        UnitType.HOUSE -> R.string.unit_type_house
        UnitType.OFFICE -> R.string.unit_type_office
        UnitType.OTHER -> R.string.unit_type_other
    }

@get:StringRes
val PaymentMethod.labelRes: Int
    get() = when (this) {
        PaymentMethod.CASH -> R.string.method_cash
        PaymentMethod.BANK -> R.string.method_bank
        PaymentMethod.JAZZCASH -> R.string.method_jazzcash
        PaymentMethod.EASYPAISA -> R.string.method_easypaisa
        PaymentMethod.DEPOSIT -> R.string.method_deposit
        PaymentMethod.OTHER -> R.string.method_other
    }

@get:StringRes
val BillType.labelRes: Int
    get() = when (this) {
        BillType.ELECTRICITY -> R.string.bill_type_electricity
        BillType.GAS -> R.string.bill_type_gas
        BillType.WATER -> R.string.bill_type_water
        BillType.OTHER -> R.string.bill_type_other
    }

@get:StringRes
val SplitMode.labelRes: Int
    get() = when (this) {
        SplitMode.EQUAL -> R.string.split_equal
        SplitMode.CUSTOM -> R.string.split_custom
    }

@get:StringRes
val ExpenseCategory.labelRes: Int
    get() = when (this) {
        ExpenseCategory.REPAIR -> R.string.expense_repair
        ExpenseCategory.MAINTENANCE -> R.string.expense_maintenance
        ExpenseCategory.TAX -> R.string.expense_tax
        ExpenseCategory.UTILITY -> R.string.expense_utility
        ExpenseCategory.OTHER -> R.string.expense_other
    }

@get:StringRes
val RentStatus.labelRes: Int
    get() = when (this) {
        RentStatus.PAID -> R.string.status_paid
        RentStatus.PARTIAL -> R.string.status_partial
        RentStatus.UNPAID -> R.string.status_unpaid
    }
