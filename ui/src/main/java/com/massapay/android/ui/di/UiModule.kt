package com.massapay.android.ui.di

import com.massapay.android.ui.walletconnect.WalletConnectService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UiModule {

    @Provides
    @Singleton
    fun provideWalletConnectService(okHttpClient: OkHttpClient): WalletConnectService {
        return WalletConnectService(okHttpClient).apply {
            // WalletConnect Cloud Project ID
            // Get yours at https://cloud.walletconnect.com/
            initialize("f929c85cac8f1fe6c80e57f3ae7cd6e8")
        }
    }
}
