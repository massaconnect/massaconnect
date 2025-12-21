package com.massapay.android.ui.accounts

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.massapay.android.core.model.Account
import com.massapay.android.core.model.AccountColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    onClose: () -> Unit,
    isDarkTheme: Boolean = false,
    viewModel: AccountsViewModel = hiltViewModel()
) {
    val accounts by viewModel.accounts.collectAsState()
    val activeAccount by viewModel.activeAccount.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    
    // Theme-aware colors
    val backgroundColor = MaterialTheme.colorScheme.background
    val cardBackground = MaterialTheme.colorScheme.surfaceVariant
    val textPrimary = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
    
    // Icon container colors
    val iconContainerColor = if (isDarkTheme) Color.White.copy(alpha = 0.1f) else Color.Black
    val iconTintColor = Color.White
    
    // Button colors
    val buttonBackground = if (isDarkTheme) Color.White else Color.Black
    val buttonText = if (isDarkTheme) Color.Black else Color.White
    
    // Accent colors
    val accentColor = if (isDarkTheme) Color(0xFF4DD0E1) else Color(0xFF0097A7)
    val successColor = Color(0xFF4CAF50)
    val warningColor = Color(0xFFFF9800)
    
    // Clipboard manager
    val clipboardManager = LocalClipboardManager.current
    var copiedAddress by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(copiedAddress) {
        if (copiedAddress != null) {
            kotlinx.coroutines.delay(2000)
            copiedAddress = null
        }
    }
    
    LaunchedEffect(uiState.accountSwitched) {
        if (uiState.accountSwitched) {
            viewModel.clearAccountSwitched()
            onClose()
        }
    }
    
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(message = error, duration = SnackbarDuration.Short)
            viewModel.clearError()
        }
    }
    
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { message ->
            snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
            viewModel.clearSuccessMessage()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Wallet Manager", 
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = textPrimary)
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(iconContainerColor)
                            .clickable { /* Refresh handled automatically */ },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = iconTintColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = backgroundColor)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = backgroundColor
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.isLoading && accounts.isEmpty()) {
                // Shimmer loading state
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item { ShimmerHeroCard(cardBackground) }
                    items(3) { ShimmerAccountCard(cardBackground) }
                }
            } else if (accounts.isEmpty()) {
                EmptyAccountsState(
                    onCreateAccount = { viewModel.showCreateDialog() },
                    modifier = Modifier.align(Alignment.Center),
                    buttonBackground = buttonBackground,
                    buttonText = buttonText,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    iconContainerColor = iconContainerColor,
                    iconTintColor = iconTintColor
                )
            } else {
                val totalBalance = viewModel.getTotalBalance()
                
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Hero Card with total balance and gradient
                    item {
                        HeroBalanceCard(
                            totalBalance = totalBalance,
                            accountCount = accounts.size,
                            isDarkTheme = isDarkTheme,
                            onAddAccount = { viewModel.showCreateDialog() }
                        )
                    }
                    
                    // Quick Stats Row
                    item {
                        QuickStatsRow(
                            accounts = accounts,
                            activeAccount = activeAccount,
                            cardBackground = cardBackground,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            iconContainerColor = iconContainerColor,
                            iconTintColor = iconTintColor,
                            accentColor = accentColor
                        )
                    }
                    
                    // Section Header
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Your Accounts",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                            Surface(
                                onClick = { viewModel.showCreateDialog() },
                                color = iconContainerColor,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = iconTintColor
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "Add New",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = iconTintColor
                                    )
                                }
                            }
                        }
                    }
                    
                    // Accounts List
                    items(
                        items = accounts.sortedBy { it.accountIndex },
                        key = { it.id }
                    ) { account ->
                        AccountCard(
                            account = account,
                            isActive = account.id == activeAccount?.id,
                            totalBalance = totalBalance,
                            onAccountClick = { 
                                if (!account.isActive) {
                                    viewModel.switchAccount(account.id)
                                }
                            },
                            onCopyAddress = {
                                clipboardManager.setText(AnnotatedString(account.address))
                                copiedAddress = account.address
                            },
                            isCopied = copiedAddress == account.address,
                            onEditClick = { viewModel.showEditDialog(account) },
                            onDeleteClick = { viewModel.showDeleteDialog(account) },
                            cardBackground = cardBackground,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            isDarkTheme = isDarkTheme,
                            iconContainerColor = iconContainerColor,
                            iconTintColor = iconTintColor,
                            accentColor = accentColor
                        )
                    }
                    
                    // Tips Section
                    item {
                        TipsCard(
                            cardBackground = cardBackground,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            iconContainerColor = iconContainerColor,
                            iconTintColor = iconTintColor
                        )
                    }
                    
                    // Bottom spacing
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }
    
    // Dialogs
    if (uiState.showCreateDialog) {
        CreateAccountDialog(
            onDismiss = { viewModel.hideCreateDialog() },
            onCreate = { name, color -> viewModel.createAccount(name, color) },
            isCreating = uiState.isCreating,
            buttonBackground = buttonBackground,
            buttonText = buttonText
        )
    }
    
    if (uiState.showEditDialog && uiState.selectedAccount != null) {
        EditAccountDialog(
            account = uiState.selectedAccount!!,
            onDismiss = { viewModel.hideEditDialog() },
            onUpdate = { name, color -> 
                viewModel.updateAccount(uiState.selectedAccount!!.id, name, color)
            },
            buttonBackground = buttonBackground,
            buttonText = buttonText
        )
    }
    
    if (uiState.showDeleteDialog && uiState.selectedAccount != null) {
        DeleteAccountDialog(
            account = uiState.selectedAccount!!,
            onDismiss = { viewModel.hideDeleteDialog() },
            onConfirm = { viewModel.deleteAccount(uiState.selectedAccount!!.id) }
        )
    }
}

