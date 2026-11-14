package com.kiraya.diary.ui.bills

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiraya.diary.core.date.DateUtils
import com.kiraya.diary.core.money.Money
import com.kiraya.diary.domain.model.Bill
import com.kiraya.diary.domain.model.BillShare
import com.kiraya.diary.domain.model.BillType
import com.kiraya.diary.domain.model.BillWithShares
import com.kiraya.diary.domain.model.Property
import com.kiraya.diary.domain.model.SplitMode
import com.kiraya.diary.domain.model.TenantWithUnit
import com.kiraya.diary.domain.repository.BillRepository
import com.kiraya.diary.domain.repository.PropertyRepository
import com.kiraya.diary.domain.repository.TenantRepository
import com.kiraya.diary.domain.usecase.BillSplitter
import com.kiraya.diary.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BillsViewModel @Inject constructor(
    billRepository: BillRepository,
) : ViewModel() {
    val bills: StateFlow<List<BillWithShares>?> = billRepository.observeBills()
        .map<List<BillWithShares>, List<BillWithShares>?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

/** One tenant row in the split list. */
data class ShareRow(
    val tenant: TenantWithUnit,
    val selected: Boolean,
    /** Custom amount text (used when split mode is CUSTOM). */
    val amount: String = "",
)

data class BillForm(
    val loaded: Boolean = false,
    val id: Long = 0,
    val type: BillType = BillType.ELECTRICITY,
    val title: String = "",
    val propertyId: Long? = null,
    val month: Int = DateUtils.currentMonthKey(),
    val dueDate: Long = DateUtils.dayInMonth(DateUtils.currentMonthKey(), 10),
    val total: String = "",
    val splitMode: SplitMode = SplitMode.EQUAL,
    val rows: List<ShareRow> = emptyList(),
    val note: String = "",
    val createdAt: Long = 0,
    val totalError: Boolean = false,
    val sharesError: Boolean = false,
    val done: Boolean = false,
) {
    val visibleRows: List<ShareRow>
        get() = rows.filter { propertyId == null || it.tenant.propertyId == propertyId || it.selected }

    /** Shares in paisa for the selected tenants, in row order. */
    fun computedShares(): List<Long>? {
        val chosen = visibleRows.filter { it.selected }
        if (chosen.isEmpty()) return null
        return when (splitMode) {
            SplitMode.EQUAL -> {
                val t = Money.parse(total) ?: return null
                BillSplitter.splitEqual(t, chosen.size)
            }
            SplitMode.CUSTOM -> chosen.map { Money.parse(it.amount.ifBlank { "0" }) ?: return null }
        }
    }
}

@HiltViewModel
class BillEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val billRepository: BillRepository,
    private val tenantRepository: TenantRepository,
    propertyRepository: PropertyRepository,
) : ViewModel() {
    private val billId: Long = savedStateHandle[Routes.ARG_BILL_ID] ?: 0L
    val isEdit: Boolean get() = billId > 0

    private val _form = MutableStateFlow(BillForm())
    val form: StateFlow<BillForm> = _form.asStateFlow()

    val properties: StateFlow<List<Property>> = propertyRepository.observeProperties()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            val all = tenantRepository.observeAllTenants().first()
            val existing = if (billId > 0) billRepository.getBill(billId) else null
            if (billId > 0 && existing == null) {
                _form.update { it.copy(done = true) }
                return@launch
            }
            val shareBy = existing?.shares?.associateBy { it.share.tenantId }.orEmpty()
            // Active tenants, plus moved-out ones already on this bill.
            val rows = all.filter { it.tenant.isActive || shareBy.containsKey(it.tenant.id) }.map { t ->
                val share = shareBy[t.tenant.id]
                ShareRow(
                    tenant = t,
                    selected = if (existing != null) share != null else true,
                    amount = share?.let { Money.toInput(it.share.amount) }.orEmpty(),
                )
            }
            _form.value = if (existing != null) {
                val b = existing.bill
                BillForm(
                    loaded = true,
                    id = b.id,
                    type = b.type,
                    title = b.title,
                    propertyId = b.propertyId,
                    month = b.month,
                    dueDate = b.dueDate,
                    total = Money.toInput(b.totalAmount),
                    splitMode = b.splitMode,
                    rows = rows,
                    note = b.note,
                    createdAt = b.createdAt,
                )
            } else {
                BillForm(loaded = true, rows = rows)
            }
        }
    }

    fun onType(v: BillType) = _form.update { it.copy(type = v) }
    fun onTitle(v: String) = _form.update { it.copy(title = v) }
    fun onTotal(v: String) = _form.update { it.copy(total = v, totalError = false) }
    fun onDueDate(v: Long) = _form.update { it.copy(dueDate = v) }
    fun onNote(v: String) = _form.update { it.copy(note = v) }
    fun onSplitMode(v: SplitMode) = _form.update { it.copy(splitMode = v, sharesError = false) }

    fun onMonth(delta: Int) = _form.update {
        val m = DateUtils.addMonths(it.month, delta)
        it.copy(month = m, dueDate = DateUtils.dayInMonth(m, DateUtils.dayOfMonth(it.dueDate)))
    }

    fun onProperty(id: Long?) = _form.update { f ->
        // Choosing a property picks its tenants by default.
        f.copy(
            propertyId = id,
            rows = f.rows.map { r -> r.copy(selected = id == null || r.tenant.propertyId == id) },
            sharesError = false,
        )
    }

    fun onToggle(tenantId: Long) = _form.update { f ->
        f.copy(
            rows = f.rows.map { if (it.tenant.tenant.id == tenantId) it.copy(selected = !it.selected) else it },
            sharesError = false,
        )
    }

    fun onShareAmount(tenantId: Long, v: String) = _form.update { f ->
        f.copy(
            rows = f.rows.map {
                if (it.tenant.tenant.id == tenantId) it.copy(amount = v.filter { c -> c.isDigit() || c == '.' }) else it
            },
            sharesError = false,
        )
    }

    fun save() {
        val f = _form.value
        val shares = f.computedShares()
        val chosen = f.visibleRows.filter { it.selected }
        val total = when (f.splitMode) {
            SplitMode.EQUAL -> Money.parse(f.total)
            SplitMode.CUSTOM -> shares?.sum()
        }
        val totalBad = total == null || total <= 0
        val sharesBad = shares == null || chosen.isEmpty() || shares.any { it < 0 }
        if (totalBad || sharesBad) {
            _form.update { it.copy(totalError = totalBad, sharesError = sharesBad) }
            return
        }
        viewModelScope.launch {
            val bill = Bill(
                id = f.id,
                propertyId = f.propertyId,
                type = f.type,
                title = f.title,
                totalAmount = total!!,
                month = f.month,
                dueDate = f.dueDate,
                splitMode = f.splitMode,
                note = f.note,
                createdAt = f.createdAt,
            )
            val shareList = chosen.zip(shares!!).map { (row, amount) ->
                BillShare(billId = f.id, tenantId = row.tenant.tenant.id, unitId = row.tenant.tenant.unitId, amount = amount)
            }
            billRepository.saveBill(bill, shareList)
            _form.update { it.copy(done = true) }
        }
    }

    fun delete() {
        viewModelScope.launch {
            billRepository.deleteBill(billId)
            _form.update { it.copy(done = true) }
        }
    }
}
