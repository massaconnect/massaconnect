package com.massapay.android.ui.swap

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import com.massapay.android.ui.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwapScreen(
    onClose: () -> Unit,
    viewModel: SwapViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Colors - using MaterialTheme for consistency
    val backgroundColor = MaterialTheme.colorScheme.background
    val cardBackground = MaterialTheme.colorScheme.surfaceVariant
    val accentColor = Color(0xFFFF9800) // Orange for swap
    val textPrimary = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
    
    // Theme detection based on background luminance (same as Staking)
    val isDarkTheme = remember(backgroundColor) {
        val bgColor = backgroundColor
        (bgColor.red * 0.299f + bgColor.green * 0.587f + bgColor.blue * 0.114f) < 0.5f
    }
    
    // Icon container colors (black with white icon in light mode)
    val iconContainerColor = if (isDarkTheme) Color.White.copy(alpha = 0.1f) else Color.Black
    val iconTintColor = Color.White
    
    // Button colors
    val buttonContainerColor = if (isDarkTheme) Color.White else Color.Black
    val buttonContentColor = if (isDarkTheme) Color.Black else Color.White
    
    // Token selection dialogs
    var showFromTokenDialog by remember { mutableStateOf(false) }
    var showToTokenDialog by remember { mutableStateOf(false) }
    
    // Swap animation
    var isSwapping by remember { mutableStateOf(false) }
    val rotationAngle by animateFloatAsState(
        targetValue = if (isSwapping) 180f else 0f,
        animationSpec = tween(300),
        finishedListener = { isSwapping = false },
        label = "swap rotation"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Swap",
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
                    // Settings/Slippage button with styled container
                    Surface(
                        onClick = { viewModel.toggleSettings() },
                        shape = RoundedCornerShape(12.dp),
                        color = iconContainerColor,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                Icons.Outlined.Tune,
                                contentDescription = "Settings",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Slippage Settings Card (collapsible)
            AnimatedVisibility(
                visible = uiState.showSettings,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBackground),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = if (isDarkTheme) 0.dp else 4.dp
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
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
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        Icons.Outlined.Tune,
                                        contentDescription = null,
                                        tint = iconTintColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    "Slippage Tolerance",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = textPrimary
                                )
                                Text(
                                    "Max price change allowed",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = textSecondary
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("0.5%", "1%", "3%", "5%").forEach { slippage ->
                                val isSelected = uiState.slippage == slippage.dropLast(1).toFloat()
                                Surface(
                                    onClick = { viewModel.setSlippage(slippage.dropLast(1).toFloat()) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) {
                                        buttonContainerColor
                                    } else {
                                        if (isDarkTheme) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.08f)
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        slippage,
                                        modifier = Modifier.padding(vertical = 12.dp),
                                        textAlign = TextAlign.Center,
                                        color = if (isSelected) {
                                            buttonContentColor
                                        } else textPrimary,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))

            // FROM Token Card
            SwapTokenCard(
                label = "From",
                token = uiState.fromToken,
                amount = uiState.fromAmount,
                balance = uiState.fromBalance,
                onAmountChange = { viewModel.updateFromAmount(it) },
                onTokenClick = { showFromTokenDialog = true },
                onMaxClick = { viewModel.setMaxAmount() },
                cardBackground = cardBackground,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                accentColor = accentColor,
                isEditable = true,
                isDarkTheme = isDarkTheme
            )

            // Swap Button (between cards)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-4).dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer ring
                Surface(
                    shape = CircleShape,
                    color = backgroundColor,
                    modifier = Modifier.size(60.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        // Inner button - uses consistent button colors
                        Surface(
                            onClick = {
                                isSwapping = true
                                viewModel.swapTokens()
                            },
                            shape = CircleShape,
                            color = buttonContainerColor,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    Icons.Default.SwapVert,
                                    contentDescription = "Swap tokens",
                                    tint = buttonContentColor,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .rotate(rotationAngle)
                                )
                            }
                        }
                    }
                }
            }

            // TO Token Card
            SwapTokenCard(
                label = "To",
                token = uiState.toToken,
                amount = uiState.toAmount,
                balance = uiState.toBalance,
                onAmountChange = { },
                onTokenClick = { showToTokenDialog = true },
                onMaxClick = { },
                cardBackground = cardBackground,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                accentColor = accentColor,
                isEditable = false,
                isCalculating = uiState.isCalculatingQuote,
                isDarkTheme = isDarkTheme
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Rate Info
            if (uiState.fromAmount.isNotEmpty() && uiState.toAmount.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = cardBackground
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = if (isDarkTheme) 0.dp else 2.dp
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Header
                        Text(
                            "Transaction Details",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = textPrimary
                        )
                        
                        Divider(color = textSecondary.copy(alpha = 0.1f))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Rate", color = textSecondary, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "1 ${uiState.fromToken.symbol} ≈ ${uiState.exchangeRate} ${uiState.toToken.symbol}",
                                color = textPrimary,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Price Impact", color = textSecondary, style = MaterialTheme.typography.bodyMedium)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (uiState.priceImpact < 1f) Color(0xFF4CAF50).copy(alpha = 0.1f) else Color(0xFFFF5722).copy(alpha = 0.1f)
                            ) {
                                Text(
                                    "${uiState.priceImpact}%",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    color = if (uiState.priceImpact < 1f) Color(0xFF4CAF50) else Color(0xFFFF5722),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Network Fee", color = textSecondary, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "~0.01 MAS",
                                color = textPrimary,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Error message
            if (uiState.error != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            uiState.error!!,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Swap Button
            Button(
                onClick = { viewModel.requestSwapConfirmation() },
                enabled = uiState.canSwap && !uiState.isLoading && !uiState.isCalculatingQuote && !uiState.isConfirmingTx,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonContainerColor,
                    contentColor = buttonContentColor,
                    disabledContainerColor = buttonContainerColor.copy(alpha = 0.3f),
                    disabledContentColor = buttonContentColor.copy(alpha = 0.5f)
                )
            ) {
                if (uiState.isLoading || uiState.isConfirmingTx) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = buttonContentColor,
                            strokeWidth = 2.dp
                        )
                        Text(
                            uiState.swapStatus.ifEmpty { if (uiState.isConfirmingTx) "Confirming..." else "Processing..." },
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                } else if (uiState.isCalculatingQuote) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = buttonContentColor.copy(alpha = 0.7f),
                            strokeWidth = 2.dp
                        )
                        Text(
                            "Getting quote...",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = buttonContentColor.copy(alpha = 0.7f)
                        )
                    }
                } else {
                    // Determine button text and icon based on operation type
                    val (buttonText, buttonIcon) = when (uiState.operationType) {
                        SwapOperationType.WRAP -> {
                            if (uiState.fromAmount.isEmpty()) "Enter amount" to Icons.Default.ArrowDownward
                            else "Wrap MAS to WMAS" to Icons.Default.ArrowDownward
                        }
                        SwapOperationType.UNWRAP -> {
                            if (uiState.fromAmount.isEmpty()) "Enter amount" to Icons.Default.ArrowUpward
                            else "Unwrap WMAS to MAS" to Icons.Default.ArrowUpward
                        }
                        SwapOperationType.SWAP -> {
                            if (uiState.fromAmount.isEmpty()) "Enter amount" to Icons.Default.SwapHoriz
                            else "Swap ${uiState.fromToken.symbol} for ${uiState.toToken.symbol}" to Icons.Default.SwapHoriz
                        }
                    }
                    
                    Icon(
                        buttonIcon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        buttonText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
            
            // Powered by DUSA
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Powered by ",
                    color = textSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "DUSA",
                    color = accentColor,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    // Token Selection Dialogs
    if (showFromTokenDialog) {
        TokenSelectionDialog(
            tokens = uiState.availableTokens,
            selectedToken = uiState.fromToken,
            excludeToken = uiState.toToken,
            onTokenSelected = {
                viewModel.selectFromToken(it)
                showFromTokenDialog = false
            },
            onDismiss = { showFromTokenDialog = false },
            cardBackground = cardBackground,
            textPrimary = textPrimary,
            accentColor = accentColor
        )
    }

    if (showToTokenDialog) {
        TokenSelectionDialog(
            tokens = uiState.availableTokens,
            selectedToken = uiState.toToken,
            excludeToken = uiState.fromToken,
            onTokenSelected = {
                viewModel.selectToToken(it)
                showToTokenDialog = false
            },
            onDismiss = { showToTokenDialog = false },
            cardBackground = cardBackground,
            textPrimary = textPrimary,
            accentColor = accentColor
        )
    }
    
    // Swap Confirmation Dialog
    if (uiState.showConfirmation && uiState.confirmationData != null) {
        SwapConfirmationDialog(
            data = uiState.confirmationData!!,
            onConfirm = { viewModel.confirmAndExecuteSwap() },
            onDismiss = { viewModel.dismissConfirmation() },
            cardBackground = cardBackground,
            textPrimary = textPrimary,
            textSecondary = textSecondary,
            accentColor = accentColor
        )
    }
    
    // Success Dialog
    if (uiState.swapSuccess) {
        SwapSuccessDialog(
            fromAmount = uiState.fromAmount,
            fromToken = uiState.fromToken,
            toAmount = uiState.toAmount,
            toToken = uiState.toToken,
            txHash = uiState.txHash,
            onDismiss = { 
                // Stay on swap screen and refresh balances
                viewModel.resetSwapAndRefresh()
            },
            accentColor = accentColor
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwapTokenCard(
    label: String,
    token: SwapToken,
    amount: String,
    balance: String,
    onAmountChange: (String) -> Unit,
    onTokenClick: () -> Unit,
    onMaxClick: () -> Unit,
    cardBackground: Color,
    textPrimary: Color,
    textSecondary: Color,
    accentColor: Color,
    isEditable: Boolean,
    isCalculating: Boolean = false,
    isDarkTheme: Boolean = false
) {
    // Press animation
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "cardScale"
    )
    
    // Entrance animation
    var visible by remember { mutableStateOf(false) }
    val offsetY by animateIntAsState(
        targetValue = if (visible) 0 else 50,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "offsetY"
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(400),
        label = "alpha"
    )
    
    LaunchedEffect(Unit) {
        visible = true
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationY = offsetY.toFloat()
                this.alpha = alpha
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    }
                )
            },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isDarkTheme) 0.dp else 4.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with label and balance
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Label badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (label == "From") accentColor.copy(alpha = 0.1f) else Color(0xFF4CAF50).copy(alpha = 0.1f)
                ) {
                    Text(
                        label,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (label == "From") accentColor else Color(0xFF4CAF50)
                    )
                }
                if (isEditable && balance.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "Balance: $balance",
                            style = MaterialTheme.typography.labelSmall,
                            color = textSecondary
                        )
                        Surface(
                            onClick = onMaxClick,
                            shape = RoundedCornerShape(6.dp),
                            color = if (isDarkTheme) Color.White else Color.Black
                        ) {
                            Text(
                                "MAX",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = if (isDarkTheme) Color.Black else Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
            // Token selector and amount input
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Token Selector
                Surface(
                    onClick = onTokenClick,
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Token icon
                        TokenIcon(token = token, size = 32.dp)
                        Text(
                            token.symbol,
                            fontWeight = FontWeight.SemiBold,
                            color = textPrimary
                        )
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = "Select token",
                            tint = textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Amount Input or Loading
                if (isCalculating && !isEditable) {
                    // Show shimmer effect while calculating
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .padding(start = 8.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = accentColor,
                                strokeWidth = 2.dp
                            )
                            Text(
                                "Calculating...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = textSecondary
                            )
                        }
                    }
                } else if (!isEditable) {
                    // For non-editable field (TO), use Text instead of TextField
                    // This ensures proper display of decimal values
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Text(
                            text = if (amount.isEmpty()) "0.00" else amount,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.End
                            ),
                            color = if (amount.isEmpty()) textSecondary else textPrimary,
                            maxLines = 1
                        )
                    }
                } else {
                    // Editable amount input (FROM field)
                    BasicTextField(
                        value = amount,
                        onValueChange = onAmountChange,
                        enabled = isEditable,
                        textStyle = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.End,
                            color = textPrimary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp, end = 4.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                if (amount.isEmpty()) {
                                    Text(
                                        "0.00",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.End
                                        ),
                                        color = textSecondary
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TokenSelectionDialog(
    tokens: List<SwapToken>,
    selectedToken: SwapToken,
    excludeToken: SwapToken,
    onTokenSelected: (SwapToken) -> Unit,
    onDismiss: () -> Unit,
    cardBackground: Color,
    textPrimary: Color,
    accentColor: Color
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = cardBackground)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Select Token",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tokens.filter { it != excludeToken }) { token ->
                        val isSelected = token == selectedToken
                        Surface(
                            onClick = { onTokenSelected(token) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) accentColor.copy(alpha = 0.15f) 
                                   else MaterialTheme.colorScheme.background
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Token icon
                                TokenIcon(token = token, size = 40.dp, fontSize = 18.sp)

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        token.symbol,
                                        fontWeight = FontWeight.SemiBold,
                                        color = textPrimary
                                    )
                                    Text(
                                        token.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = textPrimary.copy(alpha = 0.6f)
                                    )
                                }

                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = accentColor
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SwapSuccessDialog(
    fromAmount: String,
    fromToken: SwapToken,
    toAmount: String,
    toToken: SwapToken,
    txHash: String?,
    onDismiss: () -> Unit,
    accentColor: Color
) {
    val context = LocalContext.current
    
    // Animations
    var visible by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.5f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "dialogScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(300),
        label = "dialogAlpha"
    )
    val iconScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
            visibilityThreshold = 0.01f
        ),
        label = "iconScale"
    )
    val iconRotation by animateFloatAsState(
        targetValue = if (visible) 0f else -180f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "iconRotation"
    )
    
    // Trigger animation on composition
    LaunchedEffect(Unit) {
        visible = true
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                },
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Animated Success Icon with pulse effect
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(80.dp)
                ) {
                    // Outer glow pulse
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val pulseScale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.3f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulseScale"
                    )
                    val pulseAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.4f,
                        targetValue = 0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulseAlpha"
                    )
                    
                    // Pulse ring
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF4CAF50).copy(alpha = pulseAlpha),
                        modifier = Modifier
                            .size(72.dp)
                            .graphicsLayer {
                                scaleX = pulseScale
                                scaleY = pulseScale
                            }
                    ) {}
                    
                    // Main success circle
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF4CAF50).copy(alpha = 0.15f),
                        modifier = Modifier
                            .size(72.dp)
                            .graphicsLayer {
                                scaleX = iconScale
                                scaleY = iconScale
                                rotationZ = iconRotation
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                }

                Text(
                    "Swap Successful!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                // Animated swap amount with arrows
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "$fromAmount ${fromToken.symbol}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    
                    // Animated arrow
                    val arrowTransition = rememberInfiniteTransition(label = "arrowPulse")
                    val arrowOffset by arrowTransition.animateFloat(
                        initialValue = -2f,
                        targetValue = 2f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(500, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "arrowOffset"
                    )
                    
                    Icon(
                        Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier
                            .size(20.dp)
                            .graphicsLayer { translationX = arrowOffset }
                    )
                    
                    Text(
                        "$toAmount ${toToken.symbol}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = accentColor
                    )
                }

                if (txHash != null) {
                    TextButton(
                        onClick = {
                            val url = "https://explorer.massa.net/mainnet/operation/$txHash"
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                            context.startActivity(intent)
                        }
                    ) {
                        Text("View on Explorer", color = accentColor)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.OpenInNew,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    Text("Done", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SwapConfirmationDialog(
    data: SwapConfirmationData,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    cardBackground: Color,
    textPrimary: Color,
    textSecondary: Color,
    accentColor: Color
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = cardBackground)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Confirm Swap",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = textSecondary
                        )
                    }
                }
                
                Divider(color = textSecondary.copy(alpha = 0.2f))
                
                // Swap Preview
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // From Amount
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TokenIcon(token = data.fromToken, size = 40.dp)
                        Column {
                            Text(
                                "${data.fromAmount} ${data.fromToken.symbol}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                            Text(
                                "You pay",
                                style = MaterialTheme.typography.bodySmall,
                                color = textSecondary
                            )
                        }
                    }
                    
                    Icon(
                        Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                    
                    // To Amount
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TokenIcon(token = data.toToken, size = 40.dp)
                        Column {
                            Text(
                                "${data.toAmount} ${data.toToken.symbol}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                            Text(
                                "You receive",
                                style = MaterialTheme.typography.bodySmall,
                                color = textSecondary
                            )
                        }
                    }
                }
                
                Divider(color = textSecondary.copy(alpha = 0.2f))
                
                // Swap Details
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SwapDetailRow(
                        label = "Rate",
                        value = "1 ${data.fromToken.symbol} = ${data.exchangeRate} ${data.toToken.symbol}",
                        textPrimary = textPrimary,
                        textSecondary = textSecondary
                    )
                    SwapDetailRow(
                        label = "Minimum received",
                        value = "${data.minReceived} ${data.toToken.symbol}",
                        textPrimary = textPrimary,
                        textSecondary = textSecondary
                    )
                    SwapDetailRow(
                        label = "Price impact",
                        value = "${data.priceImpact}%",
                        textPrimary = if (data.priceImpact < 1f) Color(0xFF4CAF50) else Color(0xFFFF5722),
                        textSecondary = textSecondary
                    )
                    SwapDetailRow(
                        label = "Slippage tolerance",
                        value = "${data.slippage}%",
                        textPrimary = textPrimary,
                        textSecondary = textSecondary
                    )
                    SwapDetailRow(
                        label = "Network fee",
                        value = data.estimatedFee,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Warning if high price impact
                if (data.priceImpact >= 3f) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFF5722).copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Warning,
                                contentDescription = null,
                                tint = Color(0xFFFF5722),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                "High price impact! You may receive significantly less than expected.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFF5722)
                            )
                        }
                    }
                }
                
                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = textPrimary
                        )
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                    ) {
                        Text("Confirm Swap", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SwapDetailRow(
    label: String,
    value: String,
    textPrimary: Color,
    textSecondary: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = textSecondary
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = textPrimary
        )
    }
}

@Composable
private fun TokenIcon(
    token: SwapToken,
    size: androidx.compose.ui.unit.Dp,
    fontSize: androidx.compose.ui.unit.TextUnit = 14.sp
) {
    if (token.symbol == "MC") {
        // Special icon for MassaConnect token
        Image(
            painter = painterResource(id = R.drawable.massapay_logo),
            contentDescription = "MassaConnect",
            modifier = Modifier
                .size(size)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        // Default letter icon for other tokens
        Surface(
            shape = CircleShape,
            color = token.color.copy(alpha = 0.2f),
            modifier = Modifier.size(size)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text(
                    token.symbol.first().toString(),
                    fontWeight = FontWeight.Bold,
                    fontSize = fontSize,
                    color = token.color
                )
            }
        }
    }
}