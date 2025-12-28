package com.massapay.android.ui.walletconnect

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WalletConnect v2 Service for MassaPay
 * Simplified implementation using OkHttp WebSocket
 */
@Singleton
class WalletConnectService @Inject constructor(
    private val baseClient: OkHttpClient
) {
    
    companion object {
        private const val TAG = "WalletConnectService"
        private const val RELAY_URL = "wss://relay.walletconnect.com"
        
        // Massa chain
        const val MASSA_CHAIN_ID = "massa:77658377"
        const val MASSA_NAMESPACE = "massa"
    }
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val client = baseClient.newBuilder()
        .pingInterval(java.time.Duration.ofSeconds(30))
        .build()
    
    private var webSocket: WebSocket? = null
    private var projectId: String = ""
    private var symKey: ByteArray? = null
    private var pairingTopic: String? = null
    
    // State flows
    private val _connectionState = MutableStateFlow<WalletConnectState>(WalletConnectState.Disconnected)
    val connectionState: StateFlow<WalletConnectState> = _connectionState.asStateFlow()
    
    private val _sessions = MutableStateFlow<List<WalletConnectSession>>(emptyList())
    val sessions: StateFlow<List<WalletConnectSession>> = _sessions.asStateFlow()
    
    private val _events = MutableSharedFlow<WalletConnectEvent>()
    val events: SharedFlow<WalletConnectEvent> = _events.asSharedFlow()
    
    private val _pendingRequests = MutableSharedFlow<WalletConnectRequest>()
    val pendingRequests: SharedFlow<WalletConnectRequest> = _pendingRequests.asSharedFlow()
    
    // Wallet credentials
    private var walletAddress: String = ""
    private var privateKey: String = ""
    private var publicKey: String = ""
    
    // Pending proposals
    private val pendingProposals = mutableMapOf<String, SessionProposalData>()
    
    /**
     * Initialize the service with project ID
     */
    fun initialize(projectId: String) {
        this.projectId = projectId
        _connectionState.value = WalletConnectState.Ready
        Log.d(TAG, "WalletConnect service initialized")
    }
    
    /**
     * Set wallet credentials
     */
    fun setWalletCredentials(address: String, privateKey: String, publicKey: String) {
        this.walletAddress = address
        this.privateKey = privateKey
        this.publicKey = publicKey
        Log.d(TAG, "Wallet credentials set for: $address")
    }
    
    /**
     * Connect to a DApp using WalletConnect URI
     * URI format: wc:topic@version?relay-protocol=irn&symKey=key
     */
    fun pair(wcUri: String) {
        scope.launch {
            try {
                Log.d(TAG, "Pairing with URI: $wcUri")
                _connectionState.value = WalletConnectState.Connecting
                
                // Parse WC URI
                val parsed = parseWcUri(wcUri)
                if (parsed == null) {
                    _connectionState.value = WalletConnectState.Error("Invalid WalletConnect URI")
                    _events.emit(WalletConnectEvent.Error("Invalid WalletConnect URI"))
                    return@launch
                }
                
                pairingTopic = parsed.topic
                symKey = hexToBytes(parsed.symKey)
                
                // Connect to relay
                connectToRelay(parsed.topic)
                
            } catch (e: Exception) {
                Log.e(TAG, "Pairing failed", e)
                _connectionState.value = WalletConnectState.Error(e.message ?: "Pairing failed")
                _events.emit(WalletConnectEvent.Error("Pairing failed: ${e.message}"))
            }
        }
    }
    
    private fun connectToRelay(topic: String) {
        val url = "$RELAY_URL/?projectId=$projectId"
        val request = Request.Builder()
            .url(url)
            .build()
        
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket connected")
                // Subscribe to topic
                subscribeTopic(topic)
            }
            
            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "Received message: ${text.take(100)}...")
                handleRelayMessage(text)
            }
            
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket error", t)
                scope.launch {
                    _connectionState.value = WalletConnectState.Error(t.message ?: "Connection failed")
                    _events.emit(WalletConnectEvent.Error("Connection error: ${t.message}"))
                }
            }
            
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed: $reason")
                scope.launch {
                    _connectionState.value = WalletConnectState.Disconnected
                }
            }
        })
    }
    
    private fun subscribeTopic(topic: String) {
        val subscribeMsg = JSONObject().apply {
            put("id", System.currentTimeMillis())
            put("jsonrpc", "2.0")
            put("method", "irn_subscribe")
            put("params", JSONObject().apply {
                put("topic", topic)
            })
        }
        webSocket?.send(subscribeMsg.toString())
        Log.d(TAG, "Subscribed to topic: $topic")
    }
    
    private fun handleRelayMessage(message: String) {
        try {
            val json = JSONObject(message)
            
            if (json.has("params")) {
                val params = json.getJSONObject("params")
                if (params.has("message")) {
                    val encryptedMessage = params.getString("message")
                    val topic = params.optString("topic", "")
                    decryptAndHandleMessage(encryptedMessage, topic)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to handle relay message", e)
        }
    }
    
    private fun decryptAndHandleMessage(encryptedBase64: String, topic: String) {
        try {
            val key = symKey ?: return
            val encrypted = Base64.decode(encryptedBase64, Base64.NO_WRAP)
            
            // WC v2 uses AES-256-GCM with 12-byte IV prepended
            val iv = encrypted.sliceArray(0 until 12)
            val ciphertext = encrypted.sliceArray(12 until encrypted.size)
            
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val keySpec = SecretKeySpec(key, "AES")
            val gcmSpec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)
            
            val decrypted = cipher.doFinal(ciphertext)
            val messageJson = String(decrypted)
            
            Log.d(TAG, "Decrypted message: ${messageJson.take(200)}...")
            handleDecryptedMessage(messageJson, topic)
            
        } catch (e: Exception) {
            Log.e(TAG, "Decryption failed", e)
        }
    }
    
    private fun handleDecryptedMessage(message: String, topic: String) {
        scope.launch {
            try {
                val json = JSONObject(message)
                val method = json.optString("method", "")
                val id = json.optLong("id", 0)
                val params = json.optJSONObject("params")
                
                when {
                    method == "wc_sessionPropose" -> {
                        handleSessionProposal(id, params, topic)
                    }
                    method == "wc_sessionRequest" -> {
                        handleSessionRequest(id, params, topic)
                    }
                    method == "wc_sessionDelete" -> {
                        handleSessionDelete(topic)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to handle decrypted message", e)
            }
        }
    }
    
    private suspend fun handleSessionProposal(id: Long, params: JSONObject?, topic: String) {
        if (params == null) return
        
        try {
            val proposer = params.getJSONObject("proposer")
            val metadata = proposer.getJSONObject("metadata")
            
            val proposal = SessionProposalData(
                id = id,
                topic = topic,
                proposerPublicKey = proposer.getString("publicKey"),
                name = metadata.getString("name"),
                description = metadata.optString("description", ""),
                url = metadata.optString("url", ""),
                icons = metadata.optJSONArray("icons")?.let { arr ->
                    (0 until arr.length()).map { arr.getString(it) }
                } ?: emptyList()
            )
            
            pendingProposals[proposal.proposerPublicKey] = proposal
            
            _events.emit(
                WalletConnectEvent.SessionProposal(
                    proposerPublicKey = proposal.proposerPublicKey,
                    name = proposal.name,
                    description = proposal.description,
                    url = proposal.url,
                    icons = proposal.icons
                )
            )
            
            Log.d(TAG, "Session proposal received from: ${proposal.name}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse session proposal", e)
        }
    }
    
    private suspend fun handleSessionRequest(id: Long, params: JSONObject?, topic: String) {
        if (params == null) return
        
        try {
            val request = params.getJSONObject("request")
            val method = request.getString("method")
            val requestParams = request.opt("params")?.toString() ?: "{}"
            val chainId = params.optString("chainId", MASSA_CHAIN_ID)
            
            _pendingRequests.emit(
                WalletConnectRequest(
                    topic = topic,
                    requestId = id,
                    method = method,
                    params = requestParams,
                    chainId = chainId
                )
            )
            
            Log.d(TAG, "Session request: $method")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse session request", e)
        }
    }
    
    private suspend fun handleSessionDelete(topic: String) {
        _sessions.value = _sessions.value.filter { it.topic != topic }
        _events.emit(WalletConnectEvent.SessionDeleted(topic))
        Log.d(TAG, "Session deleted: $topic")
    }
    
    /**
     * Approve a session proposal
     */
    fun approveSession(proposerPublicKey: String) {
        scope.launch {
            try {
                val proposal = pendingProposals[proposerPublicKey] ?: return@launch
                
                // Create session response
                val namespaces = JSONObject().apply {
                    put(MASSA_NAMESPACE, JSONObject().apply {
                        put("accounts", JSONArray().apply {
                            put("$MASSA_CHAIN_ID:$walletAddress")
                        })
                        put("methods", JSONArray().apply {
                            put("massa_signMessage")
                            put("massa_sendTransaction")
                            put("massa_callSmartContract")
                            put("massa_getBalance")
                        })
                        put("events", JSONArray().apply {
                            put("accountsChanged")
                            put("chainChanged")
                        })
                    })
                }
                
                val response = JSONObject().apply {
                    put("id", proposal.id)
                    put("jsonrpc", "2.0")
                    put("result", JSONObject().apply {
                        put("relay", JSONObject().put("protocol", "irn"))
                        put("namespaces", namespaces)
                        put("controller", JSONObject().apply {
                            put("publicKey", generateRandomHex(32))
                            put("metadata", JSONObject().apply {
                                put("name", "MassaConnect")
                                put("description", "Massa Blockchain Mobile Wallet")
                                put("url", "https://massapay.app")
                                put("icons", JSONArray().put("https://massapay.app/icon.png"))
                            })
                        })
                    })
                }
                
                // Encrypt and send response
                sendEncryptedMessage(response.toString(), proposal.topic)
                
                // Add to sessions
                val session = WalletConnectSession(
                    topic = proposal.topic,
                    peerName = proposal.name,
                    peerUrl = proposal.url,
                    peerIcon = proposal.icons.firstOrNull() ?: "",
                    expiry = System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000 // 7 days
                )
                
                _sessions.value = _sessions.value + session
                _connectionState.value = WalletConnectState.Connected(session)
                _events.emit(WalletConnectEvent.SessionEstablished(session))
                
                pendingProposals.remove(proposerPublicKey)
                
                Log.d(TAG, "Session approved for: ${proposal.name}")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to approve session", e)
                _events.emit(WalletConnectEvent.Error("Failed to approve session: ${e.message}"))
            }
        }
    }
    
    /**
     * Reject a session proposal
     */
    fun rejectSession(proposerPublicKey: String, reason: String = "User rejected") {
        scope.launch {
            val proposal = pendingProposals[proposerPublicKey] ?: return@launch
            
            val response = JSONObject().apply {
                put("id", proposal.id)
                put("jsonrpc", "2.0")
                put("error", JSONObject().apply {
                    put("code", 5000)
                    put("message", reason)
                })
            }
            
            sendEncryptedMessage(response.toString(), proposal.topic)
            pendingProposals.remove(proposerPublicKey)
            _connectionState.value = WalletConnectState.Ready
            
            Log.d(TAG, "Session rejected")
        }
    }
    
    /**
     * Respond to a session request
     */
    fun respondToRequest(topic: String, requestId: Long, result: String) {
        scope.launch {
            try {
                val response = JSONObject().apply {
                    put("id", requestId)
                    put("jsonrpc", "2.0")
                    put("result", result)
                }
                
                sendEncryptedMessage(response.toString(), topic)
                Log.d(TAG, "Request response sent")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to respond to request", e)
            }
        }
    }
    
    /**
     * Reject a session request
     */
    fun rejectRequest(topic: String, requestId: Long, message: String = "User rejected") {
        scope.launch {
            val response = JSONObject().apply {
                put("id", requestId)
                put("jsonrpc", "2.0")
                put("error", JSONObject().apply {
                    put("code", 4001)
                    put("message", message)
                })
            }
            
            sendEncryptedMessage(response.toString(), topic)
            Log.d(TAG, "Request rejected")
        }
    }
    
    /**
     * Disconnect a session
     */
    fun disconnectSession(sessionTopic: String) {
        scope.launch {
            try {
                val deleteMsg = JSONObject().apply {
                    put("id", System.currentTimeMillis())
                    put("jsonrpc", "2.0")
                    put("method", "wc_sessionDelete")
                    put("params", JSONObject().apply {
                        put("code", 6000)
                        put("message", "User disconnected")
                    })
                }
                
                sendEncryptedMessage(deleteMsg.toString(), sessionTopic)
                
                _sessions.value = _sessions.value.filter { it.topic != sessionTopic }
                
                if (_sessions.value.isEmpty()) {
                    _connectionState.value = WalletConnectState.Ready
                }
                
                Log.d(TAG, "Session disconnected: $sessionTopic")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to disconnect session", e)
            }
        }
    }
    
    /**
     * Get active sessions
     */
    fun getActiveSessions(): List<WalletConnectSession> = _sessions.value
    
    private fun sendEncryptedMessage(message: String, topic: String) {
        try {
            val key = symKey ?: return
            
            // Generate random IV
            val iv = ByteArray(12)
            SecureRandom().nextBytes(iv)
            
            // Encrypt with AES-GCM
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val keySpec = SecretKeySpec(key, "AES")
            val gcmSpec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)
            
            val ciphertext = cipher.doFinal(message.toByteArray())
            
            // Prepend IV to ciphertext
            val encrypted = iv + ciphertext
            val encryptedBase64 = Base64.encodeToString(encrypted, Base64.NO_WRAP)
            
            // Send via relay
            val publishMsg = JSONObject().apply {
                put("id", System.currentTimeMillis())
                put("jsonrpc", "2.0")
                put("method", "irn_publish")
                put("params", JSONObject().apply {
                    put("topic", topic)
                    put("message", encryptedBase64)
                    put("ttl", 300)
                    put("prompt", true)
                    put("tag", 1108) // Session response tag
                })
            }
            
            webSocket?.send(publishMsg.toString())
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send encrypted message", e)
        }
    }
    
    private fun parseWcUri(uri: String): WcUriData? {
        try {
            // Format: wc:topic@version?relay-protocol=irn&symKey=key
            if (!uri.startsWith("wc:")) return null
            
            val withoutPrefix = uri.removePrefix("wc:")
            val parts = withoutPrefix.split("@")
            if (parts.size != 2) return null
            
            val topic = parts[0]
            val versionAndParams = parts[1].split("?")
            if (versionAndParams.size != 2) return null
            
            val version = versionAndParams[0]
            val queryParams = versionAndParams[1].split("&")
                .associate { param ->
                    val keyValue = param.split("=")
                    keyValue[0] to (keyValue.getOrNull(1) ?: "")
                }
            
            val symKey = queryParams["symKey"] ?: return null
            
            return WcUriData(
                topic = topic,
                version = version,
                symKey = symKey,
                relayProtocol = queryParams["relay-protocol"] ?: "irn"
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse WC URI", e)
            return null
        }
    }
    
    private fun hexToBytes(hex: String): ByteArray {
        val len = hex.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(hex[i], 16) shl 4) + Character.digit(hex[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }
    
    private fun generateRandomHex(length: Int): String {
        val bytes = ByteArray(length)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Clean up resources
     */
    fun disconnect() {
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        _connectionState.value = WalletConnectState.Disconnected
    }
}

// Data classes
private data class WcUriData(
    val topic: String,
    val version: String,
    val symKey: String,
    val relayProtocol: String
)

private data class SessionProposalData(
    val id: Long,
    val topic: String,
    val proposerPublicKey: String,
    val name: String,
    val description: String,
    val url: String,
    val icons: List<String>
)

/**
 * WalletConnect connection states
 */
sealed class WalletConnectState {
    object Disconnected : WalletConnectState()
    object Ready : WalletConnectState()
    object Connecting : WalletConnectState()
    data class Connected(val session: WalletConnectSession) : WalletConnectState()
    data class Error(val message: String) : WalletConnectState()
}

/**
 * WalletConnect session data
 */
data class WalletConnectSession(
    val topic: String,
    val peerName: String,
    val peerUrl: String,
    val peerIcon: String,
    val expiry: Long
)

/**
 * WalletConnect request from DApp
 */
data class WalletConnectRequest(
    val topic: String,
    val requestId: Long,
    val method: String,
    val params: String,
    val chainId: String
)

/**
 * WalletConnect events
 */
sealed class WalletConnectEvent {
    data class SessionProposal(
        val proposerPublicKey: String,
        val name: String,
        val description: String,
        val url: String,
        val icons: List<String>
    ) : WalletConnectEvent()
    
    data class SessionEstablished(val session: WalletConnectSession) : WalletConnectEvent()
    data class SessionDeleted(val topic: String) : WalletConnectEvent()
    data class Error(val message: String) : WalletConnectEvent()
}
