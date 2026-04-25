package com.rudra.swiggymind.ui.dna

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rudra.swiggymind.ui.theme.SwiggyColors
import com.rudra.swiggymind.getPlatform

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodDnaScreen(
    viewModel: FoodDnaViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("My Food DNA", fontWeight = FontWeight.Bold, color = SwiggyColors.Primary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = SwiggyColors.Primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SwiggyColors.Surface)
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SwiggyColors.Primary)
            }
        } else if (uiState.isLocked) {
            LockedDnaPlaceholder(uiState.sessionCount)
        } else {
            FoodDnaContent(paddingValues, uiState, viewModel)
        }
    }
}

@Composable
fun FoodDnaContent(paddingValues: PaddingValues, state: FoodDnaState, viewModel: FoodDnaViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .background(SwiggyColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SwiggyColors.Surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(64.dp),
                    shape = CircleShape,
                    color = SwiggyColors.PrimaryContainer
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.padding(12.dp),
                        tint = SwiggyColors.Primary
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("SwiggyMind Explorer", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = SwiggyColors.OnBackground)
                    Text(
                        "Based on your last ${state.sessionCount} interactions",
                        fontSize = 12.sp,
                        color = SwiggyColors.Subtle
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Your Food DNA", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = SwiggyColors.OnBackground)
            Surface(
                color = SwiggyColors.PrimaryContainer,
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    "AI ANALYSIS",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = SwiggyColors.Primary
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DnaTraitCard(
                    modifier = Modifier.weight(1f),
                    icon = "🌶️",
                    label = "Spice tolerance",
                    value = state.spiceTolerance,
                    valueColor = Color(0xFFF57F17), // SwiggyColors.Warning
                    progress = state.spiceProgress
                )
                DnaTraitCard(
                    modifier = Modifier.weight(1f),
                    icon = "🥗",
                    label = "Diet preference",
                    value = state.dietPreference,
                    valueColor = SwiggyColors.Success
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DnaTraitCard(
                    modifier = Modifier.weight(1f),
                    icon = "💰",
                    label = "Avg budget",
                    value = state.avgBudgetRange,
                    subValue = state.budgetTag
                )
                DnaTraitCard(
                    modifier = Modifier.weight(1f),
                    icon = "⚡",
                    label = "Priority",
                    value = state.priority
                )
            }
        }

        val analysisToShow = state.aiAnalysis

        if (analysisToShow.isNotBlank()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SwiggyColors.PrimaryContainer,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(0.5.dp, SwiggyColors.Primary.copy(alpha = 0.2f))
            ) {
                Text(
                    text = analysisToShow,
                    modifier = Modifier.padding(16.dp),
                    fontSize = 14.sp,
                    color = SwiggyColors.OnBackground,
                    fontWeight = FontWeight.Medium,
                    fontStyle = FontStyle.Italic,
                    lineHeight = 20.sp
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SwiggyColors.Surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = SwiggyColors.PrimaryContainer
                    ) {
                        Text(
                            "📍",
                            modifier = Modifier.wrapContentSize(Alignment.Center),
                            fontSize = 20.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Top cuisines", fontSize = 12.sp, color = SwiggyColors.Subtle)
                        Text(
                            state.topCuisines.joinToString(", "),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = SwiggyColors.OnBackground
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "Recent search tags",
                    fontSize = 12.sp,
                    color = SwiggyColors.Subtle,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    mainAxisSpacing = 8.dp,
                    crossAxisSpacing = 8.dp
                ) {
                    state.topDishes.forEach { dish ->
                        SuggestionChip(
                            onClick = {},
                            label = { Text(dish, fontSize = 12.sp) },
                            shape = RoundedCornerShape(8.dp),
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = SwiggyColors.Background,
                                labelColor = SwiggyColors.OnBackground
                            ),
                            border = SuggestionChipDefaults.suggestionChipBorder(
                                true,
                                borderColor = SwiggyColors.Border
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                val shareText = """
                    🧠 My SwiggyMind Food DNA
                    
                    🌶️ Spice tolerance: ${state.spiceTolerance}
                    🥗 Diet preference: ${state.dietPreference}  
                    💰 Avg budget: ${state.avgBudgetRange}
                    ⚡ Priority: ${state.priority}
                    🍽️ Top cuisine: ${state.topCuisines.firstOrNull() ?: "Unknown"}
                    
                    Discover yours on SwiggyMind
                    Built on Swiggy Builders Club
                """.trimIndent()
                
                getPlatform().shareText(shareText)
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SwiggyColors.Primary)
        ) {
            Icon(Icons.Default.Share, contentDescription = null)
            Spacer(modifier = Modifier.width(12.dp))
            Text("Share your DNA", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun DnaTraitCard(
    modifier: Modifier = Modifier,
    icon: String,
    label: String,
    value: String,
    valueColor: Color = SwiggyColors.OnBackground,
    subValue: String? = null,
    progress: Float? = null
) {
    Card(
        modifier = modifier.height(140.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SwiggyColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = SwiggyColors.Background
                ) {
                    Text(
                        icon,
                        modifier = Modifier.wrapContentSize(Alignment.Center),
                        fontSize = 20.sp
                    )
                }
                if (progress != null) {
                    Text(value, color = valueColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
            
            Column {
                Text(label, fontSize = 12.sp, color = SwiggyColors.Subtle)
                if (progress == null) {
                    Text(value, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = valueColor)
                }
                if (subValue != null) {
                    Text(subValue, fontSize = 11.sp, color = SwiggyColors.Subtle)
                }
                if (progress != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                        color = SwiggyColors.Primary,
                        trackColor = SwiggyColors.PrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
fun LockedDnaPlaceholder(sessionCount: Int) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🧬", fontSize = 64.sp)
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Chat more to unlock your Food DNA",
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = SwiggyColors.OnBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "We need at least 3 conversations to analyze your taste profile. (Current: $sessionCount)",
                textAlign = TextAlign.Center,
                color = SwiggyColors.Subtle
            )
        }
    }
}

@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    mainAxisSpacing: androidx.compose.ui.unit.Dp = 0.dp,
    crossAxisSpacing: androidx.compose.ui.unit.Dp = 0.dp,
    content: @Composable () -> Unit
) {
    androidx.compose.ui.layout.Layout(content, modifier) { measurables, constraints ->
        val sequences = mutableListOf<List<androidx.compose.ui.layout.Placeable>>()
        val sequenceMainSizes = mutableListOf<Int>()
        val sequenceCrossSizes = mutableListOf<Int>()

        var currentSequence = mutableListOf<androidx.compose.ui.layout.Placeable>()
        var currentMainSize = 0
        var currentCrossSize = 0

        measurables.forEach { measurable ->
            val placeable = measurable.measure(constraints)
            if (currentSequence.isNotEmpty() && currentMainSize + mainAxisSpacing.toPx() + placeable.width > constraints.maxWidth) {
                sequences.add(currentSequence)
                sequenceMainSizes.add(currentMainSize)
                sequenceCrossSizes.add(currentCrossSize)
                currentSequence = mutableListOf()
                currentMainSize = 0
                currentCrossSize = 0
            }
            currentSequence.add(placeable)
            currentMainSize += placeable.width + mainAxisSpacing.toPx().toInt()
            currentCrossSize = maxOf(currentCrossSize, placeable.height)
        }
        sequences.add(currentSequence)
        sequenceMainSizes.add(currentMainSize)
        sequenceCrossSizes.add(currentCrossSize)

        val width = constraints.maxWidth
        val height = sequenceCrossSizes.sum() + (sequences.size - 1) * crossAxisSpacing.toPx().toInt()

        layout(width, height) {
            var y = 0
            sequences.zip(sequenceCrossSizes).forEach { (sequence, crossSize) ->
                var x = 0
                sequence.forEach { placeable ->
                    placeable.placeRelative(x, y)
                    x += placeable.width + mainAxisSpacing.toPx().toInt()
                }
                y += crossSize + crossAxisSpacing.toPx().toInt()
            }
        }
    }
}
