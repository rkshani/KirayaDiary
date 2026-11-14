package com.kiraya.diary.ui.tenants

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiraya.diary.core.coroutines.ApplicationScope
import com.kiraya.diary.core.date.DateUtils
import com.kiraya.diary.core.money.Money
import com.kiraya.diary.data.files.PhotoStorage
import com.kiraya.diary.data.reminders.ReminderMessageBuilder
import com.kiraya.diary.data.settings.AppSettings
import com.kiraya.diary.data.settings.SettingsRepository
import com.kiraya.diary.domain.ledger.TenantLedger
import com.kiraya.diary.domain.model.Lease
import com.kiraya.diary.domain.model.Tenant
import com.kiraya.diary.domain.model.TenantWithUnit
import com.kiraya.diary.domain.model.UnitWithStatus
import com.kiraya.diary.domain.repository.PropertyRepository
import com.kiraya.diary.domain.repository.TenantRepository
import com.kiraya.diary.domain.usecase.GenerateRentEntriesUseCase
import com.kiraya.diary.domain.usecase.ObserveLedgersUseCase
import com.kiraya.diary.domain.usecase.ObserveTenantLedgerUseCase
import com.kiraya.diary.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class TenantFilter { ACTIVE, MOVED_OUT }

data class TenantListItem(
    val tenant: TenantWithUnit,
    val pending: Long,
    val overdue: Boolean,
)

data class TenantsUiState(
    val loaded: Boolean = false,
    val query: String = "",
    val filter: TenantFilter = TenantFilter.ACTIVE,
    val items: List<TenantListItem> = emptyList(),
    val totalCount: Int = 0,
)

@HiltViewModel
class TenantsViewModel @Inject constructor(
    tenantRepository: TenantRepository,
    observeLedgers: ObserveLedgersUseCase,
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(TenantFilter.ACTIVE)

    val state: StateFlow<TenantsUiState> = combine(
        tenantRepository.observeAllTenants(),
        observeLedgers(),
        query,
        filter,
    ) { tenants, ledgers, q, f ->
        val now = DateUtils.now()
        val wanted = tenants.filter { it.tenant.isActive == (f == TenantFilter.ACTIVE) }
        val needle = q.trim()
        val shown = if (needle.isEmpty()) {
            wanted
        } else {
            wanted.filter {
                it.tenant.name.contains(needle, true) ||
                    it.tenant.phone.contains(needle) ||
                    it.unitName.orEmpty().contains(needle, true) ||
                    it.propertyName.orEmpty().contains(needle, true)
            }
        }
        TenantsUiState(
            loaded = true,
            query = q,
            filter = f,
            totalCount = tenants.size,
            items = shown.map {
                val ledger = ledgers[it.tenant.id] ?: TenantLedger.EMPTY
                TenantListItem(it, ledger.pending, ledger.overdueCharges(now).isNotEmpty())
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TenantsUiState())

    fun onQuery(q: String) {
        query.value = q
    }

    fun onFilter(f: TenantFilter) {
        filter.value = f
    }
}

data class TenantDetailState(
    val loaded: Boolean = false,
    val tenant: TenantWithUnit? = null,
    val ledger: TenantLedger = TenantLedger.EMPTY,
    val lease: Lease? = null,
)

@HiltViewModel
class TenantDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val tenantRepository: TenantRepository,
    private val photoStorage: PhotoStorage,
    private val messageBuilder: ReminderMessageBuilder,
    settingsRepository: SettingsRepository,
    observeLedger: ObserveTenantLedgerUseCase,
) : ViewModel() {
    val tenantId: Long = checkNotNull(savedStateHandle[Routes.ARG_TENANT_ID])

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    /** Rent reminder text for WhatsApp/SMS in the given language. */
    fun reminderMessage(language: String): String {
        val st = state.value
        val t = st.tenant ?: return ""
        val s = settings.value
        return messageBuilder.build(
            language = language,
            tenantName = t.tenant.name,
            amount = st.ledger.pending,
            months = st.ledger.charges.filter { it.remaining > 0 }.map { it.month }.distinct().sorted(),
            currencySymbol = s.currencySymbol,
            landlordName = s.landlordName,
            unitName = t.unitName,
        )
    }

    val state: StateFlow<TenantDetailState> = combine(
        tenantRepository.observeTenant(tenantId),
        observeLedger(tenantId),
        tenantRepository.observeLease(tenantId),
    ) { tenant, ledger, lease -> TenantDetailState(true, tenant, ledger, lease) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TenantDetailState())

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            val tenant = tenantRepository.getTenant(tenantId)
            val lease = tenantRepository.getLease(tenantId)
            tenantRepository.deleteTenant(tenantId)
            photoStorage.delete(tenant?.cnicPhoto)
            photoStorage.delete(lease?.agreementPhoto)
            onDone()
        }
    }
}

data class TenantForm(
    val id: Long = 0,
    val name: String = "",
    val phone: String = "",
    val cnic: String = "",
    val cnicPhoto: String? = null,
    val unitId: Long? = null,
    val rent: String = "",
    val deposit: String = "",
    val startDate: Long = DateUtils.startOfDay(DateUtils.now()),
    val dueDay: String = "1",
    val notes: String = "",
    val nameError: Boolean = false,
    val rentError: Boolean = false,
    val depositError: Boolean = false,
    val dueDayError: Boolean = false,
    val saved: Boolean = false,
    val savedId: Long = 0,
)

