package com.rudra.swiggymind.ui.chat

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rudra.swiggymind.ui.theme.SwiggyColors
import com.rudra.swiggymind.domain.model.Restaurant
import com.rudra.swiggymind.domain.model.RestaurantRecommendation
import com.rudra.swiggymind.AppConstants
import com.rudra.swiggymind.getPlatform
import com.rudra.swiggymind.Res
import org.jetbrains.compose.resources.painterResource
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onViewDnaClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val totalConvos by viewModel.totalConversations.collectAsStateWithLifecycle()
    val totalRecs by viewModel.totalRecommendations.collectAsStateWithLifecycle()
    
    var inputText by remember { mutableStateOf("") }
    
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    var showAboutSheet by remember { mutableStateOf(false) }
    var showProfileSheet by remember { mutableStateOf(false) }
    
    val haptic = LocalHapticFeedback.current
    val uriHandler = LocalUriHandler.current

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = SwiggyColors.Surface,
                drawerShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "SwiggyMind",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.headlineMedium,
                    color = SwiggyColors.Primary,
                    fontWeight = FontWeight.ExtraBold
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(8.dp))
                
                NavigationDrawerItem(
                    label = { Text("New Chat", fontWeight = FontWeight.Bold) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        viewModel.startNewChat()
                    },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                
                NavigationDrawerItem(
                    label = { Text("About SwiggyMind", fontWeight = FontWeight.Bold) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        showAboutSheet = true
                    },
                    icon = { Icon(Icons.Default.Info, contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                
                NavigationDrawerItem(
                    label = { Text("Apply to Builders Club", fontWeight = FontWeight.Bold) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        uriHandler.openUri(AppConstants.BUILDERS_CLUB_URL)
                    },
                    icon = { Icon(Icons.AutoMirrored.Filled.Launch, contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
    
                Spacer(modifier = Modifier.weight(1f))

                Surface(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    color = SwiggyColors.PrimaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, SwiggyColors.Primary.copy(alpha = 0.15f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = SwiggyColors.Primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("SwiggyMind", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = SwiggyColors.Primary)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "A conversational intelligence layer for Swiggy. Understands your mood, budget, and cravings to give hyper-personalised picks.",
                            fontSize = 11.sp, color = SwiggyColors.OnBackground, lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Swiggy Mind Intelligence · 4-layer response guarantee",
                            fontSize = 10.sp, color = SwiggyColors.Subtle, fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    ) {
    val hasDismissedOfflineBanner by viewModel.hasDismissedOfflineBanner.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            "SwiggyMind",
                            fontWeight = FontWeight.ExtraBold,
                            color = SwiggyColors.Primary,
                            fontSize = 22.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showProfileSheet = true }) {
                            Icon(Icons.Default.AccountCircle, contentDescription = "Account", tint = SwiggyColors.OnBackground)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = SwiggyColors.Surface)
                )
                
                if (!hasDismissedOfflineBanner && uiState.aiStatus == AiStatus.FALLBACK && !uiState.isMcpEnabled) {
                    Surface(
                        color = Color(0xFFFFF8E1),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "⚡ Showing smart defaults · AI unavailable",
                                fontSize = 12.sp,
                                color = Color(0xFFF57F17),
                                modifier = Modifier.weight(1f),
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { viewModel.dismissOfflineBanner() }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp), tint = Color(0xFFF57F17))
                            }
                        }
                    }
                }
            }
        },
            bottomBar = {
                Column(modifier = Modifier.background(SwiggyColors.Surface)) {
                    HorizontalDivider(thickness = 1.dp, color = SwiggyColors.Border)
                    if (uiState.messages.isNotEmpty()) {
                        QuickFiltersRow(onFilterClick = { viewModel.sendMessage("Show me $it options") })
                    }
                    InputBar(
                        text = inputText,
                        onTextChange = { inputText = it },
                        onSend = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.sendMessage(inputText)
                            inputText = ""
                        },
                        isLoading = uiState.isLoading
                    )
                }
            }
        ) { paddingValues ->
            if (uiState.messages.isEmpty() && !uiState.isLoading) {
                EmptyChatState(
                    currentCity = uiState.currentCity,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(SwiggyColors.Background),
                    aiStatus = uiState.aiStatus,
                    onSuggestionClick = { suggestion ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.sendMessage(suggestion)
                    }
                )
            } else {
                val listState = rememberLazyListState()
                
                LaunchedEffect(uiState.messages.size, uiState.isLoading) {
                    if (uiState.messages.isNotEmpty()) {
                        listState.animateScrollToItem(
                            if (uiState.isLoading) uiState.messages.size else uiState.messages.size - 1
                        )
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(SwiggyColors.Background),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(uiState.messages) { message ->
                        ChatBubble(
                            message = message,
                            onRefine = {
                                viewModel.sendMessage("Show me something different")
                            }
                        )
                    }
                    if (uiState.isLoading) {
                        item {
                            TypingIndicator()
                        }
                    }
                }
            }
        }
    }

    if (showAboutSheet) {
        AboutBottomSheet(onDismiss = { showAboutSheet = false })
    }

    if (showProfileSheet) {
        ProfileBottomSheet(
            totalConvos = totalConvos,
            totalRecs = totalRecs,
            onClearHistory = viewModel::clearAllSessions,
            onViewDna = onViewDnaClick,
            onDismiss = { showProfileSheet = false }
        )
    }
}

