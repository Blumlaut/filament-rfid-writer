package com.blumlaut.filamenttagwriter.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.blumlaut.filamenttagwriter.FilamentViewModel
import com.blumlaut.filamenttagwriter.data.model.Filament
import com.blumlaut.filamenttagwriter.data.model.SpoolmanLoader
import com.blumlaut.filamenttagwriter.data.model.diameterString
import com.blumlaut.filamenttagwriter.nfc.NfcReaderWriter
import com.blumlaut.filamenttagwriter.ui.components.ColorSwatch
import com.blumlaut.filamenttagwriter.ui.components.NfcStatusCard
import com.blumlaut.filamenttagwriter.ui.components.parseHexColor
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadScreen(
    viewModel: FilamentViewModel,
    nfcAvailable: Boolean,
    nfcEnabled: Boolean,
) {
    val readResult = viewModel.nfcReadResult.value
    val isReading = viewModel.isReadingTag.value
    val spoolmanMatch = viewModel.spoolmanMatchResult.value

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Read Tag") })
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (!nfcAvailable || !nfcEnabled) {
                NfcStatusCard(nfcAvailable = nfcAvailable, nfcEnabled = nfcEnabled)
            } else {
                if (isReading) {
                    CircularProgressIndicator(modifier = Modifier.size(48.dp))
                    Text("Reading tag...")
                } else if (readResult == null) {
                    Text(
                        text = "Hold your phone near an NTAG213 filament tag",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            when (readResult) {
                is NfcReaderWriter.ReadResult.Success -> {
                    val filament = readResult.filament

                    // SpoolmanDB match banner
                    SpoolmanMatchBanner(
                        match = spoolmanMatch,
                        filament = filament,
                        onApplyMatch = { entry ->
                            val updated = viewModel.applySpoolmanMatch(entry)
                            if (updated != null) {
                                // Update the read result with the matched name
                                viewModel.nfcReadResult.value = NfcReaderWriter.ReadResult.Success(updated)
                                viewModel.spoolmanMatchResult.value = null
                            }
                        },
                        onSearchCatalog = {
                            // Navigate to form with pre-filled data — handled by caller
                            // For now just clear match to show search hint
                            viewModel.spoolmanMatchResult.value = null
                        },
                    )

                    FilamentInfoCard(filament = filament)

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        FilledTonalButton(
                            onClick = {
                                val saved = filament.copy(id = UUID.randomUUID().toString())
                                viewModel.saveFilament(saved)
                                viewModel.clearNfcReadResult()
                            },
                            shape = MaterialTheme.shapes.extraLarge,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                "Save to Catalog",
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        OutlinedButton(
                            onClick = { viewModel.clearNfcReadResult() },
                            shape = MaterialTheme.shapes.extraLarge,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Read Another")
                        }
                    }
                }

                is NfcReaderWriter.ReadResult.Error -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = MaterialTheme.shapes.extraLarge,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Read Failed",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = readResult.message,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = { viewModel.clearNfcReadResult() },
                        shape = MaterialTheme.shapes.extraLarge,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Try Again")
                    }
                }

                null -> {}
            }
        }
    }
}

/**
 * Banner shown after a successful tag read, offering SpoolmanDB match results.
 *
 * M3 Expressive patterns:
 * - Cards use extraLarge shape (28dp) with semantic container colors
 * - Candidate rows are ElevatedCards with large shape (16dp)
 * - Color swatches use CircleShape with semantic outline border
 * - Actions use FilledTonalButton / FilledIconButton
 * - Typography uses bodyLarge for titles, labelMedium for captions
 */
