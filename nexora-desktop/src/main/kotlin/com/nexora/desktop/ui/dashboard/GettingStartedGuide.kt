package com.nexora.desktop.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexora.desktop.api.dto.DashboardResponse
import com.nexora.desktop.util.s
import com.nexora.desktop.viewmodel.shell.NavigationViewModel
import com.nexora.desktop.viewmodel.shell.Section as NavSection

/**
 * Interactive Getting Started Checklist and Walkthrough Tutorial for new users.
 * Helps business owners and operators immediately understand next steps.
 */
@Composable
fun GettingStartedCard(
    dashboardData: DashboardResponse,
    nav: NavigationViewModel,
    modifier: Modifier = Modifier,
) {
    var dismissed by remember { mutableStateOf(false) }
    var showTutorial by remember { mutableStateOf(false) }

    if (dismissed) {
        Row(
            modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = { dismissed = false }) {
                Text(s("guide.showGuide"), fontWeight = FontWeight.SemiBold)
            }
            TextButton(onClick = { showTutorial = true }) {
                Text(s("guide.tutorialButton"), fontWeight = FontWeight.Medium)
            }
        }
        if (showTutorial) {
            WorkflowTutorialDialog(onDismiss = { showTutorial = false })
        }
        return
    }

    val hasStock = (dashboardData.stock.rawSilkKg > java.math.BigDecimal.ZERO) ||
            (dashboardData.stock.finishedSilkWarpKg > java.math.BigDecimal.ZERO) ||
            (dashboardData.stock.finishedKoraWarpKg > java.math.BigDecimal.ZERO)
    val hasOrders = dashboardData.ordersByStatus.isNotEmpty()
    val hasTransactions = dashboardData.recentTransactions.isNotEmpty()

    var step1Completed by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Header row with badge and dismiss action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(
                            "NEW USER GUIDE",
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Text(
                        s("guide.title"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(onClick = { showTutorial = true }) {
                        Text(s("guide.tutorialButton"), style = MaterialTheme.typography.labelMedium)
                    }
                    TextButton(onClick = { dismissed = true }) {
                        Text(s("guide.hideGuide"), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            Text(
                s("guide.subtitle"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Step items flow
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                GuideStepCard(
                    stepNumber = "1",
                    title = s("guide.step1Title"),
                    desc = s("guide.step1Desc"),
                    actionText = s("guide.step1Action"),
                    isCompleted = step1Completed,
                    onClick = {
                        step1Completed = true
                        nav.open(NavSection.SETTINGS)
                    },
                    modifier = Modifier.weight(1f),
                )

                GuideStepCard(
                    stepNumber = "2",
                    title = s("guide.step2Title"),
                    desc = s("guide.step2Desc"),
                    actionText = s("guide.step2Action"),
                    isCompleted = false,
                    onClick = { nav.open(NavSection.PARTIES) },
                    modifier = Modifier.weight(1f),
                )

                GuideStepCard(
                    stepNumber = "3",
                    title = s("guide.step3Title"),
                    desc = s("guide.step3Desc"),
                    actionText = s("guide.step3Action"),
                    isCompleted = hasStock,
                    onClick = { nav.open(NavSection.PURCHASES) },
                    modifier = Modifier.weight(1f),
                )

                GuideStepCard(
                    stepNumber = "4",
                    title = s("guide.step4Title"),
                    desc = s("guide.step4Desc"),
                    actionText = s("guide.step4Action"),
                    isCompleted = hasOrders,
                    onClick = { nav.open(NavSection.ORDERS) },
                    modifier = Modifier.weight(1f),
                )

                GuideStepCard(
                    stepNumber = "5",
                    title = s("guide.step5Title"),
                    desc = s("guide.step5Desc"),
                    actionText = s("guide.step5Action"),
                    isCompleted = hasTransactions,
                    onClick = { nav.open(NavSection.MONEY) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    if (showTutorial) {
        WorkflowTutorialDialog(onDismiss = { showTutorial = false })
    }
}

@Composable
private fun GuideStepCard(
    stepNumber: String,
    title: String,
    desc: String,
    actionText: String,
    isCompleted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.width(260.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = BorderStroke(
            1.dp,
            if (isCompleted) Color(0xFF10B981) else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(
                            if (isCompleted) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                            CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (isCompleted) "✓" else stepNumber,
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }

                if (isCompleted) {
                    Text(
                        s("guide.completed"),
                        color = Color(0xFF059669),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                    )
                } else {
                    Text(
                        s("guide.pending"),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )

            Text(
                desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                minLines = 2,
            )

            Spacer(Modifier.height(4.dp))

            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(actionText, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

/**
 * Rich Interactive Workflow Tutorial Dialog showing the core silk lifecycle,
 * daily operations, and productivity shortcuts.
 */
@Composable
fun WorkflowTutorialDialog(onDismiss: () -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        s("guide.tutorialTabWorkflow"),
        s("guide.tutorialTabDaily"),
        s("guide.tutorialTabTips"),
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                s("guide.tutorialTitle"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .width(680.dp)
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                PrimaryTabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, fontWeight = FontWeight.SemiBold) },
                        )
                    }
                }

                when (selectedTab) {
                    0 -> WorkflowLifecycleTab()
                    1 -> DailyOperationsTab()
                    2 -> TipsAndShortcutsTab()
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(s("common.close"))
            }
        },
    )
}

@Composable
private fun WorkflowLifecycleTab() {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(top = 8.dp),
    ) {
        Text(
            "SmartSilk Full Lifecycle: From Raw Silk to Delivered Sarees",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )

        LifecycleStepRow(
            number = "1",
            title = "Raw Silk Procurement",
            desc = "Order raw silk from suppliers. Record receipt weights and settle invoices via bank, cash, or cheques.",
        )
        LifecycleStepRow(
            number = "2",
            title = "Inventory & Quality Control",
            desc = "Track separate real-time weights for Raw Silk, Internal WIP, External Weaving WIP, and Finished Warp.",
        )
        LifecycleStepRow(
            number = "3",
            title = "Production & Outsourcing",
            desc = "Issue silk yarn for warping, twisting, or dyeing. Reconcile returned warp weights and record allowed wastage.",
        )
        LifecycleStepRow(
            number = "4",
            title = "Orders & Dispatch",
            desc = "Book customer orders, allocate finished warps, generate invoices, and record customer deliveries.",
        )
        LifecycleStepRow(
            number = "5",
            title = "Double-Entry Financials",
            desc = "Audit-logged append-only ledger for all transactions, customer receivables, and weaver piece-rate wages.",
        )
    }
}

@Composable
private fun DailyOperationsTab() {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(top = 8.dp),
    ) {
        Text(
            "Recommended Daily Ledger Routine for Silk Shop Owners",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )

        LifecycleStepRow(
            number = "🌅",
            title = "Morning Check",
            desc = "Open the Dashboard to inspect Upcoming Supplier Dues and check current Cash & Bank balances.",
        )
        LifecycleStepRow(
            number = "🏭",
            title = "During Working Hours",
            desc = "Record incoming raw silk batches, issue material to handlooms, and record finished warp receipts.",
        )
        LifecycleStepRow(
            number = "🌆",
            title = "Evening Reconciliation",
            desc = "Record daily worker wage payouts, customer payments, petty cash expenses, and check ledger balance.",
        )
    }
}

@Composable
private fun TipsAndShortcutsTab() {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(top = 8.dp),
    ) {
        Text(
            "Productivity Tips & Power Features",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )

        LifecycleStepRow(
            number = "⌨️",
            title = "F5 — Instant Server Refresh",
            desc = "Press F5 anytime to re-sync all live balances, notifications, and transactions from the server.",
        )
        LifecycleStepRow(
            number = "🔍",
            title = "Ctrl + F — Instant Search",
            desc = "Quickly filter parties, invoices, and purchase records without needing to click the search bar.",
        )
        LifecycleStepRow(
            number = "🌓",
            title = "Dark / Light Mode & Tamil Localization",
            desc = "Switch between dark and light themes in the top bar. SmartSilk includes full Tamil language support.",
        )
        LifecycleStepRow(
            number = "💾",
            title = "Standalone Local Engine vs Cloud Server",
            desc = "You can run 100% locally on your computer with complete data privacy, or connect to a central shop server.",
        )
    }
}

@Composable
private fun LifecycleStepRow(number: String, title: String, desc: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                number,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(
                desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
