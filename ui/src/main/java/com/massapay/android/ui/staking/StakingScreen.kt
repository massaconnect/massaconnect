package com.massapay.android.ui.staking

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.massapay.android.core.model.StakingInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StakingScreen(
    onClose: () -> Unit,
    isDarkTheme: Boolean,
    viewModel: StakingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showBuyDialog by remember { mutableStateOf(false) }
    var showSellDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    
    // Use MaterialTheme colors for consistent theming
    val backgroundColor = MaterialTheme.colorScheme.background
    val cardColor = MaterialTheme.colorScheme.surfaceVariant
    val textColor = MaterialTheme.colorScheme.onBackground
    val secondaryTextColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
    val accentColor = Color(0xFF6366F1)
    
    // Icon container colors (black container with white icon in light mode)
    val iconContainerColor = if (isDarkTheme) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Black
    val iconTintColor = Color.White

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Staking",
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.Default.ArrowBack, 
                            contentDescription = "Back",
                            tint = textColor
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showInfoDialog = true }) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "Info",
                            tint = textColor
                        )
                    }
                    IconButton(onClick = { viewModel.loadStakingInfo() }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = textColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundColor
                )
            )
        },
        containerColor = backgroundColor
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.isLoading && uiState.stakingInfo == null) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = accentColor
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Rolls Balance Card
                    RollsBalanceCard(
                        stakingInfo = uiState.stakingInfo,
                        isDarkTheme = isDarkTheme,
                        cardColor = cardColor,
                        textColor = textColor,
                        secondaryTextColor = secondaryTextColor,
                        accentColor = accentColor,
                        onBuyClick = { showBuyDialog = true },
                        onSellClick = { showSellDialog = true },
                        canBuy = !uiState.isProcessing && viewModel.getMaxRollsToBuy() > 0,
                        canSell = !uiState.isProcessing && viewModel.getMaxRollsToSell() > 0
                    )
                    
                    // Info Cards
                    StakingInfoCard(
                        title = "What are Rolls?",
                        description = "Rolls are used for staking in Massa. 1 Roll = 100 MAS. You need to link your Rolls to a node to earn rewards.",
                        icon = Icons.Default.Help,
                        isDarkTheme = isDarkTheme,
                        cardColor = cardColor,
                        textColor = textColor,
                        secondaryTextColor = secondaryTextColor,
                        iconContainerColor = iconContainerColor,
                        iconTintColor = iconTintColor
                    )
                    
                    StakingInfoCard(
                        title = "How to Earn Rewards",
                        description = "To earn staking rewards, you need to run a Massa node and register your staking key. Rolls alone don't generate rewards.",
                        icon = Icons.Default.EmojiEvents,
                        isDarkTheme = isDarkTheme,
                        cardColor = cardColor,
                        textColor = textColor,
                        secondaryTextColor = secondaryTextColor,
                        iconContainerColor = iconContainerColor,
                        iconTintColor = iconTintColor
                    )
                    
                    StakingInfoCard(
                        title = "Selling Rolls",
                        description = "When you sell Rolls, your MAS will be available after ~3 cycles (about 3 hours). This is called 'deferred credits'.",
                        icon = Icons.Default.Schedule,
                        isDarkTheme = isDarkTheme,
                        cardColor = cardColor,
                        textColor = textColor,
                        secondaryTextColor = secondaryTextColor,
                        iconContainerColor = iconContainerColor,
                        iconTintColor = iconTintColor
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            
            // Processing overlay
            if (uiState.isProcessing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = cardColor)
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = accentColor)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Processing...",
                                color = textColor,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
        
        // Success message
        uiState.successMessage?.let { message ->
            LaunchedEffect(message) {
                kotlinx.coroutines.delay(3000)
                viewModel.clearMessages()
            }
            
            Snackbar(
                modifier = Modifier
                    .padding(16.dp),
                containerColor = Color(0xFF4CAF50)
            ) {
                Text(message, color = Color.White)
            }
        }
        
        // Error message
        uiState.error?.let { error ->
            LaunchedEffect(error) {
                kotlinx.coroutines.delay(3000)
                viewModel.clearMessages()
            }
            
            Snackbar(
                modifier = Modifier
                    .padding(16.dp),
                containerColor = Color(0xFFF44336)
            ) {
                Text(error, color = Color.White)
            }
        }
        
        // Buy Rolls Dialog
        if (showBuyDialog) {
            RollsDialog(
                title = "Buy Rolls",
                description = "1 Roll = 100 MAS",
                maxRolls = viewModel.getMaxRollsToBuy(),
                confirmText = "Buy",
                isDarkTheme = isDarkTheme,
                onConfirm = { count ->
                    viewModel.buyRolls(count)
                    showBuyDialog = false
                },
                onDismiss = { showBuyDialog = false }
            )
        }
        
        // Sell Rolls Dialog
        if (showSellDialog) {
            RollsDialog(
                title = "Sell Rolls",
                description = "You'll receive 100 MAS per Roll (after ~3 cycles)",
                maxRolls = viewModel.getMaxRollsToSell(),
                confirmText = "Sell",
                isDarkTheme = isDarkTheme,
                onConfirm = { count ->
                    viewModel.sellRolls(count)
                    showSellDialog = false
                },
                onDismiss = { showSellDialog = false }
            )
        }
        
        // Info Dialog
        if (showInfoDialog) {
            StakingHelpDialog(
                isDarkTheme = isDarkTheme,
                onDismiss = { showInfoDialog = false }
            )
        }
    }
}

