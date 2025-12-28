package com.massapay.android.ui.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.massapay.android.core.model.Account
import com.massapay.android.core.model.AccountColor
import com.massapay.android.core.model.CreateAccountRequest
import com.massapay.android.core.model.UpdateAccountRequest
import com.massapay.android.security.wallet.AccountManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val accountManager: AccountManager
) : ViewModel() {
    
    val accounts: StateFlow<List<Account>> = accountManager.accounts
    val activeAccount: StateFlow<Account?> = accountManager.activeAccount
    
    private val _uiState = MutableStateFlow(AccountsUiState())
    val uiState: StateFlow<AccountsUiState> = _uiState.asStateFlow()
    
    init {
        loadAccounts()
    }
    
    private fun loadAccounts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            // Accounts are already loaded via StateFlow from AccountManager
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }
    
    fun createAccount(name: String, color: AccountColor? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCreating = true, error = null)
            
            val request = CreateAccountRequest(name = name, color = color)
            val result = accountManager.createAccount(request)
            
            result.fold(
                onSuccess = { account ->
                    _uiState.value = _uiState.value.copy(
                        isCreating = false,
                        showCreateDialog = false,
                        successMessage = "Account '${account.name}' created successfully"
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isCreating = false,
                        error = error.message ?: "Failed to create account"
                    )
                }
            )
        }
    }
    
    fun updateAccount(accountId: String, name: String? = null, color: AccountColor? = null) {
        viewModelScope.launch {
            val request = UpdateAccountRequest(
                accountId = accountId,
                name = name,
                color = color
            )
            
            val result = accountManager.updateAccount(request)
            
            result.fold(
                onSuccess = { account ->
                    _uiState.value = _uiState.value.copy(
                        showEditDialog = false,
                        successMessage = "Account updated successfully"
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        error = error.message ?: "Failed to update account"
                    )
                }
            )
        }
    }
    
    fun deleteAccount(accountId: String) {
        viewModelScope.launch {
            val result = accountManager.deleteAccount(accountId)
            
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        showDeleteDialog = false,
                        successMessage = "Account deleted successfully"
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        showDeleteDialog = false,
                        error = error.message ?: "Failed to delete account"
                    )
                }
            )
        }
    }
    
    fun switchAccount(accountId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSwitching = true)
            
            val result = accountManager.setActiveAccount(accountId)
            
            result.fold(
                onSuccess = { account ->
                    _uiState.value = _uiState.value.copy(
                        isSwitching = false,
                        accountSwitched = true,
                        successMessage = "Switched to '${account.name}'"
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isSwitching = false,
                        error = error.message ?: "Failed to switch account"
                    )
                }
            )
        }
    }
    
    fun getTotalBalance(): Double {
        return accountManager.getTotalBalance()
    }
    
    // UI State Management
    fun showAddOptions() {
        _uiState.value = _uiState.value.copy(showAddOptions = true)
    }
    
    fun hideAddOptions() {
        _uiState.value = _uiState.value.copy(showAddOptions = false)
    }
    
    fun showCreateDialog() {
        _uiState.value = _uiState.value.copy(showCreateDialog = true)
    }
    
    fun hideCreateDialog() {
        _uiState.value = _uiState.value.copy(showCreateDialog = false, error = null)
    }
    
    fun showImportDialog() {
        _uiState.value = _uiState.value.copy(showImportDialog = true, error = null)
    }
    
    fun hideImportDialog() {
        _uiState.value = _uiState.value.copy(showImportDialog = false, error = null)
    }
    
    fun importAccountFromS1(name: String, s1PrivateKey: String, color: AccountColor? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isImporting = true, error = null)
            
            val result = accountManager.importAccountFromS1(name, s1PrivateKey, color)
            
            result.fold(
                onSuccess = { account ->
                    _uiState.value = _uiState.value.copy(
                        isImporting = false,
                        showImportDialog = false,
                        successMessage = "Account '${account.name}' imported successfully"
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isImporting = false,
                        error = error.message ?: "Failed to import account"
                    )
                }
            )
        }
    }
    
    fun showEditDialog(account: Account) {
        _uiState.value = _uiState.value.copy(
            showEditDialog = true,
            selectedAccount = account
        )
    }
    
    fun hideEditDialog() {
        _uiState.value = _uiState.value.copy(
            showEditDialog = false,
            selectedAccount = null
        )
    }
    
    fun showDeleteDialog(account: Account) {
        _uiState.value = _uiState.value.copy(
            showDeleteDialog = true,
            selectedAccount = account
        )
    }
    
    fun hideDeleteDialog() {
        _uiState.value = _uiState.value.copy(
            showDeleteDialog = false,
            selectedAccount = null
        )
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }
    
    fun clearAccountSwitched() {
        _uiState.value = _uiState.value.copy(accountSwitched = false)
    }
}

data class AccountsUiState(
    val isLoading: Boolean = false,
    val isCreating: Boolean = false,
    val isImporting: Boolean = false,
    val isSwitching: Boolean = false,
    val accountSwitched: Boolean = false,
    val showAddOptions: Boolean = false,
    val showCreateDialog: Boolean = false,
    val showImportDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val selectedAccount: Account? = null,
    val error: String? = null,
    val successMessage: String? = null
)
