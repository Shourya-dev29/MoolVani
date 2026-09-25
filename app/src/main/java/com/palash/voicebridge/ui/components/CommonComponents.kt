package com.palash.voicebridge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.palash.voicebridge.ui.theme.*

/** Green OFFLINE READY status chip */
@Composable
fun OfflineReadyChip(
    isReady: Boolean,
    label: String,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isReady) OfflineReadyGreen else DemoModeAmber
    val icon = if (isReady) Icons.Filled.CheckCircle else Icons.Filled.Info

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = bgColor.copy(alpha = 0.15f),
        contentColor = bgColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
    }
}

/** Status row item for home screen dashboard */
@Composable
fun StatusRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = valueColor
        )
    }
}

/** Demo mode indicator banner */
@Composable
fun DemoModeBanner(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = DemoModeAmber.copy(alpha = 0.12f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                Icons.Filled.Info,
                contentDescription = null,
                tint = DemoModeAmber,
                modifier = Modifier.size(20.dp)
            )
            Column {
                Text(
                    "DEMO MODE",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = DemoModeAmber
                )
                Text(
                    "AI models not installed. Curriculum-only demo active.",
                    style = MaterialTheme.typography.bodySmall,
                    color = DemoModeAmber.copy(alpha = 0.8f)
                )
            }
        }
    }
}

/** Translation confidence badge */
@Composable
fun ConfidenceBadge(confidence: Float, verified: Boolean, modifier: Modifier = Modifier) {
    val color = when {
        verified && confidence > 0.9f -> OfflineReadyGreen
        confidence > 0.7f -> DemoModeAmber
        else -> ErrorRed
    }
    val label = when {
        verified -> "Verified Curriculum"
        confidence > 0.9f -> "High Confidence"
        confidence > 0.7f -> "Fuzzy Match"
        else -> "Low Confidence"
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f),
        contentColor = color
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

/** Section header with optional subtitle */
@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Back navigation top bar with optional subtitle */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PalashTopBar(
    title: String,
    subtitle: String? = null,
    onNavigateBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        },
        navigationIcon = {
            if (onNavigateBack != null) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Navigate Back",
                        tint = Color.White
                    )
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = PalashGreen,
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White,
            actionIconContentColor = Color.White
        )
    )
}

/** Comprehensive Offline Status Breakdown Card */
@Composable
fun OfflineStatusBreakdownCard(
    isOfflineReady: Boolean,
    hasHindiVoice: Boolean = true,
    hasSantaliVoice: Boolean = true,
    curriculumPhraseCount: Int = 120,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOfflineReady) PalashGreenContainer else SurfaceVariantLight
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                color = if (isOfflineReady) OfflineReadyGreen else DemoModeAmber,
                                shape = RoundedCornerShape(5.dp)
                            )
                    )
                    Text(
                        text = if (isOfflineReady) "Offline Ready" else "Curriculum Demo Mode",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isOfflineReady) PalashGreen else DemoModeAmber
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = (if (isOfflineReady) OfflineReadyGreen else DemoModeAmber).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "100% On-Device",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isOfflineReady) OfflineReadyGreen else DemoModeAmber
                    )
                }
            }

            HorizontalDivider(color = OutlineBorderLight)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatusTickItem(
                    label = "Hindi voice",
                    isReady = hasHindiVoice
                )
                StatusTickItem(
                    label = "Santali voice",
                    isReady = hasSantaliVoice
                )
                StatusTickItem(
                    label = "Curriculum ($curriculumPhraseCount)",
                    isReady = curriculumPhraseCount > 0
                )
            }
        }
    }
}

@Composable
private fun StatusTickItem(label: String, isReady: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondaryLight,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = if (isReady) "✓" else "—",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = if (isReady) OfflineReadyGreen else DemoModeAmber
        )
    }
}

