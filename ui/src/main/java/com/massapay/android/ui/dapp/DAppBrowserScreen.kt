package com.massapay.android.ui.dapp

import android.content.Intent
import android.net.Uri
import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.util.Log
import android.view.ViewGroup
import android.webkit.*
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.massapay.android.core.model.DAppBookmark
import com.massapay.android.core.model.DAppCategory
import com.massapay.android.core.model.MassaDApps
import kotlinx.coroutines.delay
import java.io.ByteArrayInputStream

/**
 * Helper function to handle MassaStation API emulation
 * This allows DApps using @massalabs/wallet-provider to detect MassaPay
 */
private fun handleMassaStationRequest(
    url: String,
    request: WebResourceRequest,
    walletAddress: String
): WebResourceResponse? {
    val path = request.url.path ?: ""
    Log.d("MassaPayWebView", "MassaStation path: $path, method: ${request.method}, full URL: $url")
    
    // Create JSON response based on the endpoint
    val jsonResponse = when {
        // Plugin manager - wallet-provider checks this for detection
        // Must return: name="Massa Wallet", author="Massa Labs", status="Up"
        path.contains("/plugin-manager") -> {
            Log.d("MassaPayWebView", "Returning plugin-manager response")
            """{"result":[{"name":"Massa Wallet","author":"Massa Labs","description":"MassaConnect Wallet","home":"/","icon":"","status":"Up","version":"1.0.0"}]}"""
        }
        
        // Wallet API accounts endpoint - multiple patterns
        // URL patterns:
        // - /plugin/massa-labs/massa-wallet/api/accounts (MassaStation normal)
        // - /api/accounts (MassaStation standalone mode via localhost:8080)
        // - /accounts (fallback)
        path.contains("/api/accounts") || 
        path.contains("/massa-wallet/api/accounts") || 
        (path.contains("/accounts") && !path.contains("signrules")) -> {
            Log.d("MassaPayWebView", "Returning accounts response for address: $walletAddress")
            if (walletAddress.isNotEmpty()) {
                """{"result":[{"address":"$walletAddress","nickname":"MassaPay Account","keyPair":{"publicKey":"","nonce":""},"status":"OK"}]}"""
            } else {
                """{"result":[]}"""
            }
        }
        
        // Wallet API config endpoint
        path.contains("/api/config") || path.contains("/config") -> {
            Log.d("MassaPayWebView", "Returning config response")
            """{"result":{"network":"mainnet","chainId":77658377}}"""
        }
        
        // Network info
        path.contains("/network") || path.contains("/node") -> {
            Log.d("MassaPayWebView", "Returning network response")
            """{
                "result": {
                    "chainId": 77658377,
                    "name": "mainnet",
                    "url": "https://mainnet.massa.net/api/v2",
                    "minimalFee": "10000000"
                }
            }"""
        }
        
        // Check if MassaStation is available (root or web-app)
        path.isEmpty() || path == "/" || path.contains("/web-app") -> {
            Log.d("MassaPayWebView", "Returning MassaStation root response")
            """{"status":"ok","version":"1.0.0","name":"MassaPay (MassaStation compatible)"}"""
        }
        
        // Default - return OK status
        else -> {
            Log.d("MassaPayWebView", "Returning default OK response for path: $path")
            """{"status":"ok","result":{}}"""
        }
    }
    
    return WebResourceResponse(
        "application/json",
        "UTF-8",
        200,
        "OK",
        mapOf(
            "Access-Control-Allow-Origin" to "*",
            "Access-Control-Allow-Methods" to "GET, POST, PUT, DELETE, OPTIONS",
            "Access-Control-Allow-Headers" to "*",
            "Content-Type" to "application/json"
        ),
        ByteArrayInputStream(jsonResponse.toByteArray())
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DAppBrowserScreen(
    onClose: () -> Unit,
    isDarkTheme: Boolean,
    onWalletConnectClick: (() -> Unit)? = null,
    viewModel: DAppBrowserViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var urlInput by remember { mutableStateOf("") }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var pendingUrl by remember { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    
    // Bearby protocol warning dialog state
    var showBearbyWarning by remember { mutableStateOf(false) }
    var pendingDAppUrl by remember { mutableStateOf<String?>(null) }
    var acknowledgedDApps by remember { mutableStateOf(setOf<String>()) }
    
    // Theme colors matching Dashboard - Pure black/white themes
    val backgroundColor = if (isDarkTheme) Color(0xFF000000) else Color(0xFFFFFFFF)
    val surfaceColor = if (isDarkTheme) Color(0xFF0A0A0A) else Color(0xFFFAFAFA)
    val cardBackground = if (isDarkTheme) Color(0xFF121212) else Color(0xFFF5F5F5)
    val textPrimary = if (isDarkTheme) Color.White else Color(0xFF000000)
    val textSecondary = if (isDarkTheme) Color(0xFFB0B0B0) else Color(0xFF666666)
    val accentColor = Color(0xFF7B3FE4)  // Web3 purple
    val accentGreen = Color(0xFF00C853)
    
    // Icon button colors for light/dark mode
    val iconButtonBg = if (isDarkTheme) Color(0xFF1A1A1A) else Color(0xFFEEEEEE)
    val iconButtonTint = if (isDarkTheme) Color.White else Color(0xFF333333)
    
    // Load pending URL when webView is ready
    LaunchedEffect(webView, pendingUrl) {
        if (webView != null && pendingUrl != null) {
            val url = pendingUrl!!
            pendingUrl = null
            viewModel.updateUrl(url)
            webView?.loadUrl(url)
        }
    }
    
    // Force update JavaScript after connection approved
    LaunchedEffect(uiState.pendingForceUpdate) {
        if (uiState.pendingForceUpdate && webView != null) {
            val address = uiState.walletAddress
            if (address.isNotEmpty()) {
                webView?.evaluateJavascript("""
                    (function() {
                        console.log('[MassaPay] Connection approved, forcing update with address: $address');
                        if (window.massaPayForceUpdate) {
                            window.massaPayForceUpdate('$address');
                        }
                        
                        // Also dispatch custom events
                        window.dispatchEvent(new CustomEvent('massaPay:connected', { 
                            detail: { address: '$address' } 
                        }));
                        
                        // Post message to page
                        window.postMessage({
                            type: 'ACCOUNT_CHANGED',
                            source: 'bearby-extension',
                            address: '$address'
                        }, '*');
                        
                        console.log('[MassaPay] Force update completed');
                    })();
                """.trimIndent(), null)
            }
            viewModel.clearForceUpdate()
        }
    }
    
    // Reload page after connection for DApps that need full refresh (like Dusa)
    LaunchedEffect(uiState.pendingPageReload) {
        if (uiState.pendingPageReload && webView != null) {
            // Small delay to let the response be sent first
            kotlinx.coroutines.delay(500)
            webView?.reload()
            viewModel.clearPageReload()
        }
    }
    
    // Handle back navigation
    BackHandler(enabled = uiState.canGoBack || uiState.currentUrl.isNotEmpty()) {
        if (uiState.canGoBack) {
            webView?.goBack()
        } else if (uiState.currentUrl.isNotEmpty()) {
            viewModel.updateUrl("")
            urlInput = ""
        }
    }
    
    // Function to actually load the URL
    fun loadDAppUrl(url: String) {
        urlInput = url
        if (webView != null) {
            viewModel.updateUrl(url)
            webView?.loadUrl(url)
        } else {
            pendingUrl = url
            viewModel.updateUrl(url)
        }
    }
    
    // Function to navigate - shows warning for known DApps
    fun navigateTo(url: String) {
        val finalUrl = when {
            url.startsWith("http://") || url.startsWith("https://") -> url
            url.contains(".") && !url.contains(" ") -> "https://$url"
            else -> "https://www.google.com/search?q=${java.net.URLEncoder.encode(url, "UTF-8")}"
        }
        
        // Check if this is a known DApp domain that needs warning
        val host = try {
            java.net.URI(finalUrl).host ?: ""
        } catch (e: Exception) { "" }
        
        val isDApp = host.contains("dusa.io") || 
                     host.contains("massa") || 
                     host.contains("bridge") ||
                     uiState.bookmarks.any { it.url.contains(host) }
        
        // Show warning for DApps that haven't been acknowledged this session
        if (isDApp && !acknowledgedDApps.contains(host)) {
            pendingDAppUrl = finalUrl
            showBearbyWarning = true
        } else {
            loadDAppUrl(finalUrl)
        }
    }
    
    // Bearby Protocol Warning Dialog
    if (showBearbyWarning && pendingDAppUrl != null) {
        BearbyProtocolWarningDialog(
            dappUrl = pendingDAppUrl!!,
            isDarkTheme = isDarkTheme,
            onContinue = {
                val url = pendingDAppUrl!!
                val host = try {
                    java.net.URI(url).host ?: ""
                } catch (e: Exception) { "" }
                acknowledgedDApps = acknowledgedDApps + host
                showBearbyWarning = false
                loadDAppUrl(url)
                pendingDAppUrl = null
            },
            onCancel = {
                showBearbyWarning = false
                pendingDAppUrl = null
            }
        )
    }
    
    // Handle dialog states
    when (val dialogState = uiState.dialogState) {
        is DAppDialogState.ConnectRequest -> {
            ConnectRequestDialog(
                request = dialogState.request,
                walletAddress = uiState.walletAddress,
                isDarkTheme = isDarkTheme,
                onApprove = { viewModel.approveConnect() },
                onReject = { viewModel.rejectRequest() }
            )
        }
        is DAppDialogState.SignRequest -> {
            SignRequestDialog(
                request = dialogState.request,
                isDarkTheme = isDarkTheme,
                onApprove = { viewModel.approveSign() },
                onReject = { viewModel.rejectRequest() }
            )
        }
        is DAppDialogState.TransactionRequest -> {
            TransactionRequestDialog(
                request = dialogState.request,
                isDarkTheme = isDarkTheme,
                onApprove = { viewModel.approveTransaction() },
                onReject = { viewModel.rejectRequest() }
            )
        }
        DAppDialogState.None -> { /* No dialog */ }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = surfaceColor,
                shadowElevation = if (uiState.currentUrl.isNotEmpty()) 4.dp else 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            if (uiState.currentUrl.isNotEmpty()) {
                                viewModel.updateUrl("")
                                urlInput = ""
                            } else {
                                onClose()
                            }
                        }) {
                            Icon(
                                imageVector = if (uiState.currentUrl.isNotEmpty()) 
                                    Icons.Default.ArrowBack else Icons.Default.Close,
                                contentDescription = "Close",
                                tint = textPrimary
                            )
                        }
                        
                        if (uiState.currentUrl.isEmpty()) {
                            Text(
                                text = "DApp Browser",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary,
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            // Compact URL display
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(cardBackground)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (uiState.isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = accentColor
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (uiState.currentUrl.startsWith("https"))
                                            Icons.Default.Lock else Icons.Default.Public,
                                        contentDescription = null,
                                        tint = if (uiState.currentUrl.startsWith("https")) 
                                            accentGreen else textSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = try {
                                        java.net.URI(uiState.currentUrl).host ?: uiState.currentUrl
                                    } catch (e: Exception) { uiState.currentUrl },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        
                        // Connection status badge
                        if (uiState.isConnected) {
                            Row(
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(accentGreen.copy(alpha = 0.15f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(accentGreen)
                                )
                                Text(
                                    text = "Connected",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = accentGreen,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        
                        if (uiState.currentUrl.isNotEmpty()) {
                            IconButton(onClick = { webView?.reload() }) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Reload",
                                    tint = textPrimary
                                )
                            }
                        }
                        
                        // WalletConnect button
                        if (onWalletConnectClick != null) {
                            IconButton(
                                onClick = onWalletConnectClick,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(accentColor.copy(alpha = 0.1f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Link,
                                    contentDescription = "WalletConnect",
                                    tint = accentColor
                                )
                            }
                        }
                    }
                    
                    // Navigation bar (only when browsing)
                    if (uiState.currentUrl.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledIconButton(
                                onClick = { webView?.goBack() },
                                enabled = uiState.canGoBack,
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = cardBackground,
                                    contentColor = textPrimary,
                                    disabledContainerColor = cardBackground.copy(alpha = 0.5f),
                                    disabledContentColor = textSecondary.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.ArrowBack, "Back", Modifier.size(18.dp))
                            }
                            
                            FilledIconButton(
                                onClick = { webView?.goForward() },
                                enabled = uiState.canGoForward,
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = cardBackground,
                                    contentColor = textPrimary,
                                    disabledContainerColor = cardBackground.copy(alpha = 0.5f),
                                    disabledContentColor = textSecondary.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.ArrowForward, "Forward", Modifier.size(18.dp))
                            }
                            
                            Spacer(modifier = Modifier.weight(1f))
                            
                            FilledIconButton(
                                onClick = { 
                                    viewModel.updateUrl("")
                                    urlInput = ""
                                },
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = cardBackground,
                                    contentColor = textPrimary
                                ),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Home, "Home", Modifier.size(18.dp))
                            }
                        }
                        
                        if (uiState.isLoading) {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth(),
                                color = accentColor,
                                trackColor = Color.Transparent
                            )
                        }
                    }
                }
            }
            
            // Content
            Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                if (uiState.currentUrl.isEmpty()) {
                    DAppHomeScreen(
                        bookmarks = uiState.bookmarks,
                        recentDApps = uiState.recentDApps,
                        selectedCategory = uiState.selectedCategory,
                        onCategorySelected = { category -> viewModel.selectCategory(category) },
                        isDarkTheme = isDarkTheme,
                        urlInput = urlInput,
                        onUrlInputChange = { urlInput = it },
                        onSearch = { query ->
                            navigateTo(query)
                            focusManager.clearFocus()
                        },
                        onDAppClick = { bookmark ->
                            navigateTo(bookmark.url)
                        },
                        onSubmitDApp = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:")
                                putExtra(Intent.EXTRA_EMAIL, arrayOf("mderramus@gmail.com"))
                                putExtra(Intent.EXTRA_SUBJECT, "DApp Submission Request - MassaPay")
                                putExtra(Intent.EXTRA_TEXT, """
Hello MassaPay Team,

I would like to submit my DApp for inclusion in the MassaPay ecosystem.

DApp Name: 
DApp URL: 
Description: 
Category: 
Contact Email: 

Thank you!
                                """.trimIndent())
                            }
                            context.startActivity(Intent.createChooser(intent, "Send Email"))
                        }
                    )
                }
                
                if (uiState.currentUrl.isNotEmpty()) {
                    DAppWebView(
                        walletAddress = uiState.walletAddress,
                        onWebViewCreated = { wv -> webView = wv },
                        onUrlChanged = { url ->
                            viewModel.updateUrl(url)
                            urlInput = url
                        },
                        onTitleChanged = { title ->
                            viewModel.updatePageTitle(title)
                            if (uiState.currentUrl.isNotEmpty()) {
                                viewModel.addToRecent(uiState.currentUrl, title)
                            }
                        },
                        onLoadingChanged = { viewModel.updateLoadingState(it) },
                        onNavigationChanged = { canBack, canForward ->
                            viewModel.updateNavigationState(canBack, canForward)
                        },
                        onDAppRequest = { requestJson, sendResponse ->
                            viewModel.handleDAppRequest(requestJson, sendResponse)
                        },
                        initialUrl = uiState.currentUrl,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DAppHomeScreen(
    bookmarks: List<DAppBookmark>,
    recentDApps: List<DAppBookmark>,
    selectedCategory: DAppCategory,
    onCategorySelected: (DAppCategory) -> Unit,
    isDarkTheme: Boolean,
    urlInput: String,
    onUrlInputChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onDAppClick: (DAppBookmark) -> Unit,
    onSubmitDApp: () -> Unit
) {
    val backgroundColor = if (isDarkTheme) Color(0xFF000000) else Color(0xFFFFFFFF)
    val cardBackground = if (isDarkTheme) Color(0xFF121212) else Color(0xFFF5F5F5)
    val textPrimary = if (isDarkTheme) Color.White else Color(0xFF000000)
    val textSecondary = if (isDarkTheme) Color(0xFFB0B0B0) else Color(0xFF666666)
    val accentColor = Color(0xFF7B3FE4)  // Web3 purple
    val accentCyan = Color(0xFF00D4FF)
    val accentOrange = Color(0xFFFF9500)
    
    // Icon button colors
    val iconButtonBg = if (isDarkTheme) Color(0xFF1A1A1A) else Color(0xFFEEEEEE)
    val iconButtonTint = if (isDarkTheme) Color.White else Color(0xFF333333)
    
    var showAllDApps by remember { mutableStateOf(true) }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Search Bar
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardBackground),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = if (isDarkTheme) 0.dp else 3.dp
                )
            ) {
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = onUrlInputChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search or enter URL", color = textSecondary) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Go
                    ),
                    keyboardActions = KeyboardActions(onGo = { onSearch(urlInput) }),
                    leadingIcon = {
                        Icon(Icons.Default.Search, null, tint = textSecondary)
                    },
                    trailingIcon = {
                        if (urlInput.isNotEmpty()) {
                            IconButton(onClick = { onUrlInputChange("") }) {
                                Icon(Icons.Default.Clear, "Clear", tint = textSecondary)
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = textPrimary,
                        unfocusedTextColor = textPrimary,
                        cursorColor = accentColor
                    )
                )
            }
        }
        
        // Featured Banner - Massa Ecosystem
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardBackground),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = if (isDarkTheme) 0.dp else 4.dp
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    accentColor.copy(alpha = if (isDarkTheme) 0.2f else 0.1f),
                                    accentCyan.copy(alpha = if (isDarkTheme) 0.15f else 0.05f)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Massa Ecosystem",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Explore DApps from massa.net/ecosystem",
                                style = MaterialTheme.typography.bodyMedium,
                                color = textSecondary
                            )
                        }
                        Surface(
                            modifier = Modifier.size(60.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = if (isDarkTheme) iconButtonBg else Color.Black
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Apps,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Category Tabs
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 0.dp)
            ) {
                items(DAppCategory.values()) { category ->
                    val isSelected = category == selectedCategory
                    val categoryColor = when (category) {
                        DAppCategory.ALL -> accentColor
                        DAppCategory.OFFICIAL -> Color(0xFFFF3B30)
                        DAppCategory.DEFI -> Color(0xFF00C853)
                        DAppCategory.NFT -> Color(0xFF8B5CF6)
                    }
                    
                    Surface(
                        onClick = { onCategorySelected(category) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) categoryColor else cardBackground,
                        tonalElevation = 0.dp,
                        shadowElevation = if (isSelected) 0.dp else if (isDarkTheme) 0.dp else 2.dp
                    ) {
                        Text(
                            text = category.displayName,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else textSecondary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }
        
        // DApps Header with count
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (selectedCategory == DAppCategory.ALL) "All DApps" else selectedCategory.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
                Text(
                    text = "${bookmarks.size} apps",
                    style = MaterialTheme.typography.bodySmall,
                    color = textSecondary
                )
            }
        }
        
        // DApps Grid - Show all or limited
        item {
            val dappsToShow = if (showAllDApps) bookmarks else bookmarks.take(4)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                dappsToShow.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowItems.forEach { dapp ->
                            DAppGridCard(
                                bookmark = dapp,
                                isDarkTheme = isDarkTheme,
                                onClick = { onDAppClick(dapp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (rowItems.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        
        // Submit Your DApp Section
        item {
            Card(
                onClick = onSubmitDApp,
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardBackground),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = if (isDarkTheme) 0.dp else 3.dp
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    accentOrange.copy(alpha = if (isDarkTheme) 0.15f else 0.1f),
                                    Color(0xFFFF3B30).copy(alpha = if (isDarkTheme) 0.1f else 0.05f)
                                )
                            )
                        )
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = iconButtonBg
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = accentOrange,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Submit Your DApp",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = textPrimary
                            )
                            Text(
                                text = "Want your DApp listed? Send us an email!",
                                style = MaterialTheme.typography.bodySmall,
                                color = textSecondary
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            tint = accentOrange,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
        
        // Recent DApps
        if (recentDApps.isNotEmpty()) {
            item {
                Text(
                    text = "Recent",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
            }
            items(recentDApps.take(5)) { dapp ->
                RecentDAppCard(
                    bookmark = dapp,
                    isDarkTheme = isDarkTheme,
                    onClick = { onDAppClick(dapp) }
                )
            }
        }
        
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DAppGridCard(
    bookmark: DAppBookmark,
    isDarkTheme: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardBackground = if (isDarkTheme) Color(0xFF121212) else Color(0xFFF5F5F5)
    val textPrimary = if (isDarkTheme) Color.White else Color(0xFF000000)
    val textSecondary = if (isDarkTheme) Color(0xFFB0B0B0) else Color(0xFF666666)
    
    // Color based on category
    val category = try { DAppCategory.valueOf(bookmark.category) } catch (e: Exception) { DAppCategory.DEFI }
    val cardAccent = when (category) {
        DAppCategory.ALL -> Color(0xFF7B3FE4)
        DAppCategory.OFFICIAL -> Color(0xFFFF3B30)
        DAppCategory.DEFI -> Color(0xFF00C853)
        DAppCategory.NFT -> Color(0xFF8B5CF6)
    }
    
    Card(
        onClick = onClick,
        modifier = modifier.height(110.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isDarkTheme) 0.dp else 4.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(cardAccent.copy(alpha = 0.08f), Color.Transparent)
                    )
                )
                .padding(14.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = cardAccent.copy(alpha = 0.15f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = bookmark.name.firstOrNull()?.uppercase() ?: "?",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = cardAccent
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = bookmark.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (bookmark.description.isNotEmpty()) {
                            Text(
                                text = bookmark.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Text(
                                text = try {
                                    java.net.URI(bookmark.url).host ?: bookmark.url
                                } catch (e: Exception) { bookmark.url },
                                style = MaterialTheme.typography.bodySmall,
                                color = textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = cardAccent.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = category.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = cardAccent,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecentDAppCard(
    bookmark: DAppBookmark,
    isDarkTheme: Boolean,
    onClick: () -> Unit
) {
    val cardBackground = if (isDarkTheme) Color(0xFF121212) else Color(0xFFF5F5F5)
    val textPrimary = if (isDarkTheme) Color.White else Color(0xFF000000)
    val textSecondary = if (isDarkTheme) Color(0xFFB0B0B0) else Color(0xFF666666)
    val accentColor = Color(0xFF7B3FE4)
    
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isDarkTheme) 0.dp else 3.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(10.dp),
                color = accentColor.copy(alpha = 0.15f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = bookmark.name.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = bookmark.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = textPrimary
                )
                Text(
                    text = bookmark.url,
                    style = MaterialTheme.typography.bodySmall,
                    color = textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = textSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun DAppWebView(
    walletAddress: String,
    onWebViewCreated: (WebView) -> Unit,
    onUrlChanged: (String) -> Unit,
    onTitleChanged: (String) -> Unit,
    onLoadingChanged: (Boolean) -> Unit,
    onNavigationChanged: (Boolean, Boolean) -> Unit,
    onDAppRequest: (String, (String) -> Unit) -> Unit,
    initialUrl: String,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { ctx ->
            // Enable WebView debugging
            WebView.setWebContentsDebuggingEnabled(true)
            
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    setSupportZoom(true)
                    builtInZoomControls = true
                    displayZoomControls = false
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    // CRITICAL: Allow HTTPS sites to connect to localhost (HTTP)
                    // This is needed for MassaStation emulation since DApps like Dusa
                    // connect to http://localhost:8080 from https://app.dusa.io
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    userAgentString = "$userAgentString MassaPay/1.0"
                    allowContentAccess = true
                    allowFileAccess = true
                    // Allow insecure localhost connections
                    cacheMode = WebSettings.LOAD_DEFAULT
                }
                
                // Enable Chrome client for console logging
                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                        consoleMessage?.let {
                            Log.d("MassaPayWebView", "[${it.messageLevel()}] ${it.message()} - Line ${it.lineNumber()} of ${it.sourceId()}")
                        }
                        return true
                    }
                }
                
                addJavascriptInterface(
                    MassaPayBridge { requestJson ->
                        onDAppRequest(requestJson) { responseJson ->
                            post {
                                evaluateJavascript(
                                    "window.massaPayResponse('${responseJson.replace("'", "\\'")}')",
                                    null
                                )
                            }
                        }
                    },
                    "MassaPayBridge"
                )
                
                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        url?.let { onUrlChanged(it) }
                        onLoadingChanged(true)
                        
                        // Inject early provider script BEFORE page loads
                        Log.d("MassaPayWebView", "onPageStarted: $url - Injecting early provider")
                        val earlyScript = MassaProviderScript.getEarlyProviderScript(walletAddress)
                        view?.evaluateJavascript(earlyScript, null)
                    }
                    
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        onLoadingChanged(false)
                        onNavigationChanged(canGoBack(), canGoForward())
                        
                        Log.d("MassaPayWebView", "onPageFinished: $url - Injecting full provider")
                        val script = MassaProviderScript.getProviderScript(walletAddress)
                        evaluateJavascript(script, null)
                    }
                    
                    // Intercept HTTP requests to emulate MassaStation
                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): WebResourceResponse? {
                        val url = request?.url?.toString() ?: return null
                        
                        // Log ALL https requests to station.massa domain
                        if (url.contains("station.massa") || url.contains("massa-station") || url.contains("localhost:8080")) {
                            Log.d("MassaPayWebView", "Intercepting MassaStation request: $url")
                            return handleMassaStationRequest(url, request, walletAddress)
                        }
                        
                        // Log other potentially relevant URLs for debugging
                        if (url.contains("massa") && !url.contains("massalabs.com") && !url.contains("massa.net/api")) {
                            Log.d("MassaPayWebView", "Potential Massa URL not intercepted: $url")
                        }
                        
                        return null // Let other requests pass through
                    }
                    
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        val url = request?.url?.toString() ?: return false
                        
                        // Intercept wallet installation redirects
                        // When DApps like Dusa redirect to wallet installation pages,
                        // we inject our wallet instead
                        if (url.contains("bearby.io") || 
                            url.contains("station.massa.net") ||
                            url.contains("chrome.google.com/webstore") ||
                            url.contains("addons.mozilla.org")) {
                            
                            // Instead of redirecting, trigger wallet connection
                            view?.evaluateJavascript("""
                                (function() {
                                    console.log('[MassaPay] Intercepted wallet redirect to: """ + url + """');
                                    console.log('[MassaPay] Triggering MassaConnect Wallet connection...');
                                    
                                    // Always trigger the connection flow to show approval dialog
                                    // This ensures user consent before connecting to DApp
                                    if (window.web3 && window.web3.wallet && window.web3.wallet.connect) {
                                        console.log('[MassaPay] Calling web3.wallet.connect()...');
                                        window.web3.wallet.connect().then(function(result) {
                                            console.log('[MassaPay] Connection approved:', result);
                                            var addr = window.web3.wallet.account.base58;
                                            console.log('[MassaPay] Connected address:', addr);
                                            
                                            // After approval, force update the DApp
                                            if (window.massaPayForceUpdate && addr) {
                                                window.massaPayForceUpdate(addr);
                                            }
                                        }).catch(function(err) {
                                            console.error('[MassaPay] Connection rejected or error:', err);
                                        });
                                    } else {
                                        console.error('[MassaPay] web3.wallet.connect not available');
                                    }
                                })();
                            """.trimIndent(), null)
                            
                            return true // Block the redirect
                        }
                        
                        // Let WebView handle http/https URLs
                        if (url.startsWith("http://") || url.startsWith("https://")) {
                            return false
                        }
                        // Block other schemes
                        return true
                    }
                }
                
                webChromeClient = object : WebChromeClient() {
                    override fun onReceivedTitle(view: WebView?, title: String?) {
                        super.onReceivedTitle(view, title)
                        title?.let { onTitleChanged(it) }
                    }
                    
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        super.onProgressChanged(view, newProgress)
                        onLoadingChanged(newProgress < 100)
                    }
                }
                
                onWebViewCreated(this)
                
                // Load the initial URL
                if (initialUrl.isNotEmpty()) {
                    loadUrl(initialUrl)
                }
            }
        },
        modifier = modifier
    )
}

private class MassaPayBridge(private val onMessage: (String) -> Unit) {
    @android.webkit.JavascriptInterface
    fun postMessage(message: String) {
        android.util.Log.d("MassaPayBridge", "postMessage received: $message")
        onMessage(message)
    }
}
