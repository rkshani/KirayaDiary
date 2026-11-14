package com.kiraya.diary.ui.expenses

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiraya.diary.core.date.DateUtils
import com.kiraya.diary.core.money.Money
import com.kiraya.diary.domain.model.Expense
import com.kiraya.diary.domain.model.ExpenseCategory
import com.kiraya.diary.domain.model.Property
import com.kiraya.diary.domain.repository.ExpenseRepository
import com.kiraya.diary.domain.repository.PropertyRepository
import com.kiraya.diary.domain.usecase.ObserveProfitUseCase
import com.kiraya.diary.domain.usecase.Period
import com.kiraya.diary.domain.usecase.ProfitSummary
import com.kiraya.diary.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExpensesState(
    val loaded: Boolean = false,
    val period: Period = Period.currentMonth(),
    val summary: ProfitSummary = ProfitSummary(0, 0, emptyList(), emptyList()),
)

@HiltViewModel
class ExpensesViewModel @Inject constructor(
    observeProfit: ObserveProfitUseCase,
) : ViewModel() {
    private val period = MutableStateFlow(Period.currentMonth())

    val state: StateFlow<ExpensesState> = observeProfit(period)
        .map { (p, s) -> ExpensesState(true, p, s) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExpensesState())

    fun previous() = period.update { it.previous() }
    fun next() = period.update { p -> p.next().takeUnless { it.isAfterNow() } ?: p }
    fun showYear(year: Boolean) = period.update { if (year) it.asYear() else it.asMonth() }
}

data class ExpenseForm(
    val loaded: Boolean = false,
    val id: Long = 0,
    val category: ExpenseCategory = ExpenseCategory.REPAIR,
    val amount: String = "",
    val date: Long = DateUtils.startOfDay(DateUtils.now()),
    val propertyId: Long? = null,
    val note: String = "",
    val createdAt: Long = 0,
    val amountError: Boolean = false,
    val done: Boolean = false,
)

@HiltViewModel
class ExpenseEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val expenseRepository: ExpenseRepository,
    propertyRepository: PropertyRepository,
) : ViewModel() {
    private val expenseId: Long = savedStateHandle[Routes.ARG_EXPENSE_ID] ?: 0L
    val isEdit: Boolean get() = expenseId > 0

    private val _form = MutableStateFlow(ExpenseForm())
    val form: StateFlow<ExpenseForm> = _form.asStateFlow()

    val properties: StateFlow<List<Property>> = propertyRepository.observeProperties()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            if (expenseId > 0) {
                val e = expenseRepository.getExpense(expenseId)
                _form.value = e?.let {
                    ExpenseForm(true, it.id, it.category, Money.toInput(it.amount), it.date, it.propertyId, it.note, it.createdAt)
                } ?: ExpenseForm(done = true)
            } else {
                _form.update { it.copy(loaded = true) }
            }
        }
    }

    fun onCategory(v: ExpenseCategory) = _form.update { it.copy(category = v) }
    fun onAmount(v: String) = _form.update { it.copy(amount = v, amountError = false) }
    fun onDate(v: Long) = _form.update { it.copy(date = v) }
    fun onProperty(v: Long?) = _form.update { it.copy(propertyId = v) }
    fun onNote(v: String) = _form.update { it.copy(note = v) }

    fun save() {
        val f = _form.value
        val amount = Money.parse(f.amount)
        if (amount == null || amount <= 0) {
            _form.update { it.copy(amountError = true) }
            return
        }
        viewModelScope.launch {
            expenseRepository.saveExpense(
                Expense(
                    id = f.id,
                    propertyId = f.propertyId,
                    category = f.category,
                    amount = amount,
                    date = f.date,
                    note = f.note,
                    createdAt = f.createdAt,
                ),
            )
            _form.update { it.copy(done = true) }
        }
    }

    fun delete() {
        viewModelScope.launch {
            expenseRepository.deleteExpense(expenseId)
            _form.update { it.copy(done = true) }
        }
    }
}
