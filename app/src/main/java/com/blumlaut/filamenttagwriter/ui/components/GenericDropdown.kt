package com.blumlaut.filamenttagwriter.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Generic dropdown composable wrapping ExposedDropdownMenuBox.
 *
 * Used for Material, Subtype, Diameter, and any future dropdown fields.
 * M3 Expressive: extraLarge shape.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T : Any> GenericDropdown(
    label: String,
    selectedValue: T,
    displayValue: (T) -> String,
    options: List<T>,
    onSelected: (T) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
    ) {
        OutlinedTextField(
            value = displayValue(selectedValue),
            onValueChange = {},
            label = { Text(label) },
            shape = MaterialTheme.shapes.extraLarge,
            modifier = modifier
                .fillMaxWidth()
                .menuAnchor(),
            readOnly = true,
            singleLine = true,
            trailingIcon = {
                IconButton(onClick = { onExpandedChange(!expanded) }) {
                    Icon(
                        imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Select $label",
                    )
                }
            },
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(displayValue(option)) },
                    onClick = {
                        onSelected(option)
                        onExpandedChange(false)
                    },
                )
            }
        }
    }
}
