package com.massapay.android.ui.dapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.massapay.android.core.model.*
import com.massapay.android.security.wallet.AccountManager
import com.massapay.android.network.repository.MassaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DAppBrowserUiState(
    val currentUrl: String = "",
    val pageTitle: String = "",
    val isLoading: Boolean = false,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val isConnected: Boolean = false,
    val connectedDApp: ConnectedDApp? = null,
    val bookmarks: List<DAppBookmark> = MassaDApps.allDApps,
    val recentDApps: List<DAppBookmark> = emptyList(),
    val selectedCategory: DAppCategory = DAppCategory.ALL,
    val dialogState: DAppDialogState = DAppDialogState.None,
    val walletAddress: String = "",
    val privateKey: String = "",
    val publicKey: String = "",
    val error: String? = null,
    // Flag to trigger JavaScript force update after connection
    val pendingForceUpdate: Boolean = false,
    // Flag to trigger page reload after connection (for DApps like Dusa that need full refresh)
    val pendingPageReload: Boolean = false
)

sealed class DAppDialogState {
    object None : DAppDialogState()
    data class ConnectRequest(val request: DAppConnectRequest) : DAppDialogState()
    data class SignRequest(val request: DAppSignRequest) : DAppDialogState()
    data class TransactionRequest(val request: DAppTransactionRequest) : DAppDialogState()
}

