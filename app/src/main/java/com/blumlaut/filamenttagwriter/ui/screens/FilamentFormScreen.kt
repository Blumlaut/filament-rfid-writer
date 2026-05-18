package com.blumlaut.filamenttagwriter.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.blumlaut.filamenttagwriter.FilamentViewModel
import com.blumlaut.filamenttagwriter.data.model.Epc256Encoder
import com.blumlaut.filamenttagwriter.data.model.Filament
import com.blumlaut.filamenttagwriter.data.model.Materials
import com.blumlaut.filamenttagwriter.data.model.NameParser
import com.blumlaut.filamenttagwriter.ui.components.ColorSwatch
import com.blumlaut.filamenttagwriter.ui.components.GenericDropdown
import com.blumlaut.filamenttagwriter.ui.components.isLightColor
import com.blumlaut.filamenttagwriter.ui.components.parseHexColor
import com.blumlaut.filamenttagwriter.ui.components.toArgbColor
import java.util.Locale
import com.blumlaut.filamenttagwriter.data.model.SpoolmanLoader
import com.blumlaut.filamenttagwriter.data.model.SpoolmanMaterialMapper

private val DIAMETERS = listOf(1.75f, 2.85f, 3.0f)

private val PRESET_COLORS = listOf(
    "Red" to 0xFF3700,
    "Green" to 0x33D700,
    "Blue" to 0x0080FF,
    "Orange" to 0xFF8C00,
    "Purple" to 0x735DF9,
    "White" to 0xFFFFFF,
    "Black" to 0x000000,
    "Yellow" to 0xFFC800,
    "Cyan" to 0x44F1FF,
    "Pink" to 0xFF69B4,
    "Brown" to 0x964B00,
    "Gray" to 0x808080,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilamentFormScreen(
    navController: NavController,
    viewModel: FilamentViewModel,
    editFilament: Filament? = null,
) {
    var filament by remember {
        mutableStateOf(editFilament ?: viewModel.createNewFilament())
    }
    var showMaterialDropdown by remember { mutableStateOf(false) }
    var showSubtypeDropdown by remember { mutableStateOf(false) }
    var showDiameterDropdown by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showAutofillBanner by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf(false) }

    // SpoolmanDB search
    var searchQuery by remember { mutableStateOf("") }
    var showSearchResults by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<com.blumlaut.filamenttagwriter.data.model.SpoolmanFilament>>(emptyList()) }
    var showSpoolmanBanner by remember { mutableStateOf(false) }

    // Track which fields were explicitly set by the user (not autofilled)
    var materialExplicit by remember { mutableStateOf(editFilament != null) }
    var subtypeExplicit by remember { mutableStateOf(editFilament != null) }
    var colorExplicit by remember { mutableStateOf(editFilament != null) }
    var tempExplicit by remember { mutableStateOf(editFilament != null) }

    // Update when editFilament changes
    LaunchedEffect(editFilament?.id) {
        if (editFilament != null) {
            filament = editFilament
            materialExplicit = true
            subtypeExplicit = true
            colorExplicit = true
        }
    }

    // Subtype options based on selected material
    val subtypeOptions by remember(filament.material) {
        mutableStateOf(Materials.getSubtypesForMaterial(filament.material))
    }

    // Autofill from name
    fun autofillFromName(name: String) {
        val parsed = NameParser.parse(name)
        var changed = false

        if (parsed.material != null && !materialExplicit) {
            val defaultSubtypeCode = Materials.SUBTYPE_CODES_REVERSE[parsed.material] ?: 0x0000
            val (minTemp, maxTemp) = if (!tempExplicit) Materials.getDefaultTemps(parsed.material) else filament.run { minTemp to maxTemp }
            filament = filament.copy(
                material = parsed.material,
                subtypeCode = defaultSubtypeCode,
                subtype = parsed.material,
                minTemp = minTemp,
                maxTemp = maxTemp,
            )
            changed = true
        }

        if (parsed.subtype != null && !subtypeExplicit && parsed.subtype != filament.material) {
            val code = Materials.SUBTYPE_CODES_REVERSE[parsed.subtype]
            if (code != null) {
                filament = filament.copy(subtypeCode = code, subtype = parsed.subtype)
                changed = true
            }
        }

        if (parsed.color != null && !colorExplicit) {
            NameParser.resolveColor(parsed.color)?.let { rgb ->
                filament = filament.copy(
                    color = String.format(Locale.US, "#%06X", rgb),
                    colorRgb = rgb,
                )
                changed = true
            }
        }

        if (changed) {
            showAutofillBanner = true
        }
    }

    // SpoolmanDB search
    fun onSearchQueryChange(query: String) {
        searchQuery = query
        if (query.length >= 2) {
            searchResults = viewModel.searchSpoolman(query, limit = 15)
            showSearchResults = true
        } else {
            searchResults = emptyList()
            showSearchResults = false
        }
    }

    fun applySpoolmanFilament(entry: com.blumlaut.filamenttagwriter.data.model.SpoolmanFilament) {
        val newFilament = viewModel.createFilamentFromSpoolman(entry)
        filament = newFilament
        materialExplicit = true
        subtypeExplicit = true
        colorExplicit = true
        tempExplicit = true
        showSpoolmanBanner = true
        showSearchResults = false
        searchQuery = ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (editFilament != null) "Edit Filament" else "New Filament") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                actions = {
                    TextButton(onClick = {
                        if (filament.name.isBlank()) {
                            nameError = true
                        } else {
                            viewModel.saveFilament(filament)
                            navController.navigate("catalog") {
                                popUpTo("read") { inclusive = false }
                                launchSingleTop = true
                            }
                        }
                    }) {
                        Text("Save")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Autofill banner
            if (showAutofillBanner) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    shape = MaterialTheme.shapes.extraLarge,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Auto-filled material & color from name",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                        TextButton(onClick = { showAutofillBanner = false }) {
                            Text("Dismiss")
                        }
                    }
                }
            }

            // SpoolmanDB autofill banner
            if (showSpoolmanBanner) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    shape = MaterialTheme.shapes.extraLarge,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = "Filled from SpoolmanDB catalog",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                fontWeight = FontWeight.Medium,
                            )
                            val mapping = SpoolmanMaterialMapper.mapToElegoo(
                                filament.material
                            )
                            if (mapping == null) {
                                Text(
                                    text = "Material has no ELEGOO equivalent — check material/subtype",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                        TextButton(onClick = { showSpoolmanBanner = false }) {
                            Text("Dismiss")
                        }
                    }
                }
            }

            // SpoolmanDB search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { onSearchQueryChange(it) },
                label = { Text("Search filament catalog") },
                placeholder = { Text("e.g. Prusament PLA Silk Red") },
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = { onSearchQueryChange(searchQuery) }) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Search",
                        )
                    }
                },
                supportingText = {
                    if (SpoolmanLoader.isLoaded()) {
                        Text("${SpoolmanLoader.count()} filaments available")
                    } else {
                        SpoolmanLoader.getError()?.let { err ->
                            Text("Database unavailable: $err", color = MaterialTheme.colorScheme.error)
                        } ?: run {
                            Text("Loading...")
                        }
                    }
                },
            )

            // Search results dropdown
            if (showSearchResults && searchResults.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp),
                        contentPadding = PaddingValues(4.dp),
                    ) {
                        items(searchResults, key = { it.id }) { entry ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { applySpoolmanFilament(entry) }
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${entry.manufacturer} ${entry.name}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                    )
                                    Text(
                                        text = entry.material,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                // Color swatch
                                entry.colorHex?.let { hex ->
                                    hex.parseHexColor().let { rgb ->
                                        ColorSwatch(
                                            rgb = rgb,
                                            size = 24.dp,
                                            borderColor = MaterialTheme.colorScheme.outline,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Name
            OutlinedTextField(
                value = filament.name,
                onValueChange = { newName ->
                    filament = filament.copy(name = newName)
                    autofillFromName(newName)
                },
                label = { Text("Name") },
                placeholder = { Text("e.g. ELEGOO PLA-CF Red") },
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth(),
                isError = nameError,
                supportingText = {
                    if (nameError) Text("Name is required")
                    else Text("Try \"TPU Black\" or \"PLA-CF Red\" for auto-fill")
                },
                singleLine = true,
            )

            // Material
            GenericDropdown(
                label = "Material",
                selectedValue = filament.material,
                displayValue = { it },
                options = Materials.getAllMaterials(),
                onSelected = { newMaterial ->
                    materialExplicit = true
                    // Reset subtype to default for new material family
                    val defaultSubtypeCode = Materials.SUBTYPE_CODES_REVERSE[newMaterial] ?: 0x0000
                    val (minTemp, maxTemp) = if (!tempExplicit) {
                        Materials.getDefaultTemps(newMaterial)
                    } else {
                        filament.run { minTemp to maxTemp }
                    }
                    filament = filament.copy(
                        material = newMaterial,
                        subtypeCode = defaultSubtypeCode,
                        subtype = newMaterial,
                        minTemp = minTemp,
                        maxTemp = maxTemp,
                    )
                },
                expanded = showMaterialDropdown,
                onExpandedChange = { showMaterialDropdown = it },
            )

            // Subtype (dropdown based on material family)
            if (subtypeOptions.isNotEmpty()) {
                GenericDropdown(
                    label = "Subtype",
                    selectedValue = filament.subtype,
                    displayValue = { it },
                    options = subtypeOptions,
                    onSelected = { newSubtype ->
                        subtypeExplicit = true
                        val code = Materials.SUBTYPE_CODES_REVERSE[newSubtype] ?: filament.subtypeCode
                        filament = filament.copy(subtypeCode = code, subtype = newSubtype)
                    },
                    expanded = showSubtypeDropdown,
                    onExpandedChange = { showSubtypeDropdown = it },
                )
            }

            // Color
            ColorPickerField(
                filament = filament,
                onFilamentChanged = {
                    colorExplicit = true
                    filament = it
                },
                expanded = showColorPicker,
                onExpandedChange = { showColorPicker = it },
            )

            // Diameter
            GenericDropdown(
                label = "Diameter",
                selectedValue = filament.diameter,
                displayValue = { "${"%.2f".format(it)}mm" },
                options = DIAMETERS,
                onSelected = { filament = filament.copy(diameter = it) },
                expanded = showDiameterDropdown,
                onExpandedChange = { showDiameterDropdown = it },
            )

            // Weight
            OutlinedTextField(
                value = filament.weight.toString(),
                onValueChange = {
                    it.toIntOrNull()?.let { w ->
                        filament = filament.copy(weight = w.coerceIn(0, 65535))
                    }
                },
                label = { Text("Weight (g)") },
                placeholder = { Text("1000") },
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            // Temperature Range
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = filament.minTemp.toString(),
                    onValueChange = {
                        tempExplicit = true
                        it.toIntOrNull()?.let { t ->
                            filament = filament.copy(minTemp = t.coerceIn(0, 300).toShort())
                        }
                    },
                    label = { Text("Min Temp (°C)") },
                    placeholder = { Text("190") },
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = filament.maxTemp.toString(),
                    onValueChange = {
                        tempExplicit = true
                        it.toIntOrNull()?.let { t ->
                            filament = filament.copy(maxTemp = t.coerceIn(0, 300).toShort())
                        }
                    },
                    label = { Text("Max Temp (°C)") },
                    placeholder = { Text("230") },
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
            }

        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColorPickerField(
    filament: Filament,
    onFilamentChanged: (Filament) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
) {
    var hexInput by remember { mutableStateOf(filament.color) }

    LaunchedEffect(filament.color) {
        hexInput = filament.color
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
    ) {
        OutlinedTextField(
            value = filament.color,
            onValueChange = {},
            label = { Text("Color") },
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            readOnly = true,
            singleLine = true,
            trailingIcon = {
                // M3 Expressive: circular color swatch
                ColorSwatch(
                    rgb = filament.colorRgb,
                    size = 24.dp,
                    borderColor = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(4.dp),
                )
            },
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = hexInput,
                    onValueChange = { newValue ->
                        hexInput = newValue
                        if (newValue.startsWith("#") && newValue.length == 7) {
                            val rgb = newValue.parseHexColor()
                            onFilamentChanged(filament.copy(
                                color = newValue.uppercase(),
                                colorRgb = rgb,
                            ))
                        }
                    },
                    label = { Text("Hex") },
                    shape = MaterialTheme.shapes.extraLarge,
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                // M3 Expressive: circular color preview
                ColorSwatch(
                    rgb = filament.colorRgb,
                    size = 32.dp,
                    borderColor = MaterialTheme.colorScheme.outline,
                )
            }

            Text(
                text = "Presets",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PRESET_COLORS.chunked(4).forEach { rowColors ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        rowColors.forEach { (name, rgb) ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(rgb.toArgbColor())
                                        .border(
                                            2.dp,
                                            if (rgb == filament.colorRgb) MaterialTheme.colorScheme.primary else Color.Gray,
                                            CircleShape,
                                        )
                                        .clickable {
                                            onFilamentChanged(filament.copy(
                                                color = String.format(Locale.US, "#%06X", rgb),
                                                colorRgb = rgb,
                                            ))
                                            hexInput = String.format(Locale.US, "#%06X", rgb)
                                            onExpandedChange(false)
                                        },
                                )
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