@Composable
private fun HeroBalanceCard(
    totalBalance: Double,
    accountCount: Int,
    isDarkTheme: Boolean,
    onAddAccount: () -> Unit
) {
    val gradientColors = listOf(Color(0xFF000000), Color(0xFF1A1A2E), Color(0xFF16213E))
    
    // Animated rotation for the decorative element
    val infiniteTransition = rememberInfiniteTransition(label = "hero")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(gradientColors))
        ) {
            // Decorative circles
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .offset(x = (-30).dp, y = (-30).dp)
                    .rotate(rotation)
                    .background(
                        Color.White.copy(alpha = 0.05f),
                        CircleShape
                    )
            )
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 30.dp, y = 30.dp)
                    .background(
                        Color.White.copy(alpha = 0.08f),
                        CircleShape
                    )
            )
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Total Portfolio",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "${String.format("%.4f", totalBalance)} MAS",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 36.sp
                            ),
                            color = Color.White
                        )
                    }
                    
                    // Add Account FAB
                    FloatingActionButton(
                        onClick = onAddAccount,
                        containerColor = Color.White,
                        contentColor = Color.Black,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Account")
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Stats row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    HeroStatItem(
                        icon = Icons.Outlined.AccountBalanceWallet,
                        value = accountCount.toString(),
                        label = "Accounts"
                    )
                    HeroStatItem(
                        icon = Icons.Outlined.Security,
                        value = "Secured",
                        label = "Status"
                    )
                    HeroStatItem(
                        icon = Icons.Outlined.Verified,
                        value = "Active",
                        label = "Network"
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroStatItem(
    icon: ImageVector,
    value: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun QuickStatsRow(
    accounts: List<Account>,
    activeAccount: Account?,
    cardBackground: Color,
    textPrimary: Color,
    textSecondary: Color,
    iconContainerColor: Color,
    iconTintColor: Color,
    accentColor: Color
) {
    val highestBalance = accounts.maxByOrNull { it.balance.toDoubleOrNull() ?: 0.0 }
    val accountsWithBalance = accounts.count { (it.balance.toDoubleOrNull() ?: 0.0) > 0 }
    
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            QuickStatCard(
                icon = Icons.Default.Star,
                title = "Active Account",
                value = activeAccount?.name ?: "None",
                cardBackground = cardBackground,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                iconContainerColor = accentColor.copy(alpha = 0.15f),
                iconTint = accentColor
            )
        }
        item {
            QuickStatCard(
                icon = Icons.Default.TrendingUp,
                title = "Highest Balance",
                value = highestBalance?.name ?: "N/A",
                cardBackground = cardBackground,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                iconContainerColor = Color(0xFF4CAF50).copy(alpha = 0.15f),
                iconTint = Color(0xFF4CAF50)
            )
        }
        item {
            QuickStatCard(
                icon = Icons.Default.AccountBalance,
                title = "With Balance",
                value = "$accountsWithBalance / ${accounts.size}",
                cardBackground = cardBackground,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                iconContainerColor = Color(0xFFFF9800).copy(alpha = 0.15f),
                iconTint = Color(0xFFFF9800)
            )
        }
    }
}

@Composable
private fun QuickStatCard(
    icon: ImageVector,
    title: String,
    value: String,
    cardBackground: Color,
    textPrimary: Color,
    textSecondary: Color,
    iconContainerColor: Color,
    iconTint: Color
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconContainerColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.bodySmall, color = textSecondary)
                Text(
                    value,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountCard(
    account: Account,
    isActive: Boolean,
    totalBalance: Double,
    onAccountClick: () -> Unit,
    onCopyAddress: () -> Unit,
    isCopied: Boolean,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    cardBackground: Color,
    textPrimary: Color,
    textSecondary: Color,
    isDarkTheme: Boolean,
    iconContainerColor: Color,
    iconTintColor: Color,
    accentColor: Color
) {
    val accountBalance = account.balance.toDoubleOrNull() ?: 0.0
    val percentage = if (totalBalance > 0) (accountBalance / totalBalance * 100) else 0.0
    
    Card(
        onClick = onAccountClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isActive) 6.dp else 2.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Left accent bar for active account
            if (isActive) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(accentColor, accentColor.copy(alpha = 0.5f))
                            ),
                            shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp)
                        )
                )
            }
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Account Avatar - accent color for active, black for others
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isActive) accentColor else iconContainerColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = account.name.firstOrNull()?.uppercase() ?: "A",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = account.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isActive) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = accentColor.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(accentColor)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        "Active",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = accentColor,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "${String.format("%.4f", accountBalance)} MAS",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = if (isActive) accentColor else textPrimary
                    )
                }
                
                // Actions menu
                Column(horizontalAlignment = Alignment.End) {
                    IconButton(onClick = onEditClick, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp), tint = textSecondary)
                    }
                    if (account.accountIndex != 0 && !isActive) {
                        IconButton(onClick = onDeleteClick, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Portfolio percentage bar
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Portfolio Share",
                        style = MaterialTheme.typography.bodySmall,
                        color = textSecondary
                    )
                    Text(
                        "${String.format("%.1f", percentage)}%",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = (percentage / 100f).toFloat().coerceIn(0f, 1f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (isActive) accentColor else textSecondary.copy(alpha = 0.5f),
                    trackColor = textSecondary.copy(alpha = 0.1f)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Divider(color = textSecondary.copy(alpha = 0.1f), thickness = 1.dp)
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Address and copy
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Key,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = textSecondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${account.address.take(10)}...${account.address.takeLast(6)}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        ),
                        color = textSecondary
                    )
                }
                
                Surface(
                    onClick = onCopyAddress,
                    color = if (isCopied) Color(0xFF4CAF50).copy(alpha = 0.15f) else cardBackground,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                            contentDescription = if (isCopied) "Copied" else "Copy",
                            modifier = Modifier.size(14.dp),
                            tint = if (isCopied) Color(0xFF4CAF50) else textSecondary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isCopied) "Copied!" else "Copy",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = if (isCopied) Color(0xFF4CAF50) else textSecondary
                        )
                    }
                }
            }
            
            // Tap to switch hint
            if (!isActive) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.TouchApp,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = accentColor.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Tap to switch to this account",
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor.copy(alpha = 0.6f)
                    )
                }
            }
        }
        } // Close Row with accent bar
    }
}