@Composable
private fun EmptyChatState(
    currentCity: String,
    modifier: Modifier = Modifier,
    aiStatus: AiStatus = AiStatus.CLOUD,
    onSuggestionClick: (String) -> Unit
) {
    Column(
        modifier = modifier.padding(20.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "What should we order today?",
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            color = SwiggyColors.OnBackground
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val alpha by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 0.3f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "alpha"
            )

            val dotColor = when (aiStatus) {
                AiStatus.CLOUD, AiStatus.MCP -> SwiggyColors.Success
                AiStatus.FALLBACK -> Color(0xFFF57F17)
                AiStatus.OFFLINE -> Color(0xFFC62828)
            }
            
            val statusText = when (aiStatus) {
                AiStatus.MCP -> "Live MCP · Builders Club"
                AiStatus.CLOUD -> "AI-Powered · OpenRouter"
                AiStatus.FALLBACK -> "Smart defaults active"
                AiStatus.OFFLINE -> "Offline mode"
            }
            
            val textColor = when (aiStatus) {
                AiStatus.CLOUD, AiStatus.MCP -> SwiggyColors.Success
                else -> SwiggyColors.Subtle
            }

            Box(modifier = Modifier.size(8.dp).background(dotColor.copy(alpha = if (aiStatus == AiStatus.CLOUD) alpha else 1f), CircleShape))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = statusText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Tell me your mood, craving, budget, or occasion — I'll find the perfect match.",
            fontSize = 15.sp,
            color = SwiggyColors.Subtle,
            lineHeight = 21.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "📍 Showing results near $currentCity",
            fontSize = 12.sp,
            color = SwiggyColors.Subtle
        )
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Trending in $currentCity →",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = SwiggyColors.Subtle
        )
        Spacer(modifier = Modifier.height(12.dp))
        
        val trending = listOf("Gujarati Thali", "Pav Bhaji", "Cold Coffee", "Biryani")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(trending) { tag ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SwiggyColors.Surface,
                    border = BorderStroke(1.dp, SwiggyColors.Border),
                    onClick = { onSuggestionClick("Show me $tag options") }
                ) {
                    Text(
                        text = tag,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        fontSize = 12.sp,
                        color = SwiggyColors.OnBackground
                    )
                }
            }
        }
    }
}

@Composable
fun QuickFiltersRow(onFilterClick: (String) -> Unit) {
    val filters = listOf("Under ₹150", "Veg only", "Quick delivery", "High rating")
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(filters) { filter ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SwiggyColors.Surface,
                border = BorderStroke(1.dp, SwiggyColors.Border),
                onClick = { onFilterClick(filter) }
            ) {
                Text(
                    text = filter,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    fontSize = 12.sp,
                    color = SwiggyColors.OnBackground
                )
            }
        }
    }
}