@HiltViewModel
class DAppBrowserViewModel @Inject constructor(
    private val accountManager: AccountManager,
    private val massaRepository: MassaRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(DAppBrowserUiState())
    val uiState: StateFlow<DAppBrowserUiState> = _uiState.asStateFlow()
    
    // Pending request callbacks
    private var pendingRequestId: Int? = null
    private var pendingCallback: ((String) -> Unit)? = null
    
    init {
        loadWalletAddress()
    }
    
    private fun loadWalletAddress() {
        viewModelScope.launch {
            accountManager.activeAccount.collect { account: Account? ->
                account?.let { acc ->
                    _uiState.update { state ->
                        state.copy(
                            walletAddress = acc.address,
                            privateKey = acc.privateKeyS1,
                            publicKey = acc.publicKey
                        )
                    }
                }
            }
        }
    }
    
    fun updateUrl(url: String) {
        _uiState.update { it.copy(currentUrl = url) }
    }
    
    fun updatePageTitle(title: String) {
        _uiState.update { it.copy(pageTitle = title) }
    }
    
    fun updateLoadingState(isLoading: Boolean) {
        _uiState.update { it.copy(isLoading = isLoading) }
    }
    
    fun updateNavigationState(canGoBack: Boolean, canGoForward: Boolean) {
        _uiState.update { it.copy(canGoBack = canGoBack, canGoForward = canGoForward) }
    }
    
    fun selectCategory(category: DAppCategory) {
        _uiState.update { 
            it.copy(
                selectedCategory = category,
                bookmarks = MassaDApps.getDAppsByCategory(category)
            )
        }
    }

    /**
     * Handle incoming request from DApp via JavaScript bridge
     */
    fun handleDAppRequest(
        requestJson: String,
        sendResponse: (String) -> Unit
    ) {
        android.util.Log.d("DAppBrowser", "handleDAppRequest received: $requestJson")
        viewModelScope.launch {
            try {
                val request = parseDAppRequest(requestJson)
                android.util.Log.d("DAppBrowser", "Parsed request method: ${request.method}")
                
                when (request.method) {
                    "wallet_connect" -> handleConnectRequest(request, sendResponse)
                    "massa_getBalance" -> handleGetBalance(request, sendResponse)
                    "massa_getNetwork" -> handleGetNetwork(request, sendResponse)
                    "massa_signMessage" -> handleSignMessage(request, sendResponse)
                    "massa_sendTransaction" -> handleSendTransaction(request, sendResponse)
                    "massa_callSmartContract" -> {
                        android.util.Log.d("DAppBrowser", "Routing to handleCallSmartContract")
                        handleCallSmartContract(request, sendResponse)
                    }
                    "massa_executeSmartContract" -> handleExecuteSmartContract(request, sendResponse)
                    "massa_executeBytecode" -> {
                        android.util.Log.d("DAppBrowser", "Routing to handleExecuteBytecode")
                        handleExecuteBytecode(request, sendResponse)
                    }
                    "massa_buyRolls" -> handleBuyRolls(request, sendResponse)
                    "massa_sellRolls" -> handleSellRolls(request, sendResponse)
                    "wallet_disconnect" -> handleDisconnect(request, sendResponse)
                    else -> {
                        sendErrorResponse(request.id, DAppErrorCodes.UNSUPPORTED_METHOD, 
                            "Method ${request.method} not supported", sendResponse)
                    }
                }
            } catch (e: Exception) {
                sendErrorResponse(0, DAppErrorCodes.INTERNAL_ERROR, 
                    e.message ?: "Unknown error", sendResponse)
            }
        }
    }
    
    private data class ParsedRequest(
        val id: Int,
        val method: String,
        val params: Map<String, Any>
    )
    
    private fun parseDAppRequest(json: String): ParsedRequest {
        val jsonObject = org.json.JSONObject(json)
        val id = jsonObject.optInt("id", 0)
        val method = jsonObject.optString("method", "")
        
        // Handle params as object or array
        val params = mutableMapOf<String, Any>()
        val paramsObj = jsonObject.optJSONObject("params")
        val paramsArray = jsonObject.optJSONArray("params")
        
        if (paramsObj != null) {
            paramsObj.keys().forEach { key ->
                params[key] = paramsObj.get(key)
            }
        } else if (paramsArray != null) {
            for (i in 0 until paramsArray.length()) {
                params["param$i"] = paramsArray.get(i)
            }
        }
        
        return ParsedRequest(id, method, params)
    }
    
    private fun handleConnectRequest(request: ParsedRequest, sendResponse: (String) -> Unit) {
        val currentUrl = _uiState.value.currentUrl
        val origin = try {
            java.net.URI(currentUrl).host ?: currentUrl
        } catch (e: Exception) {
            currentUrl
        }
        
        // Store pending request
        pendingRequestId = request.id
        pendingCallback = sendResponse
        
        // Show connect dialog
        _uiState.update { state ->
            state.copy(
                dialogState = DAppDialogState.ConnectRequest(
                    DAppConnectRequest(
                        origin = origin,
                        name = state.pageTitle.ifEmpty { origin },
                        requestedPermissions = listOf(
                            DAppPermission.VIEW_ACCOUNT,
                            DAppPermission.SIGN_TRANSACTION
                        )
                    )
                )
            )
        }
    }
    
    private suspend fun handleGetBalance(request: ParsedRequest, sendResponse: (String) -> Unit) {
        if (!_uiState.value.isConnected) {
            sendErrorResponse(request.id, DAppErrorCodes.UNAUTHORIZED, 
                "Wallet not connected", sendResponse)
            return
        }
        
        try {
            val address = request.params["address"]?.toString() 
                ?: request.params["param0"]?.toString()
                ?: _uiState.value.walletAddress
            
            val result = massaRepository.getAddressBalance(address)
            when (result) {
                is com.massapay.android.core.util.Result.Success -> {
                    sendSuccessResponse(request.id, mapOf(
                        "address" to address,
                        "balance" to result.data
                    ), sendResponse)
                }
                is com.massapay.android.core.util.Result.Error -> {
                    sendErrorResponse(request.id, DAppErrorCodes.INTERNAL_ERROR, 
                        result.exception.message ?: "Failed to get balance", sendResponse)
                }
                is com.massapay.android.core.util.Result.Loading -> {
                    // Ignore loading state
                }
            }
        } catch (e: Exception) {
            sendErrorResponse(request.id, DAppErrorCodes.INTERNAL_ERROR, 
                e.message ?: "Failed to get balance", sendResponse)
        }
    }
    
    private fun handleGetNetwork(request: ParsedRequest, sendResponse: (String) -> Unit) {
        sendSuccessResponse(request.id, mapOf(
            "networkId" to "mainnet",
            "chainId" to 77658377L,
            "name" to "Massa Mainnet",
            "url" to "https://mainnet.massa.net/api/v2"
        ), sendResponse)
    }
    
    private fun handleSignMessage(request: ParsedRequest, sendResponse: (String) -> Unit) {
        if (!_uiState.value.isConnected) {
            sendErrorResponse(request.id, DAppErrorCodes.UNAUTHORIZED, 
                "Wallet not connected", sendResponse)
            return
        }
        
        val message = request.params["message"]?.toString() 
            ?: request.params["param0"]?.toString() 
            ?: ""
        
        pendingRequestId = request.id
        pendingCallback = sendResponse
        
        _uiState.update { state ->
            state.copy(
                dialogState = DAppDialogState.SignRequest(
                    DAppSignRequest(
                        origin = state.connectedDApp?.origin ?: "",
                        message = message
                    )
                )
            )
        }
    }
    
    private fun handleSendTransaction(request: ParsedRequest, sendResponse: (String) -> Unit) {
        if (!_uiState.value.isConnected) {
            sendErrorResponse(request.id, DAppErrorCodes.UNAUTHORIZED, 
                "Wallet not connected", sendResponse)
            return
        }
        
        try {
            pendingRequestId = request.id
            pendingCallback = sendResponse
            
            _uiState.update { state ->
                state.copy(
                    dialogState = DAppDialogState.TransactionRequest(
                        DAppTransactionRequest(
                            origin = state.connectedDApp?.origin ?: "",
                            toAddress = request.params["toAddress"]?.toString() 
                                ?: request.params["to"]?.toString() ?: "",
                            amount = request.params["amount"]?.toString() ?: "0",
                            fee = request.params["fee"]?.toString(),
                            data = request.params["data"]?.toString(),
                            contractAddress = request.params["contractAddress"]?.toString(),
                            functionName = request.params["functionName"]?.toString(),
                            parameters = null
                        )
                    )
                )
            }
        } catch (e: Exception) {
            sendErrorResponse(request.id, DAppErrorCodes.INVALID_PARAMS, 
                e.message ?: "Invalid parameters", sendResponse)
        }
    }
    
    private fun handleBuyRolls(request: ParsedRequest, sendResponse: (String) -> Unit) {
        if (!_uiState.value.isConnected) {
            sendErrorResponse(request.id, DAppErrorCodes.UNAUTHORIZED, 
                "Wallet not connected", sendResponse)
            return
        }
        
        val amount = request.params["amount"]?.toString() ?: "1"
        val fee = request.params["fee"]?.toString()
        
        pendingRequestId = request.id
        pendingCallback = sendResponse
        
        _uiState.update { state ->
            state.copy(
                dialogState = DAppDialogState.TransactionRequest(
                    DAppTransactionRequest(
                        origin = state.connectedDApp?.origin ?: "Staking",
                        toAddress = "",
                        amount = amount,
                        fee = fee,
                        data = "BUY_ROLLS",
                        contractAddress = null,
                        functionName = "buyRolls",
                        parameters = null
                    )
                )
            )
        }
    }
    
    private fun handleSellRolls(request: ParsedRequest, sendResponse: (String) -> Unit) {
        if (!_uiState.value.isConnected) {
            sendErrorResponse(request.id, DAppErrorCodes.UNAUTHORIZED, 
                "Wallet not connected", sendResponse)
            return
        }
        
        val amount = request.params["amount"]?.toString() ?: "1"
        val fee = request.params["fee"]?.toString()
        
        pendingRequestId = request.id
        pendingCallback = sendResponse
        
        _uiState.update { state ->
            state.copy(
                dialogState = DAppDialogState.TransactionRequest(
                    DAppTransactionRequest(
                        origin = state.connectedDApp?.origin ?: "Staking",
                        toAddress = "",
                        amount = amount,
                        fee = fee,
                        data = "SELL_ROLLS",
                        contractAddress = null,
                        functionName = "sellRolls",
                        parameters = null
                    )
                )
            )
        }
    }
    
    private fun handleCallSmartContract(request: ParsedRequest, sendResponse: (String) -> Unit) {
        // For smart contract calls, we check if wallet is ready (has address and keys)
        // The connection state might not be updated if DApp connected via early provider
        val currentState = _uiState.value
        if (currentState.walletAddress.isEmpty() || currentState.privateKey.isEmpty()) {
            android.util.Log.e("DAppBrowser", "Wallet not ready: address=${currentState.walletAddress.isNotEmpty()}, hasPrivateKey=${currentState.privateKey.isNotEmpty()}")
            sendErrorResponse(request.id, DAppErrorCodes.UNAUTHORIZED, 
                "Wallet not ready", sendResponse)
            return
        }
        
        // Auto-set connected state if we have wallet info
        if (!currentState.isConnected && currentState.walletAddress.isNotEmpty()) {
            android.util.Log.d("DAppBrowser", "Auto-connecting wallet for smart contract call")
            _uiState.update { it.copy(isConnected = true) }
        }
        
        try {
            // Extract smart contract call parameters
            val targetAddress = request.params["targetAddress"]?.toString() 
                ?: request.params["contractAddress"]?.toString()
                ?: request.params["address"]?.toString()
                ?: request.params["toAddr"]?.toString()
                ?: ""
            val functionName = request.params["functionName"]?.toString()
                ?: request.params["targetFunction"]?.toString()
                ?: request.params["func"]?.toString()
                ?: ""
            val coins = request.params["coins"]?.toString() 
                ?: request.params["amount"]?.toString()
                ?: "0"
            val fee = request.params["fee"]?.toString()
            val parameter = request.params["parameter"]?.toString()
                ?: request.params["params"]?.toString()
            val unsafeParameters = request.params["unsafeParameters"]?.toString()
                ?: request.params["unsafeParams"]?.toString()
            val maxGas = request.params["maxGas"]?.toString()
                ?: request.params["gasLimit"]?.toString()
            
            android.util.Log.d("DAppBrowser", "CallSmartContract: target=$targetAddress, func=$functionName, coins=$coins, maxGas=$maxGas")
            android.util.Log.d("DAppBrowser", "CallSmartContract: parameter=$parameter, unsafeParams=$unsafeParameters")
            
            pendingRequestId = request.id
            pendingCallback = sendResponse
            
            // Store extra params for smart contract execution
            pendingSmartContractParams = SmartContractParams(
                targetAddress = targetAddress,
                functionName = functionName,
                parameter = parameter,
                unsafeParameters = unsafeParameters,
                coins = coins,
                maxGas = maxGas,
                fee = fee
            )
            
            // Show transaction confirmation dialog
            _uiState.update { state ->
                state.copy(
                    dialogState = DAppDialogState.TransactionRequest(
                        DAppTransactionRequest(
                            origin = state.connectedDApp?.origin ?: "",
                            toAddress = targetAddress,
                            amount = coins,
                            fee = fee,
                            data = unsafeParameters ?: parameter,
                            contractAddress = targetAddress,
                            functionName = functionName,
                            parameters = null
                        )
                    )
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("DAppBrowser", "CallSmartContract error: ${e.message}", e)
            sendErrorResponse(request.id, DAppErrorCodes.INVALID_PARAMS, 
                e.message ?: "Invalid parameters", sendResponse)
        }
    }
    
    // Data class to store pending smart contract call params
    private data class SmartContractParams(
        val targetAddress: String,
        val functionName: String,
        val parameter: String?,
        val unsafeParameters: String?,
        val coins: String,
        val maxGas: String?,
        val fee: String?
    )
    
    private var pendingSmartContractParams: SmartContractParams? = null
    
    private fun handleExecuteSmartContract(request: ParsedRequest, sendResponse: (String) -> Unit) {
        // Same as send transaction - needs confirmation
        handleSendTransaction(request, sendResponse)
    }
    
    private fun handleExecuteBytecode(request: ParsedRequest, sendResponse: (String) -> Unit) {
        // For bytecode execution, we check if wallet is ready
        val currentState = _uiState.value
        if (currentState.walletAddress.isEmpty() || currentState.privateKey.isEmpty()) {
            android.util.Log.e("DAppBrowser", "Wallet not ready for executeBytecode")
            sendErrorResponse(request.id, DAppErrorCodes.UNAUTHORIZED, 
                "Wallet not ready", sendResponse)
            return
        }
        
        // Auto-set connected state if we have wallet info
        if (!currentState.isConnected && currentState.walletAddress.isNotEmpty()) {
            android.util.Log.d("DAppBrowser", "Auto-connecting wallet for executeBytecode")
            _uiState.update { it.copy(isConnected = true) }
        }
        
        try {
            // Extract bytecode execution parameters
            val bytecode = request.params["bytecode"]?.toString()
                ?: request.params["bytecodeBase64"]?.toString()
                ?: request.params["data"]?.toString()
                ?: ""
            val datastore = request.params["datastore"]?.toString()
                ?: request.params["datastoreEntries"]?.toString()
            val coins = request.params["coins"]?.toString() 
                ?: request.params["maxCoins"]?.toString()
                ?: request.params["amount"]?.toString()
                ?: "0"
            val fee = request.params["fee"]?.toString()
            val maxGas = request.params["maxGas"]?.toString()
                ?: request.params["gasLimit"]?.toString()
            
            android.util.Log.d("DAppBrowser", "ExecuteBytecode: bytecodeLen=${bytecode.length}, coins=$coins, maxGas=$maxGas")
            
            pendingRequestId = request.id
            pendingCallback = sendResponse
            
            // Store params for bytecode execution
            pendingSmartContractParams = SmartContractParams(
                targetAddress = "", // No target address for bytecode execution
                functionName = "executeBytecode",
                parameter = bytecode,
                unsafeParameters = datastore,
                coins = coins,
                maxGas = maxGas,
                fee = fee
            )
            
            // Show transaction confirmation dialog
            _uiState.update { state ->
                state.copy(
                    dialogState = DAppDialogState.TransactionRequest(
                        DAppTransactionRequest(
                            origin = state.connectedDApp?.origin ?: "DApp",
                            toAddress = "Bytecode Execution",
                            amount = coins,
                            fee = fee,
                            data = "Execute Bytecode (${bytecode.take(20)}...)",
                            contractAddress = null,
                            functionName = "executeBytecode",
                            parameters = null
                        )
                    )
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("DAppBrowser", "ExecuteBytecode error: ${e.message}", e)
            sendErrorResponse(request.id, DAppErrorCodes.INVALID_PARAMS, 
                e.message ?: "Invalid parameters", sendResponse)
        }
    }

    private fun handleDisconnect(request: ParsedRequest, sendResponse: (String) -> Unit) {
        _uiState.update { state ->
            state.copy(
                isConnected = false,
                connectedDApp = null
            )
        }
        sendSuccessResponse(request.id, true, sendResponse)
    }
    
    /**
     * User approved connection request
     */
    fun approveConnect() {
        val requestId = pendingRequestId ?: return
        val callback = pendingCallback ?: return
        val request = (_uiState.value.dialogState as? DAppDialogState.ConnectRequest)?.request ?: return
        
        _uiState.update { state ->
            state.copy(
                isConnected = true,
                connectedDApp = ConnectedDApp(
                    origin = request.origin,
                    name = request.name,
                    iconUrl = request.iconUrl,
                    permissions = request.requestedPermissions
                ),
                dialogState = DAppDialogState.None,
                pendingForceUpdate = true, // Signal to force JS update
                pendingPageReload = true // Signal to reload page after connection
            )
        }
        
        sendSuccessResponse(requestId, mapOf(
            "address" to _uiState.value.walletAddress,
            "networkId" to "mainnet"
        ), callback)
        
        clearPendingRequest()
    }
    
    /**
     * Clear the force update flag after it's been handled
     */
    fun clearForceUpdate() {
        _uiState.update { it.copy(pendingForceUpdate = false) }
    }
    
    /**
     * Clear the page reload flag after it's been handled
     */
    fun clearPageReload() {
        _uiState.update { it.copy(pendingPageReload = false) }
    }
    
    /**
     * User approved sign request
     */
    fun approveSign() {
        viewModelScope.launch {
            val requestId = pendingRequestId ?: return@launch
            val callback = pendingCallback ?: return@launch
            val request = (_uiState.value.dialogState as? DAppDialogState.SignRequest)?.request ?: return@launch
            
            try {
                val currentState = _uiState.value
                if (currentState.walletAddress.isNotEmpty()) {
                    // TODO: Implement actual signing with private key
                    val signature = "signed_${request.message.hashCode()}"
                    
                    sendSuccessResponse(requestId, mapOf(
                        "signature" to signature,
                        "publicKey" to currentState.publicKey
                    ), callback)
                } else {
                    sendErrorResponse(requestId, DAppErrorCodes.INTERNAL_ERROR, 
                        "No active account", callback)
                }
            } catch (e: Exception) {
                sendErrorResponse(requestId, DAppErrorCodes.INTERNAL_ERROR, 
                    e.message ?: "Signing failed", callback)
            }
            
            _uiState.update { it.copy(dialogState = DAppDialogState.None) }
            clearPendingRequest()
        }
    }
    
    /**
     * User approved transaction
     */
    fun approveTransaction() {
        viewModelScope.launch {
            val requestId = pendingRequestId ?: return@launch
            val callback = pendingCallback ?: return@launch
            val txRequest = (_uiState.value.dialogState as? DAppDialogState.TransactionRequest)?.request ?: return@launch
            
            try {
                val currentState = _uiState.value
                val fee = txRequest.fee ?: "0.01"
                
                // Check if this is a smart contract call (from handleCallSmartContract)
                val scParams = pendingSmartContractParams
                
                if (scParams != null) {
                    // Check if this is an executeBytecode call
                    if (scParams.functionName == "executeBytecode") {
                        // Execute bytecode operation
                        android.util.Log.d("DAppBrowser", "Executing bytecode operation")
                        
                        val result = massaRepository.executeBytecode(
                            from = currentState.walletAddress,
                            bytecode = scParams.parameter ?: "",
                            datastore = scParams.unsafeParameters,
                            coins = scParams.coins,
                            fee = scParams.fee ?: fee,
                            maxGas = scParams.maxGas,
                            privateKey = currentState.privateKey,
                            publicKey = currentState.publicKey
                        )
                        
                        when (result) {
                            is com.massapay.android.core.util.Result.Success -> {
                                android.util.Log.d("DAppBrowser", "Execute bytecode success: ${result.data}")
                                sendSuccessResponse(requestId, result.data, callback)
                            }
                            is com.massapay.android.core.util.Result.Error -> {
                                android.util.Log.e("DAppBrowser", "Execute bytecode failed: ${result.exception.message}")
                                sendErrorResponse(requestId, DAppErrorCodes.INTERNAL_ERROR, 
                                    result.exception.message ?: "Execute bytecode failed", callback)
                            }
                            is com.massapay.android.core.util.Result.Loading -> {
                                // Ignore loading state
                            }
                        }
                    } else {
                        // Smart contract call (e.g., swap)
                        android.util.Log.d("DAppBrowser", "Executing smart contract call: ${scParams.functionName}")
                        android.util.Log.d("DAppBrowser", "unsafeParameters: ${scParams.unsafeParameters}")
                        
                        val result = massaRepository.callSmartContract(
                            from = currentState.walletAddress,
                            targetAddress = scParams.targetAddress,
                            functionName = scParams.functionName,
                            parameter = scParams.unsafeParameters ?: scParams.parameter,
                            coins = scParams.coins,
                            fee = scParams.fee ?: fee,
                            maxGas = scParams.maxGas,
                            privateKey = currentState.privateKey,
                            publicKey = currentState.publicKey
                        )
                        
                        when (result) {
                            is com.massapay.android.core.util.Result.Success -> {
                                // IMPORTANT: Return operationId as STRING directly for DApps like Dusa
                                android.util.Log.d("DAppBrowser", "Smart contract call success: ${result.data}")
                                sendSuccessResponse(requestId, result.data, callback)
                            }
                            is com.massapay.android.core.util.Result.Error -> {
                                android.util.Log.e("DAppBrowser", "Smart contract call failed: ${result.exception.message}")
                                sendErrorResponse(requestId, DAppErrorCodes.INTERNAL_ERROR, 
                                    result.exception.message ?: "Smart contract call failed", callback)
                            }
                            is com.massapay.android.core.util.Result.Loading -> {
                                // Ignore loading state
                            }
                        }
                    }
                    
                    pendingSmartContractParams = null
                } else {
                    // Regular transaction (MAS transfer)
                    val result = massaRepository.sendTransaction(
                        from = currentState.walletAddress,
                        to = txRequest.toAddress,
                        amount = txRequest.amount,
                        fee = fee,
                        privateKey = currentState.privateKey,
                        publicKey = currentState.publicKey
                    )
                    
                    when (result) {
                        is com.massapay.android.core.util.Result.Success -> {
                            // Return operationId as STRING for consistency
                            sendSuccessResponse(requestId, result.data, callback)
                        }
                        is com.massapay.android.core.util.Result.Error -> {
                            sendErrorResponse(requestId, DAppErrorCodes.INTERNAL_ERROR, 
                                result.exception.message ?: "Transaction failed", callback)
                        }
                        is com.massapay.android.core.util.Result.Loading -> {
                            // Ignore loading state
                        }
                    }
                }
            } catch (e: Exception) {
                sendErrorResponse(requestId, DAppErrorCodes.INTERNAL_ERROR, 
                    e.message ?: "Transaction failed", callback)
            }
            
            _uiState.update { it.copy(dialogState = DAppDialogState.None) }
            clearPendingRequest()
        }
    }
    
    /**
     * User rejected the request
     */
    fun rejectRequest() {
        val requestId = pendingRequestId ?: return
        val callback = pendingCallback ?: return
        
        sendErrorResponse(requestId, DAppErrorCodes.USER_REJECTED, "User rejected", callback)
        
        _uiState.update { it.copy(dialogState = DAppDialogState.None) }
        clearPendingRequest()
    }
    
    private fun clearPendingRequest() {
        pendingRequestId = null
        pendingCallback = null
    }
    
    private fun sendSuccessResponse(id: Int, result: Any, callback: (String) -> Unit) {
        val response = org.json.JSONObject().apply {
            put("id", id)
            put("result", when (result) {
                is Map<*, *> -> org.json.JSONObject(result as Map<String, Any>)
                else -> result
            })
        }
        val responseStr = response.toString()
        android.util.Log.d("DAppBrowser", "Sending success response: $responseStr")
        callback(responseStr)
        android.util.Log.d("DAppBrowser", "Success response callback executed for id: $id")
    }
    
    private fun sendErrorResponse(id: Int, code: Int, message: String, callback: (String) -> Unit) {
        val response = org.json.JSONObject().apply {
            put("id", id)
            put("error", org.json.JSONObject().apply {
                put("code", code)
                put("message", message)
            })
        }
        val responseStr = response.toString()
        android.util.Log.d("DAppBrowser", "Sending error response: $responseStr")
        callback(responseStr)
        android.util.Log.d("DAppBrowser", "Error response callback executed for id: $id")
    }
    
    fun addBookmark(url: String, title: String) {
        _uiState.update { state ->
            val newBookmark = DAppBookmark(url = url, name = title)
            state.copy(bookmarks = state.bookmarks + newBookmark)
        }
    }
    
    fun removeBookmark(bookmark: DAppBookmark) {
        _uiState.update { state ->
            state.copy(bookmarks = state.bookmarks.filter { it.url != bookmark.url })
        }
    }
    
    fun addToRecent(url: String, title: String) {
        _uiState.update { state ->
            val newRecent = DAppBookmark(url = url, name = title)
            val updated = listOf(newRecent) + state.recentDApps.filter { it.url != url }
            state.copy(recentDApps = updated.take(10)) // Keep last 10
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
