package com.smartledger.app.feature.more.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smartledger.app.core.ui.theme.GoldPrimary
import com.smartledger.app.core.ui.theme.SilkDarkBackground
import com.smartledger.app.core.ui.theme.SilkDarkCard

/**
 * "More" tab — Shows core operational and manufacturing features:
 * Procurement, Inventory, Production, Outsourcing, Workforce, Reports.
 */
@Composable
fun MoreScreen(
    onNavigateToProcurement: () -> Unit = {},
    onNavigateToInventory: () -> Unit,
    onNavigateToProduction: () -> Unit,
    onNavigateToOutsourcing: () -> Unit,
    onNavigateToWorkforce: () -> Unit,
    onNavigateToReports: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SilkDarkBackground)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Text(
            text = "Operations & Modules",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )
        Text(
            text = "Silk manufacturing, procurement, inventory & workforce management",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF94A3B8)
        )

        Spacer(modifier = Modifier.height(20.dp))

        MoreNavigationCard(
            title = "Silk Procurement & Purchases",
            subtitle = "Track raw silk cocoons, purchase orders & material delivery receipts",
            icon = Icons.Default.ShoppingBag,
            badge = "ACTIVE",
            badgeColor = GoldPrimary,
            onClick = onNavigateToProcurement
        )

        Spacer(modifier = Modifier.height(12.dp))

        MoreNavigationCard(
            title = "Inventory & Stock",
            subtitle = "Raw materials, in-process goods and stock movement ledger",
            icon = Icons.Default.Inventory,
            badge = "ACTIVE",
            badgeColor = GoldPrimary,
            onClick = onNavigateToInventory
        )

        Spacer(modifier = Modifier.height(12.dp))

        MoreNavigationCard(
            title = "Production & Batch Tracking",
            subtitle = "Warping, twisting, sizing and weaving work orders",
            icon = Icons.Default.Build,
            badge = "ACTIVE",
            badgeColor = GoldPrimary,
            onClick = onNavigateToProduction
        )

        Spacer(modifier = Modifier.height(12.dp))

        MoreNavigationCard(
            title = "Outsourced Job Work",
            subtitle = "External manufacturer yarn dispatch and fabric return tracking",
            icon = Icons.Default.Handshake,
            badge = "ACTIVE",
            badgeColor = GoldPrimary,
            onClick = onNavigateToOutsourcing
        )

        Spacer(modifier = Modifier.height(12.dp))

        MoreNavigationCard(
            title = "Workforce & Labor",
            subtitle = "Attendance logs, piece-rate wages and contractor compensation",
            icon = Icons.Default.Badge,
            badge = "ACTIVE",
            badgeColor = GoldPrimary,
            onClick = onNavigateToWorkforce
        )

        Spacer(modifier = Modifier.height(12.dp))

        MoreNavigationCard(
            title = "Business Reports & Analytics",
            subtitle = "Outstanding receivables, supplier payables, and profitability",
            icon = Icons.Default.Assessment,
            badge = "ACTIVE",
            badgeColor = GoldPrimary,
            onClick = onNavigateToReports
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun MoreNavigationCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    badge: String,
    badgeColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SilkDarkCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = badgeColor.copy(alpha = 0.15f),
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = badgeColor,
                    modifier = Modifier
                        .padding(10.dp)
                        .size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )

                    Surface(
                        color = badgeColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = badge,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = badgeColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = Color(0xFF64748B),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