@Composable
fun ChatBubble(
    message: ChatMessage,
    onRefine: () -> Unit = {}
) {
    val isAi = !message.isFromUser
    val isLive = !message.isAiFallback && !message.isRelaxed

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isFromUser) Alignment.End else Alignment.Start
    ) {
        if (isAi) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = SwiggyColors.Primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "SwiggyMind",
                    style = MaterialTheme.typography.labelSmall,
                    color = SwiggyColors.Subtle,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )

                Text(
                    text = "  |  ",
                    style = MaterialTheme.typography.labelSmall,
                    color = SwiggyColors.Border,
                    fontWeight = FontWeight.Light
                )

                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(
                            if (isLive) SwiggyColors.Success else SwiggyColors.Subtle,
                            CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = when {
                        message.isMcp -> "✦ Live MCP"
                        isLive -> "✦ Mind Intelligence"
                        else -> "Smart Defaults"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isLive || message.isMcp) SwiggyColors.Success else SwiggyColors.Subtle
                )
            }
        }

        Surface(
            shape = if (message.isFromUser)
                RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp)
            else
                RoundedCornerShape(12.dp),
            color = if (message.isFromUser) SwiggyColors.Primary else Color.White,
            border = if (!message.isFromUser)
                BorderStroke(1.dp, Color(0xFFE0E0E0)) else null,
            shadowElevation = 0.dp
        ) {
            Text(
                text = message.text,
                color = if (message.isFromUser) SwiggyColors.OnPrimary else SwiggyColors.OnBackground,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                modifier = Modifier.padding(
                    horizontal = 12.dp, 
                    vertical = 10.dp
                )
            )
        }

        if (message.isGrocery) {
            Spacer(modifier = Modifier.height(12.dp))
            GroceryListCard(
                ingredients = message.ingredients,
                isRecommended = message.recommendations.isEmpty() // Only true if it's just items, not a store
            )
        }

        if (message.recommendations.isNotEmpty() && !message.isGrocery) {
            Spacer(modifier = Modifier.height(12.dp))
            var cardsVisible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { cardsVisible = true }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(start = 4.dp, end = 4.dp, bottom = 12.dp)
            ) {
                items(message.recommendations.size) { index ->
                    val recommendation = message.recommendations[index]
                    androidx.compose.animation.AnimatedVisibility(
                        visible = cardsVisible,
                        enter = slideInVertically(
                            initialOffsetY = { it / 2 },
                            animationSpec = tween(durationMillis = 400, delayMillis = index * 150)
                        ) + fadeIn(animationSpec = tween(durationMillis = 400, delayMillis = index * 150))
                    ) {
                        RecommendationCard(
                            restaurant = recommendation.restaurant,
                            reasoning = recommendation.reason,
                            isAiFallback = message.isAiFallback,
                            isRelaxed = message.isRelaxed,
                            isMcp = message.isMcp
                        )
                    }
                }
            }

            TextButton(
                onClick = onRefine,
                modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
            ) {
                Text("Refine this →", fontSize = 13.sp, color = SwiggyColors.Primary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun GroceryListCard(ingredients: List<String>, isRecommended: Boolean = false) {
    val uriHandler = LocalUriHandler.current
    
    Card(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                if (isRecommended) "Quick Picks" else "Shopping List", 
                fontWeight = FontWeight.Bold, 
                fontSize = 18.sp, 
                color = SwiggyColors.OnBackground
            )
            Text(
                if (isRecommended) "Available on Instamart" else "Here's what you'll need", 
                fontSize = 13.sp, 
                color = SwiggyColors.Subtle
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            ingredients.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "• $item",
                        fontSize = 14.sp,
                        color = SwiggyColors.OnBackground
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = {
                    uriHandler.openUri(AppConstants.INSTAMART_URL)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = SwiggyColors.Primary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    if (isRecommended) "Order on Instamart →" else "Shop on Instamart →", 
                    color = Color.White, 
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun RecommendationCard(
    restaurant: Restaurant,
    reasoning: String,
    isAiFallback: Boolean = false,
    isRelaxed: Boolean = false,
    isMcp: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    Box(modifier = Modifier.width(280.dp).padding(vertical = 4.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth().clickable {
                uriHandler.openUri("${AppConstants.SWIGGY_SEARCH_URL}${restaurant.name}")
            },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SwiggyColors.Surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column {
                AsyncImage(
                    model = restaurant.imageUrl,
                    contentDescription = restaurant.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.height(140.dp).fillMaxWidth()
                )
                
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = restaurant.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = SwiggyColors.OnBackground,
                            modifier = Modifier.weight(1f)
                        )
                        
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = SwiggyColors.SuccessLight
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = restaurant.rating.toString(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SwiggyColors.Success
                                )
                                Icon(Icons.Default.Star, contentDescription = null, tint = SwiggyColors.Success, modifier = Modifier.size(10.dp))
                            }
                        }
                    }
                    
                    Text(
                        text = "${restaurant.cuisine.take(2).joinToString(", ")} • ${restaurant.deliveryTimeMinutes} mins",
                        fontSize = 12.sp,
                        color = SwiggyColors.Subtle,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = when {
                            isMcp -> SwiggyColors.Primary.copy(alpha = 0.1f)
                            isAiFallback || isRelaxed -> Color(0xFF757575).copy(alpha = 0.1f)
                            else -> SwiggyColors.Primary.copy(alpha = 0.1f)
                        },
                        border = BorderStroke(0.5.dp, if (isAiFallback || isRelaxed && !isMcp) Color(0xFF757575).copy(alpha = 0.2f) else SwiggyColors.Primary.copy(alpha = 0.2f))
                    ) {
                        Text(
                            when {
                                isMcp -> "MCP-Powered"
                                isAiFallback || isRelaxed -> "Top Rated"
                                else -> "AI-Powered"
                            },
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = when {
                                isMcp -> SwiggyColors.Primary
                                isAiFallback || isRelaxed -> Color(0xFF757575)
                                else -> SwiggyColors.Primary
                            }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expanded = !expanded }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Why this match?",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = SwiggyColors.OnBackground
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = SwiggyColors.Subtle
                        )
                    }
                    
                    androidx.compose.animation.AnimatedVisibility(visible = expanded) {
                        Text(
                            text = reasoning,
                            fontSize = 12.sp,
                            color = SwiggyColors.Subtle,
                            modifier = Modifier.padding(top = 4.dp),
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
        
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 8.dp, y = (-8).dp)
                .size(32.dp),
            shape = CircleShape,
            color = SwiggyColors.Surface,
            shadowElevation = 4.dp,
            onClick = {
                val searchUrl = "${AppConstants.SWIGGY_SEARCH_URL}${restaurant.name}"
                getPlatform().shareText("Check out ${restaurant.name} on Swiggy! $searchUrl")
            }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Share,
                    contentDescription = "Share",
                    modifier = Modifier.size(16.dp),
                    tint = SwiggyColors.Subtle
                )
            }
        }
    }
}

