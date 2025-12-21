package com.massapay.android.ui.portfolio

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioScreen(
    onClose: () -> Unit,
    viewModel: PortfolioViewModel = hiltViewModel()
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
    
    // Entrance animation
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    
    val contentAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(400),
        label = "contentAlpha"
    )

    // Shimmer animation for loading
    val shimmerTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerAlpha by shimmerTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerAlpha"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "My Portfolio",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Refresh button with styled container
                    Surface(
                        onClick = { viewModel.refresh() },
                        shape = RoundedCornerShape(12.dp),
                        color = iconContainerColor,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = iconTintColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .graphicsLayer { alpha = contentAlpha },
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Total Value Hero Card with gradient
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF1a1a2e),
                                        Color(0xFF16213e)
                                    )
                                ),
                                shape = RoundedCornerShape(24.dp)
                            )
                            .padding(24.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Icon container - white icon on black bg (or inverse for light theme)
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .background(
                                        color = Color.White,
                                        shape = RoundedCornerShape(20.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.AccountBalanceWallet,
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp),
                                    tint = Color.Black
                                )
                            }
                            
                            Text(
                                "Total Portfolio Value",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            
                            if (uiState.isLoading) {
                                Box(
                                    modifier = Modifier
                                        .width(180.dp)
                                        .height(48.dp)
                                        .background(
                                            Color.White.copy(alpha = shimmerAlpha * 0.2f),
                                            RoundedCornerShape(12.dp)
                                        )
                                )
                            } else {
                                Text(
                                    "$${String.format("%,.2f", uiState.totalUsdValue.toDouble())}",
                                    style = MaterialTheme.typography.displaySmall.copy(
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = (-1).sp
                                    ),
                                    color = Color.White
                                )
                            }
                            
                            // Assets count badge
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color.White.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    "${uiState.tokens.size} Assets",
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // Assets Section Header
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = iconContainerColor
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                Icons.Outlined.Token,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                tint = iconTintColor
                            )
                        }
                    }
                    Text(
                        "Your Assets",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = textPrimary
                    )
                }
            }

            // Loading shimmer
            if (uiState.isLoading) {
                items(5) {
                    TokenShimmerItem(
                        shimmerAlpha = shimmerAlpha,
                        cardColor = cardColor,
                        shimmerColor = textSecondary,
                        isDarkTheme = isDarkTheme
                    )
                }
            }
            // Error state
            else if (uiState.error != null) {
                item {
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
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Surface(
                                modifier = Modifier.size(64.dp),
                                shape = CircleShape,
                                color = Color(0xFFFF9800).copy(alpha = 0.15f)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = Color(0xFFFF9800),
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                            
                            Text(
                                "Unable to Load",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = textPrimary
                            )
                            
                            Text(
                                uiState.error ?: "Unknown error",
                                color = textSecondary,
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            
                            Button(
                                onClick = { viewModel.refresh() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = buttonContainerColor,
                                    contentColor = buttonContentColor
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Retry", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            // Empty state
            else if (uiState.tokens.isEmpty()) {
                item {
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
                                .padding(40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Surface(
                                modifier = Modifier.size(80.dp),
                                shape = CircleShape,
                                color = iconContainerColor
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Icon(
                                        Icons.Outlined.AccountBalanceWallet,
                                        contentDescription = null,
                                        tint = iconTintColor,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                            }
                            
                            Text(
                                "No Tokens Yet",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = textPrimary
                            )
                            
                            Text(
                                "Your portfolio is empty.\nStart by receiving some tokens.",
                                color = textSecondary,
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
            // Token list
            else {
                items(uiState.tokens, key = { it.symbol }) { token ->
                    TokenItem(
                        token = token,
                        cardColor = cardColor,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        isDarkTheme = isDarkTheme,
                        iconContainerColor = iconContainerColor,
                        iconTintColor = iconTintColor
                    )
                }
            }
            
            // Bottom spacer
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun TokenItem(
    token: PortfolioToken,
    cardColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    isDarkTheme: Boolean,
    iconContainerColor: Color,
    iconTintColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isDarkTheme) 0.dp else 4.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Token icon with styled container
            Surface(
                modifier = Modifier.size(52.dp),
                shape = RoundedCornerShape(14.dp),
                color = token.color.copy(alpha = 0.15f)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        token.symbol.first().toString(),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = token.color
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Token info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    token.symbol,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = textPrimary
                )
                Text(
                    token.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = textSecondary
                )
            }
            
            // Balance and value
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    token.balanceFormatted,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = textPrimary
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "$${String.format("%.2f", token.usdValue.toDouble())}",
                        style = MaterialTheme.typography.bodySmall,
                        color = textSecondary
                    )
                    // Percentage badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = token.color.copy(alpha = 0.15f)
                    ) {
                        Text(
                            "${String.format("%.1f", token.percentage)}%",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = token.color,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TokenShimmerItem(
    shimmerAlpha: Float,
    cardColor: Color,
    shimmerColor: Color,
    isDarkTheme: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isDarkTheme) 0.dp else 4.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon shimmer
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(shimmerColor.copy(alpha = shimmerAlpha * 0.3f))
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Text shimmer
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .width(70.dp)
                        .height(20.dp)
                        .background(
                            shimmerColor.copy(alpha = shimmerAlpha * 0.3f),
                            RoundedCornerShape(6.dp)
                        )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .height(14.dp)
                        .background(
                            shimmerColor.copy(alpha = shimmerAlpha * 0.2f),
                            RoundedCornerShape(4.dp)
                        )
                )
            }
            
            // Value shimmer
            Column(horizontalAlignment = Alignment.End) {
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(20.dp)
                        .background(
                            shimmerColor.copy(alpha = shimmerAlpha * 0.3f),
                            RoundedCornerShape(6.dp)
                        )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(14.dp)
                        .background(
                            shimmerColor.copy(alpha = shimmerAlpha * 0.2f),
                            RoundedCornerShape(4.dp)
                        )
                )
            }
        }
    }
}
