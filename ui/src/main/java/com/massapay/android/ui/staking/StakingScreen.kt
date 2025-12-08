package com.massapay.android.ui.staking

import androidx.compose.animation.AnimatedVisibility
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
    
    val backgroundColor = if (isDarkTheme) Color(0xFF0D0D15) else Color.White
    val cardColor = if (isDarkTheme) Color(0xFF1A1A2E) else Color(0xFFF8F9FA)
    val textColor = if (isDarkTheme) Color.White else Color.Black
    val secondaryTextColor = if (isDarkTheme) Color.White.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.6f)
    val accentColor = Color(0xFF6366F1)

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
                        accentColor = accentColor
                    )
                    
                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Buy Rolls Button
                        Button(
                            onClick = { showBuyDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentColor
                            ),
                            enabled = !uiState.isProcessing && viewModel.getMaxRollsToBuy() > 0
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
                            onClick = { showSellDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (isDarkTheme) Color.White else Color.Black
                            ),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        if (isDarkTheme) Color.White.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.3f),
                                        if (isDarkTheme) Color.White.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.3f)
                                    )
                                )
                            ),
                            enabled = !uiState.isProcessing && viewModel.getMaxRollsToSell() > 0
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
                    
                    // Info Cards
                    StakingInfoCard(
                        title = "What are Rolls?",
                        description = "Rolls are used for staking in Massa. 1 Roll = 100 MAS. You need to link your Rolls to a node to earn rewards.",
                        icon = Icons.Default.Help,
                        isDarkTheme = isDarkTheme,
                        cardColor = cardColor,
                        textColor = textColor,
                        secondaryTextColor = secondaryTextColor
                    )
                    
                    StakingInfoCard(
                        title = "How to Earn Rewards",
                        description = "To earn staking rewards, you need to run a Massa node and register your staking key. Rolls alone don't generate rewards.",
                        icon = Icons.Default.EmojiEvents,
                        isDarkTheme = isDarkTheme,
                        cardColor = cardColor,
                        textColor = textColor,
                        secondaryTextColor = secondaryTextColor
                    )
                    
                    StakingInfoCard(
                        title = "Selling Rolls",
                        description = "When you sell Rolls, your MAS will be available after ~3 cycles (about 3 hours). This is called 'deferred credits'.",
                        icon = Icons.Default.Schedule,
                        isDarkTheme = isDarkTheme,
                        cardColor = cardColor,
                        textColor = textColor,
                        secondaryTextColor = secondaryTextColor
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
    accentColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Roll icon
            Box(
                modifier = Modifier
                    .size(80.dp)
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
                    modifier = Modifier.size(40.dp),
                    tint = Color.White
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Rolls count
            Text(
                text = "${stakingInfo?.totalRolls ?: 0}",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            
            Text(
                text = "Rolls",
                style = MaterialTheme.typography.titleMedium,
                color = secondaryTextColor
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Value in MAS
            Text(
                text = "≈ ${String.format("%.0f", stakingInfo?.rollsValueInMas ?: 0.0)} MAS",
                style = MaterialTheme.typography.bodyLarge,
                color = accentColor,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Details row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                RollsDetailItem(
                    label = "Final",
                    value = "${stakingInfo?.finalRolls ?: 0}",
                    textColor = textColor,
                    secondaryTextColor = secondaryTextColor
                )
                
                RollsDetailItem(
                    label = "Pending",
                    value = "${stakingInfo?.candidateRolls ?: 0}",
                    textColor = textColor,
                    secondaryTextColor = secondaryTextColor
                )
                
                RollsDetailItem(
                    label = "Available MAS",
                    value = String.format("%.2f", stakingInfo?.balance?.toDoubleOrNull() ?: 0.0),
                    textColor = textColor,
                    secondaryTextColor = secondaryTextColor
                )
            }
            
            // Deferred credits if any
            val deferredCredits = stakingInfo?.deferredCredits?.toDoubleOrNull() ?: 0.0
            if (deferredCredits > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Pending: ${String.format("%.2f", deferredCredits)} MAS",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFF9800)
                )
            }
        }
    }
}

@Composable
private fun RollsDetailItem(
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
    secondaryTextColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = Color(0xFF6366F1)
            )
            Spacer(modifier = Modifier.width(12.dp))
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
    
    val dialogColor = if (isDarkTheme) Color(0xFF1A1A2E) else Color.White
    val textColor = if (isDarkTheme) Color.White else Color.Black
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogColor,
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
    val dialogColor = if (isDarkTheme) Color(0xFF1A1A2E) else Color.White
    val textColor = if (isDarkTheme) Color.White else Color.Black
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogColor,
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
