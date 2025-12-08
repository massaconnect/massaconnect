package com.massapay.android.ui.accounts

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
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
    viewModel: AccountsViewModel = hiltViewModel()
) {
    val accounts by viewModel.accounts.collectAsState()
    val activeAccount by viewModel.activeAccount.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    
    // Theme-aware colors matching app style
    val backgroundColor = MaterialTheme.colorScheme.background
    val cardBackground = MaterialTheme.colorScheme.surfaceVariant
    val textPrimary = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
    
    // Detect dark theme for button styling
    val isDarkTheme = backgroundColor == Color.Black || backgroundColor == Color(0xFF000000)
    
    // Button colors - black in light theme, white in dark theme
    val buttonBackground = if (isDarkTheme) Color.White else Color.Black
    val buttonText = if (isDarkTheme) Color.Black else Color.White
    
    // Clipboard manager
    val clipboardManager = LocalClipboardManager.current
    var copiedAddress by remember { mutableStateOf<String?>(null) }
    
    // Reset copied state after delay
    LaunchedEffect(copiedAddress) {
        if (copiedAddress != null) {
            kotlinx.coroutines.delay(2000)
            copiedAddress = null
        }
    }
    
    // Navigate back when account is switched successfully
    LaunchedEffect(uiState.accountSwitched) {
        if (uiState.accountSwitched) {
            viewModel.clearAccountSwitched()
            onClose()
        }
    }
    
    // Show snackbar for errors and success messages
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Short
            )
            viewModel.clearError()
        }
    }
    
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            viewModel.clearSuccessMessage()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(
                            "Accounts", 
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Text(
                            "${accounts.size} ${if (accounts.size == 1) "account" else "accounts"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = textSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.Default.ArrowBack, 
                            contentDescription = "Back",
                            tint = textPrimary
                        )
                    }
                },
                actions = {
                    // Add account button with proper theme styling
                    Button(
                        onClick = { viewModel.showCreateDialog() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = buttonBackground,
                            contentColor = buttonText
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.Add, 
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("New", fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundColor
                )
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
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = textPrimary
                )
            } else if (accounts.isEmpty()) {
                EmptyAccountsState(
                    onCreateAccount = { viewModel.showCreateDialog() },
                    modifier = Modifier.align(Alignment.Center),
                    buttonBackground = buttonBackground,
                    buttonText = buttonText,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Total Balance Card
                    item {
                        TotalBalanceCard(
                            totalBalance = viewModel.getTotalBalance(),
                            accountCount = accounts.size,
                            cardBackground = cardBackground,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary
                        )
                    }
                    
                    // Accounts List
                    items(
                        items = accounts.sortedBy { it.accountIndex },
                        key = { it.id }
                    ) { account ->
                        AccountCard(
                            account = account,
                            isActive = account.id == activeAccount?.id,
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
                            isSwitching = uiState.isSwitching,
                            cardBackground = cardBackground,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            isDarkTheme = isDarkTheme
                        )
                    }
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
private fun TotalBalanceCard(
    totalBalance: Double,
    accountCount: Int,
    cardBackground: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = cardBackground
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                "Total Balance",
                style = MaterialTheme.typography.bodyMedium,
                color = textSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "${String.format("%.4f", totalBalance)} MAS",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp
                ),
                color = textPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Across $accountCount ${if (accountCount == 1) "account" else "accounts"}",
                style = MaterialTheme.typography.bodySmall,
                color = textSecondary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountCard(
    account: Account,
    isActive: Boolean,
    onAccountClick: () -> Unit,
    onCopyAddress: () -> Unit,
    isCopied: Boolean,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    isSwitching: Boolean,
    cardBackground: Color,
    textPrimary: Color,
    textSecondary: Color,
    isDarkTheme: Boolean
) {
    val accountColor = remember(account.color) {
        Color(android.graphics.Color.parseColor(account.color.hex))
    }
    
    val accentColor = if (isDarkTheme) Color(0xFF4DD0E1) else Color(0xFF0097A7)
    
    Card(
        onClick = onAccountClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = cardBackground
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp,
            pressedElevation = 6.dp
        ),
        border = if (isActive) BorderStroke(2.dp, accentColor) else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Color indicator with initial - larger and more prominent
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(accountColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = account.name.firstOrNull()?.uppercase() ?: "A",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = accountColor
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Account info
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
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(accentColor)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "Active",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = accentColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    // Balance prominently displayed
                    Text(
                        text = "${String.format("%.4f", account.balance.toDoubleOrNull() ?: 0.0)} MAS",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 20.sp
                        ),
                        color = if (isActive) accentColor else textPrimary
                    )
                }
                
                // Edit/Delete buttons - only show on non-active or allow edit
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit",
                            modifier = Modifier.size(18.dp),
                            tint = textSecondary
                        )
                    }
                    if (account.accountIndex != 0 && !isActive) {
                        IconButton(
                            onClick = onDeleteClick,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Divider
            Divider(
                color = textSecondary.copy(alpha = 0.1f),
                thickness = 1.dp
            )
            
            Spacer(modifier = Modifier.height(10.dp))
            
            // Address row with copy button - flat design, no background
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${account.address.take(14)}...${account.address.takeLast(8)}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    ),
                    color = textSecondary
                )
                
                // Copy button - minimal style
                Row(
                    modifier = Modifier
                        .clickable(onClick = onCopyAddress)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                        contentDescription = if (isCopied) "Copied" else "Copy",
                        modifier = Modifier.size(16.dp),
                        tint = if (isCopied) Color(0xFF4CAF50) else accountColor
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isCopied) "✓" else "Copy",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = if (isCopied) Color(0xFF4CAF50) else accountColor
                    )
                }
            }
            
            // Tap to switch hint for non-active accounts
            if (!isActive) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.TouchApp,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = textSecondary.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Tap to switch",
                        style = MaterialTheme.typography.labelSmall,
                        color = textSecondary.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyAccountsState(
    onCreateAccount: () -> Unit,
    modifier: Modifier = Modifier,
    buttonBackground: Color,
    buttonText: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.AccountBalanceWallet,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = textSecondary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "No Accounts",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = textPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Create your first account to get started",
            style = MaterialTheme.typography.bodyMedium,
            color = textSecondary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onCreateAccount,
            colors = ButtonDefaults.buttonColors(
                containerColor = buttonBackground,
                contentColor = buttonText
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create Account", fontWeight = FontWeight.SemiBold)
        }
    }
}