@Composable
private fun SpoolmanMatchBanner(
    match: SpoolmanLoader.SpoolmanMatchResult?,
    filament: Filament,
    onApplyMatch: (com.blumlaut.filamenttagwriter.data.model.SpoolmanFilament) -> Unit,
    onSearchCatalog: () -> Unit,
) {
    when (match) {
        is SpoolmanLoader.SpoolmanMatchResult.ExactMatch -> {
            // M3 Expressive: tertiaryContainer card, extraLarge shape, emphasized action
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                ),
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = "Found in catalog",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                        Text(
                            text = "${match.filament.manufacturer} ${match.filament.name}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    }
                    FilledTonalButton(
                        onClick = { onApplyMatch(match.filament) },
                        shape = MaterialTheme.shapes.extraLarge,
                    ) {
                        Text("Use Name")
                    }
                }
            }
        }

        is SpoolmanLoader.SpoolmanMatchResult.MultipleMatches -> {
            // M3 Expressive: secondaryContainer card, extraLarge shape
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    // Header
                    Text(
                        text = "${match.candidates.size} possible matches",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Candidate list — each item is an ElevatedCard (M3 Expressive: large shape)
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 200.dp),
                        contentPadding = PaddingValues(bottom = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(match.candidates, key = { it.id }) { entry ->
                            SpoolmanCandidateCard(
                                entry = entry,
                                onUse = { onApplyMatch(entry) },
                            )
                        }
                    }

                    // Search button — M3 Expressive: text button with icon
                    TextButton(
                        onClick = onSearchCatalog,
                        shape = MaterialTheme.shapes.extraLarge,
                        modifier = Modifier.align(Alignment.End),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Search for exact brand")
                    }
                }
            }
        }

        is SpoolmanLoader.SpoolmanMatchResult.NoMatch -> {
            // Only show hint if SpoolmanDB is loaded
            if (SpoolmanLoader.isLoaded()) {
                // M3 Expressive: subtle OutlinedCard, extraLarge shape
                OutlinedCard(
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                    ) {
                        Text(
                            text = "No catalog match — name is auto-generated",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        FilledTonalButton(
                            onClick = onSearchCatalog,
                            shape = MaterialTheme.shapes.extraLarge,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Search")
                        }
                    }
                }
            }
        }

        null -> {}
    }
}

/**
 * Single candidate in the match banner.
 *
 * M3 Expressive: ElevatedCard bubble with rounded shape (not CutCornerShape),
 * CircleShape swatch with outline border, FilledIconButton with checkmark.
 */
@Composable
private fun SpoolmanCandidateCard(
    entry: com.blumlaut.filamenttagwriter.data.model.SpoolmanFilament,
    onUse: () -> Unit,
) {
    ElevatedCard(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(12.dp),
        ) {
            // Color swatch — M3 Expressive: CircleShape with semantic outline border
            entry.colorHex?.let { hex ->
                hex.parseHexColor().let { rgb ->
                    ColorSwatch(
                        rgb = rgb,
                        size = 32.dp,
                        borderColor = MaterialTheme.colorScheme.outline,
                    )
                }
            }

            // Text content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${entry.manufacturer} ${entry.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = entry.material,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Action — M3 Expressive: round FilledIconButton
            FilledIconButton(onClick = onUse) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowRight,
                    contentDescription = "Use this name",
                )
            }
        }
    }
}

@Composable
private fun FilamentInfoCard(filament: Filament) {
    Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                ColorSwatch(rgb = filament.colorRgb, size = 48.dp)
                Column {
                    Text(
                        text = filament.name.ifBlank { "Unknown Filament" },
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = filament.subtype,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            FilamentDetailRow("Material", filament.material)
            FilamentDetailRow("Subtype", filament.subtype)
            FilamentDetailRow("Diameter", filament.diameterString)
            FilamentDetailRow("Weight", "${filament.weight}g")
            FilamentDetailRow("Color", filament.color)
            FilamentDetailRow("Temp Range", "${filament.minTemp}–${filament.maxTemp}°C")
            FilamentDetailRow(
                "Manufacturer",
                "0x${filament.manufacturerCode.toString(16).uppercase()}",
            )
        }
    }
}

@Composable
private fun FilamentDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
    HorizontalDivider(modifier = Modifier.padding(top = 2.dp))
}