@Composable
private fun RollsBalanceCard(
    stakingInfo: StakingInfo?,
    isDarkTheme: Boolean,
    cardColor: Color,
    textColor: Color,
    secondaryTextColor: Color,
    accentColor: Color,
    onBuyClick: () -> Unit,
    onSellClick: () -> Unit,
    canBuy: Boolean,
    canSell: Boolean
) {
    // Animated roll count
    val targetRolls = stakingInfo?.totalRolls?.toFloat() ?: 0f
    val animatedRolls by animateFloatAsState(
        targetValue = targetRolls,
        animationSpec = tween(
            durationMillis = 1000,
            easing = FastOutSlowInEasing
        ),
        label = "rollsAnimation"
    )
    
    // Animated MAS value
    val targetMasValue = stakingInfo?.rollsValueInMas?.toFloat() ?: 0f
    val animatedMasValue by animateFloatAsState(
        targetValue = targetMasValue,
        animationSpec = tween(
            durationMillis = 1000,
            easing = FastOutSlowInEasing
        ),
        label = "masValueAnimation"
    )
    
    // Icon rotation animation
    val infiniteTransition = rememberInfiniteTransition(label = "iconPulse")
    val iconScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "iconScale"
    )
    val iconGlow by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "iconGlow"
    )
    
    // Entrance animation
    var visible by remember { mutableStateOf(false) }
    val cardScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.9f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "cardScale"
    )
    val cardAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(500),
        label = "cardAlpha"
    )
    
    LaunchedEffect(Unit) {
        visible = true
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
                alpha = cardAlpha
            },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isDarkTheme) 0.dp else 4.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header row with title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Your Staking",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor
                )
                
                // Status badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (targetRolls > 0) Color(0xFF4CAF50).copy(alpha = 0.15f) else secondaryTextColor.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = if (targetRolls > 0) "Active" else "Inactive",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (targetRolls > 0) Color(0xFF4CAF50) else secondaryTextColor
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Main balance section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left side - Icon and Rolls
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Animated Roll icon with glow
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(72.dp)
                    ) {
                        // Glow effect
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .graphicsLayer {
                                    scaleX = iconScale * 1.1f
                                    scaleY = iconScale * 1.1f
                                }
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            accentColor.copy(alpha = iconGlow),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                        
                        // Main icon
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .graphicsLayer {
                                    scaleX = iconScale
                                    scaleY = iconScale
                                }
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(accentColor, Color(0xFF8B5CF6))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Token,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = Color.White
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column {
                        // Animated Rolls count
                        Text(
                            text = "${animatedRolls.toInt()}",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        Text(
                            text = "Rolls",
                            style = MaterialTheme.typography.bodyMedium,
                            color = secondaryTextColor
                        )
                    }
                }
                
                // Right side - MAS Value
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${String.format("%.0f", animatedMasValue)}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                    Text(
                        text = "MAS",
                        style = MaterialTheme.typography.bodyMedium,
                        color = secondaryTextColor
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Details row in a subtle container
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = if (isDarkTheme) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.03f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StakingStatItem(
                        label = "Final",
                        value = "${stakingInfo?.finalRolls ?: 0}",
                        textColor = textColor,
                        secondaryTextColor = secondaryTextColor
                    )
                    
                    // Divider
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(40.dp)
                            .background(secondaryTextColor.copy(alpha = 0.2f))
                    )
                    
                    StakingStatItem(
                        label = "Pending",
                        value = "${stakingInfo?.candidateRolls ?: 0}",
                        textColor = textColor,
                        secondaryTextColor = secondaryTextColor
                    )
                    
                    // Divider
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(40.dp)
                            .background(secondaryTextColor.copy(alpha = 0.2f))
                    )
                    
                    StakingStatItem(
                        label = "Available",
                        value = String.format("%.1f", stakingInfo?.balance?.toDoubleOrNull() ?: 0.0),
                        textColor = textColor,
                        secondaryTextColor = secondaryTextColor
                    )
                }
            }
            
            // Deferred credits if any
            val deferredCredits = stakingInfo?.deferredCredits?.toDoubleOrNull() ?: 0.0
            if (deferredCredits > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFF9800).copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFFFF9800)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Pending: ${String.format("%.2f", deferredCredits)} MAS",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFFF9800)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Action Buttons - Modern style
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Buy Rolls Button
                Button(
                    onClick = onBuyClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDarkTheme) Color.White else Color.Black,
                        contentColor = if (isDarkTheme) Color.Black else Color.White,
                        disabledContainerColor = if (isDarkTheme) Color.White.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.3f),
                        disabledContentColor = if (isDarkTheme) Color.Black.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.5f)
                    ),
                    enabled = canBuy
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Buy Rolls", fontWeight = FontWeight.SemiBold)
                }
                
                // Sell Rolls Button
                OutlinedButton(
                    onClick = onSellClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = textColor,
                        disabledContentColor = textColor.copy(alpha = 0.4f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.5.dp,
                        if (canSell) textColor.copy(alpha = 0.3f) else textColor.copy(alpha = 0.1f)
                    ),
                    enabled = canSell
                ) {
                    Icon(
                        Icons.Default.Remove,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sell Rolls", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun StakingStatItem(
    label: String,
    value: String,
    textColor: Color,
    secondaryTextColor: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = textColor
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = secondaryTextColor
        )
    }
}

