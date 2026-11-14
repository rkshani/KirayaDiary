package com.kiraya.diary.domain.model

/** Enum names are stored in the database. Never rename an entry. */

enum class UnitType { FLAT, SHOP, ROOM, HOUSE, OFFICE, OTHER }

enum class PaymentMethod { CASH, BANK, JAZZCASH, EASYPAISA, DEPOSIT, OTHER }

enum class BillType { ELECTRICITY, GAS, WATER, OTHER }

enum class SplitMode { EQUAL, CUSTOM }

enum class ExpenseCategory { REPAIR, MAINTENANCE, TAX, UTILITY, OTHER }

enum class RentStatus { PAID, PARTIAL, UNPAID }
