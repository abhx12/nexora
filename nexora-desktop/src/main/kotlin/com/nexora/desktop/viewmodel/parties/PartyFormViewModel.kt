package com.nexora.desktop.viewmodel.parties

import androidx.lifecycle.viewModelScope
import com.nexora.desktop.api.dto.PartyRequest
import com.nexora.desktop.api.dto.PartyResponse
import com.nexora.desktop.api.dto.PhoneDto
import com.nexora.desktop.repository.ApiResult
import com.nexora.desktop.repository.PartyRepository
import com.nexora.desktop.ui.components.toDecimalOrNull
import com.nexora.desktop.util.s
import com.nexora.desktop.viewmodel.common.FormViewModel
import kotlinx.coroutines.launch

data class PhoneRow(val number: String = "", val label: String = "", val primary: Boolean = false)

data class PartyForm(
    val name: String = "",
    val roles: Set<String> = emptySet(),
    /** Roles the party already has: shown checked and disabled, roles are never removed (FR-PARTY-09). */
    val existingRoles: Set<String> = emptySet(),
    val phones: List<PhoneRow> = listOf(PhoneRow(label = s("party.labelMobile"), primary = true)),
    val addressLine: String = "",
    val area: String = "",
    val city: String = "",
    val district: String = "",
    val state: String = s("party.defaultState"),
    val pincode: String = "",
    val creditDays: String = "",
    val customerCreditDays: String = "",
    val rollingRate: String = "",
    val warpingRate: String = "",
    val note: String = "",
    val loading: Boolean = false,
)

/** S-PTY-02 Party form for create and edit (FR-PARTY-01, -02, -04, -05, -08, -09, -13, -14). */
class PartyFormViewModel(private val repository: PartyRepository, private val partyId: String?) :
    FormViewModel<PartyForm, PartyResponse>(PartyForm(loading = partyId != null)) {

    val isEdit: Boolean get() = partyId != null

    init {
        if (partyId != null) {
            viewModelScope.launch {
                repository.get(partyId).getOrNull()?.let { party -> preset { fromParty(party) } }
            }
        }
    }

    private fun fromParty(p: PartyResponse) = PartyForm(
        name = p.name,
        roles = p.roles.toSet(),
        existingRoles = p.roles.toSet(),
        phones = p.phones.map { PhoneRow(it.phoneNumber, it.label, it.primary) },
        addressLine = p.addressLine.orEmpty(),
        area = p.area.orEmpty(),
        city = p.city.orEmpty(),
        district = p.district.orEmpty(),
        state = p.state.orEmpty(),
        pincode = p.pincode.orEmpty(),
        creditDays = p.defaultCreditDays?.toString().orEmpty(),
        customerCreditDays = p.customerCreditDays?.toString().orEmpty(),
        rollingRate = p.defaultRollingRatePerHour?.toPlainString().orEmpty(),
        warpingRate = p.defaultWarpingRatePerKg?.toPlainString().orEmpty(),
        note = p.note.orEmpty(),
    )

    fun toggleRole(role: String) = edit { f ->
        if (role in f.existingRoles) f else f.copy(roles = if (role in f.roles) f.roles - role else f.roles + role)
    }

    fun addPhone() = edit { it.copy(phones = it.phones + PhoneRow(label = s("party.labelMobile"))) }

    fun removePhone(index: Int) = edit { it.copy(phones = it.phones.filterIndexed { i, _ -> i != index }) }

    fun updatePhone(index: Int, change: (PhoneRow) -> PhoneRow) = edit { f ->
        f.copy(phones = f.phones.mapIndexed { i, row -> if (i == index) change(row) else row })
    }

    /** Exactly one primary number: choosing one clears the others. */
    fun makePrimary(index: Int) = edit { f -> f.copy(phones = f.phones.mapIndexed { i, row -> row.copy(primary = i == index) }) }

    /** Client checks mirroring 05 `PartyFields`; keys are the API field names so server errors land in the same place. */
    override fun validate(form: PartyForm): Map<String, String> {
        val errors = mutableMapOf<String, String>()
        if (form.name.isBlank()) errors["name"] = s("validation.required")
        if (form.roles.isEmpty()) errors["roles"] = s("party.chooseRole")
        form.phones.forEachIndexed { i, phone ->
            if (phone.number.isBlank()) return@forEachIndexed // a row without a number is ignored (not sent)
            if (!PHONE.matches(phone.number)) {
                errors["phones[$i].phoneNumber"] = "${s("party.phoneInvalid")} (entered: '${phone.number}')."
            }
            if (phone.label.isBlank()) errors["phones[$i].label"] = s("validation.required")
        }
        if (form.pincode.isNotBlank()) {
            val pin = form.pincode.trim()
            if (pin.length != 6 || !pin.all { it.isDigit() }) {
                errors["pincode"] = "Enter a valid 6-digit postal PIN code (entered: '${form.pincode}')."
            }
        }
        if ("SUPPLIER" in form.roles && form.creditDays.isNotBlank()) {
            val days = form.creditDays.toIntOrNull()
            if (days == null || days !in 0..365) {
                errors["defaultCreditDays"] = "Enter credit days between 0 and 365 (entered: '${form.creditDays}')."
            }
        }
        if ("CUSTOMER" in form.roles && form.customerCreditDays.isNotBlank()) {
            val days = form.customerCreditDays.toIntOrNull()
            if (days == null || days !in 0..365) {
                errors["customerCreditDays"] = "Enter credit days between 0 and 365 (entered: '${form.customerCreditDays}')."
            }
        }
        return errors
    }

    override suspend fun send(form: PartyForm, idempotencyKey: String): ApiResult<PartyResponse> {
        val phones = form.phones.filter { it.number.isNotBlank() }.let { rows ->
            // If no row is marked primary the first one becomes primary (06 S-PTY-02).
            if (rows.none { it.primary }) rows.mapIndexed { i, r -> r.copy(primary = i == 0) } else rows
        }.map { PhoneDto(phoneNumber = it.number.trim(), label = it.label.trim(), primary = it.primary) }
        val worker = "WORKER" in form.roles
        val request = PartyRequest(
            name = form.name.trim(),
            roles = if (partyId == null) form.roles.sorted() else null,
            addressLine = form.addressLine.trim().ifEmpty { null },
            area = form.area.trim().ifEmpty { null },
            city = form.city.trim().ifEmpty { null },
            district = form.district.trim().ifEmpty { null },
            state = form.state.trim().ifEmpty { null },
            pincode = form.pincode.trim().ifEmpty { null },
            note = form.note.trim().ifEmpty { null },
            phones = phones,
            defaultCreditDays = if ("SUPPLIER" in form.roles) form.creditDays.toIntOrNull() else null,
            customerCreditDays = if ("CUSTOMER" in form.roles) form.customerCreditDays.toIntOrNull() else null,
            defaultRollingRatePerHour = if (worker) form.rollingRate.toDecimalOrNull() else null,
            defaultWarpingRatePerKg = if (worker) form.warpingRate.toDecimalOrNull() else null,
        )
        if (partyId == null) return repository.create(request)
        // Edit: PATCH the fields, then add each newly ticked role (roles have their own endpoint, E-PARTY-05).
        var result = repository.update(partyId, request)
        for (role in (form.roles - form.existingRoles).sorted()) {
            if (result !is ApiResult.Success) break
            result = repository.addRole(partyId, role)
        }
        return result
    }

    companion object {
        /** 05 `Phone`: digits, spaces, `+`, `-`, brackets, 6-30 characters. */
        val PHONE = Regex("^[0-9+()\\- ]{6,30}$")
    }
}
