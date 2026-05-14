package com.blumlaut.filamenttagwriter.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun HomeScreen(navController: NavController) {
    Scaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("Blumlaut's Filament Tag Writer", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(32.dp))

            Button(onClick = { navController.navigate("read") }) {
                Text("Read Tag")
            }
            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = { navController.navigate("catalog") }) {
                Text("Catalog")
            }
        }
    }
}
