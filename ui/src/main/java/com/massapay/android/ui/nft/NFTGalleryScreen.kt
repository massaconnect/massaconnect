package com.massapay.android.ui.nft

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.SubcomposeAsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.massapay.android.core.model.NFT

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NFTGalleryScreen(
    onNFTClick: (NFT) -> Unit,
    onClose: () -> Unit,
    isDarkTheme: Boolean,
    viewModel: NFTGalleryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Define colors based on theme
    val backgroundColor = if (isDarkTheme) Color(0xFF0D0D15) else Color.White
    val contentColor = if (isDarkTheme) Color.White else Color.Black
    
    // State for import dialog
    var showImportDialog by remember { mutableStateOf(false) }
    var contractAddressInput by remember { mutableStateOf("") }
    var collectionNameInput by remember { mutableStateOf("") }

    // Show success message when collection is added
    LaunchedEffect(uiState.addCollectionSuccess) {
        if (uiState.addCollectionSuccess) {
            viewModel.resetAddCollectionState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "NFT Gallery",
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.Default.ArrowBack, 
                            contentDescription = "Back",
                            tint = contentColor
                        )
                    }
                },
                actions = {
                    // Import collection button
                    IconButton(onClick = { showImportDialog = true }) {
                        Icon(
                            Icons.Default.Add, 
                            contentDescription = "Import Collection",
                            tint = contentColor
                        )
                    }
                    // Refresh button
                    IconButton(onClick = { viewModel.refreshNFTs() }) {
                        Icon(
                            Icons.Default.Refresh, 
                            contentDescription = "Refresh",
                            tint = contentColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundColor,
                    titleContentColor = contentColor,
                    navigationIconContentColor = contentColor,
                    actionIconContentColor = contentColor
                )
            )
        },
        containerColor = backgroundColor
    ) { padding ->
    
        // Import Collection Dialog
        if (showImportDialog) {
            AlertDialog(
                onDismissRequest = { 
                    showImportDialog = false
                    contractAddressInput = ""
                    collectionNameInput = ""
                },
                title = { 
                    Text(
                        "Import NFT Collection",
                        fontWeight = FontWeight.Bold
                    ) 
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Enter the smart contract address of the NFT collection you want to import.",
                            fontSize = 14.sp,
                            color = if (isDarkTheme) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.7f)
                        )
                        
                        OutlinedTextField(
                            value = contractAddressInput,
                            onValueChange = { contractAddressInput = it },
                            label = { Text("Contract Address (AS...)") },
                            placeholder = { Text("AS1...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        OutlinedTextField(
                            value = collectionNameInput,
                            onValueChange = { collectionNameInput = it },
                            label = { Text("Collection Name (optional)") },
                            placeholder = { Text("My NFT Collection") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        if (uiState.error != null && uiState.isAddingCollection.not()) {
                            Text(
                                uiState.error ?: "",
                                color = Color.Red,
                                fontSize = 12.sp
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (contractAddressInput.isNotBlank()) {
                                viewModel.addCustomCollection(
                                    contractAddressInput.trim(),
                                    collectionNameInput.takeIf { it.isNotBlank() }
                                )
                                showImportDialog = false
                                contractAddressInput = ""
                                collectionNameInput = ""
                            }
                        },
                        enabled = contractAddressInput.startsWith("AS") && !uiState.isAddingCollection
                    ) {
                        if (uiState.isAddingCollection) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            Text("Import")
                        }
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { 
                            showImportDialog = false
                            contractAddressInput = ""
                            collectionNameInput = ""
                        }
                    ) {
                        Text("Cancel")
                    }
                },
                containerColor = if (isDarkTheme) Color(0xFF1A1A2E) else Color.White
            )
        }
    
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(backgroundColor)
        ) {
            when {
                uiState.isLoading && uiState.nfts.isEmpty() -> {
                    // Loading skeleton with shimmer effect
                    LoadingSkeleton(isDarkTheme = isDarkTheme)
                }
                uiState.nfts.isEmpty() -> {
                    EmptyGallery(
                        modifier = Modifier.align(Alignment.Center),
                        isDarkTheme = isDarkTheme
                    )
                }
                else -> {
                    val context = LocalContext.current
                    
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = uiState.nfts,
                            key = { "${it.contractAddress}_${it.tokenId}" }
                        ) { nft ->
                            NFTCard(
                                nft = nft,
                                onClick = { onNFTClick(nft) },
                                isDarkTheme = isDarkTheme
                            )
                        }
                        
                        // Submit Collection Banner - spans full width
                        item(span = { GridItemSpan(2) }) {
                            SubmitCollectionBanner(
                                isDarkTheme = isDarkTheme,
                                onClick = {
                                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                                        data = Uri.parse("mailto:")
                                        putExtra(Intent.EXTRA_EMAIL, arrayOf("mderramus@gmail.com"))
                                        putExtra(Intent.EXTRA_SUBJECT, "NFT Collection Submission - MassaPay")
                                        putExtra(Intent.EXTRA_TEXT, """
Hello MassaPay Team,

I would like to submit my NFT collection for verification in MassaPay.

Collection Name: 
Contract Address (AS...): 
Description: 
Metadata API URL: 
Image Base URL: 
Total Supply: 
Website/Social Links: 
Contact Email: 

Thank you!
                                        """.trimIndent())
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Send Email"))
                                }
                            )
                        }
                    }
                }
            }
            
            // Loading indicator overlay
            if (uiState.isLoading && uiState.nfts.isNotEmpty()) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp)
                        .size(24.dp),
                    strokeWidth = 2.dp
                )
            }
        }

        // Error snackbar
        AnimatedVisibility(
            visible = uiState.error != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            uiState.error?.let { error ->
                ErrorSnackbar(
                    error = error,
                    onRetry = { viewModel.loadNFTs() }
                )
            }
        }

        // Transfer success feedback
        if (uiState.transferSuccess) {
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(2000)
                viewModel.resetTransferState()
            }
        }
    }
}