@Composable
fun InputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    isLoading: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = RoundedCornerShape(25.dp),
                color = Color(0xFFF1F2F4)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = "Voice Input",
                        tint = SwiggyColors.Subtle
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (text.isEmpty()) {
                            Text(
                                "Tell SwiggyMind what you need..",
                                color = SwiggyColors.Subtle,
                                fontSize = 14.sp
                            )
                        }
                        androidx.compose.foundation.text.BasicTextField(
                            value = text,
                            onValueChange = onTextChange,
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(fontSize = 14.sp, color = SwiggyColors.OnBackground)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            FloatingActionButton(
                onClick = onSend,
                containerColor = SwiggyColors.Primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.size(50.dp),
                elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
            }
        }
    }
}

@Composable
fun TypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")

    Row(
        modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.AutoAwesome,
            contentDescription = null,
            tint = SwiggyColors.Primary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
            shadowElevation = 1.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(0, 150, 300).forEach { delay ->
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 0.3f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600, delayMillis = delay),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "dot_$delay"
                    )
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(SwiggyColors.Primary.copy(alpha = alpha), CircleShape)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutBottomSheet(onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        sheetState = rememberModalBottomSheetState()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "About SwiggyMind",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "SwiggyMind is an AI-powered food discovery assistant that helps you find the perfect meal, grocery, or dineout venue using advanced reasoning and the Swiggy ecosystem.",
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = SwiggyColors.Subtle,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(24.dp))
            
            val uriHandler = LocalUriHandler.current
            Button(
                onClick = { uriHandler.openUri(AppConstants.GITHUB_URL) },
                colors = ButtonDefaults.buttonColors(containerColor = SwiggyColors.OnBackground),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Code, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("View on GitHub")
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Built on Swiggy Builders Club",
                style = MaterialTheme.typography.labelSmall,
                color = SwiggyColors.Primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileBottomSheet(
    totalConvos: Int,
    totalRecs: Int,
    onClearHistory: () -> Unit,
    onViewDna: () -> Unit,
    onDismiss: () -> Unit
) {
    var showClearDialog by remember { mutableStateOf(false) }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear all history?", fontWeight = FontWeight.Bold) },
            text = { Text("This will permanently delete all your conversations, recommendations, and Food DNA data. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showClearDialog = false
                    onClearHistory()
                    onDismiss()
                }) {
                    Text("Clear everything", color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Keep my history", color = SwiggyColors.Subtle)
                }
            },
            containerColor = SwiggyColors.Surface
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SwiggyColors.Surface,
        sheetState = rememberModalBottomSheetState()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(bottom = 32.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(64.dp),
                    shape = RoundedCornerShape(32.dp),
                    color = SwiggyColors.Background
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.padding(12.dp),
                        tint = SwiggyColors.Subtle
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        "SwiggyMind User",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text("Food Explorer", color = SwiggyColors.Subtle)
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatItem("Conversations", totalConvos.toString())
                StatItem("Recommendations", totalRecs.toString())
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            OutlinedButton(
                onClick = {
                    val dnaString = "My SwiggyMind Food DNA: $totalConvos chats deep!"
                    getPlatform().shareText(dnaString)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.2.dp, SwiggyColors.Primary),
                contentPadding = PaddingValues(16.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, tint = SwiggyColors.Primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Share Food DNA", fontWeight = FontWeight.Bold, color = SwiggyColors.Primary, fontSize = 16.sp)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            TextButton(
                onClick = {
                    onViewDna()
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("View Detailed Insights", color = SwiggyColors.Subtle, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = {
                    showClearDialog = true
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEBEE)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Clear all history", fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = SwiggyColors.Primary)
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
