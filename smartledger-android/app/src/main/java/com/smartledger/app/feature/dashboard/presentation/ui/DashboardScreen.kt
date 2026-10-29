package com.smartledger.app.feature.dashboard.presentation.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartledger.app.core.ui.components.ErrorMessage
import com.smartledger.app.core.ui.theme.CreditGreen
import com.smartledger.app.core.ui.theme.DebitRed
import com.smartledger.app.core.ui.theme.GoldPrimary
import com.smartledger.app.core.ui.theme.SilkDarkBackground
import com.smartledger.app.core.ui.theme.SilkDarkCard
import com.smartledger.app.core.ui.theme.SilkDarkSurface
import com.smartledger.app.core.util.UiState
import com.smartledger.app.core.util.toIndianRupee
import com.smartledger.app.core.util.toWeightString
import com.smartledger.app.feature.dashboard.data.dto.DashboardSummaryResponse
import com.smartledger.app.feature.dashboard.presentation.viewmodel.DashboardViewModel

/**
 * Live Business Visibility Dashboard Screen (Phase 3).
 *
 * Requirements:
 * - Row 1: Cash card + Bank card (INR formatted)
 * - Row 2: Customer Outstanding + Supplier Payable (side by side, INR formatted)
 * - Row 3: Raw Silk kg + External WIP kg (3 decimal places)
 * - Row 4: Pending Orders count + Production count + Outsourcing count
 * - Pull-to-refresh on entire screen
 * - Shimmer skeleton loading state while fetching
 * - Strictly READ ONLY
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val pullToRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refresh() },
        state = pullToRefreshState,
        modifier = Modifier
            .fillMaxSize()
            .background(SilkDarkBackground)
    ) {
        when (val state = uiState) {
            is UiState.Loading -> {
                DashboardSkeleton()
            }
            is UiState.Success -> {
                DashboardContent(
                    summary = state.data,
                    isRefreshing = isRefreshing,
                    onRefresh = { viewModel.refresh() }
                )
            }
            is UiState.Error -> {
                ErrorMessage(
                    message = state.message,
                    onRetry = { viewModel.loadSummary() }
                )
            }
        }
    }
}

@Composable
fun DashboardContent(
    summary: DashboardSummaryResponse,
    isRefreshing: Boolean,
    onRefresh: () -> Unit
) {
    val scrollState = rememberScrollState()

    val infiniteTransition = rememberInfiniteTransition(label = "refresh_spin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // ── Top Header ─────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "SmartLedger",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = CreditGreen.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "LIVE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = CreditGreen
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = "Silk Warp Business Overview",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8)
                )
            }

            IconButton(
                onClick = onRefresh,
                enabled = !isRefreshing
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh Dashboard",
                    tint = GoldPrimary,
                    modifier = if (isRefreshing) Modifier.rotate(rotation) else Modifier
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── Row 1: Cash + Bank Cards ───────────────────────────────────────
        SectionHeader(title = "LIQUIDITY & CASH FLOW")
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = "Cash in Hand",
                value = summary.cash.toIndianRupee(),
                icon = Icons.Default.AccountBalanceWallet,
                accentColor = GoldPrimary,
                modifier = Modifier.weight(1f)
            )

            MetricCard(
                title = "Bank Balance",
                value = summary.bank.toIndianRupee(),
                icon = Icons.Default.AccountBalance,
                accentColor = Color(0xFF38BDF8),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Row 2: Customer Outstanding + Supplier Payable ─────────────────
        SectionHeader(title = "RECEIVABLES & PAYABLES")
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = "Cust. Outstanding",
                subtitle = "To Receive",
                value = summary.customerOutstanding.toIndianRupee(),
                icon = Icons.Default.ArrowDownward,
                accentColor = CreditGreen,
                modifier = Modifier.weight(1f)
            )

            MetricCard(
                title = "Supplier Payable",
                subtitle = "To Pay",
                value = summary.supplierPayable.toIndianRupee(),
                icon = Icons.Default.ArrowUpward,
                accentColor = DebitRed,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Row 3: Raw Silk Stock + External WIP ───────────────────────────
        SectionHeader(title = "SILK INVENTORY & OUTSOURCED WIP")
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = "Raw Silk Stock",
                subtitle = "In Godown",
                value = summary.rawSilkKg.toWeightString(),
                icon = Icons.Default.Inventory2,
                accentColor = Color(0xFFFBBF24),
                modifier = Modifier.weight(1f)
            )

            MetricCard(
                title = "External WIP",
                subtitle = "At Manufacturers",
                value = summary.externalWipKg.toWeightString(),
                icon = Icons.Default.LocalShipping,
                accentColor = Color(0xFFA78BFA),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Row 4: Pipeline Counters ───────────────────────────────────────
        SectionHeader(title = "OPERATIONAL PIPELINE")
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CounterCard(
                title = "Orders Pending",
                count = summary.pendingOrders,
                icon = Icons.Default.ShoppingBag,
                accentColor = Color(0xFFF59E0B),
                modifier = Modifier.weight(1f)
            )

            CounterCard(
                title = "In Production",
                count = summary.pendingProduction,
                icon = Icons.Default.Build,
                accentColor = Color(0xFF10B981),
                modifier = Modifier.weight(1f)
            )

            CounterCard(
                title = "Outsourced Jobs",
                count = summary.pendingOutsourcing,
                icon = Icons.Default.Handshake,
                accentColor = Color(0xFF6366F1),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Bottom Read-Only notice
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "• Live Read-Only Dashboard • Auto-aggregated from Ledger •",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF64748B)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp,
            color = Color(0xFF94A3B8)
        )
    )
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SilkDarkCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium.copy(color = Color(0xFF94A3B8)),
                    maxLines = 1
                )
                Surface(
                    shape = CircleShape,
                    color = accentColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = accentColor
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                ),
                maxLines = 1
            )
        }
    }
}

@Composable
fun CounterCard(
    title: String,
    count: Int,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SilkDarkSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = accentColor.copy(alpha = 0.15f),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                ),
                maxLines = 1
            )
        }
    }
}

// ── Shimmer Skeleton Placeholder ──────────────────────────────────────────

fun Modifier.shimmerLoading(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer_transition")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_float"
    )

    val brush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF1E293B),
            Color(0xFF334155),
            Color(0xFF1E293B)
        ),
        start = Offset.Zero,
        end = Offset(x = translateAnim, y = translateAnim)
    )
    this.background(brush)
}

@Composable
fun DashboardSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header skeleton
        Box(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .height(28.dp)
                .shimmerLoading()
        )
        Spacer(modifier = Modifier.height(24.dp))

        // Row 1 skeletons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(90.dp)
                    .shimmerLoading()
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(90.dp)
                    .shimmerLoading()
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Row 2 skeletons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(90.dp)
                    .shimmerLoading()
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(90.dp)
                    .shimmerLoading()
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Row 3 skeletons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(90.dp)
                    .shimmerLoading()
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(90.dp)
                    .shimmerLoading()
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Row 4 skeletons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(84.dp)
                    .shimmerLoading()
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(84.dp)
                    .shimmerLoading()
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(84.dp)
                    .shimmerLoading()
            )
        }
    }
}
