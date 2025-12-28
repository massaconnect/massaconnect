package com.massapay.android.ui.dapp

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.massapay.android.core.model.DAppConnectRequest
import com.massapay.android.core.model.DAppPermission
import com.massapay.android.core.model.DAppSignRequest
import com.massapay.android.core.model.DAppTransactionRequest

/**
 * Warning dialog shown when opening a DApp for the first time
 * Explains that MassaPay uses Bearby protocol for DApp connectivity
 */
@Composable
fun BearbyProtocolWarningDialog(
    dappUrl: String,
    isDarkTheme: Boolean,
    onContinue: () -> Unit,
    onCancel: () -> Unit
) {
    val backgroundColor = if (isDarkTheme) Color(0xFF1A1A2E) else Color.White
    val surfaceColor = if (isDarkTheme) Color(0xFF252540) else Color(0xFFF5F5F5)
    val textPrimary = if (isDarkTheme) Color.White else Color.Black
    val textSecondary = if (isDarkTheme) Color.White.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.6f)
    val accentColor = Color(0xFF6366F1)
    val infoColor = Color(0xFF06B6D4)
    
    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icon with gradient background
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    accentColor.copy(alpha = 0.3f),
                                    infoColor.copy(alpha = 0.2f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(36.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Text(
                    text = "DApp Connection",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // URL being opened
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = surfaceColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = null,
                            tint = textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = try {
                                java.net.URI(dappUrl).host ?: dappUrl
                            } catch (e: Exception) { dappUrl },
                            style = MaterialTheme.typography.bodyMedium,
                            color = textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Info card about Bearby protocol
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = infoColor.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = infoColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Bearby Protocol",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = infoColor
                            )
                        }
                        
                        Text(
                            text = "MassaConnect connects to DApps using the Bearby wallet protocol. This ensures compatibility with all Massa ecosystem applications.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = textPrimary,
                            lineHeight = 22.sp
                        )
                        
                        Divider(
                            color = infoColor.copy(alpha = 0.2f),
                            thickness = 1.dp
                        )
                        
                        // Key points
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ProtocolInfoItem(
                                icon = Icons.Default.CheckCircle,
                                text = "Your transactions require approval",
                                color = Color(0xFF22C55E)
                            )
                            ProtocolInfoItem(
                                icon = Icons.Default.CheckCircle,
                                text = "DApps can see your public address",
                                color = Color(0xFF22C55E)
                            )
                            ProtocolInfoItem(
                                icon = Icons.Default.CheckCircle,
                                text = "Compatible with Dusa, MassaBridge & more",
                                color = Color(0xFF22C55E)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = textPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }
                    
                    Button(
                        onClick = onContinue,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInBrowser,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Continue")
                    }
                }
            }
        }
    }
}

@Composable
private fun ProtocolInfoItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = color.copy(alpha = 0.9f)
        )
    }
}

@Composable
fun ConnectRequestDialog(
    request: DAppConnectRequest,
    walletAddress: String,
    isDarkTheme: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val backgroundColor = if (isDarkTheme) Color(0xFF1A1A2E) else Color.White
    val surfaceColor = if (isDarkTheme) Color(0xFF252540) else Color(0xFFF5F5F5)
    val textPrimary = if (isDarkTheme) Color.White else Color.Black
    val textSecondary = if (isDarkTheme) Color.White.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.6f)
    val accentColor = Color(0xFF6366F1)
    
    Dialog(
        onDismissRequest = onReject,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // DApp icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(accentColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = request.name.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Connection Request",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = request.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = textPrimary
                )
                
                Text(
                    text = request.origin,
                    style = MaterialTheme.typography.bodySmall,
                    color = textSecondary
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Wallet info
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = surfaceColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(accentColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Your Wallet",
                                style = MaterialTheme.typography.labelSmall,
                                color = textSecondary
                            )
                            Text(
                                text = "${walletAddress.take(10)}...${walletAddress.takeLast(8)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = textPrimary
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Permissions
                Text(
                    text = "This app wants to:",
                    style = MaterialTheme.typography.labelMedium,
                    color = textSecondary,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    request.requestedPermissions.forEach { permission ->
                        PermissionItem(
                            permission = permission,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onReject,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = textPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Reject")
                    }
                    
                    Button(
                        onClick = onApprove,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Connect")
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionItem(
    permission: DAppPermission,
    textPrimary: Color,
    textSecondary: Color
) {
    val (icon, title, description) = when (permission) {
        DAppPermission.VIEW_ACCOUNT -> Triple(
            Icons.Default.Visibility,
            "View wallet address",
            "See your public address"
        )
        DAppPermission.SIGN_MESSAGE -> Triple(
            Icons.Default.Draw,
            "Sign messages",
            "Request signature for messages"
        )
        DAppPermission.SIGN_TRANSACTION -> Triple(
            Icons.Default.Send,
            "Request transactions",
            "Request approval for transactions"
        )
        DAppPermission.READ_BALANCE -> Triple(
            Icons.Default.AccountBalance,
            "View balance",
            "See your wallet balance"
        )
    }
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF22C55E),
            modifier = Modifier.size(20.dp)
        )
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = textPrimary
            )
        }
    }
}

