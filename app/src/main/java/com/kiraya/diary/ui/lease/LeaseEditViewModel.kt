package com.kiraya.diary.ui.lease

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiraya.diary.core.coroutines.ApplicationScope
import com.kiraya.diary.core.date.DateUtils
import com.kiraya.diary.core.money.Money
import com.kiraya.diary.data.files.PhotoStorage
import com.kiraya.diary.domain.model.Lease
import com.kiraya.diary.domain.repository.TenantRepository
import com.kiraya.diary.domain.usecase.SaveLeaseUseCase
import com.kiraya.diary.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LeaseForm(
    val loaded: Boolean = false,
    val exists: Boolean = false,
    val tenantName: String = "",
    val startDate: Long = DateUtils.startOfDay(DateUtils.now()),
    val endDate: Long? = null,
    val baseRent: String = "",
    val increasePercent: String = "",
    val agreementPhoto: String? = null,
    val notes: String = "",
    val rentError: Boolean = false,
    val percentError: Boolean = false,
    val endError: Boolean = false,
    val done: Boolean = false,
)

@HiltViewModel
class LeaseEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val tenantRepository: TenantRepository,
    private val saveLease: SaveLeaseUseCase,
    private val photoStorage: PhotoStorage,
    @ApplicationScope private val appScope: CoroutineScope,
) : ViewModel() {
    private val tenantId: Long = checkNotNull(savedStateHandle[Routes.ARG_TENANT_ID])
    private var original: Lease? = null

    private val _form = MutableStateFlow(LeaseForm())
    val form: StateFlow<LeaseForm> = _form.asStateFlow()

    init {
        viewModelScope.launch {
            val tenant = tenantRepository.getTenant(tenantId) ?: return@launch
            val lease = tenantRepository.getLease(tenantId)
            original = lease
            _form.value = if (lease != null) {
                LeaseForm(
                    loaded = true,
                    exists = true,
                    tenantName = tenant.name,
                    startDate = lease.startDate,
                    endDate = lease.endDate,
                    baseRent = Money.toInput(lease.baseRent),
                    increasePercent = formatPercent(lease.yearlyIncreasePercent),
                    agreementPhoto = lease.agreementPhoto,
                    notes = lease.notes,
                )
            } else {
                LeaseForm(
                    loaded = true,
                    tenantName = tenant.name,
                    startDate = tenant.startDate,
                    endDate = DateUtils.addYears(tenant.startDate, 1),
                    baseRent = Money.toInput(tenant.rentAmount),
                )
            }
        }
    }

    fun onStart(v: Long) = _form.update { it.copy(startDate = v, endError = false) }
    fun onEnd(v: Long?) = _form.update { it.copy(endDate = v, endError = false) }
    fun onEndInYears(years: Int) = _form.update {
        it.copy(endDate = DateUtils.addYears(it.startDate, years) - DateUtils.DAY_MILLIS, endError = false)
    }
    fun onRent(v: String) = _form.update { it.copy(baseRent = v, rentError = false) }
    fun onPercent(v: String) = _form.update {
        it.copy(increasePercent = v.filter { c -> c.isDigit() || c == '.' }.take(5), percentError = false)
    }
    fun onNotes(v: String) = _form.update { it.copy(notes = v) }

    fun onPhotoPicked(uri: Uri) {
        viewModelScope.launch {
            val name = photoStorage.import(uri, "agreement") ?: return@launch
            val previous = _form.value.agreementPhoto
            _form.update { it.copy(agreementPhoto = name) }
            if (previous != null && previous != original?.agreementPhoto) photoStorage.delete(previous)
        }
    }

    fun onPhotoRemoved() {
        val previous = _form.value.agreementPhoto
        _form.update { it.copy(agreementPhoto = null) }
        if (previous != null && previous != original?.agreementPhoto) {
            viewModelScope.launch { photoStorage.delete(previous) }
        }
    }

    fun save() {
        val f = _form.value
        val rent = Money.parse(f.baseRent)
        val percent = if (f.increasePercent.isBlank()) 0.0 else f.increasePercent.toDoubleOrNull()
        val endBad = f.endDate != null && f.endDate <= f.startDate
        if (rent == null || rent <= 0 || percent == null || percent !in 0.0..100.0 || endBad) {
            _form.update {
                it.copy(
                    rentError = rent == null || rent <= 0,
                    percentError = percent == null || percent !in 0.0..100.0,
                    endError = endBad,
                )
            }
            return
        }
        viewModelScope.launch {
            saveLease(
                Lease(
                    tenantId = tenantId,
                    startDate = f.startDate,
                    endDate = f.endDate,
                    baseRent = rent,
                    yearlyIncreasePercent = percent,
                    agreementPhoto = f.agreementPhoto,
                    notes = f.notes,
                ),
            )
            original?.agreementPhoto?.let { old -> if (old != f.agreementPhoto) photoStorage.delete(old) }
            original = original?.copy(agreementPhoto = f.agreementPhoto)
            _form.update { it.copy(done = true) }
        }
    }

    fun delete() {
        viewModelScope.launch {
            tenantRepository.deleteLease(tenantId)
            photoStorage.delete(original?.agreementPhoto)
            val unsaved = _form.value.agreementPhoto
            if (unsaved != original?.agreementPhoto) photoStorage.delete(unsaved)
            original = null
            _form.update { it.copy(agreementPhoto = null, done = true) }
        }
    }

    override fun onCleared() {
        val f = _form.value
        val photo = f.agreementPhoto
        if (!f.done && photo != null && photo != original?.agreementPhoto) {
            appScope.launch { photoStorage.delete(photo) }
        }
    }

    private fun formatPercent(p: Double): String =
        if (p == 0.0) "" else if (p % 1.0 == 0.0) p.toLong().toString() else p.toString()
}
