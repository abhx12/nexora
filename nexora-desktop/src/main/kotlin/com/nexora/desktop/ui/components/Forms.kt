package com.nexora.desktop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexora.desktop.api.ApiError
import com.nexora.desktop.util.s

/** Key figures (tiles, balances, outstanding, stock) are at least 20 sp (NFR-USE-03); this style is 24 sp. */
val FigureStyle = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.SemiBold)

/**
 * Keyboard rules for every form (06 rule P2): Ctrl+S saves, Esc cancels, Enter saves unless the focused field
 * is multi-line (a multi-line field uses Enter itself, so the event never bubbles up to here).
 */
fun Modifier.formKeys(onSave: () -> Unit, onCancel: () -> Unit): Modifier = this
    .onPreviewKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
        when {
            event.isCtrlPressed && event.key == Key.S -> { onSave(); true }
            event.key == Key.Escape -> { onCancel(); true }
            else -> false
        }
    }
    .onKeyEvent { event ->
        if (event.type == KeyEventType.KeyDown && (event.key == Key.Enter || event.key == Key.NumPadEnter)) {
            onSave()
            true
        } else {
            false
        }
    }

/**
 * Create/edit form shown in the 560 px right-hand panel. When the user cancels after editing, a confirmation
 * asks before the input is thrown away.
 */
@Composable
fun FormPanel(
    title: String,
    saveLabel: String,
    submitting: Boolean,
    dirty: Boolean,
    error: ApiError?,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    footerNote: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    var confirmDiscard by remember { mutableStateOf(false) }
    val cancel = { if (dirty) confirmDiscard = true else onCancel() }
    Column(Modifier.fillMaxSize().padding(16.dp).formKeys(onSave, cancel)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        error?.let { ErrorBanner(it, modifier = Modifier.padding(vertical = 8.dp)) }
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
        )
        footerNote?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)) {
            TextButton(onClick = cancel) { Text(s("common.cancel")) }
            SubmitButton(saveLabel, submitting, onSave, tag = "form.save")
        }
    }
    if (confirmDiscard) {
        ConfirmDialog(
            title = s("form.discardTitle"),
            recordName = title,
            effect = s("form.discardText"),
            confirmLabel = s("form.discard"),
            onConfirm = { confirmDiscard = false; onCancel() },
            onCancel = { confirmDiscard = false },
        )
    }
}

/** A short action (receive, return, transfer, reversal) as a 480 px modal dialog with the same keys as a form. */
@Composable
fun FormDialog(
    title: String,
    saveLabel: String,
    submitting: Boolean,
    error: ApiError?,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBox(onDismiss = onCancel) {
        Column(Modifier.formKeys(onSave, onCancel), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            error?.let { ErrorBanner(it) }
            content()
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)) {
                TextButton(onClick = onCancel) { Text(s("common.cancel")) }
                SubmitButton(saveLabel, submitting, onSave, tag = "dialog.save")
            }
        }
    }
}

/**
 * Modal drawn inside the main window (a dimmed layer plus a centred card), so it behaves the same on Windows,
 * Linux and in tests. Clicks on the dimmed area do nothing; Esc closes.
 */
@Composable
fun ModalBox(onDismiss: () -> Unit, width: Dp = 480.dp, content: @Composable () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Color(0x66000000))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            .onPreviewKeyEvent { if (it.type == KeyEventType.KeyDown && it.key == Key.Escape) { onDismiss(); true } else false },
        contentAlignment = Alignment.Center,
    ) {
        Surface(Modifier.width(width).heightIn(max = 700.dp), tonalElevation = 6.dp, shadowElevation = 12.dp) {
            Box(Modifier.padding(20.dp).verticalScroll(rememberScrollState())) { content() }
        }
    }
}

/**
 * Accepts only valid mobile/phone inputs:
 * - Digits only, maximum 10 digits for local mobile numbers (e.g. 9840123456).
 * - Or leading '+' followed by country code and digits, maximum 13 chars (e.g. +919840123456).
 * Returns the sanitized string, or null if invalid (rejecting the keystroke).
 */
fun acceptPhoneInput(typed: String): String? {
    val trimmed = typed.trim()
    if (trimmed.isEmpty()) return ""
    if (trimmed.startsWith("+")) {
        val digits = trimmed.substring(1)
        if (!digits.all { it.isDigit() } || digits.length > 12) return null
        return "+$digits"
    }
    if (!trimmed.all { it.isDigit() } || trimmed.length > 10) return null
    return trimmed
}

/**
 * Accepts only valid Indian PIN code inputs: strictly digits only, max 6 digits.
 */
fun acceptPincodeInput(typed: String): String? {
    val trimmed = typed.trim()
    if (trimmed.isEmpty()) return ""
    if (!trimmed.all { it.isDigit() } || trimmed.length > 6) return null
    return trimmed
}

