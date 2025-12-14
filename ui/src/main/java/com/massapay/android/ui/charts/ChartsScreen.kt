package com.massapay.android.ui.charts

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.massapay.android.ui.dashboard.DashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartsScreen(
    onBackClick: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Theme colors from MaterialTheme
    val backgroundColor = MaterialTheme.colorScheme.background
    val cardColor = MaterialTheme.colorScheme.surfaceVariant
    val textPrimary = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
    
    // Theme detection based on background luminance
    val isDarkTheme = remember(backgroundColor) {
        (backgroundColor.red * 0.299f + backgroundColor.green * 0.587f + backgroundColor.blue * 0.114f) < 0.5f
    }
    
    // Consistent styling colors
    val iconContainerColor = if (isDarkTheme) Color.White.copy(alpha = 0.1f) else Color.Black
    val iconTintColor = Color.White
    val buttonContainerColor = if (isDarkTheme) Color.White else Color.Black
    val buttonContentColor = if (isDarkTheme) Color.Black else Color.White
    
    // Theme-aware colors for positive/negative values
    val positiveColor = Color(0xFF4CAF50)
    val negativeColor = Color(0xFFFF5252)
    
    // Entrance animation
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    
    val contentAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(400),
        label = "contentAlpha"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Massa Statistics",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundColor,
                    titleContentColor = textPrimary,
                    navigationIconContentColor = textPrimary
                )
            )
        },
        containerColor = backgroundColor
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .graphicsLayer { alpha = contentAlpha }
        ) {
            uiState.massaStats?.let { stats ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Price Header Card - Hero style
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = cardColor),
                        shape = RoundedCornerShape(24.dp),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = if (isDarkTheme) 0.dp else 4.dp
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Icon container
                            Surface(
                                modifier = Modifier.size(64.dp),
                                shape = CircleShape,
                                color = iconContainerColor
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Icon(
                                        Icons.Filled.CurrencyExchange,
                                        contentDescription = null,
                                        modifier = Modifier.size(32.dp),
                                        tint = iconTintColor
                                    )
                                }
                            }
                            
                            Text(
                                "Current Price",
                                style = MaterialTheme.typography.bodyMedium,
                                color = textSecondary
                            )
                            
                            Text(
                                "$${String.format("%.6f", stats.price)}",
                                style = MaterialTheme.typography.displaySmall.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = (-1).sp
                                ),
                                color = textPrimary
                            )
                            
                            // Rank badge
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = buttonContainerColor
                            ) {
                                Text(
                                    "Rank #${stats.rank}",
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = buttonContentColor
                                )
                            }
                        }
                    }

                    // 24h Change Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = cardColor),
                        shape = RoundedCornerShape(24.dp),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = if (isDarkTheme) 0.dp else 4.dp
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Icon container with color based on change
                            Surface(
                                modifier = Modifier.size(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                color = if (stats.percentChange24h >= 0) 
                                    positiveColor.copy(alpha = 0.15f) 
                                else 
                                    negativeColor.copy(alpha = 0.15f)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Icon(
                                        if (stats.percentChange24h >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                        contentDescription = null,
                                        modifier = Modifier.size(28.dp),
                                        tint = if (stats.percentChange24h >= 0) positiveColor else negativeColor
                                    )
                                }
                            }
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "24 Hour Change",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = textSecondary
                                )
                                Text(
                                    "${if (stats.percentChange24h >= 0) "+" else ""}${String.format("%.2f", stats.percentChange24h)}%",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = if (stats.percentChange24h >= 0) positiveColor else negativeColor
                                )
                            }
                        }
                    }

                    // Period Changes Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = cardColor),
                        shape = RoundedCornerShape(24.dp),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = if (isDarkTheme) 0.dp else 4.dp
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    modifier = Modifier.size(44.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    color = iconContainerColor
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        Icon(
                                            Icons.Outlined.Timeline,
                                            contentDescription = null,
                                            modifier = Modifier.size(22.dp),
                                            tint = iconTintColor
                                        )
                                    }
                                }
                                Text(
                                    "Price Changes",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = textPrimary
                                )
                            }
                            
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = if (isDarkTheme) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.03f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    PeriodChangeRow("7 Days", stats.percentChange7d, textSecondary, positiveColor, negativeColor)
                                    Divider(color = textSecondary.copy(alpha = 0.1f))
                                    PeriodChangeRow("30 Days", stats.percentChange30d, textSecondary, positiveColor, negativeColor)
                                }
                            }
                        }
                    }

                    // Market Stats Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = cardColor),
                        shape = RoundedCornerShape(24.dp),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = if (isDarkTheme) 0.dp else 4.dp
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    modifier = Modifier.size(44.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    color = iconContainerColor
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        Icon(
                                            Icons.Outlined.BarChart,
                                            contentDescription = null,
                                            modifier = Modifier.size(22.dp),
                                            tint = iconTintColor
                                        )
                                    }
                                }
                                Text(
                                    "Market Statistics",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = textPrimary
                                )
                            }
                            
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = if (isDarkTheme) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.03f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    StatRow(
                                        icon = Icons.Outlined.AccountBalance,
                                        label = "Market Cap",
                                        value = formatCurrency(stats.marketCap),
                                        textPrimary = textPrimary,
                                        textSecondary = textSecondary,
                                        iconContainerColor = iconContainerColor,
                                        iconTintColor = iconTintColor
                                    )
                                    Divider(color = textSecondary.copy(alpha = 0.1f))
                                    StatRow(
                                        icon = Icons.Outlined.ShowChart,
                                        label = "24h Volume",
                                        value = formatCurrency(stats.volume24h),
                                        textPrimary = textPrimary,
                                        textSecondary = textSecondary,
                                        iconContainerColor = iconContainerColor,
                                        iconTintColor = iconTintColor
                                    )
                                    Divider(color = textSecondary.copy(alpha = 0.1f))
                                    StatRow(
                                        icon = Icons.Outlined.Token,
                                        label = "Total Supply",
                                        value = formatNumber(stats.totalSupply),
                                        textPrimary = textPrimary,
                                        textSecondary = textSecondary,
                                        iconContainerColor = iconContainerColor,
                                        iconTintColor = iconTintColor
                                    )
                                }
                            }
                        }
                    }

                    // ATH Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = cardColor),
                        shape = RoundedCornerShape(24.dp),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = if (isDarkTheme) 0.dp else 4.dp
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    modifier = Modifier.size(44.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    color = iconContainerColor
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        Icon(
                                            Icons.Outlined.EmojiEvents,
                                            contentDescription = null,
                                            modifier = Modifier.size(22.dp),
                                            tint = iconTintColor
                                        )
                                    }
                                }
                                Text(
                                    "All-Time High",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = textPrimary
                                )
                            }
                            
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = if (isDarkTheme) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.03f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            "ATH Price",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = textSecondary
                                        )
                                        Text(
                                            "$${String.format("%.6f", stats.athPrice)}",
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = textPrimary
                                        )
                                    }
                                    
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = negativeColor.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            "${String.format("%.1f", stats.percentFromAth)}%",
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = negativeColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                }
            } ?: run {
                // Loading state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Loading animation
                        Surface(
                            modifier = Modifier.size(80.dp),
                            shape = CircleShape,
                            color = cardColor
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(40.dp),
                                    color = buttonContainerColor,
                                    strokeWidth = 3.dp
                                )
                            }
                        }
                        Text(
                            "Loading statistics...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = textSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PeriodChangeRow(
    period: String,
    change: Double,
    textSecondary: Color,
    positiveColor: Color,
    negativeColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            period,
            style = MaterialTheme.typography.bodyLarge,
            color = textSecondary
        )
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (change >= 0) positiveColor.copy(alpha = 0.15f) else negativeColor.copy(alpha = 0.15f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    if (change >= 0) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = if (change >= 0) positiveColor else negativeColor
                )
                Text(
                    "${if (change >= 0) "+" else ""}${String.format("%.2f", change)}%",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (change >= 0) positiveColor else negativeColor
                )
            }
        }
    }
}

