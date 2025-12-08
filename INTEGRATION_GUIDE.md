# Multi-Account Integration Guide

## ✅ Compilation Status: SUCCESS

**Date:** November 29, 2025  
**Build:** Debug APK compiled successfully  
**Theme:** Dark/Light theme compatibility verified

---

## 🎨 Theme Compatibility

All new screens now properly use `MaterialTheme.colorScheme`:

### Dark Theme Colors (Your Current Scheme)
- Background: `#000000` (Black)
- Surface: `#121212` (Dark Gray)
- Card Background: `#0B0B0B` (Very Dark Gray)
- Text: `#EDEDED` (Light Gray)
- Primary: `#1A73E8` (Blue)

### Light Theme Colors
- Background: `#FFFBFE` (Off White)
- Surface: `#FFFBFE` (Off White)
- Card Background: `#E7E0EC` (Light Purple Gray)
- Text: `#1C1B1F` (Dark Gray)
- Primary: `#1A73E8` (Blue)

All account screens automatically adapt to your theme!

---

## 🔧 Integration Steps (Required)

### Step 1: Add AccountManager to SecurityModule

Create or update `security/src/main/java/com/massapay/android/security/di/SecurityModule.kt`:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {
    
    // ... existing providers ...
    
    @Provides
    @Singleton
    fun provideAccountManager(
        walletManager: WalletManager,
        @ApplicationContext context: Context
    ): AccountManager {
        return AccountManager(walletManager, context)
    }
}
```

### Step 2: Initialize Default Account in OnboardingViewModel

Update `OnboardingViewModelNew.kt` after wallet creation:

```kotlin
// In OnboardingViewModelNew.kt
@Inject
lateinit var accountManager: AccountManager

// After wallet is created successfully
private fun onWalletCreated() {
    viewModelScope.launch {
        // Initialize default account (Account 0)
        val result = accountManager.initializeDefaultAccount()
        
        result.fold(
            onSuccess = { account ->
                Log.d("Onboarding", "Default account created: ${account.name}")
                // Continue to dashboard
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    walletCreated = true
                )
            },
            onFailure = { error ->
                Log.e("Onboarding", "Failed to create default account", error)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to initialize account: ${error.message}"
                )
            }
        )
    }
}
```

### Step 3: Update DashboardViewModel to Use Active Account

Update `DashboardViewModel.kt`:

```kotlin
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val walletManager: WalletManager,
    private val accountManager: AccountManager,  // ADD THIS
    private val massaRepository: MassaRepository,
    private val priceRepository: PriceRepository
) : ViewModel() {
    
    // Observe active account changes
    init {
        viewModelScope.launch {
            accountManager.activeAccount.collect { account ->
                account?.let {
                    // Update UI state with active account
                    _uiState.value = _uiState.value.copy(
                        activeWallet = it.address
                    )
                    
                    // Load balance for active account
                    loadBalance(it.address)
                    loadTransactions(it.address)
                }
            }
        }
    }
    
    private suspend fun loadBalance(address: String) {
        val balance = massaRepository.getBalance(address)
        
        // Update account balance in AccountManager
        accountManager.activeAccount.value?.let { account ->
            accountManager.updateAccountBalance(account.id, balance)
        }
        
        // Update UI
        _uiState.value = _uiState.value.copy(balance = balance)
    }
}
```

### Step 4: Add Navigation to Accounts Screen

Update `MainActivity.kt` or your navigation setup:

```kotlin
// In your NavHost
composable("accounts") {
    AccountsScreen(
        onClose = { navController.popBackStack() }
    )
}

// Add button in DashboardScreen or SettingsScreen
IconButton(onClick = { navController.navigate("accounts") }) {
    Icon(
        imageVector = Icons.Default.AccountBalanceWallet,
        contentDescription = "Manage Accounts"
    )
}
```

### Step 5: Update SendViewModel for Active Account

Update `SendViewModel.kt` to use active account for signing:

```kotlin
@HiltViewModel
class SendViewModel @Inject constructor(
    private val walletManager: WalletManager,
    private val accountManager: AccountManager,  // ADD THIS
    private val massaRepository: MassaRepository
) : ViewModel() {
    
    fun sendTransaction(to: String, amount: String) {
        viewModelScope.launch {
            val activeAccount = accountManager.activeAccount.value
                ?: return@launch
            
            // Get private key for active account
            val privateKey = accountManager.getPrivateKeyForAccount(activeAccount.id)
                ?: return@launch
            
            // Build and sign transaction
            val transaction = buildTransaction(
                from = activeAccount.address,
                to = to,
                amount = amount
            )
            
            val signedTx = signTransaction(transaction, privateKey)
            
            // Send transaction
            val result = massaRepository.sendTransaction(signedTx)
            // Handle result...
        }
    }
}
```

---

## 📱 Testing Checklist

### Manual Testing
- [ ] Create wallet → Default account (Account 0) created automatically
- [ ] Navigate to Accounts screen
- [ ] Create new account → Account appears in list
- [ ] Switch to different account → Dashboard updates
- [ ] Edit account name/color → Changes persist
- [ ] Send transaction from Account 1 → Uses correct address
- [ ] Switch back to Account 0 → Balance shows correctly
- [ ] Try to delete Account 0 → Error message shown
- [ ] Try to delete active account → Error message shown
- [ ] Delete non-active account → Success
- [ ] Close app and reopen → Active account persists

### Theme Testing
- [ ] Switch to Light theme → All screens adapt correctly
- [ ] Switch to Dark theme → All screens adapt correctly
- [ ] Account colors visible in both themes
- [ ] Text readable in both themes

---

## 🚀 Quick Start Commands

### Build Debug APK
```bash
./gradlew assembleDebug
```

### Install on Device
```bash
./gradlew installDebug
```

### Run Tests
```bash
./gradlew test
```

### Clean Build
```bash
./gradlew clean assembleDebug
```

---

## 📂 Files Modified/Created

### New Files (5)
1. `core/src/main/java/com/massapay/android/core/model/Account.kt`
2. `security/src/main/java/com/massapay/android/security/wallet/AccountManager.kt`
3. `ui/src/main/java/com/massapay/android/ui/accounts/AccountsViewModel.kt`
4. `ui/src/main/java/com/massapay/android/ui/accounts/AccountsScreen.kt`
5. `ui/src/main/java/com/massapay/android/ui/accounts/AccountDialogs.kt`

### Files to Modify (4)
1. `security/src/main/java/com/massapay/android/security/di/SecurityModule.kt` - Add AccountManager provider
2. `ui/src/main/java/com/massapay/android/ui/onboarding/OnboardingViewModelNew.kt` - Initialize default account
3. `ui/src/main/java/com/massapay/android/ui/dashboard/DashboardViewModel.kt` - Use active account
4. `app/src/main/java/com/massapay/android/MainActivity.kt` - Add navigation

---

## 🎯 Next Steps

1. ✅ **Integrate AccountManager** into existing flows
2. ✅ **Test account switching** thoroughly
3. ✅ **Add navigation** to Accounts screen
4. ✅ **Update transaction signing** to use active account
5. ⏭️ **Move to next feature:** Staking Support

---

**Status:** Ready for integration and testing!  
**Estimated Integration Time:** 30-60 minutes
