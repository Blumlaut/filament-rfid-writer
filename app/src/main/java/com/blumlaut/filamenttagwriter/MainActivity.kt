package com.blumlaut.filamenttagwriter

import android.content.Intent
import android.nfc.NfcAdapter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DeviceHub
import androidx.compose.material.icons.filled.SimCardDownload
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.blumlaut.filamenttagwriter.nfc.NfcManager
import com.blumlaut.filamenttagwriter.ui.screens.CatalogScreen
import com.blumlaut.filamenttagwriter.ui.screens.FilamentFormScreen
import com.blumlaut.filamenttagwriter.ui.screens.PrinterScreen
import com.blumlaut.filamenttagwriter.ui.screens.ReadScreen
import com.blumlaut.filamenttagwriter.ui.screens.WriteScreen
import com.blumlaut.filamenttagwriter.ui.theme.FilamentTagWriterTheme

private data class BottomNavItem(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val BOTTOM_NAV_ITEMS = listOf(
    BottomNavItem("read", "Read", Icons.AutoMirrored.Filled.CallReceived),
    BottomNavItem("catalog", "Catalog", Icons.AutoMirrored.Filled.List),
    BottomNavItem("printer", "Printer", Icons.Default.DeviceHub),
    BottomNavItem("write", "Write", Icons.Default.SimCardDownload),
)

class MainActivity : ComponentActivity() {

    private val viewModel: FilamentViewModel by viewModels {
        object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return FilamentViewModel((application as FilamentTagApp).database) as T
            }
        }
    }

    private val printerViewModel: PrinterViewModel by viewModels {
        object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return PrinterViewModel((application as FilamentTagApp).database) as T
            }
        }
    }

    private lateinit var nfcManager: NfcManager
    private var nfcAdapter: NfcAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        nfcManager = NfcManager(this)

        // Process any NFC intent that launched the app
        if (nfcAdapter?.isEnabled == true) {
            handleNfcIntent(intent)
        }

        enableEdgeToEdge()
        setContent {
            FilamentTagWriterTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    val currentBackStackEntry = navController.currentBackStackEntryAsState()
                    val currentRoute = currentBackStackEntry.value?.destination?.route

                    // Determine if bottom bar should be shown
                    val showBottomBar = currentRoute in BOTTOM_NAV_ITEMS.map { it.route }

                    Scaffold(
                        bottomBar = {
                            if (showBottomBar) {
                                NavigationBar {
                                    BOTTOM_NAV_ITEMS.forEach { item ->
                                        NavigationBarItem(
                                            icon = { Icon(item.icon, contentDescription = item.label) },
                                            label = { Text(item.label) },
                                            selected = currentRoute == item.route,
                                            onClick = {
                                                navController.navigate(item.route) {
                                                    popUpTo(navController.graph.findStartDestination().id) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            },
                                        )
                                    }
                                }
                            }
                        },
                    ) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = "read",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                        ) {
                            composable("read") {
                                ReadScreen(
                                    viewModel = viewModel,
                                    nfcAvailable = nfcManager.nfcAvailable,
                                    nfcEnabled = nfcManager.nfcEnabled,
                                )
                            }
                            composable("catalog") {
                                CatalogScreen(
                                    navController = navController,
                                    viewModel = viewModel,
                                )
                            }
                            composable("printer") {
                                PrinterScreen(
                                    viewModel = printerViewModel,
                                )
                            }
                            composable("write") {
                                WriteScreen(
                                    navController = navController,
                                    viewModel = viewModel,
                                    nfcAvailable = nfcManager.nfcAvailable,
                                    nfcEnabled = nfcManager.nfcEnabled,
                                )
                            }
                            composable("form/new") {
                                FilamentFormScreen(
                                    navController = navController,
                                    viewModel = viewModel,
                                )
                            }
                            composable("form/edit/{filamentId}") { backStackEntry ->
                                val fid = backStackEntry.arguments?.getString("filamentId")
                                val editFilament = fid?.let { viewModel.getFilamentById(it) }
                                FilamentFormScreen(
                                    navController = navController,
                                    viewModel = viewModel,
                                    editFilament = editFilament,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (nfcAdapter?.isEnabled == true) {
            nfcManager.enableForegroundDispatch(this, javaClass)
        }
    }

    override fun onPause() {
        super.onPause()
        nfcManager.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNfcIntent(intent)
    }

    /**
     * Handle NFC intents (TECH_DISCOVERED, TAG_DISCOVERED, NDEF_DISCOVERED).
     * Routes the tag to the appropriate action based on the current tab.
     *
     * CRITICAL: Reading must start immediately — Android drops the NFC RF link
     * ~2 seconds after tag discovery.
     */
    private fun handleNfcIntent(intent: Intent) {
        if (intent.hasExtra(NfcAdapter.EXTRA_TAG)) {
            nfcManager.processNfcIntent(intent)
            val tag = nfcManager.lastTag.value
            tag?.let {
                android.util.Log.d("MainActivity", "[NFC-Debug] Tag discovered, current tab: ${viewModel.currentTab.value}")
                when (viewModel.currentTab.value) {
                    "read" -> {
                        android.util.Log.d("MainActivity", "[NFC-Debug] Starting immediate read")
                        viewModel.readTag(it)
                    }
                    "write" -> {
                        android.util.Log.d("MainActivity", "[NFC-Debug] Tag on Write tab, triggering write")
                        viewModel.writeTag(it)
                    }
                    else -> {
                        android.util.Log.d("MainActivity", "[NFC-Debug] Ignoring tag on non-NFC tab: ${viewModel.currentTab.value}")
                    }
                }
            }
        }
    }
}
