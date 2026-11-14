package com.kiraya.diary.ui.properties

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiraya.diary.core.money.Money
import com.kiraya.diary.domain.model.Property
import com.kiraya.diary.domain.model.PropertyWithCounts
import com.kiraya.diary.domain.model.RentalUnit
import com.kiraya.diary.domain.model.UnitType
import com.kiraya.diary.domain.model.UnitWithStatus
import com.kiraya.diary.domain.repository.PropertyRepository
import com.kiraya.diary.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PropertiesViewModel @Inject constructor(
    repository: PropertyRepository,
) : ViewModel() {
    val properties: StateFlow<List<PropertyWithCounts>?> = repository.observePropertiesWithCounts()
        .map<List<PropertyWithCounts>, List<PropertyWithCounts>?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

data class PropertyDetailState(
    val property: Property? = null,
    val units: List<UnitWithStatus> = emptyList(),
    val loaded: Boolean = false,
)

@HiltViewModel
class PropertyDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: PropertyRepository,
) : ViewModel() {
    private val propertyId: Long = checkNotNull(savedStateHandle[Routes.ARG_PROPERTY_ID])

    val state: StateFlow<PropertyDetailState> = combine(
        repository.observeProperty(propertyId),
        repository.observeUnits(propertyId),
    ) { property, units -> PropertyDetailState(property, units, loaded = true) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PropertyDetailState())

    fun deleteProperty(onDone: () -> Unit) {
        viewModelScope.launch {
            repository.deleteProperty(propertyId)
            onDone()
        }
    }

    fun deleteUnit(unitId: Long) {
        viewModelScope.launch { repository.deleteUnit(unitId) }
    }
}

data class PropertyForm(
    val id: Long = 0,
    val name: String = "",
    val address: String = "",
    val notes: String = "",
    val createdAt: Long = 0,
    val nameError: Boolean = false,
    val saved: Boolean = false,
)

@HiltViewModel
class PropertyEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: PropertyRepository,
) : ViewModel() {
    private val propertyId: Long = savedStateHandle[Routes.ARG_PROPERTY_ID] ?: 0L

    private val _form = MutableStateFlow(PropertyForm())
    val form: StateFlow<PropertyForm> = _form.asStateFlow()

    val isEdit: Boolean get() = propertyId > 0

    init {
        if (propertyId > 0) {
            viewModelScope.launch {
                repository.getProperty(propertyId)?.let { p ->
                    _form.value = PropertyForm(p.id, p.name, p.address, p.notes, p.createdAt)
                }
            }
        }
    }

    fun onName(v: String) = _form.update { it.copy(name = v, nameError = false) }
    fun onAddress(v: String) = _form.update { it.copy(address = v) }
    fun onNotes(v: String) = _form.update { it.copy(notes = v) }

    fun save() {
        val f = _form.value
        if (f.name.isBlank()) {
            _form.update { it.copy(nameError = true) }
            return
        }
        viewModelScope.launch {
            repository.saveProperty(Property(f.id, f.name, f.address, f.notes, f.createdAt))
            _form.update { it.copy(saved = true) }
        }
    }
}

data class UnitForm(
    val id: Long = 0,
    val name: String = "",
    val type: UnitType = UnitType.FLAT,
    val rent: String = "",
    val notes: String = "",
    val createdAt: Long = 0,
    val nameError: Boolean = false,
    val rentError: Boolean = false,
    val saved: Boolean = false,
)

@HiltViewModel
class UnitEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: PropertyRepository,
) : ViewModel() {
    private val propertyId: Long = checkNotNull(savedStateHandle[Routes.ARG_PROPERTY_ID])
    private val unitId: Long = savedStateHandle[Routes.ARG_UNIT_ID] ?: 0L

    private val _form = MutableStateFlow(UnitForm())
    val form: StateFlow<UnitForm> = _form.asStateFlow()

    val isEdit: Boolean get() = unitId > 0

    init {
        if (unitId > 0) {
            viewModelScope.launch {
                repository.getUnit(unitId)?.let { u ->
                    _form.value = UnitForm(u.id, u.name, u.type, Money.toInput(u.defaultRent), u.notes, u.createdAt)
                }
            }
        }
    }

    fun onName(v: String) = _form.update { it.copy(name = v, nameError = false) }
    fun onType(v: UnitType) = _form.update { it.copy(type = v) }
    fun onRent(v: String) = _form.update { it.copy(rent = v, rentError = false) }
    fun onNotes(v: String) = _form.update { it.copy(notes = v) }

    fun save() {
        val f = _form.value
        val rent = if (f.rent.isBlank()) 0L else Money.parse(f.rent)
        if (f.name.isBlank() || rent == null || rent < 0) {
            _form.update { it.copy(nameError = f.name.isBlank(), rentError = rent == null || rent < 0) }
            return
        }
        viewModelScope.launch {
            repository.saveUnit(RentalUnit(f.id, propertyId, f.name, f.type, rent, f.notes, f.createdAt))
            _form.update { it.copy(saved = true) }
        }
    }
}
