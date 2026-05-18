package com.blumlaut.filamenttagwriter.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/**
 * Pulsing skeleton placeholder with opacity sweep animation.
 *
 * M3 Expressive pattern: subtle pulse (opacity 0.5 → 0.2),
 * stagger delay for sweep effect across multiple elements.
 */
@Composable
fun PulsingSkeleton(
    modifier: Modifier = Modifier,
    staggerIndex: Int = 0,
) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val opacity by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 800,
                delayMillis = staggerIndex * 100,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "skeleton opacity",
    )

    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = opacity * 0.3f)),
    )
}

/**
 * Skeleton for a single filament catalog card item.
 * Matches the shape and size of [FilamentCard] exactly.
 */
@Composable
fun FilamentCardSkeleton(
    modifier: Modifier = Modifier,
    staggerIndex: Int = 0,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header row: swatch + text + action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                // Color swatch circle
                PulsingSkeleton(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    staggerIndex = staggerIndex,
                )
                // Title + subtitle
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    PulsingSkeleton(
                        modifier = Modifier.fillMaxWidth(0.6f).height(18.dp),
                        staggerIndex = staggerIndex,
                    )
                    PulsingSkeleton(
                        modifier = Modifier.fillMaxWidth(0.35f).height(14.dp),
                        staggerIndex = staggerIndex + 1,
                    )
                }
                // Action icons (3 small squares)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(3) {
                        PulsingSkeleton(
                            modifier = Modifier.size(24.dp).clip(MaterialTheme.shapes.small),
                            staggerIndex = staggerIndex + it,
                        )
                    }
                }
            }
            // Detail chips row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            ) {
                repeat(4) {
                    PulsingSkeleton(
                        modifier = Modifier.weight(1f).height(20.dp),
                        staggerIndex = staggerIndex + it,
                    )
                }
            }
        }
    }
}

/**
 * Skeleton for the printer card (header + status + tray grid).
 * Matches the shape and size of [PrinterCard] exactly.
 */
@Composable
fun PrinterCardSkeleton(
    modifier: Modifier = Modifier,
    staggerIndex: Int = 0,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: name + IP + connection buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    PulsingSkeleton(
                        modifier = Modifier.fillMaxWidth(0.5f).height(20.dp),
                        staggerIndex = staggerIndex,
                    )
                    PulsingSkeleton(
                        modifier = Modifier.fillMaxWidth(0.3f).height(14.dp),
                        staggerIndex = staggerIndex + 1,
                    )
                }
                // Connection buttons
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    PulsingSkeleton(
                        modifier = Modifier.size(32.dp).clip(CircleShape),
                        staggerIndex = staggerIndex + 2,
                    )
                    PulsingSkeleton(
                        modifier = Modifier.size(32.dp).clip(MaterialTheme.shapes.small),
                        staggerIndex = staggerIndex + 3,
                    )
                }
            }

            // Status overview area
            Spacer(modifier = Modifier.height(12.dp))
            SkeletonSurfaceContainer(
                modifier = Modifier.fillMaxWidth().height(80.dp),
                staggerIndex = staggerIndex,
            )

            // Tray grid area (2x2)
            Spacer(modifier = Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Import button
                PulsingSkeleton(
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                    staggerIndex = staggerIndex + 1,
                )
                // 2x2 tray grid placeholder
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        repeat(2) {
                            PulsingSkeleton(
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                staggerIndex = staggerIndex + it * 2,
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        repeat(2) {
                            PulsingSkeleton(
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                staggerIndex = staggerIndex + it * 2 + 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Skeleton for a surface container (rounded, tinted background area).
 */
@Composable
fun SkeletonSurfaceContainer(
    modifier: Modifier = Modifier,
    staggerIndex: Int = 0,
) {
    val transition = rememberInfiniteTransition(label = "skeleton surface")
    val opacity by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 800,
                delayMillis = staggerIndex * 100,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "skeleton surface opacity",
    )

    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = opacity * 0.3f)),
    )
}
