package com.blumlaut.filamenttagwriter.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Filament RFID Writer") })
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // M3 Expressive: emphasized headlineLarge as hero moment
            Text(
                text = "Filament RFID Writer",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Read, write, and catalog NTAG213 RFID tags on filament spools",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(48.dp))

            // M3 Expressive: extraLarge shape (28dp) + emphasized title on buttons
            FilledTonalButton(
                onClick = { navController.navigate("read") },
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                Text(
                    "Read Tag",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            FilledTonalButton(
                onClick = { navController.navigate("catalog") },
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                Text(
                    "Catalog",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { navController.navigate("form/new") },
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                Text(
                    "+ New Filament Profile",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

