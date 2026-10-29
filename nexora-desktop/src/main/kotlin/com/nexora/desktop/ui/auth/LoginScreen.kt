package com.nexora.desktop.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexora.desktop.api.dto.MeResponse
import com.nexora.desktop.util.s
import com.nexora.desktop.viewmodel.auth.LoginViewModel
import java.awt.Toolkit
import java.awt.event.KeyEvent

/** S-AUTH-02 Login. Enter in either field logs in; the whole screen works without a mouse. */
@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoggedIn: (MeResponse) -> Unit,
    onChangeServer: () -> Unit = {},
    onStartSetup: () -> Unit = {},
) {
    val state by viewModel.state.collectAsState()
    var capsLock by remember { mutableStateOf(false) }
    val usernameFocus = remember { FocusRequester() }
    val passwordFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        // Start typing straight away: the password when the username is remembered, else the username.
        if (state.username.isEmpty()) usernameFocus.requestFocus() else passwordFocus.requestFocus()
    }

    val submitOnEnter = Modifier.onPreviewKeyEvent { event ->
        capsLock = capsLockOn()
        if (event.type == KeyEventType.KeyDown && (event.key == Key.Enter || event.key == Key.NumPadEnter)) {
            viewModel.login(onLoggedIn)
            true
        } else {
            false
        }
    }

    // Soft modern background gradient
    val bgGradient = Brush.verticalGradient(
        listOf(
            Color(0xFFEBF1F7),
            Color(0xFFDEE7F2),
            Color(0xFFD4E0EE),
        )
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(bgGradient),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            modifier = Modifier
                .width(880.dp)
                .padding(24.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
            ) {
                // Left side: Brand Showcase Hero Panel
                BrandHeroPanel(
                    modifier = Modifier
                        .width(360.dp)
                        .fillMaxHeight(),
                )

                // Right side: Login Form Panel
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(horizontal = 36.dp, vertical = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    // Header tag & title
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            s("login.welcome").uppercase(),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                        )

                        Spacer(Modifier.height(4.dp))

                        Text(
                            s("login.title"),
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )

                        Text(
                            s("login.subtitle"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        )
                    }

                    if (state.credentialStoreMissing) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFFFBEB), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(8.dp))
                                .padding(10.dp),
                        ) {
                            Text(
                                s("login.noCredentialStore"),
                                color = Color(0xFF92400E),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    // Username field
                    OutlinedTextField(
                        value = state.username,
                        onValueChange = { viewModel.onUsernameChange(it.take(50)) },
                        label = { Text(s("login.username")) },
                        leadingIcon = {
                            UserGlyph(
                                tint = if (state.usernameError != null) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.primary,
                            )
                        },
                        isError = state.usernameError != null,
                        supportingText = state.usernameError?.let { { Text(it) } },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(usernameFocus)
                            .then(submitOnEnter)
                            .testTag("login.username"),
                    )

                    // Password field
                    OutlinedTextField(
                        value = state.password,
                        onValueChange = { viewModel.onPasswordChange(it.take(72)) },
                        label = { Text(s("login.password")) },
                        leadingIcon = {
                            LockGlyph(
                                tint = if (state.passwordError != null) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.primary,
                            )
                        },
                        isError = state.passwordError != null,
                        supportingText = (state.passwordError ?: if (capsLock) s("login.capsLock") else null)?.let { { Text(it) } },
                        singleLine = true,
                        visualTransformation = if (state.showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            TextButton(onClick = viewModel::toggleShowPassword) {
                                Text(
                                    if (state.showPassword) s("login.hidePassword") else s("login.showPassword"),
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(passwordFocus)
                            .then(submitOnEnter)
                            .testTag("login.password"),
                    )

                    // Links: Forgot Password & Change Server
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(onClick = viewModel::onForgotPasswordClick) {
                            Text(
                                s("login.forgotPassword"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                        TextButton(onClick = onChangeServer) {
                            Text(
                                s("login.changeServer"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    // Caps Lock indicator badge
                    if (capsLock && state.passwordError == null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFEF3C7), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Text(
                                "⇪ " + s("login.capsLock"),
                                color = Color(0xFFB45309),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }

                    // Error banner
                    state.error?.let { err ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFEE2E2), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFFFCA5A5), RoundedCornerShape(8.dp))
                                .padding(10.dp),
                        ) {
                            Text(
                                err,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    // Submit button
                    Button(
                        onClick = { viewModel.login(onLoggedIn) },
                        enabled = !state.submitting,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                    ) {
                        if (state.submitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp,
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(s("login.submit"), fontWeight = FontWeight.SemiBold)
                        } else {
                            Text(
                                s("login.submit"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }

                    // Divider & New User Registration
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f))
                        Text(
                            "  " + s("login.orNewUser") + "  ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f))
                    }

                    OutlinedButton(
                        onClick = { viewModel.onCreateAccountClick(onStartSetup) },
                        enabled = !state.checkingSetup && !state.submitting,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                    ) {
                        if (state.checkingSetup) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            s("login.createAccount"),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }

        // Forgot Password Dialog
        if (state.showForgotPasswordDialog) {
            AlertDialog(
                onDismissRequest = viewModel::dismissForgotPasswordDialog,
                title = {
                    Text(
                        s("login.forgotPasswordTitle"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                },
                text = {
                    Text(
                        s("login.forgotPasswordContent"),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                confirmButton = {
                    Button(onClick = viewModel::dismissForgotPasswordDialog) {
                        Text(s("common.close"))
                    }
                },
            )
        }

        // Already Configured Dialog
        if (state.showAlreadyRegisteredDialog) {
            AlertDialog(
                onDismissRequest = viewModel::dismissAlreadyRegisteredDialog,
                title = {
                    Text(
                        s("login.alreadyRegisteredTitle"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                },
                text = {
                    Text(
                        s("login.alreadyRegisteredContent"),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                confirmButton = {
                    Button(onClick = viewModel::dismissAlreadyRegisteredDialog) {
                        Text(s("common.close"))
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        viewModel.dismissAlreadyRegisteredDialog()
                        onChangeServer()
                    }) {
                        Text(s("login.changeServer"))
                    }
                },
            )
        }
    }
}

/** Left hero branding panel showcasing SmartSilk and its core enterprise capabilities. */
@Composable
private fun BrandHeroPanel(modifier: Modifier = Modifier) {
    val heroGradient = Brush.verticalGradient(
        listOf(
            Color(0xFF0F2E4F),
            Color(0xFF163E66),
            Color(0xFF1F4E79),
        )
    )

    Box(
        modifier = modifier
            .background(heroGradient)
            .padding(32.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Top branding badge
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(
                        "NEXORA ENTERPRISE",
                        color = Color(0xFFBAE6FD),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                    )
                }

                // Silk Loom / Weave Emblem
                SilkLoomEmblem(Modifier.size(56.dp))

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "SmartSilk",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                        ),
                    )
                    Text(
                        s("login.brandSubtitle"),
                        color = Color(0xFFD0E1F9),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color.White.copy(alpha = 0.15f)),
                )

                // Feature points
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FeatureRow(s("login.featureLedgers"))
                    FeatureRow(s("login.featureInventory"))
                    FeatureRow(s("login.featureSecurity"))
                }
            }

            // Bottom security note
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color(0xFF38BDF8), CircleShape),
                )
                Text(
                    s("login.securityNote"),
                    color = Color(0xFFA5C5E8),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Composable
private fun FeatureRow(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Canvas(modifier = Modifier.size(10.dp)) {
            drawCircle(
                color = Color(0xFF38BDF8),
                radius = size.minDimension / 2f,
                style = Fill,
            )
        }
        Text(
            text,
            color = Color(0xFFF0F6FC),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
        )
    }
}

/** Custom vector emblem representing silk yarn warping and weaving. */
@Composable
private fun SilkLoomEmblem(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Outer rounded diamond badge
        drawRoundRect(
            color = Color(0xFF38BDF8).copy(alpha = 0.2f),
            size = Size(w, h),
            cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx()),
        )
        drawRoundRect(
            color = Color(0xFF38BDF8),
            size = Size(w, h),
            cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx()),
            style = Stroke(width = 1.5.dp.toPx()),
        )

        // Interlacing silk yarn weft/warp threads
        val threadColor = Color.White
        val stroke = 2.dp.toPx()

        // Vertical threads
        drawLine(threadColor, Offset(w * 0.35f, h * 0.22f), Offset(w * 0.35f, h * 0.78f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(threadColor, Offset(w * 0.50f, h * 0.20f), Offset(w * 0.50f, h * 0.80f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(threadColor, Offset(w * 0.65f, h * 0.22f), Offset(w * 0.65f, h * 0.78f), strokeWidth = stroke, cap = StrokeCap.Round)

        // Horizontal weaving threads
        val goldColor = Color(0xFFFBBF24)
        drawLine(goldColor, Offset(w * 0.22f, h * 0.38f), Offset(w * 0.78f, h * 0.38f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(goldColor, Offset(w * 0.20f, h * 0.50f), Offset(w * 0.80f, h * 0.50f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(goldColor, Offset(w * 0.22f, h * 0.62f), Offset(w * 0.78f, h * 0.62f), strokeWidth = stroke, cap = StrokeCap.Round)
    }
}

/** Clean vector glyph for username. */
@Composable
private fun UserGlyph(tint: Color) {
    Canvas(modifier = Modifier.size(18.dp)) {
        val w = size.width
        val h = size.height

        // Head
        drawCircle(
            color = tint,
            radius = w * 0.22f,
            center = Offset(w * 0.5f, h * 0.30f),
            style = Stroke(width = 1.8.dp.toPx()),
        )

        // Shoulders arc
        val path = Path().apply {
            moveTo(w * 0.15f, h * 0.85f)
            cubicTo(
                w * 0.20f, h * 0.58f,
                w * 0.80f, h * 0.58f,
                w * 0.85f, h * 0.85f,
            )
        }
        drawPath(
            path = path,
            color = tint,
            style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round),
        )
    }
}

/** Clean vector glyph for password. */
@Composable
private fun LockGlyph(tint: Color) {
    Canvas(modifier = Modifier.size(18.dp)) {
        val w = size.width
        val h = size.height

        // Shackle
        val shacklePath = Path().apply {
            moveTo(w * 0.30f, h * 0.45f)
            lineTo(w * 0.30f, h * 0.26f)
            cubicTo(
                w * 0.30f, h * 0.10f,
                w * 0.70f, h * 0.10f,
                w * 0.70f, h * 0.26f,
            )
            lineTo(w * 0.70f, h * 0.45f)
        }
        drawPath(
            path = shacklePath,
            color = tint,
            style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round),
        )

        // Body
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.18f, h * 0.45f),
            size = Size(w * 0.64f, h * 0.45f),
            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
            style = Stroke(width = 1.8.dp.toPx()),
        )

        // Keyhole
        drawCircle(
            color = tint,
            radius = 1.5.dp.toPx(),
            center = Offset(w * 0.5f, h * 0.63f),
            style = Fill,
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.5f, h * 0.63f),
            end = Offset(w * 0.5f, h * 0.75f),
            strokeWidth = 1.5.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}

/** Caps Lock state from the OS; some platforms (and headless tests) cannot tell, which counts as "off". */
private fun capsLockOn(): Boolean =
    runCatching { Toolkit.getDefaultToolkit().getLockingKeyState(KeyEvent.VK_CAPS_LOCK) }.getOrDefault(false)