@Composable
private fun LoadingSkeleton(isDarkTheme: Boolean = false) {
    val shimmerColors = if (isDarkTheme) {
        listOf(
            Color(0xFF1A1A2E),
            Color(0xFF2A2A4E),
            Color(0xFF1A1A2E)
        )
    } else {
        listOf(
            Color(0xFFE0E0E0),
            Color(0xFFF5F5F5),
            Color(0xFFE0E0E0)
        )
    }
    
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )
    
    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 500f, 0f),
        end = Offset(translateAnim, 0f)
    )
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Loading status message
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = if (isDarkTheme) Color(0xFF4DD0E1) else Color(0xFF0097A7)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Searching for your NFTs...",
                fontSize = 14.sp,
                color = if (isDarkTheme) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.6f)
            )
        }
        
        // Skeleton grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(6) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.8f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDarkTheme) Color(0xFF1A1A2E) else Color.White
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = if (isDarkTheme) 0.dp else 2.dp
                    )
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Image placeholder with shimmer
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .background(brush)
                        )
                        
                        // Text placeholders
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            // Title placeholder
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.7f)
                                    .height(16.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(brush)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            // Subtitle placeholder
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.5f)
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(brush)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NFTCard(
    nft: NFT,
    onClick: () -> Unit,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val cardColor = if (isDarkTheme) {
        Color(0xFF1A1A2E)
    } else {
        Color.White
    }
    
    // Press animation
    var isPressed by remember { mutableStateOf(false) }
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessHigh
        ),
        label = "nftCardScale"
    )
    
    // Entrance animation
    var isVisible by remember { mutableStateOf(false) }
    val animatedAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(400),
        label = "nftCardAlpha"
    )
    val offsetY by androidx.compose.animation.core.animateIntAsState(
        targetValue = if (isVisible) 0 else 30,
        animationSpec = androidx.compose.animation.core.tween(400),
        label = "nftCardOffsetY"
    )
    
    LaunchedEffect(Unit) {
        isVisible = true
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.8f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = animatedAlpha
                translationY = offsetY.toFloat()
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onClick() }
                )
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isDarkTheme) 2.dp else 6.dp,
            pressedElevation = 0.dp
        )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // NFT Image or Ghost Placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(
                        if (isDarkTheme) Color(0xFF2D2D44) else Color(0xFFF5F5F5)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (nft.imageUrl.isNotBlank()) {
                    // Real image
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(nft.imageUrl)
                            .crossfade(true)
                            .size(coil.size.Size.ORIGINAL)
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .allowHardware(false) // Disable hardware bitmaps for better compatibility
                            .build(),
                        contentDescription = nft.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        loading = {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp),
                                    strokeWidth = 2.dp,
                                    color = if (isDarkTheme) Color.White else Color.Black
                                )
                            }
                        },
                        error = {
                            // Show broken image icon on error
                            Icon(
                                Icons.Default.BrokenImage,
                                contentDescription = "Error loading image",
                                modifier = Modifier.size(48.dp),
                                tint = if (isDarkTheme) Color.White.copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.4f)
                            )
                        }
                    )
                } else {
                    // Placeholder for NFTs without image
                    Icon(
                        Icons.Default.Image,
                        contentDescription = "No image",
                        modifier = Modifier.size(48.dp),
                        tint = if (isDarkTheme) Color.White.copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.4f)
                    )
                }

                // Collection badge if verified
                nft.collection?.let { collection ->
                    if (collection.verified) {
                        Box(
                            modifier = Modifier
                                .padding(8.dp)
                                .align(Alignment.TopEnd)
                        ) {
                            Icon(
                                Icons.Default.Verified,
                                contentDescription = "Verified",
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(Color.White, CircleShape)
                                    .padding(2.dp),
                                tint = Color(0xFF2196F3)
                            )
                        }
                    }
                }
            }

            // NFT Info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = nft.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isDarkTheme) Color.White else Color.Black
                )
                
                nft.collection?.let { collection ->
                    Text(
                        text = collection.name,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (isDarkTheme) Color.White.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

/**
 * Empty state screen with FontAwesome-style ghost icon
 */
@Composable
fun EmptyGallery(
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = false
) {
    // Ghost: black in light mode, white in dark mode
    val ghostColor = if (isDarkTheme) Color.White else Color.Black
    
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            // FontAwesome-style Ghost Icon
            GhostIcon(
                color = ghostColor.copy(alpha = 0.12f),
                modifier = Modifier.size(180.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "No NFTs Found",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = ghostColor
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "This wallet doesn't have any NFTs yet.\nWhen you collect NFTs on Massa,\nthey will appear here.",
                style = MaterialTheme.typography.bodyLarge,
                color = ghostColor.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )
        }
    }
}

/**
 * Custom Ghost icon similar to FontAwesome's ghost solid icon
 * https://fontawesome.com/icons/ghost?s=solid
 */
@Composable
private fun GhostIcon(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        
        val path = Path().apply {
            // Start from bottom left wavy part
            moveTo(width * 0.05f, height)
            
            // Left wave
            lineTo(width * 0.05f, height * 0.85f)
            quadraticBezierTo(
                width * 0.12f, height * 0.92f,
                width * 0.18f, height * 0.85f
            )
            quadraticBezierTo(
                width * 0.25f, height * 0.78f,
                width * 0.32f, height * 0.85f
            )
            quadraticBezierTo(
                width * 0.39f, height * 0.92f,
                width * 0.46f, height * 0.85f
            )
            quadraticBezierTo(
                width * 0.50f, height * 0.80f,
                width * 0.54f, height * 0.85f
            )
            quadraticBezierTo(
                width * 0.61f, height * 0.92f,
                width * 0.68f, height * 0.85f
            )
            quadraticBezierTo(
                width * 0.75f, height * 0.78f,
                width * 0.82f, height * 0.85f
            )
            quadraticBezierTo(
                width * 0.88f, height * 0.92f,
                width * 0.95f, height * 0.85f
            )
            
            // Right side up
            lineTo(width * 0.95f, height * 0.40f)
            
            // Top rounded part (head)
            quadraticBezierTo(
                width * 0.95f, height * 0.05f,
                width * 0.50f, height * 0.05f
            )
            quadraticBezierTo(
                width * 0.05f, height * 0.05f,
                width * 0.05f, height * 0.40f
            )
            
            // Close path
            close()
        }
        
        drawPath(
            path = path,
            color = color,
            style = Fill
        )
        
        // Left eye
        drawOval(
            color = color.copy(alpha = 1f),
            topLeft = Offset(width * 0.25f, height * 0.30f),
            size = Size(width * 0.15f, height * 0.18f)
        )
        
        // Right eye
        drawOval(
            color = color.copy(alpha = 1f),
            topLeft = Offset(width * 0.60f, height * 0.30f),
            size = Size(width * 0.15f, height * 0.18f)
        )
        
        // Mouth (small oval)
        drawOval(
            color = color.copy(alpha = 1f),
            topLeft = Offset(width * 0.40f, height * 0.55f),
            size = Size(width * 0.20f, height * 0.12f)
        )
    }
}

