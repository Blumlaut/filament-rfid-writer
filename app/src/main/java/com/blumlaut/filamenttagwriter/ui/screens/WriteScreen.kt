package com.blumlaut.filamenttagwriter.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.blumlaut.filamenttagwriter.FilamentViewModel
import com.blumlaut.filamenttagwriter.data.model.Filament
import com.blumlaut.filamenttagwriter.nfc.NfcReaderWriter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WriteScreen(
    navController: NavController,
    viewModel: FilamentViewModel,
    nfcAvailable: Boolean,
    nfcEnabled: Boolean,
) {
    val filaments by viewModel.catalog.collectAsStateWithLifecycle()
    val selectedFilament = viewModel.selectedFilamentForWrite.value
    val writeResult = viewModel.nfcWriteResult.value
    val isWriting = viewModel.isWritingTag.value
    var showDropdown by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Write to Tag") })
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            if (filaments.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "No filaments in catalog", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Add a filament profile first.", color = MaterialTheme.colorScheme.onSecondaryContainer)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(onClick = { navController.navigate("form/new") }) { Text("Add Filament") }
                            OutlinedButton(onClick = { navController.navigate("read") }) { Text("Read Tag") }
                        }
                    }
                }
            } else {
                Box {
                    OutlinedButton(onClick = { showDropdown = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(text = selectedFilament?.name?.ifBlank { "Select Filament" } ?: "Select Filament")
                    }
                    DropdownMenu(expanded = showDropdown, onDismissRequest = { showDropdown = false }) {
                        filaments.forEach { filament ->
                            DropdownMenuItem(
                                text = { Text("${filament.name.ifBlank { "Unnamed" }} (${filament.subtype})") },
                                onClick = {
                                    viewModel.selectedFilamentForWrite.value = filament
                                    showDropdown = false
                                },
                            )
                        }
                    }
                }

                selectedFilament?.let { filament ->
                    Card {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = filament.name.ifBlank { "Unnamed" }, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = "${filament.subtype} | ${"%.2f".format(filament.diameter)}mm | ${filament.weight}g | ${filament.color} | ${filament.minTemp}–${filament.maxTemp}°C",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            if (!nfcAvailable) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = "NFC hardware not available on this device.", modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                }
            } else if (!nfcEnabled) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = "Please enable NFC in device settings.", modifier = Modifier.padding(16.dp))
                }
            } else if (selectedFilament != null) {
                if (isWriting) {
                    CircularProgressIndicator(modifier = Modifier.size(48.dp))
                    Text("Writing to tag...")
                } else if (writeResult == null) {
                    Text(
                        text = "Hold your phone near an NTAG213 tag to write",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            when (writeResult) {
                NfcReaderWriter.WriteResult.Success -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = "Write Successful!", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(text = "The filament data has been written to the tag.", color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                    OutlinedButton(onClick = { viewModel.clearNfcWriteResult() }, modifier = Modifier.fillMaxWidth()) { Text("Write Another") }
                }

                is NfcReaderWriter.WriteResult.Error -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = "Write Failed", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = (writeResult as NfcReaderWriter.WriteResult.Error).message, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                    OutlinedButton(onClick = { viewModel.clearNfcWriteResult() }, modifier = Modifier.fillMaxWidth()) { Text("Try Again") }
                }

                null -> {}
            }
        }
    }
}