@Composable
private fun StatRow(
    icon: ImageVector,
    label: String,
    value: String,
    textPrimary: Color,
    textSecondary: Color,
    iconContainerColor: Color,
    iconTintColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(36.dp),
                shape = RoundedCornerShape(10.dp),
                color = iconContainerColor
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = iconTintColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                color = textSecondary
            )
        }
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = textPrimary
        )
    }
}

private fun formatCurrency(value: Double): String {
    return when {
        value >= 1_000_000_000 -> "$${String.format("%.2f", value / 1_000_000_000)}B"
        value >= 1_000_000 -> "$${String.format("%.2f", value / 1_000_000)}M"
        value >= 1_000 -> "$${String.format("%.2f", value / 1_000)}K"
        else -> "$${String.format("%.2f", value)}"
    }
}

private fun formatNumber(value: Long): String {
    val doubleValue = value.toDouble()
    return when {
        doubleValue >= 1_000_000_000 -> "${String.format("%.2f", doubleValue / 1_000_000_000)}B"
        doubleValue >= 1_000_000 -> "${String.format("%.2f", doubleValue / 1_000_000)}M"
        doubleValue >= 1_000 -> "${String.format("%.2f", doubleValue / 1_000)}K"
        else -> String.format("%.0f", doubleValue)
    }
}
