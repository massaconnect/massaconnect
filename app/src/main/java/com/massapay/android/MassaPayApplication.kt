package com.massapay.android

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MassaPayApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        // WalletConnect is initialized via Hilt DI in UiModule
        android.util.Log.d("MassaPayApp", "Application created")
    }
}