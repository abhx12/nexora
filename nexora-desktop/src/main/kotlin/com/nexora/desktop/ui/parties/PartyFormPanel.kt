package com.nexora.desktop.ui.parties

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.nexora.desktop.ui.components.FieldError
import com.nexora.desktop.ui.components.FormPanel
import com.nexora.desktop.ui.components.MoneyField
import com.nexora.desktop.ui.components.PhoneInputField
import com.nexora.desktop.ui.components.PincodeInputField
import com.nexora.desktop.ui.components.TextInput
import com.nexora.desktop.ui.components.enumLabel
import com.nexora.desktop.ui.components.screenViewModel
import com.nexora.desktop.util.s
import com.nexora.desktop.viewmodel.parties.PartyFormViewModel
import com.nexora.desktop.viewmodel.shell.NavigationViewModel
import com.nexora.desktop.viewmodel.shell.Screen

/** S-PTY-02 Party form (create / edit). */
@Composable
fun PartyFormPanel(nav: NavigationViewModel, partyId: String?) {
    val vm: PartyFormViewModel = screenViewModel(partyId)
    val form by vm.form.collectAsState()
    val status by vm.status.collectAsState()
    val errors = status.fieldErrors
    LaunchedEffect(status.result) {
        status.result?.let { party ->
            nav.closeOverlay()
            // After a create the new party opens; after an edit the open detail screen reloads (same as F5).
            if (vm.isEdit) nav.reload() else nav.push(Screen.PartyDetail(party.id))
        }
    }
    FormPanel(
        title = if (vm.isEdit) s("party.editTitle") else s("party.newTitle"),
        saveLabel = s("common.save"),
        submitting = status.submitting,
        dirty = status.dirty,
        error = status.error,
        onSave = vm::save,
        onCancel = nav::closeOverlay,
    ) {
        TextInput(s("party.name"), form.name, { v -> vm.edit { it.copy(name = v) } }, error = errors["name"], maxLength = 200, tag = "party.name")
        Text(s("party.roles"), style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PARTY_ROLES.forEach { role ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = role in form.roles,
                        onCheckedChange = { vm.toggleRole(role) },
                        // Existing roles are checked and locked: roles are never removed (FR-PARTY-09).
                        enabled = role !in form.existingRoles,
                        modifier = Modifier.semantics { contentDescription = enumLabel("PartyRole", role) },
                    )
                    Text(enumLabel("PartyRole", role))
                }
            }
        }
        errors["roles"]?.let { FieldError(it) }
        Text(s("party.phones"), style = MaterialTheme.typography.labelLarge)
        form.phones.forEachIndexed { index, phone ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PhoneInputField(
                    label = s("party.phoneNumber"),
                    value = phone.number,
                    onValueChange = { v -> vm.updatePhone(index) { it.copy(number = v) } },
                    modifier = Modifier.width(200.dp),
                    error = errors["phones[$index].phoneNumber"],
                )
                TextInput(
                    s("party.phoneLabel"), phone.label, { v -> vm.updatePhone(index) { it.copy(label = v) } },
                    Modifier.width(140.dp), errors["phones[$index].label"], maxLength = 50,
                )
                RadioButton(
                    selected = phone.primary,
                    onClick = { vm.makePrimary(index) },
                    modifier = Modifier.semantics { contentDescription = s("party.primary") },
                )
                TextButton(onClick = { vm.removePhone(index) }) { Text(s("common.remove")) }
            }
        }
        TextButton(onClick = vm::addPhone) { Text(s("party.addPhone")) }
        errors["phones"]?.let { FieldError(it) }
        TextInput(s("party.addressLine"), form.addressLine, { v -> vm.edit { it.copy(addressLine = v) } }, error = errors["addressLine"], maxLength = 255)
        TextInput(s("party.area"), form.area, { v -> vm.edit { it.copy(area = v) } }, error = errors["area"], maxLength = 155)
        TextInput(s("party.city"), form.city, { v -> vm.edit { it.copy(city = v) } }, error = errors["city"], maxLength = 100)
        TextInput(s("party.district"), form.district, { v -> vm.edit { it.copy(district = v) } }, error = errors["district"], maxLength = 100)
        TextInput(s("party.state"), form.state, { v -> vm.edit { it.copy(state = v) } }, error = errors["state"], maxLength = 50)
        PincodeInputField(
            label = s("party.pincode"),
            value = form.pincode,
            onValueChange = { v -> vm.edit { it.copy(pincode = v) } },
            error = errors["pincode"],
        )
        if ("SUPPLIER" in form.roles) {
            TextInput(
                s("party.creditDays"), form.creditDays, { v -> vm.edit { it.copy(creditDays = v.filter(Char::isDigit)) } },
                error = errors["defaultCreditDays"], maxLength = 3,
            )
        }
        if ("CUSTOMER" in form.roles) {
            TextInput(
                s("party.customerCreditDays"), form.customerCreditDays, { v -> vm.edit { it.copy(customerCreditDays = v.filter(Char::isDigit)) } },
                error = errors["customerCreditDays"], maxLength = 3,
            )
        }
        if ("WORKER" in form.roles) {
            MoneyField(s("party.rollingRate"), form.rollingRate, { v -> vm.edit { it.copy(rollingRate = v) } }, error = errors["defaultRollingRatePerHour"])
            MoneyField(s("party.warpingRate"), form.warpingRate, { v -> vm.edit { it.copy(warpingRate = v) } }, error = errors["defaultWarpingRatePerKg"])
        }
        TextInput(s("common.notes"), form.note, { v -> vm.edit { it.copy(note = v) } }, error = errors["note"], singleLine = false, maxLength = 1000)
    }
}