@Composable
private fun TipsCard(
    cardBackground: Color,
    textPrimary: Color,
    textSecondary: Color,
    iconContainerColor: Color,
    iconTintColor: Color
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(iconContainerColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = iconTintColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Security Tips",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            TipItem(
                icon = Icons.Outlined.Shield,
                text = "Each account has its own unique seed phrase",
                textSecondary = textSecondary
            )
            Spacer(modifier = Modifier.height(12.dp))
            TipItem(
                icon = Icons.Outlined.Backup,
                text = "Always backup your recovery phrases securely",
                textSecondary = textSecondary
            )
            Spacer(modifier = Modifier.height(12.dp))
            TipItem(
                icon = Icons.Outlined.VisibilityOff,
                text = "Never share your private keys with anyone",
                textSecondary = textSecondary
            )
        }
    }
}

@Composable
private fun TipItem(
    icon: ImageVector,
    text: String,
    textSecondary: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = textSecondary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = textSecondary
        )
    }
}

@Composable
private fun ShimmerHeroCard(cardBackground: Color) {
    val shimmerColors = listOf(
        cardBackground.copy(alpha = 0.6f),
        cardBackground.copy(alpha = 0.2f),
        cardBackground.copy(alpha = 0.6f)
    )
    
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )
    
    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim, y = translateAnim)
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        shape = RoundedCornerShape(28.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(brush))
    }
}

@Composable
private fun ShimmerAccountCard(cardBackground: Color) {
    val shimmerColors = listOf(
        cardBackground.copy(alpha = 0.6f),
        cardBackground.copy(alpha = 0.2f),
        cardBackground.copy(alpha = 0.6f)
    )
    
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )
    
    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim, y = translateAnim)
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(brush))
    }
}

@Composable
private fun EmptyAccountsState(
    onCreateAccount: () -> Unit,
    modifier: Modifier = Modifier,
    buttonBackground: Color,
    buttonText: Color,
    textPrimary: Color,
    textSecondary: Color,
    iconContainerColor: Color,
    iconTintColor: Color
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(iconContainerColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.AccountBalanceWallet,
                contentDescription = null,
                modifier = Modifier.size(50.dp),
                tint = iconTintColor
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "No Accounts Yet",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = textPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Create your first account to start\nmanaging your Massa assets",
            style = MaterialTheme.typography.bodyMedium,
            color = textSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onCreateAccount,
            colors = ButtonDefaults.buttonColors(
                containerColor = buttonBackground,
                contentColor = buttonText
            ),
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create First Account", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}
