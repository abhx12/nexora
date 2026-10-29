package com.smartledger.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class — entry point for Hilt dependency injection.
 * Must be registered in AndroidManifest.xml via android:name=".SmartLedgerApp"
 */
@HiltAndroidApp
class SmartLedgerApp : Application()
