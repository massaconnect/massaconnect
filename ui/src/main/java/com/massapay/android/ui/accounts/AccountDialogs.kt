package com.massapay.android.ui.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.massapay.android.core.model.Account
import com.massapay.android.core.model.AccountColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAccountDialog(
    onDismiss: () -> Unit,
    onCreate: (String, AccountColor?) -> Unit,
    isCreating: Boolean,
    buttonBackground: Color = Color.Black,
    buttonText: Color = Color.White
) {
    var accountName by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf<AccountColor?>(null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Create New Account",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "A new unique seed phrase will be generated for this account.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                
                // Account name input
                OutlinedTextField(
                    value = accountName,
                    onValueChange = { accountName = it },
                    label = { Text("Account Name") },
                    placeholder = { Text("e.g., Savings, Trading") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isCreating,
                    shape = RoundedCornerShape(12.dp)
                )
                
                // Color picker
                Text(
                    "Choose Color (Optional)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(120.dp)
                ) {
                    items(AccountColor.values().toList()) { color ->
                        ColorOption(
                            color = color,
                            isSelected = selectedColor == color,
                            onClick = { 
                                selectedColor = if (selectedColor == color) null else color
                            },
                            enabled = !isCreating
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(accountName, selectedColor) },
                enabled = accountName.isNotBlank() && !isCreating,
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonBackground,
                    contentColor = buttonText
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isCreating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = buttonText
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Create", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isCreating
            ) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAccountDialog(
    account: Account,
    onDismiss: () -> Unit,
    onUpdate: (String, AccountColor?) -> Unit,
    buttonBackground: Color = Color.Black,
    buttonText: Color = Color.White
) {
    var accountName by remember { mutableStateOf(account.name) }
    var selectedColor by remember { mutableStateOf(account.color) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Edit Account",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Account name input
                OutlinedTextField(
                    value = accountName,
                    onValueChange = { accountName = it },
                    label = { Text("Account Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                
                // Color picker
                Text(
                    "Choose Color",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(120.dp)
                ) {
                    items(AccountColor.values().toList()) { color ->
                        ColorOption(
                            color = color,
                            isSelected = selectedColor == color,
                            onClick = { selectedColor = color }
                        )
                    }
                }
                
                // Account info (read-only)
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    InfoRow("Address", "${account.address.take(12)}...${account.address.takeLast(8)}")
                    InfoRow("Balance", "${String.format("%.4f", account.balance.toDoubleOrNull() ?: 0.0)} MAS")
                    if (account.mnemonic.isNotBlank()) {
                        InfoRow("Type", "Full Wallet (with seed phrase)")
                    } else {
                        InfoRow("Type", "Imported (private key only)")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onUpdate(accountName, selectedColor) },
                enabled = accountName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonBackground,
                    contentColor = buttonText
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun DeleteAccountDialog(
    account: Account,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                "Delete Account?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Are you sure you want to delete '${account.name}'?",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "⚠️ Warning",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            "This action cannot be undone. Make sure you have backed up the seed phrase if you want to recover this account later.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
                
                InfoRow("Balance", "${String.format("%.4f", account.balance.toDoubleOrNull() ?: 0.0)} MAS")
                InfoRow("Address", "${account.address.take(12)}...${account.address.takeLast(8)}")
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Delete", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
private fun ColorOption(
    color: AccountColor,
    isSelected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    val colorValue = remember(color) {
        Color(android.graphics.Color.parseColor(color.hex))
    }
    
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(
                if (isSelected) 
                    colorValue 
                else 
                    colorValue.copy(alpha = 0.3f)
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Selected",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}

// Add Account Options Dialog (Create New or Import)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountOptionsDialog(
    onDismiss: () -> Unit,
    onCreateNew: () -> Unit,
    onImportS1: () -> Unit,
    buttonBackground: Color = Color.Black,
    buttonText: Color = Color.White
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Add Account",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Choose how to add a new account:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                
                // Create New Option
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        onDismiss()
                        onCreateNew()
                    },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(buttonBackground, RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                tint = buttonText,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Create New Account",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Generate a new wallet with seed phrase",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
                
                // Import with S1 Option
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        onDismiss()
                        onImportS1()
                    },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(buttonBackground, RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Key,
                                contentDescription = null,
                                tint = buttonText,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Import with Private Key",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Import existing wallet using S1 key",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

// Import Account with S1 Private Key Dialog
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportS1AccountDialog(
    onDismiss: () -> Unit,
    onImport: (String, String, AccountColor?) -> Unit,
    isImporting: Boolean,
    error: String? = null,
    buttonBackground: Color = Color.Black,
    buttonText: Color = Color.White
) {
    var accountName by remember { mutableStateOf("") }
    var privateKey by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf<AccountColor?>(null) }
    var showKey by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = { if (!isImporting) onDismiss() },
        title = {
            Text(
                "Import with Private Key",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Warning Card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFF9800).copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                "Important Notice",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF9800)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "This account will be imported with the private key only. You will NOT have access to the seed phrase. Make sure you have a backup of your seed phrase in Massa Station, Bearby or another wallet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
                
                // Account name input
                OutlinedTextField(
                    value = accountName,
                    onValueChange = { accountName = it },
                    label = { Text("Account Name") },
                    placeholder = { Text("e.g., Massa Station, Bearby") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isImporting,
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = {
                        Icon(Icons.Default.Label, contentDescription = null)
                    }
                )
                
                // Private Key input
                OutlinedTextField(
                    value = privateKey,
                    onValueChange = { privateKey = it },
                    label = { Text("S1 Private Key") },
                    placeholder = { Text("S1...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isImporting,
                    shape = RoundedCornerShape(12.dp),
                    visualTransformation = if (showKey) 
                        androidx.compose.ui.text.input.VisualTransformation.None 
                    else 
                        androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    leadingIcon = {
                        Icon(Icons.Default.Key, contentDescription = null)
                    },
                    trailingIcon = {
                        IconButton(onClick = { showKey = !showKey }) {
                            Icon(
                                if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showKey) "Hide key" else "Show key"
                            )
                        }
                    }
                )
                
                // Color picker
                Text(
                    "Choose Color (Optional)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(72.dp)
                ) {
                    items(AccountColor.values().take(10).toList()) { color ->
                        ColorOptionSmall(
                            color = color,
                            isSelected = selectedColor == color,
                            onClick = { 
                                selectedColor = if (selectedColor == color) null else color
                            },
                            enabled = !isImporting
                        )
                    }
                }
                
                // Error message
                if (error != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                error,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onImport(accountName, privateKey.trim(), selectedColor) },
                enabled = accountName.isNotBlank() && privateKey.trim().startsWith("S1") && privateKey.trim().length > 50 && !isImporting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonBackground,
                    contentColor = buttonText
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isImporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = buttonText
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Import", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isImporting
            ) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
private fun ColorOptionSmall(
    color: AccountColor,
    isSelected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    val colorValue = remember(color) {
        Color(android.graphics.Color.parseColor(color.hex))
    }
    
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(
                if (isSelected) 
                    colorValue 
                else 
                    colorValue.copy(alpha = 0.3f)
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Selected",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