@Composable
private fun StakingInfoCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isDarkTheme: Boolean,
    cardColor: Color,
    textColor: Color,
    secondaryTextColor: Color,
    iconContainerColor: Color,
    iconTintColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isDarkTheme) 0.dp else 4.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Icon with styled container
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(12.dp),
                color = iconContainerColor
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = iconTintColor
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = secondaryTextColor
                )
            }
        }
    }
}

@Composable
private fun RollsDialog(
    title: String,
    description: String,
    maxRolls: Int,
    confirmText: String,
    isDarkTheme: Boolean,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var rollCount by remember { mutableStateOf(1) }
    
    val dialogColor = if (isDarkTheme) Color(0xFF121212) else Color.White
    val textColor = if (isDarkTheme) Color.White else Color.Black
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogColor,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(title, fontWeight = FontWeight.Bold, color = textColor)
        },
        text = {
            Column {
                Text(
                    description,
                    color = textColor.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Roll count selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { if (rollCount > 1) rollCount-- },
                        enabled = rollCount > 1
                    ) {
                        Icon(
                            Icons.Default.Remove,
                            contentDescription = "Decrease",
                            tint = if (rollCount > 1) textColor else textColor.copy(alpha = 0.3f)
                        )
                    }
                    
                    Text(
                        text = "$rollCount",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    
                    IconButton(
                        onClick = { if (rollCount < maxRolls) rollCount++ },
                        enabled = rollCount < maxRolls
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Increase",
                            tint = if (rollCount < maxRolls) textColor else textColor.copy(alpha = 0.3f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Max: $maxRolls rolls",
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Cost/receive info
                val amount = rollCount * 100.0
                Text(
                    text = if (confirmText == "Buy") "Cost: $amount MAS + fee" else "You'll receive: $amount MAS",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF6366F1),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(rollCount) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDarkTheme) Color.White else Color.Black,
                    contentColor = if (isDarkTheme) Color.Black else Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(confirmText, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = textColor.copy(alpha = 0.7f))
            }
        }
    )
}

@Composable
private fun StakingHelpDialog(
    isDarkTheme: Boolean,
    onDismiss: () -> Unit
) {
    val dialogColor = if (isDarkTheme) Color(0xFF121212) else Color.White
    val textColor = if (isDarkTheme) Color.White else Color.Black
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogColor,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text("About Staking", fontWeight = FontWeight.Bold, color = textColor)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Massa uses a Proof-of-Stake consensus mechanism with 'Rolls'.",
                    color = textColor,
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Text(
                    "• 1 Roll = 100 MAS",
                    color = textColor,
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Text(
                    "• Rolls are used to participate in block production",
                    color = textColor,
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Text(
                    "• To earn rewards, you need to run a node and register your staking key",
                    color = textColor,
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Text(
                    "• Selling rolls has a delay of ~3 cycles before MAS is available",
                    color = textColor,
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    "For more information, visit docs.massa.net",
                    color = Color(0xFF6366F1),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got it", color = Color(0xFF6366F1), fontWeight = FontWeight.SemiBold)
            }
        }
    )
}