@Composable
fun SignRequestDialog(
    request: DAppSignRequest,
    isDarkTheme: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val backgroundColor = if (isDarkTheme) Color(0xFF1A1A2E) else Color.White
    val surfaceColor = if (isDarkTheme) Color(0xFF252540) else Color(0xFFF5F5F5)
    val textPrimary = if (isDarkTheme) Color.White else Color.Black
    val textSecondary = if (isDarkTheme) Color.White.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.6f)
    val accentColor = Color(0xFF6366F1)
    val warningColor = Color(0xFFF59E0B)
    
    Dialog(
        onDismissRequest = onReject,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(warningColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Draw,
                        contentDescription = null,
                        tint = warningColor,
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Sign Message",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = request.origin,
                    style = MaterialTheme.typography.bodySmall,
                    color = textSecondary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Warning
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = warningColor.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = warningColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Only sign messages from trusted sources. Signing can authorize actions on your behalf.",
                            style = MaterialTheme.typography.bodySmall,
                            color = warningColor
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Message
                Text(
                    text = "Message",
                    style = MaterialTheme.typography.labelMedium,
                    color = textSecondary,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp, max = 200.dp),
                    colors = CardDefaults.cardColors(containerColor = surfaceColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = request.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = textPrimary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onReject,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = textPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Reject")
                    }
                    
                    Button(
                        onClick = onApprove,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Sign")
                    }
                }
            }
        }
    }
}

/**
 * Format nanoMAS value to human-readable MAS with proper decimals
 * nanoMAS = MAS * 10^9
 */
private fun formatNanoMasToMas(nanoMasString: String?): String {
    if (nanoMasString.isNullOrEmpty() || nanoMasString == "0") return "0"
    
    return try {
        val nanoMas = nanoMasString.toBigDecimalOrNull() ?: return nanoMasString
        val mas = nanoMas.divide(java.math.BigDecimal("1000000000"))
        
        // Format with up to 9 decimal places, removing trailing zeros
        val formatted = mas.stripTrailingZeros().toPlainString()
        
        // If the value is very small, show at least some precision
        if (mas > java.math.BigDecimal.ZERO && mas < java.math.BigDecimal("0.000001")) {
            "< 0.000001"
        } else {
            formatted
        }
    } catch (e: Exception) {
        nanoMasString
    }
}

/**
 * Format gas value to readable format (millions)
 */
private fun formatGas(gasString: String?): String {
    if (gasString.isNullOrEmpty()) return "N/A"
    
    return try {
        val gas = gasString.toLongOrNull() ?: return gasString
        when {
            gas >= 1_000_000_000 -> String.format("%.1fB", gas / 1_000_000_000.0)
            gas >= 1_000_000 -> String.format("%.1fM", gas / 1_000_000.0)
            gas >= 1_000 -> String.format("%.1fK", gas / 1_000.0)
            else -> gas.toString()
        }
    } catch (e: Exception) {
        gasString
    }
}

@Composable
fun TransactionRequestDialog(
    request: DAppTransactionRequest,
    isDarkTheme: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val backgroundColor = if (isDarkTheme) Color(0xFF1A1A2E) else Color.White
    val surfaceColor = if (isDarkTheme) Color(0xFF252540) else Color(0xFFF5F5F5)
    val textPrimary = if (isDarkTheme) Color.White else Color.Black
    val textSecondary = if (isDarkTheme) Color.White.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.6f)
    val accentColor = Color(0xFF6366F1)
    val dangerColor = Color(0xFFEF4444)
    
    // Format values for display
    val formattedAmount = formatNanoMasToMas(request.amount)
    val formattedFee = formatNanoMasToMas(request.fee)
    
    Dialog(
        onDismissRequest = onReject,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(dangerColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = null,
                        tint = dangerColor,
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Confirm Transaction",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = request.origin,
                    style = MaterialTheme.typography.bodySmall,
                    color = textSecondary
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Amount (formatted from nanoMAS to MAS)
                if (request.amount.isNotEmpty() && request.amount != "0" && formattedAmount != "0") {
                    Text(
                        text = "$formattedAmount MAS",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // Transaction details
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = surfaceColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // To address
                        TransactionDetailRow(
                            label = "To",
                            value = "${request.toAddress.take(12)}...${request.toAddress.takeLast(8)}",
                            textPrimary = textPrimary,
                            textSecondary = textSecondary
                        )
                        
                        // Fee (formatted from nanoMAS to MAS)
                        if (!formattedFee.isNullOrEmpty() && formattedFee != "0" && formattedFee != "N/A") {
                            TransactionDetailRow(
                                label = "Network Fee",
                                value = "$formattedFee MAS",
                                textPrimary = textPrimary,
                                textSecondary = textSecondary
                            )
                        }
                        
                        // Contract function
                        request.functionName?.let { function ->
                            TransactionDetailRow(
                                label = "Function",
                                value = function,
                                textPrimary = textPrimary,
                                textSecondary = textSecondary
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Warning
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = dangerColor.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = dangerColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "This transaction will be sent to the Massa blockchain and cannot be reversed.",
                            style = MaterialTheme.typography.bodySmall,
                            color = dangerColor
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onReject,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = textPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Reject")
                    }
                    
                    Button(
                        onClick = onApprove,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Confirm")
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionDetailRow(
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
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = textSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 200.dp)
        )
    }
}