@Composable
private fun ErrorSnackbar(
    error: String,
    onRetry: () -> Unit
) {
    Snackbar(
        modifier = Modifier.padding(16.dp),
        action = {
            TextButton(onClick = onRetry) {
                Text("Retry", color = Color.White)
            }
        },
        containerColor = MaterialTheme.colorScheme.error
    ) {
        Text(error, color = Color.White)
    }
}

@Composable
private fun SubmitCollectionBanner(
    isDarkTheme: Boolean,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "banner")
    
    // Animated gradient shift
    val animatedOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradientShift"
    )
    
    // Floating particles animation
    val floatAnimation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float"
    )
    
    // Glow pulse animation
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )
    
    val gradientColors = listOf(
        Color(0xFF6366F1), // Indigo
        Color(0xFF8B5CF6), // Purple
        Color(0xFFEC4899), // Pink
        Color(0xFFF59E0B), // Amber
        Color(0xFF10B981), // Emerald
        Color(0xFF6366F1)  // Back to Indigo
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            // Animated gradient background
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val width = size.width
                val height = size.height
                
                // Main gradient background
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = gradientColors,
                        start = Offset(width * animatedOffset, 0f),
                        end = Offset(width * (1 - animatedOffset), height)
                    ),
                    cornerRadius = CornerRadius(20.dp.toPx())
                )
                
                // Overlay darker gradient for depth
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.1f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.3f)
                        )
                    ),
                    cornerRadius = CornerRadius(20.dp.toPx())
                )
                
                // Floating geometric shapes
                val particleOffset = floatAnimation * 20f
                
                // Diamond shape 1
                val diamond1 = Path().apply {
                    moveTo(width * 0.15f, height * 0.3f + particleOffset)
                    lineTo(width * 0.18f, height * 0.4f + particleOffset)
                    lineTo(width * 0.15f, height * 0.5f + particleOffset)
                    lineTo(width * 0.12f, height * 0.4f + particleOffset)
                    close()
                }
                drawPath(diamond1, Color.White.copy(alpha = 0.3f))
                
                // Circle glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = glowPulse * 0.4f),
                            Color.Transparent
                        ),
                        center = Offset(width * 0.85f, height * 0.25f),
                        radius = 60f
                    ),
                    center = Offset(width * 0.85f, height * 0.25f),
                    radius = 60f
                )
                
                // Small floating squares
                drawRect(
                    color = Color.White.copy(alpha = 0.2f),
                    topLeft = Offset(width * 0.75f, height * 0.6f - particleOffset),
                    size = Size(15f, 15f)
                )
                
                drawRect(
                    color = Color.White.copy(alpha = 0.15f),
                    topLeft = Offset(width * 0.25f, height * 0.7f + particleOffset * 0.5f),
                    size = Size(10f, 10f)
                )
                
                // Decorative lines
                drawLine(
                    color = Color.White.copy(alpha = 0.3f),
                    start = Offset(width * 0.05f, height * 0.8f),
                    end = Offset(width * 0.25f, height * 0.8f),
                    strokeWidth = 2f,
                    cap = StrokeCap.Round
                )
                
                drawLine(
                    color = Color.White.copy(alpha = 0.3f),
                    start = Offset(width * 0.75f, height * 0.85f),
                    end = Offset(width * 0.95f, height * 0.85f),
                    strokeWidth = 2f,
                    cap = StrokeCap.Round
                )
                
                // Star/sparkle effect
                val starCenter = Offset(width * 0.9f, height * 0.4f - particleOffset * 0.5f)
                val starSize = 8f
                drawLine(
                    color = Color.White.copy(alpha = glowPulse),
                    start = Offset(starCenter.x - starSize, starCenter.y),
                    end = Offset(starCenter.x + starSize, starCenter.y),
                    strokeWidth = 2f
                )
                drawLine(
                    color = Color.White.copy(alpha = glowPulse),
                    start = Offset(starCenter.x, starCenter.y - starSize),
                    end = Offset(starCenter.x, starCenter.y + starSize),
                    strokeWidth = 2f
                )
            }
            
            // Content overlay
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // NFT icon with glow effect
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    // Glow behind icon
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(
                                Color.White.copy(alpha = glowPulse * 0.3f),
                                CircleShape
                            )
                    )
                    Icon(
                        imageVector = Icons.Default.Collections,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "🎨 Submit Your Collection",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(6.dp))
                
                Text(
                    text = "Get your NFT collection verified on MassaPay",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // CTA Button
                Surface(
                    modifier = Modifier
                        .wrapContentWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.2f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Apply Now",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}