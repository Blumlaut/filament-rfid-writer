package com.blumlaut.filamenttagwriter.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Shared NFC status card shown on Read and Write screens.
 *
 * Displays an error card when NFC hardware is unavailable,
 * or a warning card when NFC is disabled in device settings.
 * Renders nothing when NFC is available and enabled.
 */
@Composable
fun NfcStatusCard(
    nfcAvailable: Boolean,
    nfcEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    when {
        !nfcAvailable -> {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = MaterialTheme.shapes.extraLarge,
                modifier = modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "NFC hardware not available on this device.",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }

        !nfcEnabled -> {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                shape = MaterialTheme.shapes.extraLarge,
                modifier = modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "Please enable NFC in device settings.",
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}
