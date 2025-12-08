package com.massapay.android.ui.walletconnect

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletConnectScreen(
    onClose: () -> Unit,
    onScanQR: () -> Unit,
    isDarkTheme: Boolean,
    viewModel: WalletConnectViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showManualInput by remember { mutableStateOf(false) }
    var manualUri by remember { mutableStateOf("") }
    
    // Theme colors
    val backgroundColor = if (isDarkTheme) Color(0xFF0D0D15) else Color(0xFFFAFAFA)
    val surfaceColor = if (isDarkTheme) Color(0xFF1A1A2E) else Color.White
    val cardBackground = if (isDarkTheme) Color(0xFF16162A) else Color.White
    val textPrimary = if (isDarkTheme) Color.White else Color(0xFF1A1A2E)
    val textSecondary = if (isDarkTheme) Color.White.copy(alpha = 0.6f) else Color(0xFF64748B)
    val accentColor = Color(0xFF6366F1)
    val accentGreen = Color(0xFF22C55E)
    val errorColor = Color(0xFFEF4444)
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Bar
            TopAppBar(
                title = {
                    Text(
                        "WalletConnect",
                        color = textPrimary,
                        fontWeight = FontWeight.Bold
                    )
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundColor
                )
            )
            
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Connection Status
                item {
                    ConnectionStatusCard(
                        state = uiState.connectionState,
                        isDarkTheme = isDarkTheme,
                        cardBackground = cardBackground,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        accentColor = accentColor,
                        accentGreen = accentGreen
                    )
                }
                
                // Connect Section
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBackground)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Outlined.QrCodeScanner,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(48.dp)
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Text(
                                "Connect to DApp",
                                color = textPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            
                            Text(
                                "Scan QR code from any DApp that supports WalletConnect",
                                color = textSecondary,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Button(
                                onClick = onScanQR,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = accentColor
                                )
                            ) {
                                Icon(
                                    Icons.Default.QrCodeScanner,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Scan QR Code",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            OutlinedButton(
                                onClick = { showManualInput = !showManualInput },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    if (showManualInput) "Hide Manual Input" else "Enter URI Manually",
                                    color = textSecondary
                                )
                            }
                            
                            AnimatedVisibility(visible = showManualInput) {
                                Column(
                                    modifier = Modifier.padding(top = 12.dp)
                                ) {
                                    OutlinedTextField(
                                        value = manualUri,
                                        onValueChange = { manualUri = it },
                                        label = { Text("WalletConnect URI") },
                                        placeholder = { Text("wc:...") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    Button(
                                        onClick = {
                                            if (manualUri.startsWith("wc:")) {
                                                viewModel.connectWithUri(manualUri)
                                                manualUri = ""
                                                showManualInput = false
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        enabled = manualUri.startsWith("wc:"),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Connect")
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Active Sessions
                if (uiState.activeSessions.isNotEmpty()) {
                    item {
                        Text(
                            "Active Sessions",
                            color = textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    
                    items(uiState.activeSessions) { session ->
                        SessionCard(
                            session = session,
                            onDisconnect = { viewModel.disconnectSession(session.topic) },
                            isDarkTheme = isDarkTheme,
                            cardBackground = cardBackground,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            errorColor = errorColor
                        )
                    }
                }
                
                // How it works
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDarkTheme) Color(0xFF1E1E3F) else Color(0xFFF0F0FF)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                "How it works",
                                color = accentColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            HowItWorksStep(
                                number = "1",
                                text = "Open a DApp that supports WalletConnect",
                                accentColor = accentColor,
                                textColor = textPrimary
                            )
                            HowItWorksStep(
                                number = "2",
                                text = "Click 'Connect Wallet' and select WalletConnect",
                                accentColor = accentColor,
                                textColor = textPrimary
                            )
                            HowItWorksStep(
                                number = "3",
                                text = "Scan the QR code with MassaPay",
                                accentColor = accentColor,
                                textColor = textPrimary
                            )
                            HowItWorksStep(
                                number = "4",
                                text = "Approve the connection and start using the DApp!",
                                accentColor = accentColor,
                                textColor = textPrimary
                            )
                        }
                    }
                }
                
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
        
        // Session Proposal Dialog
        uiState.pendingProposal?.let { proposal ->
            SessionProposalDialog(
                proposal = proposal,
                walletAddress = uiState.walletAddress,
                onApprove = { viewModel.approveSession() },
                onReject = { viewModel.rejectSession() },
                isDarkTheme = isDarkTheme
            )
        }
        
        // Pending Request Dialog
        uiState.pendingRequest?.let { request ->
            RequestDialog(
                request = request,
                onApprove = { result -> viewModel.approveRequest(result) },
                onReject = { viewModel.rejectRequest() },
                isDarkTheme = isDarkTheme
            )
        }
        
        // Error Snackbar
        uiState.error?.let { error ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                action = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("Dismiss", color = Color.White)
                    }
                },
                containerColor = errorColor
            ) {
                Text(error, color = Color.White)
            }
        }
    }
}

@Composable
private fun ConnectionStatusCard(
    state: WalletConnectState,
    isDarkTheme: Boolean,
    cardBackground: Color,
    textPrimary: Color,
    textSecondary: Color,
    accentColor: Color,
    accentGreen: Color
) {
    val (statusText, statusColor, statusIcon) = when (state) {
        is WalletConnectState.Disconnected -> Triple("Ready to Connect", textSecondary, Icons.Outlined.WifiOff)
        is WalletConnectState.Ready -> Triple("Ready", accentGreen, Icons.Outlined.CheckCircle)
        is WalletConnectState.Connecting -> Triple("Connecting...", accentColor, Icons.Outlined.Sync)
        is WalletConnectState.Connected -> Triple("Connected to ${state.session.peerName}", accentGreen, Icons.Outlined.Link)
        is WalletConnectState.Error -> Triple("Error: ${state.message}", Color(0xFFEF4444), Icons.Outlined.Error)
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(statusColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    statusIcon,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Status",
                    color = textSecondary,
                    fontSize = 12.sp
                )
                Text(
                    statusText,
                    color = textPrimary,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SessionCard(
    session: WalletConnectSession,
    onDisconnect: () -> Unit,
    isDarkTheme: Boolean,
    cardBackground: Color,
    textPrimary: Color,
    textSecondary: Color,
    errorColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // DApp Icon
            if (session.peerIcon.isNotEmpty()) {
                AsyncImage(
                    model = session.peerIcon,
                    contentDescription = session.peerName,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF6366F1).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        session.peerName.take(2).uppercase(),
                        color = Color(0xFF6366F1),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    session.peerName,
                    color = textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
                Text(
                    session.peerUrl,
                    color = textSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            IconButton(
                onClick = onDisconnect,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(errorColor.copy(alpha = 0.1f))
            ) {
                Icon(
                    Icons.Default.LinkOff,
                    contentDescription = "Disconnect",
                    tint = errorColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun HowItWorksStep(
    number: String,
    text: String,
    accentColor: Color,
    textColor: Color
) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(accentColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                number,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.width(10.dp))
        
        Text(
            text,
            color = textColor,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SessionProposalDialog(
    proposal: SessionProposalUi,
    walletAddress: String,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    isDarkTheme: Boolean
) {
    val backgroundColor = if (isDarkTheme) Color(0xFF1A1A2E) else Color.White
    val textPrimary = if (isDarkTheme) Color.White else Color(0xFF1A1A2E)
    val textSecondary = if (isDarkTheme) Color.White.copy(alpha = 0.6f) else Color(0xFF64748B)
    
    AlertDialog(
        onDismissRequest = onReject,
        containerColor = backgroundColor,
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                proposal.icon?.let { icon ->
                    AsyncImage(
                        model = icon,
                        contentDescription = proposal.name,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
                Text(
                    "Connection Request",
                    color = textPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column {
                Text(
                    proposal.name,
                    color = textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
                Text(
                    proposal.url,
                    color = textSecondary,
                    fontSize = 12.sp
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    proposal.description,
                    color = textSecondary,
                    fontSize = 14.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDarkTheme) Color(0xFF0D0D15) else Color(0xFFF5F5F5)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "Connect with:",
                            color = textSecondary,
                            fontSize = 12.sp
                        )
                        Text(
                            walletAddress.take(10) + "..." + walletAddress.takeLast(8),
                            color = textPrimary,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onApprove,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF22C55E)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Approve")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onReject,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Reject", color = textSecondary)
            }
        }
    )
}

@Composable
private fun RequestDialog(
    request: WalletConnectRequest,
    onApprove: (String) -> Unit,
    onReject: () -> Unit,
    isDarkTheme: Boolean
) {
    val backgroundColor = if (isDarkTheme) Color(0xFF1A1A2E) else Color.White
    val textPrimary = if (isDarkTheme) Color.White else Color(0xFF1A1A2E)
    val textSecondary = if (isDarkTheme) Color.White.copy(alpha = 0.6f) else Color(0xFF64748B)
    
    // Parse the request type
    val title = when {
        request.method.contains("sign", ignoreCase = true) -> "Sign Message"
        request.method.contains("transaction", ignoreCase = true) -> "Confirm Transaction"
        request.method.contains("call", ignoreCase = true) -> "Smart Contract Call"
        else -> "Request"
    }
    
    AlertDialog(
        onDismissRequest = onReject,
        containerColor = backgroundColor,
        title = {
            Text(
                title,
                color = textPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    "Method: ${request.method}",
                    color = textSecondary,
                    fontSize = 12.sp
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDarkTheme) Color(0xFF0D0D15) else Color(0xFFF5F5F5)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        request.params,
                        color = textPrimary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(12.dp),
                        maxLines = 8,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    // TODO: Actually process the request and get real result
                    onApprove("{\"success\": true}") 
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6366F1)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Approve")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onReject,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Reject", color = textSecondary)
            }
        }
    )
}
