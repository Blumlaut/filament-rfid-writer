package com.blumlaut.filamenttagwriter.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.blumlaut.filamenttagwriter.FilamentViewModel
import com.blumlaut.filamenttagwriter.data.model.Filament

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(
    navController: NavController,
    viewModel: FilamentViewModel,
) {
    val filaments by viewModel.catalog.collectAsStateWithLifecycle()
    var deleteCandidate by remember { mutableStateOf<Filament?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Filament Catalog") })
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("form/new") },
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Filament")
            }
        },
    ) { padding ->
        if (filaments.isEmpty()) {
            Column(
                modifier = Modifier.padding(padding).fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "No filaments yet",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tap + to add a filament profile, or read a tag to import one.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(filaments, key = { it.id }) { filament ->
                    FilamentCard(
                        filament = filament,
                        onEdit = { navController.navigate("form/edit/${filament.id}") },
                        onDelete = {
                            deleteCandidate = filament
                            showDeleteDialog = true
                        },
                    )
                }
            }
        }
    }

    if (showDeleteDialog && deleteCandidate != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Filament") },
            text = { Text("Delete '${deleteCandidate?.name ?: "Unknown"}' from catalog? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteCandidate?.let { viewModel.deleteFilament(it) }
                        showDeleteDialog = false
                        deleteCandidate = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
fun FilamentCard(
    filament: Filament,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSelect: (() -> Unit)? = null,
    isSelected: Boolean = false,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onSelect?.invoke() },
        shape = RoundedCornerShape(12.dp),
        colors = if (isSelected) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        } else {
            CardDefaults.cardColors()
        },
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF000000L or (filament.colorRgb and 0xFFFFFF).toLong())),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = filament.name.ifBlank { "Unnamed" },
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = filament.subtype,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (isSelected) {
                        Icon(Icons.Filled.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    TextButton(onClick = onEdit) { Text("Edit") }
                    TextButton(onClick = onDelete) { Text("Del") }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) {
                DetailChip("${"%.2f".format(filament.diameter)}mm")
                DetailChip("${filament.weight}g")
                DetailChip(filament.color)
                DetailChip("${filament.minTemp}–${filament.maxTemp}°C")
            }
        }
    }
}

@Composable
private fun DetailChip(text: String) {
    Surface(shape = RoundedCornerShape(8.dp), tonalElevation = 2.dp) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}
