package com.blumlaut.filamenttagwriter.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
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

/** Check if a filament matches a search query (name, material, subtype, color). */
fun Filament.matchesQuery(query: String): Boolean {
    if (query.isBlank()) return true
    val q = query.lowercase()
    return name.lowercase().contains(q) ||
        material.lowercase().contains(q) ||
        subtype.lowercase().contains(q) ||
        color.lowercase().contains(q) ||
        (color.removePrefix("#")).lowercase().contains(q)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(
    navController: NavController,
    viewModel: FilamentViewModel,
) {
    val filaments by viewModel.catalog.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var deleteCandidate by remember { mutableStateOf<Filament?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val filteredFilaments = filaments.filter { it.matchesQuery(searchQuery) }

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
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                // M3 Expressive: extraLarge pill shape for search
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by name, material, or color") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    singleLine = true,
                )

                if (filteredFilaments.isEmpty() && searchQuery.isNotEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = "No filaments match",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(filteredFilaments, key = { it.id }) { filament ->
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
    // M3 Expressive: extraLarge shape (28dp) for card hero surface
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onSelect?.invoke() },
        shape = MaterialTheme.shapes.extraLarge,
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
                // M3 Expressive: circular color swatch
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape)
                        .background(Color(0xFF000000L or (filament.colorRgb and 0xFFFFFF).toLong())),
                )
                Column(modifier = Modifier.weight(1f)) {
                    // M3 Expressive: emphasized headline for selection
                    Text(
                        text = filament.name.ifBlank { "Unnamed" },
                        style = if (isSelected) MaterialTheme.typography.titleLarge
                               else MaterialTheme.typography.titleMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = filament.subtype,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                // M3 Expressive: icon buttons with 40dp touch targets
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (isSelected) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    IconButton(onClick = onEdit) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = "Edit filament",
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Delete filament",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
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
    // M3 Expressive: extraSmall shape (4dp) + labelSmall for compact chips
    Surface(
        shape = MaterialTheme.shapes.extraSmall,
        tonalElevation = 1.dp,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}
