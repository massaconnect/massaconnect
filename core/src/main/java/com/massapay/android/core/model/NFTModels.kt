package com.massapay.android.core.model

import java.io.Serializable

data class NFT(
    val tokenId: String,
    val contractAddress: String,
    val name: String,
    val description: String,
    val imageUrl: String,
    val attributes: List<NFTAttribute> = emptyList(),
    val collection: NFTCollection? = null,
    val ownerAddress: String,
    val metadataUri: String = "",
    val standard: NFTStandard = NFTStandard.MRC1155,
    val createdAt: Long = System.currentTimeMillis(),
    val lastTransferAt: Long? = null,
    val rarity: NFTRarity = NFTRarity.COMMON
) : Serializable

data class NFTAttribute(
    val traitType: String,
    val value: String,
    val displayType: String? = null,
    val maxValue: Int? = null  // For percentage-based traits
) : Serializable

data class NFTCollection(
    val address: String,
    val name: String,
    val symbol: String,
    val description: String = "",
    val imageUrl: String? = null,
    val bannerUrl: String? = null,
    val verified: Boolean = false,
    val totalSupply: Int = 0,
    val floorPrice: String? = null,
    val website: String? = null,
    val twitter: String? = null,
    val discord: String? = null
) : Serializable

enum class NFTStandard {
    MRC1155,
    MRC721
}

enum class NFTRarity(val displayName: String, val colorHex: String) {
    COMMON("Common", "#9E9E9E"),
    UNCOMMON("Uncommon", "#4CAF50"),
    RARE("Rare", "#2196F3"),
    EPIC("Epic", "#9C27B0"),
    LEGENDARY("Legendary", "#FF9800"),
    MYTHIC("Mythic", "#F44336")
}

/**
 * Transfer request for NFTs
 */
data class NFTTransferRequest(
    val nft: NFT,
    val toAddress: String,
    val quantity: Int = 1  // For MRC1155 multi-edition NFTs
)