/**
 * Accepts only valid GSTIN inputs: uppercase alphanumeric only, max 15 characters.
 */
fun acceptGstinInput(typed: String): String? {
    val trimmed = typed.trim().uppercase()
    if (trimmed.isEmpty()) return ""
    if (!trimmed.all { it.isLetterOrDigit() } || trimmed.length > 15) return null
    return trimmed
}

/**
 * Accepts only digits with an explicit maximum length.
 */
fun acceptDigitsInput(typed: String, maxDigits: Int): String? {
    val trimmed = typed.trim()
    if (trimmed.isEmpty()) return ""
    if (!trimmed.all { it.isDigit() } || trimmed.length > maxDigits) return null
    return trimmed
}

/** Plain text input with label, error under it, optional max length and keystroke filter. */
@Composable
fun TextInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
    singleLine: Boolean = true,
    maxLength: Int = 1000,
    enabled: Boolean = true,
    tag: String? = null,
    filter: ((String) -> String?)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { typed ->
            val text = if (filter != null) filter(typed) else typed.take(maxLength)
            if (text != null) onValueChange(text.take(maxLength))
        },
        label = { Text(label) },
        isError = error != null,
        supportingText = error?.let { { FieldError(it) } },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 2,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().let { if (tag != null) it.testTag(tag) else it },
    )
}

/** Specialized phone/mobile number field: max 10 digits (or 13 with +country code), digits only. */
@Composable
fun PhoneInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
    enabled: Boolean = true,
    tag: String? = null,
) = TextInput(
    label = label,
    value = value,
    onValueChange = onValueChange,
    modifier = modifier,
    error = error,
    singleLine = true,
    maxLength = 13,
    enabled = enabled,
    tag = tag,
    filter = ::acceptPhoneInput,
)

/** Specialized 6-digit Indian PIN code field: strictly digits only, max 6 chars. */
@Composable
fun PincodeInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
    enabled: Boolean = true,
    tag: String? = null,
) = TextInput(
    label = label,
    value = value,
    onValueChange = onValueChange,
    modifier = modifier,
    error = error,
    singleLine = true,
    maxLength = 6,
    enabled = enabled,
    tag = tag,
    filter = ::acceptPincodeInput,
)

/** Specialized 15-character GSTIN field: uppercase alphanumeric only, max 15 chars. */
@Composable
fun GstinInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
    enabled: Boolean = true,
    tag: String? = null,
) = TextInput(
    label = label,
    value = value,
    onValueChange = onValueChange,
    modifier = modifier,
    error = error,
    singleLine = true,
    maxLength = 15,
    enabled = enabled,
    tag = tag,
    filter = ::acceptGstinInput,
)

/** Segmented choice (Credit / Cash, Rolling / Warping ...) as chips; each chip is a Tab stop. */
@Composable
fun <T> Segmented(label: String, options: List<Pair<T, String>>, selected: T?, onSelect: (T) -> Unit, enabled: Boolean = true) {
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { (value, text) ->
                FilterChip(
                    selected = value == selected,
                    onClick = { onSelect(value) },
                    label = { Text(text) },
                    enabled = enabled,
                    modifier = Modifier.heightIn(min = 32.dp),
                )
            }
        }
    }
}

/** Select from a list: a button showing the choice that opens a menu (arrow keys + Enter work in the menu). */
@Composable
fun <T> Dropdown(
    label: String,
    options: List<Pair<T, String>>,
    selected: T?,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
    tag: String? = null,
) {
    var open by remember { mutableStateOf(false) }
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Box {
            OutlinedButton(
                onClick = { open = true },
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = label }.let { if (tag != null) it.testTag(tag) else it },
            ) {
                Text(options.firstOrNull { it.first == selected }?.second ?: s("common.choose"), Modifier.weight(1f))
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                options.forEach { (value, text) ->
                    DropdownMenuItem(text = { Text(text) }, onClick = { onSelect(value); open = false })
                }
            }
        }
        error?.let { FieldError(it) }
    }
}

/** "Label: value" line for detail headers. */
@Composable
fun LabeledValue(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.padding(end = 24.dp, bottom = 8.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

/** A key figure tile: label and a big number (>= 20 sp), optionally clickable to its detail screen. */
@Composable
fun FigureTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColour: Color? = null,
    badge: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    Surface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        tonalElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = modifier.padding(4.dp).let { if (onClick != null) it.clickable(onClick = onClick) else it },
    ) {
        Column(Modifier.padding(14.dp).width(200.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (badge != null) badge()
            }
            androidx.compose.foundation.layout.Spacer(Modifier.height(4.dp))
            Text(value, style = FigureStyle, color = valueColour ?: MaterialTheme.colorScheme.onSurface, modifier = Modifier.testTag("figure"))
        }
    }
}

/** A labelled section with a bold heading. */
@Composable
fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        content()
    }
}
