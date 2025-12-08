package com.massapay.android.network.repository

import android.content.Context
import android.content.SharedPreferences
import com.massapay.android.core.model.NFT
import com.massapay.android.core.model.NFTCollection
import com.massapay.android.core.model.NFTAttribute
import com.massapay.android.core.util.Result
import com.massapay.android.network.api.MassaApi
import com.massapay.android.network.model.JsonRpcRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NFTRepository @Inject constructor(
    private val massaApi: MassaApi,
    @ApplicationContext private val context: Context
) {
    // Use an internal default IPFS gateway
    private val ipfsGateway: String = "https://ipfs.io/ipfs/"
    
    // SharedPreferences for custom collections
    private val prefs: SharedPreferences = context.getSharedPreferences("nft_collections", Context.MODE_PRIVATE)
    
    // Custom collections added by user
    private val customCollections = mutableListOf<KnownCollection>()
    
    // All known NFT collections on Massa mainnet (from PurrfectUniverse)
    private val knownCollections = listOf(
        KnownCollection(
            contractAddress = "AS1ZPeucY78TVDzL2nQHwFVA1o8x3KjbPDYRQy7CbYC4rSbLfo6D",
            name = "Awesome LAMAssa",
            symbol = "LAMASSA",
            description = "A unique collection of hand-drawn LAMAssa images",
            imageBaseUrl = "https://api.purrfectuniverse.com/lamassa-p/",
            metadataBaseUrl = "https://api.purrfectuniverse.com/lamassa-p/",
            totalSupply = 1000,
            verified = true
        ),
        KnownCollection(
            contractAddress = "AS12CDQjkEDicscZFmSeXMLYfsrcMT4tasLb4HZuzn8cwwaM7DYck",
            name = "Charlie",
            symbol = "CHARLIE",
            description = "With the dawn of the PUR Marketplace, the true essence of Charlie is revealed",
            imageBaseUrl = "https://api.purrfectuniverse.com/charlie-p/",
            metadataBaseUrl = "https://api.purrfectuniverse.com/charlie-p/",
            totalSupply = 1000,
            verified = true
        ),
        KnownCollection(
            contractAddress = "AS1ZPKQb6txkyJ2v8KgwFkNrUmKdRBhV4FY5M6zA4tDB78ia4jog",
            name = "Apes of the Freedom Realm",
            symbol = "APE",
            description = "Explore a bold and expressive NFT collection featuring apes as symbols of freedom",
            imageBaseUrl = "https://massahub.network/massahub-nft-api/ape/",
            metadataBaseUrl = "https://massahub.network/massahub-nft-api/ape/",
            totalSupply = 1000,
            verified = true
        ),
        KnownCollection(
            contractAddress = "AS1GTt9VEKJwMQnY2NVAPYSKwkoCYiuiRczWpeUU9vjBn5m82tgn",
            name = "MW0rld Items",
            symbol = "MW0RLD",
            description = "Collection of essential in-game items for MW0rld",
            imageBaseUrl = "https://api.purrfectuniverse.com/mworld-items/",
            metadataBaseUrl = "https://api.purrfectuniverse.com/mworld-items/",
            totalSupply = 50000,
            verified = true
        ),
        KnownCollection(
            contractAddress = "AS1eRQ2sa1SzAwWzft1xqRHvPiDt9e7AduFQpvW9afdMVXbw63Yj",
            name = "Fisherman John's Postcard",
            symbol = "FJP",
            description = "Old photos transformed into stylish postcards by fisherman John",
            imageBaseUrl = "https://api.purrfectuniverse.com/fjp/",
            metadataBaseUrl = "https://api.purrfectuniverse.com/fjp/",
            totalSupply = 200,
            verified = true
        )
    )
    
    private val httpClient = OkHttpClient()
    
    init {
        // Load custom collections from SharedPreferences
        loadCustomCollections()
    }
    
    /**
     * Get all collections (built-in + custom)
     */
    private fun getAllCollections(): List<KnownCollection> {
        return knownCollections + customCollections
    }
    
    /**
     * Add a custom NFT collection by contract address
     * Returns Result.Success with collection name or Result.Error
     */
    suspend fun addCustomCollection(contractAddress: String, name: String? = null): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                // Validate contract address format
                if (!contractAddress.startsWith("AS")) {
                    return@withContext Result.Error(Exception("Invalid contract address. Must start with 'AS'"))
                }
                
                // Check if already exists
                if (getAllCollections().any { it.contractAddress == contractAddress }) {
                    return@withContext Result.Error(Exception("Collection already exists"))
                }
                
                // Try to get collection info from contract
                val collectionName = name ?: getCollectionName(contractAddress) ?: "Custom Collection"
                val symbol = getCollectionSymbol(contractAddress) ?: "NFT"
                
                val newCollection = KnownCollection(
                    contractAddress = contractAddress,
                    name = collectionName,
                    symbol = symbol,
                    description = "Custom imported collection",
                    imageBaseUrl = "",  // Will try to fetch from tokenURI
                    metadataBaseUrl = "",
                    totalSupply = 10000,  // Default max to scan
                    verified = false
                )
                
                customCollections.add(newCollection)
                saveCustomCollections()
                
                android.util.Log.d("NFTRepository", "Added custom collection: $collectionName ($contractAddress)")
                Result.Success(collectionName)
            } catch (e: Exception) {
                android.util.Log.e("NFTRepository", "Error adding custom collection", e)
                Result.Error(e)
            }
        }
    }
    
    /**
     * Remove a custom collection
     */
    fun removeCustomCollection(contractAddress: String): Boolean {
        val removed = customCollections.removeAll { it.contractAddress == contractAddress }
        if (removed) {
            saveCustomCollections()
        }
        return removed
    }
    
    /**
     * Get list of custom collection addresses
     */
    fun getCustomCollectionAddresses(): List<String> = customCollections.map { it.contractAddress }
    
    /**
     * Save custom collections to SharedPreferences
     */
    private fun saveCustomCollections() {
        val json = JSONArray()
        customCollections.forEach { collection ->
            json.put(JSONObject().apply {
                put("contractAddress", collection.contractAddress)
                put("name", collection.name)
                put("symbol", collection.symbol)
                put("description", collection.description)
                put("imageBaseUrl", collection.imageBaseUrl)
                put("metadataBaseUrl", collection.metadataBaseUrl)
                put("totalSupply", collection.totalSupply)
            })
        }
        prefs.edit().putString("custom_collections", json.toString()).apply()
    }
    
    /**
     * Load custom collections from SharedPreferences
     */
    private fun loadCustomCollections() {
        try {
            val json = prefs.getString("custom_collections", null) ?: return
            val array = JSONArray(json)
            customCollections.clear()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                customCollections.add(KnownCollection(
                    contractAddress = obj.getString("contractAddress"),
                    name = obj.getString("name"),
                    symbol = obj.optString("symbol", "NFT"),
                    description = obj.optString("description", ""),
                    imageBaseUrl = obj.optString("imageBaseUrl", ""),
                    metadataBaseUrl = obj.optString("metadataBaseUrl", ""),
                    totalSupply = obj.optInt("totalSupply", 10000),
                    verified = false
                ))
            }
            android.util.Log.d("NFTRepository", "Loaded ${customCollections.size} custom collections")
        } catch (e: Exception) {
            android.util.Log.e("NFTRepository", "Error loading custom collections", e)
        }
    }
    
    /**
     * Try to get collection name from contract
     */
    private suspend fun getCollectionName(contractAddress: String): String? {
        return queryStringFunction(contractAddress, "name")
    }
    
    /**
     * Try to get collection symbol from contract
     */
    private suspend fun getCollectionSymbol(contractAddress: String): String? {
        return queryStringFunction(contractAddress, "symbol")
    }
    
    /**
     * Query a string-returning function from contract
     */
    private suspend fun queryStringFunction(contractAddress: String, functionName: String): String? {
        try {
            val jsonBody = JSONObject().apply {
                put("jsonrpc", "2.0")
                put("id", 1)
                put("method", "execute_read_only_call")
                put("params", JSONArray().apply {
                    put(JSONArray().apply {
                        put(JSONObject().apply {
                            put("target_address", contractAddress)
                            put("target_function", functionName)
                            put("parameter", JSONArray())
                            put("max_gas", 100000000)
                        })
                    })
                })
            }.toString()
            
            val request = Request.Builder()
                .url("https://mainnet.massa.net/api/v2")
                .post(jsonBody.toRequestBody("application/json".toMediaTypeOrNull()))
                .build()
            
            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: return null
                val json = JSONObject(body)
                val resultArray = json.optJSONArray("result")
                if (resultArray != null && resultArray.length() > 0) {
                    val firstResult = resultArray.getJSONObject(0)
                    val resultData = firstResult.optJSONObject("result")
                    val okArray = resultData?.optJSONArray("Ok")
                    
                    if (okArray != null && okArray.length() > 0) {
                        // Decode string: first 4 bytes are length, rest is UTF-8
                        if (okArray.length() >= 4) {
                            var length = 0
                            for (i in 0..3) {
                                length = length or ((okArray.getInt(i) and 0xFF) shl (i * 8))
                            }
                            if (length > 0 && okArray.length() >= 4 + length) {
                                val bytes = ByteArray(length)
                                for (i in 0 until length) {
                                    bytes[i] = okArray.getInt(4 + i).toByte()
                                }
                                return String(bytes, Charsets.UTF_8)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.d("NFTRepository", "Error querying $functionName: ${e.message}")
        }
        return null
    }
    
    // NFT cache for instant loading
    private val nftCache = mutableMapOf<String, List<NFT>>()
    private var lastCacheTime = 0L
    private val CACHE_DURATION_MS = 30 * 1000L // 30 seconds for testing
    
    fun getNFTs(address: String): Flow<Result<List<NFT>>> = flow {
        try {
            emit(Result.Loading)
            android.util.Log.d("NFTRepository", "Fetching NFTs for address: $address")
            
            // Check cache first for instant display
            val cachedNfts = nftCache[address]
            val cacheAge = System.currentTimeMillis() - lastCacheTime
            if (cachedNfts != null && cachedNfts.isNotEmpty() && cacheAge < CACHE_DURATION_MS) {
                android.util.Log.d("NFTRepository", "Returning ${cachedNfts.size} cached NFTs")
                emit(Result.Success(cachedNfts))
                return@flow
            }
            
            val allNfts = mutableListOf<NFT>()
            
            // First try PurrfectUniverse API to get user's NFTs
            try {
                val puNfts = fetchFromPurrfectUniverseAPI(address)
                allNfts.addAll(puNfts)
                android.util.Log.d("NFTRepository", "Found ${puNfts.size} NFTs from PurrfectUniverse API")
            } catch (e: Exception) {
                android.util.Log.w("NFTRepository", "PurrfectUniverse API failed: ${e.message}")
            }
            
            // Query collections in parallel if API didn't return results
            if (allNfts.isEmpty()) {
                val collections = getAllCollections()
                
                // Query all collections in parallel using coroutines
                coroutineScope {
                    val results = collections.map { collection ->
                        async(Dispatchers.IO) {
                            try {
                                queryNFTContract(address, collection)
                            } catch (e: Exception) {
                                android.util.Log.w("NFTRepository", "Error querying ${collection.name}: ${e.message}")
                                emptyList()
                            }
                        }
                    }
                    
                    // Collect results
                    results.forEach { deferred ->
                        val nftsFromContract = deferred.await()
                        allNfts.addAll(nftsFromContract)
                    }
                }
            }
            
            // Update cache
            if (allNfts.isNotEmpty()) {
                nftCache[address] = allNfts.toList()
                lastCacheTime = System.currentTimeMillis()
            }
            
            emit(Result.Success(allNfts))
            
        } catch (e: Exception) {
            android.util.Log.e("NFTRepository", "Error getting NFTs", e)
            emit(Result.Error(e))
        }
    }
    
    private suspend fun fetchFromPurrfectUniverseAPI(address: String): List<NFT> = withContext(Dispatchers.IO) {
        val nfts = mutableListOf<NFT>()
        
        // Try to fetch user's NFTs from PurrfectUniverse API
        try {
            val request = Request.Builder()
                .url("https://api.purrfectuniverse.com/user/$address/nfts")
                .build()
            
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return@withContext emptyList()
                    val jsonArray = JSONArray(body)
                    
                    for (i in 0 until jsonArray.length()) {
                        val nftJson = jsonArray.getJSONObject(i)
                        val nft = parseNFTFromJson(nftJson, address)
                        if (nft != null) {
                            nfts.add(nft)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.d("NFTRepository", "PurrfectUniverse user API not available: ${e.message}")
        }
        
        nfts
    }
    
    private suspend fun queryNFTContract(address: String, collection: KnownCollection): List<NFT> = withContext(Dispatchers.IO) {
        val nfts = mutableListOf<NFT>()
        
        try {
            // Massa AS serialization format for strings:
            // First 4 bytes: string length (u32, little-endian)
            // Then: UTF-8 bytes of the string
            val addressBytes = address.toByteArray(Charsets.UTF_8)
            val parameter = mutableListOf<Int>()
            
            // Add length prefix (4 bytes, little-endian u32)
            val length = addressBytes.size
            parameter.add(length and 0xFF)
            parameter.add((length shr 8) and 0xFF)
            parameter.add((length shr 16) and 0xFF)
            parameter.add((length shr 24) and 0xFF)
            
            // Add the address bytes
            addressBytes.forEach { parameter.add(it.toInt() and 0xFF) }
            
            val balanceRequest = JsonRpcRequest(
                method = "execute_read_only_call",
                params = listOf(
                    listOf(
                        mapOf(
                            "target_address" to collection.contractAddress,
                            "target_function" to "balanceOf",
                            "parameter" to parameter,
                            "max_gas" to 100000000,
                            "caller_address" to null,
                            "coins" to null,
                            "fee" to null
                        )
                    )
                )
            )
            
            android.util.Log.d("NFTRepository", "Calling balanceOf for ${collection.name} with param length: ${parameter.size}")
            
            // Make raw HTTP call to avoid Gson parsing issues
            val requestBody = """{"jsonrpc":"2.0","id":1,"method":"execute_read_only_call","params":[[{"target_address":"${collection.contractAddress}","target_function":"balanceOf","parameter":$parameter,"max_gas":100000000,"caller_address":null,"coins":null,"fee":null}]]}"""
            
            val mediaType = "application/json".toMediaTypeOrNull()
            val httpRequest = Request.Builder()
                .url("https://mainnet.massa.net/api/v2")
                .post(requestBody.toRequestBody(mediaType))
                .build()
            
            httpClient.newCall(httpRequest).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                android.util.Log.d("NFTRepository", "balanceOf raw response: $responseBody")
                
                if (response.isSuccessful && responseBody.contains("\"result\"")) {
                    val json = JSONObject(responseBody)
                    
                    if (json.has("result")) {
                        val resultArray = json.optJSONArray("result")
                        if (resultArray != null && resultArray.length() > 0) {
                            val firstResult = resultArray.getJSONObject(0)
                            val resultObj = firstResult.optJSONObject("result")
                            
                            if (resultObj != null && resultObj.has("Ok")) {
                                val okArray = resultObj.optJSONArray("Ok")
                                if (okArray != null && okArray.length() > 0) {
                                    // Decode u256 balance (first 8 bytes as u64 for simplicity)
                                    val balance = decodeU64FromJsonArray(okArray)
                                    android.util.Log.d("NFTRepository", "Balance for ${collection.name}: $balance")
                                    
                                    if (balance > 0) {
                                        // User owns NFTs from this collection
                                        val tokenIds = getOwnedTokenIds(address, collection, balance)
                                        
                                        for (tokenId in tokenIds) {
                                            try {
                                                val nft = fetchNFTDetails(tokenId, collection, address)
                                                if (nft != null) {
                                                    nfts.add(nft)
                                                }
                                            } catch (e: Exception) {
                                                android.util.Log.w("NFTRepository", "Error fetching NFT #$tokenId: ${e.message}")
                                            }
                                        }
                                    }
                                }
                            } else if (resultObj != null && resultObj.has("Error")) {
                                android.util.Log.w("NFTRepository", "Contract error: ${resultObj.getString("Error")}")
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("NFTRepository", "Error querying contract ${collection.contractAddress}: ${e.message}")
        }
        
        nfts
    }
    
    private fun decodeU64FromJsonArray(array: org.json.JSONArray): Int {
        if (array.length() == 0) return 0
        var result = 0L
        for (i in 0 until minOf(8, array.length())) {
            val byteVal = array.optInt(i, 0).toLong() and 0xFF
            result = result or (byteVal shl (i * 8))
        }
        return result.toInt()
    }
    
    private fun decodeU64FromBytes(bytes: List<*>): Int {
        if (bytes.isEmpty()) return 0
        var result = 0L
        for (i in bytes.indices.take(8)) {
            val byteVal = when (val b = bytes[i]) {
                is Number -> b.toLong() and 0xFF
                else -> 0L
            }
            result = result or (byteVal shl (i * 8))
        }
        return result.toInt()
    }
    
    private suspend fun getOwnedTokenIds(address: String, collection: KnownCollection, balance: Int): List<Int> {
        val tokenIds = mutableListOf<Int>()
        val addressBytes = address.toByteArray(Charsets.UTF_8).map { it.toInt() and 0xFF }
        
        // Try to call tokensOfOwner if available
        try {
            val request = JsonRpcRequest(
                method = "execute_read_only_call",
                params = listOf(
                    listOf(
                        mapOf(
                            "target_address" to collection.contractAddress,
                            "target_function" to "tokensOfOwner",
                            "parameter" to addressBytes,
                            "max_gas" to 100000000,
                            "caller_address" to null,
                            "coins" to null,
                            "fee" to null
                        )
                    )
                )
            )
            
            val response = massaApi.callView(request)
            if (response.error == null && response.result != null) {
                val responseList = response.result as? List<*>
                val firstResponse = responseList?.firstOrNull() as? Map<String, Any>
                val resultData = firstResponse?.get("result") as? Map<String, Any>
                val okBytes = resultData?.get("Ok") as? List<*>
                
                if (okBytes != null && okBytes.isNotEmpty()) {
                    // Decode token IDs array from bytes
                    val ids = decodeTokenIdsFromBytes(okBytes)
                    tokenIds.addAll(ids)
                    android.util.Log.d("NFTRepository", "Found token IDs from tokensOfOwner: $ids")
                }
            }
        } catch (e: Exception) {
            android.util.Log.d("NFTRepository", "tokensOfOwner not available: ${e.message}")
        }
        
        // If tokensOfOwner failed, try to enumerate by checking ownership with PARALLEL queries
        if (tokenIds.isEmpty() && balance > 0) {
            android.util.Log.d("NFTRepository", "Scanning for owned tokens (balance: $balance), looking for address: $address")
            
            // First find the highest existing token using binary search
            val maxTokenId = findHighestTokenId(collection)
            android.util.Log.d("NFTRepository", "Highest existing token ID: $maxTokenId")
            
            // Scan in batches of 20 tokens in parallel for speed
            val batchSize = 20
            val foundTokens = java.util.concurrent.ConcurrentHashMap<Int, String>()
            
            coroutineScope {
                for (batchStart in maxTokenId downTo 1 step batchSize) {
                    // Check if we already found all tokens
                    if (foundTokens.size >= balance) break
                    
                    val batchEnd = maxOf(1, batchStart - batchSize + 1)
                    val batchJobs = (batchStart downTo batchEnd).map { tokenId ->
                        async(Dispatchers.IO) {
                            try {
                                val owner = getTokenOwner(tokenId, collection)
                                if (owner != null && owner.equals(address, ignoreCase = true)) {
                                    foundTokens[tokenId] = owner
                                    android.util.Log.d("NFTRepository", "Found owned token: $tokenId owned by $owner")
                                }
                            } catch (e: Exception) {
                                // Token might not exist
                            }
                        }
                    }
                    
                    // Wait for this batch to complete
                    batchJobs.forEach { it.await() }
                    
                    // Check if we found enough
                    if (foundTokens.size >= balance) break
                }
            }
            
            tokenIds.addAll(foundTokens.keys.sortedDescending())
        }
        
        return tokenIds
    }
    
    private suspend fun getTotalSupply(collection: KnownCollection): Int? {
        try {
            val jsonBody = JSONObject().apply {
                put("jsonrpc", "2.0")
                put("id", 1)
                put("method", "execute_read_only_call")
                put("params", org.json.JSONArray().apply {
                    put(org.json.JSONArray().apply {
                        put(JSONObject().apply {
                            put("target_address", collection.contractAddress)
                            put("target_function", "totalSupply")
                            put("parameter", org.json.JSONArray()) // No parameters
                            put("max_gas", 100000000)
                        })
                    })
                })
            }.toString()
            
            val request = Request.Builder()
                .url("https://mainnet.massa.net/api/v2")
                .post(jsonBody.toRequestBody("application/json".toMediaTypeOrNull()))
                .build()
            
            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: return null
                val json = JSONObject(body)
                val resultArray = json.optJSONArray("result")
                if (resultArray != null && resultArray.length() > 0) {
                    val firstResult = resultArray.getJSONObject(0)
                    val resultData = firstResult.optJSONObject("result")
                    val okArray = resultData?.optJSONArray("Ok")
                    
                    if (okArray != null && okArray.length() >= 8) {
                        // Read u256 as little-endian (but we only need first 8 bytes for reasonable supply)
                        var supply = 0L
                        for (i in 0..7) {
                            supply = supply or ((okArray.getInt(i).toLong() and 0xFF) shl (i * 8))
                        }
                        android.util.Log.d("NFTRepository", "Total supply for ${collection.name}: $supply")
                        return supply.toInt()
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.d("NFTRepository", "Error getting totalSupply: ${e.message}")
        }
        return null
    }
    
    /**
     * Find the highest existing token ID using binary search.
     * This is needed because totalSupply represents max tokens, not minted tokens.
     */
    private suspend fun findHighestTokenId(collection: KnownCollection): Int {
        var low = 1
        var high = collection.totalSupply
        var lastExisting = 0
        
        // Binary search to find the highest minted token
        while (low <= high) {
            val mid = (low + high) / 2
            val exists = tokenExists(mid, collection)
            
            if (exists) {
                lastExisting = mid
                low = mid + 1  // Look for higher tokens
            } else {
                high = mid - 1  // Look for lower tokens
            }
        }
        
        android.util.Log.d("NFTRepository", "Binary search found highest token: $lastExisting for ${collection.name}")
        return lastExisting
    }
    
    /**
     * Check if a token exists (has an owner)
     */
    private suspend fun tokenExists(tokenId: Int, collection: KnownCollection): Boolean {
        val owner = getTokenOwner(tokenId, collection)
        return owner != null && owner.isNotEmpty()
    }
    
    private fun decodeTokenIdsFromBytes(bytes: List<*>): List<Int> {
        val ids = mutableListOf<Int>()
        var offset = 0
        // First read the length (u32)
        if (bytes.size >= 4) {
            var length = 0L
            for (i in 0..3) {
                val byteVal = (bytes[i] as? Number)?.toLong()?.and(0xFF) ?: 0L
                length = length or (byteVal shl (i * 8))
            }
            offset = 4
            // Then read each token ID (u64)
            for (j in 0 until length.toInt()) {
                if (offset + 8 > bytes.size) break
                var id = 0L
                for (i in 0..7) {
                    val byteVal = (bytes[offset + i] as? Number)?.toLong()?.and(0xFF) ?: 0L
                    id = id or (byteVal shl (i * 8))
                }
                ids.add(id.toInt())
                offset += 8
            }
        }
        return ids
    }
    
    private suspend fun getTokenOwner(tokenId: Int, collection: KnownCollection): String? {
        try {
            // Encode tokenId as u256 bytes (32 bytes, little-endian)
            val tokenIdBytes = mutableListOf<Int>()
            var value = tokenId.toLong()
            // First 8 bytes contain the value (little-endian)
            for (i in 0..7) {
                tokenIdBytes.add((value and 0xFF).toInt())
                value = value shr 8
            }
            // Remaining 24 bytes are zeros for u256
            for (i in 8..31) {
                tokenIdBytes.add(0)
            }
            
            // Use raw HTTP call to avoid Gson parsing issues
            val jsonBody = JSONObject().apply {
                put("jsonrpc", "2.0")
                put("id", 1)
                put("method", "execute_read_only_call")
                put("params", org.json.JSONArray().apply {
                    put(org.json.JSONArray().apply {
                        put(JSONObject().apply {
                            put("target_address", collection.contractAddress)
                            put("target_function", "ownerOf")
                            put("parameter", org.json.JSONArray(tokenIdBytes))
                            put("max_gas", 100000000)
                        })
                    })
                })
            }.toString()
            
            val request = Request.Builder()
                .url("https://mainnet.massa.net/api/v2")
                .post(jsonBody.toRequestBody("application/json".toMediaTypeOrNull()))
                .build()
            
            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: return null
                android.util.Log.d("NFTRepository", "ownerOf response for token $tokenId: $body")
                
                val json = JSONObject(body)
                val resultArray = json.optJSONArray("result")
                if (resultArray != null && resultArray.length() > 0) {
                    val firstResult = resultArray.getJSONObject(0)
                    val resultData = firstResult.optJSONObject("result")
                    val okArray = resultData?.optJSONArray("Ok")
                    
                    if (okArray != null && okArray.length() > 0) {
                        // The response is just the raw UTF-8 bytes of the address (no length prefix)
                        val bytes = ByteArray(okArray.length())
                        for (i in 0 until okArray.length()) {
                            bytes[i] = okArray.getInt(i).toByte()
                        }
                        val owner = String(bytes, Charsets.UTF_8)
                        android.util.Log.d("NFTRepository", "Token $tokenId owner: $owner")
                        return owner
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.d("NFTRepository", "Error getting owner of token $tokenId: ${e.message}")
        }
        return null
    }
    
    /**
     * Get tokenURI from NFT contract - this contains metadata URL
     */
    private suspend fun getTokenURI(tokenId: Int, contractAddress: String): String? {
        try {
            // Encode tokenId as u256 (32 bytes)
            val tokenIdBytes = mutableListOf<Int>()
            var value = tokenId.toLong()
            for (i in 0..7) {
                tokenIdBytes.add((value and 0xFF).toInt())
                value = value shr 8
            }
            for (i in 8..31) {
                tokenIdBytes.add(0)
            }
            
            val jsonBody = JSONObject().apply {
                put("jsonrpc", "2.0")
                put("id", 1)
                put("method", "execute_read_only_call")
                put("params", JSONArray().apply {
                    put(JSONArray().apply {
                        put(JSONObject().apply {
                            put("target_address", contractAddress)
                            put("target_function", "tokenURI")
                            put("parameter", JSONArray(tokenIdBytes))
                            put("max_gas", 100000000)
                        })
                    })
                })
            }.toString()
            
            val request = Request.Builder()
                .url("https://mainnet.massa.net/api/v2")
                .post(jsonBody.toRequestBody("application/json".toMediaTypeOrNull()))
                .build()
            
            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: return null
                val json = JSONObject(body)
                val resultArray = json.optJSONArray("result")
                if (resultArray != null && resultArray.length() > 0) {
                    val firstResult = resultArray.getJSONObject(0)
                    val resultData = firstResult.optJSONObject("result")
                    val okArray = resultData?.optJSONArray("Ok")
                    
                    if (okArray != null && okArray.length() >= 4) {
                        // Decode string: first 4 bytes are length, rest is UTF-8
                        var length = 0
                        for (i in 0..3) {
                            length = length or ((okArray.getInt(i) and 0xFF) shl (i * 8))
                        }
                        if (length > 0 && okArray.length() >= 4 + length) {
                            val bytes = ByteArray(length)
                            for (i in 0 until length) {
                                bytes[i] = okArray.getInt(4 + i).toByte()
                            }
                            val uri = String(bytes, Charsets.UTF_8)
                            android.util.Log.d("NFTRepository", "Token $tokenId URI: $uri")
                            return resolveIpfsUrl(uri)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.d("NFTRepository", "tokenURI not available for token $tokenId: ${e.message}")
        }
        return null
    }

    private suspend fun fetchNFTDetails(tokenId: Int, collection: KnownCollection, ownerAddress: String): NFT? {
        var name = "${collection.name} #$tokenId"
        var description = collection.description
        var imageUrl = ""
        val attributes = mutableListOf<NFTAttribute>()
        
        // First try to get tokenURI from contract (most reliable source)
        val tokenUri = getTokenURI(tokenId, collection.contractAddress)
        android.util.Log.d("NFTRepository", "Token $tokenId - tokenURI from contract: $tokenUri")
        
        // Build list of metadata URLs to try (in order of preference)
        val metadataUrls = mutableListOf<String>()
        
        // 1. First priority: tokenURI from contract
        if (!tokenUri.isNullOrEmpty()) {
            metadataUrls.add(tokenUri)
            // If tokenURI doesn't have .json, also try with .json
            if (!tokenUri.endsWith(".json") && !tokenUri.contains("?")) {
                metadataUrls.add("$tokenUri.json")
            }
        }
        
        // 2. Second priority: collection's metadata base URL
        if (collection.metadataBaseUrl.isNotEmpty()) {
            metadataUrls.add("${collection.metadataBaseUrl}$tokenId")
            metadataUrls.add("${collection.metadataBaseUrl}$tokenId.json")
            metadataUrls.add("${collection.metadataBaseUrl}$tokenId/metadata")
            metadataUrls.add("${collection.metadataBaseUrl}$tokenId/metadata.json")
        }
        
        // Try each metadata URL
        for (metadataUrl in metadataUrls) {
            if (imageUrl.isNotEmpty()) break
            
            try {
                android.util.Log.d("NFTRepository", "Trying metadata URL: $metadataUrl")
                val request = Request.Builder()
                    .url(metadataUrl)
                    .addHeader("Accept", "application/json, */*")
                    .build()
                
                val response = httpClient.newCall(request).execute()
                response.use { resp ->
                    if (resp.isSuccessful) {
                        val contentType = resp.header("Content-Type", "")
                        val body = resp.body?.string()
                        
                        // Check if it's JSON metadata
                        if (body != null && (body.trimStart().startsWith("{") || contentType?.contains("json") == true)) {
                            try {
                                val json = JSONObject(body)
                                name = json.optString("name", name)
                                description = json.optString("description", description)
                                
                                // Get image URL from metadata - try multiple fields
                                val possibleImageFields = listOf("image", "image_url", "imageUrl", "image_uri", "imageUri", "media", "animation_url")
                                for (field in possibleImageFields) {
                                    val jsonImage = json.optString(field, "")
                                    if (jsonImage.isNotEmpty()) {
                                        imageUrl = resolveIpfsUrl(jsonImage)
                                        android.util.Log.d("NFTRepository", "Got image from metadata field '$field': $imageUrl")
                                        break
                                    }
                                }
                                
                                // Also check nested 'metadata' or 'properties' objects
                                val nestedObjects = listOf("metadata", "properties", "data")
                                for (nestedName in nestedObjects) {
                                    if (imageUrl.isEmpty() && json.has(nestedName)) {
                                        val nested = json.optJSONObject(nestedName)
                                        if (nested != null) {
                                            for (field in possibleImageFields) {
                                                val nestedImage = nested.optString(field, "")
                                                if (nestedImage.isNotEmpty()) {
                                                    imageUrl = resolveIpfsUrl(nestedImage)
                                                    android.util.Log.d("NFTRepository", "Got image from $nestedName.$field: $imageUrl")
                                                    break
                                                }
                                            }
                                        }
                                    }
                                }
                                
                                val attrsArray = json.optJSONArray("attributes")
                                if (attrsArray != null) {
                                    for (i in 0 until attrsArray.length()) {
                                        val attr = attrsArray.getJSONObject(i)
                                        attributes.add(NFTAttribute(
                                            traitType = attr.optString("trait_type", ""),
                                            value = attr.optString("value", ""),
                                            displayType = attr.optString("display_type", null)
                                        ))
                                    }
                                }
                            } catch (je: Exception) {
                                android.util.Log.d("NFTRepository", "Error parsing JSON from $metadataUrl: ${je.message}")
                            }
                        } else if (body != null && contentType?.startsWith("image/") == true) {
                            // The URL itself is the image
                            imageUrl = metadataUrl
                            android.util.Log.d("NFTRepository", "URL is direct image: $imageUrl")
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.d("NFTRepository", "Metadata not available at $metadataUrl: ${e.message}")
            }
        }
        
        // Fallback: try common image URL patterns if no image found yet
        if (imageUrl.isEmpty()) {
            val imagePatterns = mutableListOf<String>()
            
            // Try collection's image base URL patterns
            if (collection.imageBaseUrl.isNotEmpty()) {
                imagePatterns.addAll(listOf(
                    "${collection.imageBaseUrl}$tokenId/image",
                    "${collection.imageBaseUrl}$tokenId.png",
                    "${collection.imageBaseUrl}$tokenId.jpg",
                    "${collection.imageBaseUrl}$tokenId.gif",
                    "${collection.imageBaseUrl}$tokenId.webp",
                    "${collection.imageBaseUrl}$tokenId",
                    "${collection.imageBaseUrl}images/$tokenId.png",
                    "${collection.imageBaseUrl}images/$tokenId"
                ))
            }
            
            // Try patterns based on tokenURI if available
            if (!tokenUri.isNullOrEmpty()) {
                // If tokenURI is for metadata, try replacing with image patterns
                val baseUri = tokenUri.substringBeforeLast("/")
                if (baseUri.isNotEmpty()) {
                    imagePatterns.addAll(listOf(
                        "$baseUri/$tokenId.png",
                        "$baseUri/$tokenId/image",
                        "$baseUri/images/$tokenId.png"
                    ))
                }
            }
            
            for (pattern in imagePatterns) {
                if (imageUrl.isNotEmpty()) break
                try {
                    val request = Request.Builder()
                        .url(pattern)
                        .head()
                        .build()
                    
                    httpClient.newCall(request).execute().use { response ->
                        val contentType = response.header("Content-Type", "")
                        if (response.isSuccessful && (contentType?.startsWith("image/") == true || response.code == 200)) {
                            imageUrl = pattern
                            android.util.Log.d("NFTRepository", "Found image at fallback URL: $imageUrl")
                        }
                    }
                } catch (e: Exception) {
                    // Continue trying other patterns
                }
            }
        }
        
        // Last resort: use tokenURI directly if it looks like an image URL
        if (imageUrl.isEmpty() && !tokenUri.isNullOrEmpty()) {
            val lowerUri = tokenUri.lowercase()
            if (lowerUri.endsWith(".png") || lowerUri.endsWith(".jpg") || 
                lowerUri.endsWith(".jpeg") || lowerUri.endsWith(".gif") || 
                lowerUri.endsWith(".webp") || lowerUri.endsWith(".svg") ||
                lowerUri.contains("/image") || lowerUri.contains("ipfs")) {
                imageUrl = resolveIpfsUrl(tokenUri)
                android.util.Log.d("NFTRepository", "Using tokenURI as image: $imageUrl")
            }
        }
        
        android.util.Log.d("NFTRepository", "NFT Details - Name: $name, Image: $imageUrl")
        
        return NFT(
            tokenId = tokenId.toString(),
            contractAddress = collection.contractAddress,
            name = name,
            description = description,
            imageUrl = imageUrl,
            attributes = attributes,
            collection = NFTCollection(
                address = collection.contractAddress,
                name = collection.name,
                symbol = collection.symbol,
                description = collection.description,
                imageUrl = "${collection.imageBaseUrl}banner.png",
                verified = collection.verified
            ),
            ownerAddress = ownerAddress,
            metadataUri = "${collection.metadataBaseUrl}$tokenId"
        )
    }
    
    private fun parseNFTFromJson(json: JSONObject, ownerAddress: String): NFT? {
        return try {
            NFT(
                tokenId = json.optString("tokenId", json.optString("token_id", "")),
                contractAddress = json.optString("contractAddress", json.optString("contract_address", "")),
                name = json.optString("name", "Unknown NFT"),
                description = json.optString("description", ""),
                imageUrl = resolveIpfsUrl(json.optString("image", "")),
                attributes = emptyList(),
                collection = null,
                ownerAddress = ownerAddress,
                metadataUri = json.optString("metadataUri", "")
            )
        } catch (e: Exception) {
            null
        }
    }

    suspend fun transferNFT(
        from: String,
        to: String,
        contractAddress: String,
        tokenId: String
    ): Result<String> {
        return try {
            val operation = mapOf(
                "sender" to from,
                "recipient" to to,
                "contract" to contractAddress,
                "function" to "transferFrom",
                "args" to listOf(from, to, tokenId)
            )

            val request = JsonRpcRequest(
                method = "send_operations",
                params = listOf(listOf(operation))
            )

            val response = massaApi.sendOperation(request)
            response.error?.let {
                return Result.Error(Exception(it.message))
            }

            val operationId = response.result?.firstOrNull() ?: throw Exception("No transaction hash returned")
            Result.Success(operationId)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    private fun resolveIpfsUrl(url: String?): String {
        return when {
            url == null -> ""
            url.startsWith("ipfs://") -> {
                val hash = url.removePrefix("ipfs://")
                "$ipfsGateway$hash"
            }
            else -> url
        }
    }
}

private data class KnownCollection(
    val contractAddress: String,
    val name: String,
    val symbol: String,
    val description: String,
    val imageBaseUrl: String,
    val metadataBaseUrl: String,
    val totalSupply: Int,
    val verified: Boolean
)