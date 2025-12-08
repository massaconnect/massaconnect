package com.massapay.android.ui.walletconnect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.massapay.android.security.wallet.AccountManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WalletConnectUiState(
    val connectionState: WalletConnectState = WalletConnectState.Disconnected,
    val activeSessions: List<WalletConnectSession> = emptyList(),
    val pendingProposal: SessionProposalUi? = null,
    val pendingRequest: WalletConnectRequest? = null,
    val error: String? = null,
    val isScanning: Boolean = false,
    val walletAddress: String = ""
)

data class SessionProposalUi(
    val proposerPublicKey: String,
    val name: String,
    val description: String,
    val url: String,
    val icon: String?
)

@HiltViewModel
class WalletConnectViewModel @Inject constructor(
    private val walletConnectService: WalletConnectService,
    private val accountManager: AccountManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(WalletConnectUiState())
    val uiState: StateFlow<WalletConnectUiState> = _uiState.asStateFlow()
    
    init {
        observeWalletConnectState()
        observeWalletConnectEvents()
        observePendingRequests()
        loadWalletCredentials()
        loadActiveSessions()
    }
    
    private fun observeWalletConnectState() {
        viewModelScope.launch {
            walletConnectService.connectionState.collect { state ->
                _uiState.update { it.copy(connectionState = state) }
            }
        }
    }
    
    private fun observeWalletConnectEvents() {
        viewModelScope.launch {
            walletConnectService.events.collect { event ->
                when (event) {
                    is WalletConnectEvent.SessionProposal -> {
                        _uiState.update { state ->
                            state.copy(
                                pendingProposal = SessionProposalUi(
                                    proposerPublicKey = event.proposerPublicKey,
                                    name = event.name,
                                    description = event.description,
                                    url = event.url,
                                    icon = event.icons.firstOrNull()
                                )
                            )
                        }
                    }
                    is WalletConnectEvent.SessionEstablished -> {
                        _uiState.update { state ->
                            state.copy(
                                pendingProposal = null,
                                activeSessions = state.activeSessions + event.session
                            )
                        }
                    }
                    is WalletConnectEvent.SessionDeleted -> {
                        _uiState.update { state ->
                            state.copy(
                                activeSessions = state.activeSessions.filter { it.topic != event.topic }
                            )
                        }
                    }
                    is WalletConnectEvent.Error -> {
                        _uiState.update { it.copy(error = event.message) }
                    }
                }
            }
        }
    }
    
    private fun observePendingRequests() {
        viewModelScope.launch {
            walletConnectService.pendingRequests.collect { request ->
                _uiState.update { it.copy(pendingRequest = request) }
            }
        }
    }
    
    private fun loadWalletCredentials() {
        viewModelScope.launch {
            accountManager.activeAccount.collect { account ->
                account?.let {
                    walletConnectService.setWalletCredentials(
                        address = it.address,
                        privateKey = it.privateKeyS1,
                        publicKey = it.publicKey
                    )
                    _uiState.update { state -> state.copy(walletAddress = it.address) }
                }
            }
        }
    }
    
    private fun loadActiveSessions() {
        viewModelScope.launch {
            val sessions = walletConnectService.getActiveSessions()
            _uiState.update { it.copy(activeSessions = sessions) }
        }
    }
    
    fun connectWithUri(wcUri: String) {
        walletConnectService.pair(wcUri)
    }
    
    fun approveSession() {
        val proposal = _uiState.value.pendingProposal ?: return
        walletConnectService.approveSession(proposal.proposerPublicKey)
    }
    
    fun rejectSession() {
        val proposal = _uiState.value.pendingProposal ?: return
        walletConnectService.rejectSession(proposal.proposerPublicKey)
        _uiState.update { it.copy(pendingProposal = null) }
    }
    
    fun disconnectSession(topic: String) {
        walletConnectService.disconnectSession(topic)
    }
    
    fun approveRequest(result: String) {
        val request = _uiState.value.pendingRequest ?: return
        walletConnectService.respondToRequest(request.topic, request.requestId, result)
        _uiState.update { it.copy(pendingRequest = null) }
    }
    
    fun rejectRequest() {
        val request = _uiState.value.pendingRequest ?: return
        walletConnectService.rejectRequest(request.topic, request.requestId)
        _uiState.update { it.copy(pendingRequest = null) }
    }
    
    fun setScanning(isScanning: Boolean) {
        _uiState.update { it.copy(isScanning = isScanning) }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
