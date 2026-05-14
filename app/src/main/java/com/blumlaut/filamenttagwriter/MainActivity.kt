package com.blumlaut.filamenttagwriter

import android.nfc.NfcAdapter
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.blumlaut.filamenttagwriter.ui.screens.CatalogScreen
import com.blumlaut.filamenttagwriter.ui.screens.HomeScreen
import com.blumlaut.filamenttagwriter.ui.screens.ReadScreen
import com.blumlaut.filamenttagwriter.ui.screens.WriteScreen
import com.blumlaut.filamenttagwriter.ui.theme.FilamentTagWriterTheme

class MainActivity : ComponentActivity() {

    private var nfcAdapter: NfcAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        enableEdgeToEdge()
        setContent {
            FilamentTagWriterTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "home") {
                        composable("home") { HomeScreen(navController) }
                        composable("read") { ReadScreen(navController, nfcAdapter) }
                        composable("write/{filamentId}") { backStackEntry ->
                            val filamentId = backStackEntry.arguments?.getString("filamentId")
                            WriteScreen(navController, nfcAdapter, filamentId)
                        }
                        composable("catalog") { CatalogScreen(navController) }
                    }
                }
            }
        }
    }
}
