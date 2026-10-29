package com.smartledger.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.smartledger.app.core.ui.theme.SmartLedgerTheme
import com.smartledger.app.core.navigation.AppNavHost
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main entry point of the SmartLedger Android app.
 * Uses Hilt (@AndroidEntryPoint) and installs the SplashScreen API.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Install the splash screen before super.onCreate
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmartLedgerTheme {
                AppNavHost()
            }
        }
    }
}
