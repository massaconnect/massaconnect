package com.massapay.android.core.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * DApp Browser Models for Massa blockchain interaction
 */

/**
 * Represents a connected DApp
 */
@Parcelize
data class ConnectedDApp(
    val origin: String,
    val name: String,
    val iconUrl: String? = null,
    val connectedAt: Long = System.currentTimeMillis(),
    val permissions: List<DAppPermission> = listOf(DAppPermission.VIEW_ACCOUNT)
) : Parcelable

/**
 * Permissions that a DApp can request
 */
enum class DAppPermission {
    VIEW_ACCOUNT,      // View wallet address
    SIGN_MESSAGE,      // Sign arbitrary messages
    SIGN_TRANSACTION,  // Sign and broadcast transactions
    READ_BALANCE       // Read account balance
}

/**
 * Request from DApp to connect
 */
@Parcelize
data class DAppConnectRequest(
    val origin: String,
    val name: String,
    val iconUrl: String? = null,
    val requestedPermissions: List<DAppPermission> = listOf(DAppPermission.VIEW_ACCOUNT)
) : Parcelable

/**
 * Request from DApp to sign a message
 */
@Parcelize
data class DAppSignRequest(
    val origin: String,
    val message: String,
    val description: String? = null
) : Parcelable

/**
 * Request from DApp to execute a transaction
 */
@Parcelize
data class DAppTransactionRequest(
    val origin: String,
    val toAddress: String,
    val amount: String,           // Amount in MAS
    val fee: String? = null,      // Fee in MAS (optional, will use default)
    val data: String? = null,     // Smart contract call data
    val contractAddress: String? = null,
    val functionName: String? = null,
    val parameters: List<String>? = null
) : Parcelable

/**
 * Request from DApp to call a smart contract (read-only)
 */
@Parcelize
data class DAppCallRequest(
    val origin: String,
    val contractAddress: String,
    val functionName: String,
    val parameters: List<String> = emptyList()
) : Parcelable

/**
 * Response to send back to DApp
 */
sealed class DAppResponse {
    data class Success(val data: Any?) : DAppResponse()
    data class Error(val code: Int, val message: String) : DAppResponse()
    object Rejected : DAppResponse()
}

/**
 * DApp error codes
 */
object DAppErrorCodes {
    const val USER_REJECTED = 4001
    const val UNAUTHORIZED = 4100
    const val UNSUPPORTED_METHOD = 4200
    const val DISCONNECTED = 4900
    const val CHAIN_NOT_SUPPORTED = 4901
    const val INVALID_PARAMS = -32602
    const val INTERNAL_ERROR = -32603
}

/**
 * DApp categories for the Massa ecosystem
 */
enum class DAppCategory(val displayName: String) {
    ALL("All"),
    OFFICIAL("Official"),
    DEFI("DeFi"),
    NFT("NFTs")
}

/**
 * Bookmarked DApp for quick access
 */
@Parcelize
data class DAppBookmark(
    val url: String,
    val name: String,
    val iconUrl: String? = null,
    val description: String = "",
    val category: String = DAppCategory.DEFI.name,
    val addedAt: Long = System.currentTimeMillis()
) : Parcelable

/**
 * Popular DApps for Massa ecosystem - organized by categories from massa.net/ecosystem
 */
object MassaDApps {
    
    // Official Massa DApps
    private val officialDApps = listOf(
        DAppBookmark(
            url = "https://massa.net/",
            name = "Massa",
            iconUrl = "https://massa.net/favicon.ico",
            description = "Official Massa website",
            category = DAppCategory.OFFICIAL.name
        ),
        DAppBookmark(
            url = "https://explorer.massa.net/",
            name = "Massa Explorer",
            iconUrl = "https://massa.net/favicon.ico",
            description = "Official blockchain explorer",
            category = DAppCategory.OFFICIAL.name
        ),
        DAppBookmark(
            url = "https://station.massa.net/",
            name = "Massa Station",
            iconUrl = "https://massa.net/favicon.ico",
            description = "Official desktop wallet",
            category = DAppCategory.OFFICIAL.name
        ),
        DAppBookmark(
            url = "https://bridge.massa.net/",
            name = "Massa Bridge",
            iconUrl = "https://massa.net/favicon.ico",
            description = "Official bridge",
            category = DAppCategory.OFFICIAL.name
        )
    )
    
    // DeFi DApps
    private val defiDApps = listOf(
        DAppBookmark(
            url = "https://app.dusa.io/",
            name = "Dusa",
            iconUrl = "https://app.dusa.io/favicon.ico",
            description = "The most efficient DEX on Massa",
            category = DAppCategory.DEFI.name
        )
    )
    
    // NFT DApps
    private val nftDApps = listOf(
        DAppBookmark(
            url = "https://www.purrfectuniverse.com/",
            name = "Purrfect Universe",
            iconUrl = "https://www.purrfectuniverse.com/favicon.ico",
            description = "Cat-themed NFT universe",
            category = DAppCategory.NFT.name
        )
    )
    
    // All DApps combined
    val allDApps: List<DAppBookmark> = officialDApps + defiDApps + nftDApps
    
    // Filtered by category
    fun getDAppsByCategory(category: DAppCategory): List<DAppBookmark> {
        return when (category) {
            DAppCategory.ALL -> allDApps
            DAppCategory.OFFICIAL -> officialDApps
            DAppCategory.DEFI -> defiDApps
            DAppCategory.NFT -> nftDApps
        }
    }
    
    // Legacy property for backwards compatibility
    val popularDApps: List<DAppBookmark> = defiDApps
}
