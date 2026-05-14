package com.blumlaut.filamenttagwriter.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
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
import com.blumlaut.filamenttagwriter.nfc.NfcReaderWriter
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
            if (!nfcAvailable) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "NFC hardware not available on this device.",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            } else if (!nfcEnabled) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "Please enable NFC in device settings.",
                        modifier = Modifier.padding(16.dp),
                    )
                }
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
                    val filament = (readResult as NfcReaderWriter.ReadResult.Success).filament
                    FilamentInfoCard(filament = filament)

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        // M3 Expressive: extraLarge shape + emphasized title
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
                                text = (readResult as NfcReaderWriter.ReadResult.Error).message,
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

@Composable
private fun FilamentInfoCard(filament: Filament) {
    // M3 Expressive: extraLarge shape, circular swatch, emphasized title
    Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier.size(48.dp).clip(CircleShape)
                        .background(Color(0xFF000000L or (filament.colorRgb and 0xFFFFFF).toLong())),
                )
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
            FilamentDetailRow("Diameter", "${"%.2f".format(filament.diameter)}mm")
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
