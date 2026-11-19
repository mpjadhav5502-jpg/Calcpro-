package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CurrencyExchange
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LocalOffer
import androidx.compose.material.icons.rounded.Percent
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.ads.AdManager
import com.example.ui.components.AdBannerSlot
import com.example.ui.navigation.CalcRoute

private data class DashboardToolItem(
    val route: CalcRoute,
    val titleRes: Int,
    val descRes: Int,
    val icon: ImageVector,
    val tag: String,
    val isHighlighted: Boolean = false
)

@Composable
fun HomeDashboardScreen(
    isPremium: Boolean,
    adManager: AdManager,
    onNavigate: (CalcRoute) -> Unit
) {
    val tools = listOf(
        DashboardToolItem(
            route = CalcRoute.StandardCalculator,
            titleRes = R.string.tool_standard,
            descRes = R.string.tool_standard_desc,
            icon = Icons.Rounded.Calculate,
            tag = "card_standard",
            isHighlighted = true
        ),
        DashboardToolItem(
            route = CalcRoute.ScientificCalculator,
            titleRes = R.string.tool_scientific,
            descRes = R.string.tool_scientific_desc,
            icon = Icons.Rounded.Science,
            tag = "card_scientific",
            isHighlighted = true
        ),
        DashboardToolItem(
            route = CalcRoute.PercentageCalculator,
            titleRes = R.string.tool_percentage,
            descRes = R.string.tool_percentage_desc,
            icon = Icons.Rounded.Percent,
            tag = "card_percentage"
        ),
        DashboardToolItem(
            route = CalcRoute.GstCalculator,
            titleRes = R.string.tool_gst,
            descRes = R.string.tool_gst_desc,
            icon = Icons.Rounded.ReceiptLong,
            tag = "card_gst"
        ),
        DashboardToolItem(
            route = CalcRoute.DiscountCalculator,
            titleRes = R.string.tool_discount,
            descRes = R.string.tool_discount_desc,
            icon = Icons.Rounded.LocalOffer,
            tag = "card_discount"
        ),
        DashboardToolItem(
            route = CalcRoute.UnitConverter,
            titleRes = R.string.tool_unit,
            descRes = R.string.tool_unit_desc,
            icon = Icons.Rounded.Straighten,
            tag = "card_unit"
        ),
        DashboardToolItem(
            route = CalcRoute.CurrencyConverter,
            titleRes = R.string.tool_currency,
            descRes = R.string.tool_currency_desc,
            icon = Icons.Rounded.CurrencyExchange,
            tag = "card_currency"
        ),
        DashboardToolItem(
            route = CalcRoute.History,
            titleRes = R.string.tool_history,
            descRes = R.string.tool_history_desc,
            icon = Icons.Rounded.History,
            tag = "card_history"
        ),
        DashboardToolItem(
            route = CalcRoute.Settings,
            titleRes = R.string.tool_settings,
            descRes = R.string.tool_settings_desc,
            icon = Icons.Rounded.Settings,
            tag = "card_settings"
        ),
        DashboardToolItem(
            route = CalcRoute.Premium,
            titleRes = R.string.tool_premium,
            descRes = R.string.tool_premium_desc,
            icon = Icons.Rounded.AutoAwesome,
            tag = "card_premium"
        )
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            if (!isPremium) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    AdBannerSlot(adManager = adManager, isPremium = isPremium)
                }
            }
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val columns = when {
                maxWidth >= 840.dp -> 4
                maxWidth >= 600.dp -> 3
                else -> 2
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item(span = { GridItemSpan(columns) }) {
                    HeroDashboardHeader(
                        isPremium = isPremium,
                        onQuickOpenStandard = { onNavigate(CalcRoute.StandardCalculator) },
                        onOpenPremium = { onNavigate(CalcRoute.Premium) }
                    )
                }

                items(tools, key = { it.tag }) { tool ->
                    DashboardToolCard(
                        item = tool,
                        isPremiumActive = isPremium && tool.route == CalcRoute.Premium,
                        onClick = { onNavigate(tool.route) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroDashboardHeader(
    isPremium: Boolean,
    onQuickOpenStandard: () -> Unit,
    onOpenPremium: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onQuickOpenStandard)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(144.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_hero_banner),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = 0.42f,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
                            )
                        )
                    )
            )
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.img_calcpro_logo),
                            contentDescription = stringResource(R.string.app_name),
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.home_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        color = if (isPremium) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f)
                        else MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.clickable(onClick = onOpenPremium)
                    ) {
                        Text(
                            text = if (isPremium) stringResource(R.string.premium_active)
                            else stringResource(R.string.tool_standard) + " →",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isPremium) MaterialTheme.colorScheme.tertiary
                            else MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardToolCard(
    item: DashboardToolItem,
    isPremiumActive: Boolean,
    onClick: () -> Unit
) {
    val containerColor = when {
        item.isHighlighted -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        isPremiumActive -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f)
        else -> MaterialTheme.colorScheme.surface
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(
            width = 1.dp,
            color = if (item.isHighlighted) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(132.dp)
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .testTag(item.tag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (item.isHighlighted) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = stringResource(item.titleRes),
                        tint = if (item.isHighlighted) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Column {
                Text(
                    text = stringResource(item.titleRes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(item.descRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