@HiltViewModel
class TenantEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val tenantRepository: TenantRepository,
    propertyRepository: PropertyRepository,
    private val photoStorage: PhotoStorage,
    private val generateRentEntries: GenerateRentEntriesUseCase,
    @ApplicationScope private val appScope: CoroutineScope,
) : ViewModel() {
    private val tenantId: Long = savedStateHandle[Routes.ARG_TENANT_ID] ?: 0L
    private val presetUnitId: Long = savedStateHandle[Routes.ARG_UNIT_ID] ?: 0L

    private var original: Tenant? = null
    private val _form = MutableStateFlow(TenantForm(unitId = presetUnitId.takeIf { it > 0 }))
    val form: StateFlow<TenantForm> = _form.asStateFlow()

    val units: StateFlow<List<UnitWithStatus>> = propertyRepository.observeAllUnits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val isEdit: Boolean get() = tenantId > 0

    init {
        viewModelScope.launch {
            if (tenantId > 0) {
                tenantRepository.getTenant(tenantId)?.let { t ->
                    original = t
                    _form.value = TenantForm(
                        id = t.id,
                        name = t.name,
                        phone = t.phone,
                        cnic = t.cnic,
                        cnicPhoto = t.cnicPhoto,
                        unitId = t.unitId,
                        rent = Money.toInput(t.rentAmount),
                        deposit = Money.toInput(t.securityDeposit),
                        startDate = t.startDate,
                        dueDay = t.dueDay.toString(),
                        notes = t.notes,
                    )
                }
            } else if (presetUnitId > 0) {
                propertyRepository.getUnit(presetUnitId)?.let { u ->
                    if (u.defaultRent > 0) _form.update { it.copy(rent = Money.toInput(u.defaultRent)) }
                }
            }
        }
    }

    fun onName(v: String) = _form.update { it.copy(name = v, nameError = false) }
    fun onPhone(v: String) = _form.update { it.copy(phone = v.filter { c -> c.isDigit() || c == '+' || c == ' ' || c == '-' }) }
    fun onCnic(v: String) = _form.update { it.copy(cnic = formatCnic(v)) }
    fun onRent(v: String) = _form.update { it.copy(rent = v, rentError = false) }
    fun onDeposit(v: String) = _form.update { it.copy(deposit = v, depositError = false) }
    fun onStartDate(v: Long) = _form.update { it.copy(startDate = v) }
    fun onDueDay(v: String) = _form.update { it.copy(dueDay = v.filter(Char::isDigit).take(2), dueDayError = false) }
    fun onNotes(v: String) = _form.update { it.copy(notes = v) }

    fun onUnit(unit: UnitWithStatus?) {
        _form.update { f ->
            val fillRent = f.rent.isBlank() && unit != null && unit.unit.defaultRent > 0
            f.copy(
                unitId = unit?.unit?.id,
                rent = if (fillRent) Money.toInput(unit!!.unit.defaultRent) else f.rent,
            )
        }
    }

    fun onPhotoPicked(uri: Uri) {
        viewModelScope.launch {
            val name = photoStorage.import(uri, "cnic") ?: return@launch
            val previous = _form.value.cnicPhoto
            _form.update { it.copy(cnicPhoto = name) }
            // Drop a photo picked earlier in this edit (never the saved one).
            if (previous != null && previous != original?.cnicPhoto) photoStorage.delete(previous)
        }
    }

    fun onPhotoRemoved() {
        val previous = _form.value.cnicPhoto
        _form.update { it.copy(cnicPhoto = null) }
        if (previous != null && previous != original?.cnicPhoto) {
            viewModelScope.launch { photoStorage.delete(previous) }
        }
    }

    fun save() {
        val f = _form.value
        val rent = Money.parse(f.rent)
        val deposit = if (f.deposit.isBlank()) 0L else Money.parse(f.deposit)
        val dueDay = f.dueDay.toIntOrNull()
        val errors = f.copy(
            nameError = f.name.isBlank(),
            rentError = rent == null || rent <= 0,
            depositError = deposit == null || deposit < 0,
            dueDayError = dueDay == null || dueDay !in 1..28,
        )
        if (errors.nameError || errors.rentError || errors.depositError || errors.dueDayError) {
            _form.value = errors
            return
        }
        viewModelScope.launch {
            val base = original ?: Tenant(name = "", rentAmount = 0, startDate = f.startDate)
            val tenant = base.copy(
                name = f.name,
                phone = f.phone,
                cnic = f.cnic,
                cnicPhoto = f.cnicPhoto,
                unitId = f.unitId,
                rentAmount = rent!!,
                securityDeposit = deposit!!,
                startDate = f.startDate,
                dueDay = dueDay!!,
                notes = f.notes,
            )
            val id = tenantRepository.saveTenant(tenant)
            // Create rent entries from the start month so dues show right away.
            if (tenant.isActive) generateRentEntries(id)
            original?.cnicPhoto?.let { old -> if (old != f.cnicPhoto) photoStorage.delete(old) }
            original = tenant.copy(id = id)
            _form.update { it.copy(saved = true, savedId = id) }
        }
    }

    override fun onCleared() {
        // Photo picked but the form was not saved: remove the unused file.
        val f = _form.value
        val photo = f.cnicPhoto
        if (!f.saved && photo != null && photo != original?.cnicPhoto) {
            appScope.launch { photoStorage.delete(photo) }
        }
    }

    companion object {
        /** 3520212345671 -> 35202-1234567-1 */
        fun formatCnic(input: String): String {
            val d = input.filter(Char::isDigit).take(13)
            return buildString {
                d.forEachIndexed { i, c ->
                    if (i == 5 || i == 12) append('-')
                    append(c)
                }
            }
        }
    }
}
