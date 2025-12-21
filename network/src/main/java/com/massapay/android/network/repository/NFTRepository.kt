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
import kotlinx.coroutines.supervisorScope
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
    
    // Optimized HTTP client for parallel RPC calls
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
    
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
    private val CACHE_DURATION_MS = 60 * 1000L // 60 seconds cache
    
    // Balance cache to avoid repeated queries
    private val balanceCache = mutableMapOf<String, Map<String, Int>>() // address -> (contract -> balance)
    
    /**
     * Clears the NFT cache to force a fresh fetch on next call
     */
    fun clearCache() {
        nftCache.clear()
        balanceCache.clear()
        lastCacheTime = 0L
        android.util.Log.d("NFTRepository", "NFT cache cleared")
    }
    
    fun getNFTs(address: String): Flow<Result<List<NFT>>> = flow {
        try {
            emit(Result.Loading)
            android.util.Log.d("NFTRepository", "🔍 Starting NFT scan for: $address")
            val startTime = System.currentTimeMillis()
            
            // Check cache first for instant display
            val cachedNfts = nftCache[address]
            val cacheAge = System.currentTimeMillis() - lastCacheTime
            if (cachedNfts != null && cachedNfts.isNotEmpty() && cacheAge < CACHE_DURATION_MS) {
                android.util.Log.d("NFTRepository", "✅ Returning ${cachedNfts.size} cached NFTs (${cacheAge}ms old)")
                emit(Result.Success(cachedNfts))
                return@flow
            }
            
            val allNfts = mutableListOf<NFT>()
            val collections = getAllCollections()
            
            // STEP 1: Query balanceOf for ALL collections in parallel (fast filtering)
            android.util.Log.d("NFTRepository", "📊 Step 1: Checking balances for ${collections.size} collections in parallel...")
            
            val collectionsWithBalance = mutableListOf<Pair<KnownCollection, Int>>()
            
            supervisorScope {
                val balanceJobs = collections.map { collection ->
                    async(Dispatchers.IO) {
                        try {
                            val balance = getBalanceOf(address, collection)
                            if (balance > 0) {
                                android.util.Log.d("NFTRepository", "💰 ${collection.name}: balance = $balance")
                                Pair(collection, balance)
                            } else {
                                null
                            }
                        } catch (e: Exception) {
                            android.util.Log.w("NFTRepository", "⚠️ ${collection.name}: balance check failed - ${e.message}")
                            null
                        }
                    }
                }
                
                balanceJobs.awaitAll().filterNotNull().forEach { 
                    collectionsWithBalance.add(it) 
                }
            }
            
            val balanceCheckTime = System.currentTimeMillis() - startTime
            android.util.Log.d("NFTRepository", "✅ Balance check completed in ${balanceCheckTime}ms. Found ${collectionsWithBalance.size} collections with NFTs")
            
            // STEP 2: For collections with balance, find owned token IDs
            if (collectionsWithBalance.isNotEmpty()) {
                android.util.Log.d("NFTRepository", "🔎 Step 2: Finding token IDs for ${collectionsWithBalance.size} collections...")
                
                supervisorScope {
                    val nftJobs = collectionsWithBalance.map { (collection, balance) ->
                        async(Dispatchers.IO) {
                            try {
                                val tokenIds = getOwnedTokenIdsFast(address, collection, balance)
                                android.util.Log.d("NFTRepository", "🎯 ${collection.name}: found ${tokenIds.size} token IDs: $tokenIds")
                                
                                // Fetch NFT details in parallel
                                val nftDetails = tokenIds.mapNotNull { tokenId ->
                                    try {
                                        fetchNFTDetails(tokenId, collection, address)
                                    } catch (e: Exception) {
                                        android.util.Log.w("NFTRepository", "⚠️ Failed to fetch NFT #$tokenId: ${e.message}")
                                        null
                                    }
                                }
                                nftDetails
                            } catch (e: Exception) {
                                android.util.Log.w("NFTRepository", "⚠️ Error processing ${collection.name}: ${e.message}")
                                emptyList()
                            }
                        }
                    }
                    
                    nftJobs.awaitAll().forEach { nftsFromCollection ->
                        allNfts.addAll(nftsFromCollection)
                    }
                }
            }
            
            val totalTime = System.currentTimeMillis() - startTime
            android.util.Log.d("NFTRepository", "🏁 NFT scan completed in ${totalTime}ms. Found ${allNfts.size} NFTs total")
            
            // Update cache
            if (allNfts.isNotEmpty()) {
                nftCache[address] = allNfts.toList()
                lastCacheTime = System.currentTimeMillis()
            }
            
            emit(Result.Success(allNfts))
            
        } catch (e: Exception) {
            android.util.Log.e("NFTRepository", "❌ Error getting NFTs", e)
            emit(Result.Error(e))
        }
    }
    
    /**
     * Fast balance check using direct RPC call
     */
    private suspend fun getBalanceOf(address: String, collection: KnownCollection): Int = withContext(Dispatchers.IO) {
        val addressBytes = address.toByteArray(Charsets.UTF_8)
        val parameter = mutableListOf<Int>()
        
        // Add length prefix (4 bytes, little-endian u32)
        val length = addressBytes.size
        parameter.add(length and 0xFF)
        parameter.add((length shr 8) and 0xFF)
        parameter.add((length shr 16) and 0xFF)
        parameter.add((length shr 24) and 0xFF)
        addressBytes.forEach { parameter.add(it.toInt() and 0xFF) }
        
        val requestBody = """{"jsonrpc":"2.0","id":1,"method":"execute_read_only_call","params":[[{"target_address":"${collection.contractAddress}","target_function":"balanceOf","parameter":$parameter,"max_gas":100000000}]]}"""
        
        val request = Request.Builder()
            .url("https://mainnet.massa.net/api/v2")
            .post(requestBody.toRequestBody("application/json".toMediaTypeOrNull()))
            .build()
        
        httpClient.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: return@withContext 0
            if (response.isSuccessful && body.contains("\"result\"")) {
                val json = JSONObject(body)
                val resultArray = json.optJSONArray("result")
                if (resultArray != null && resultArray.length() > 0) {
                    val firstResult = resultArray.getJSONObject(0)
                    val resultObj = firstResult.optJSONObject("result")
                    val okArray = resultObj?.optJSONArray("Ok")
                    if (okArray != null && okArray.length() > 0) {
                        return@withContext decodeU64FromJsonArray(okArray)
                    }
                }
            }
        }
        0
    }
    
    /**
     * Fast method to get owned token IDs
     * Tries multiple strategies: tokenOfOwnerByIndex, tokensOfOwner, then smart scanning
     */
    private suspend fun getOwnedTokenIdsFast(address: String, collection: KnownCollection, balance: Int): List<Int> {
        val tokenIds = mutableListOf<Int>()
        
        // Strategy 1: Try tokenOfOwnerByIndex (ERC721Enumerable standard)
        val idsFromEnumerable = tryTokenOfOwnerByIndex(address, collection, balance)
        if (idsFromEnumerable.isNotEmpty()) {
            android.util.Log.d("NFTRepository", "✅ Got ${idsFromEnumerable.size} IDs from tokenOfOwnerByIndex")
            return idsFromEnumerable
        }
        
        // Strategy 2: Try tokensOfOwner (some contracts have this)
        val idsFromTokensOf = tryTokensOfOwner(address, collection)
        if (idsFromTokensOf.isNotEmpty()) {
            android.util.Log.d("NFTRepository", "✅ Got ${idsFromTokensOf.size} IDs from tokensOfOwner")
            return idsFromTokensOf
        }
        
        // Strategy 3: Smart scan - only if we have to
        android.util.Log.d("NFTRepository", "🔄 Using smart scan for ${collection.name}")
        return smartScanForTokens(address, collection, balance)
    }
    
    /**
     * Try to use tokenOfOwnerByIndex (ERC721Enumerable)
     */
    private suspend fun tryTokenOfOwnerByIndex(address: String, collection: KnownCollection, balance: Int): List<Int> = withContext(Dispatchers.IO) {
        val tokenIds = mutableListOf<Int>()
        
        try {
            // Query all indices in parallel
            supervisorScope {
                val jobs = (0 until balance).map { index ->
                    async {
                        try {
                            getTokenOfOwnerByIndex(address, collection, index)
                        } catch (e: Exception) {
                            null
                        }
                    }
                }
                jobs.awaitAll().filterNotNull().forEach { tokenIds.add(it) }
            }
        } catch (e: Exception) {
            android.util.Log.d("NFTRepository", "tokenOfOwnerByIndex not available: ${e.message}")
        }
        
        tokenIds
    }
    
    /**
     * Get single token ID by index
     */
    private suspend fun getTokenOfOwnerByIndex(address: String, collection: KnownCollection, index: Int): Int? = withContext(Dispatchers.IO) {
        val addressBytes = address.toByteArray(Charsets.UTF_8)
        val parameter = mutableListOf<Int>()
        
        // Encode address (4-byte length + UTF-8)
        parameter.add(addressBytes.size and 0xFF)
        parameter.add((addressBytes.size shr 8) and 0xFF)
        parameter.add((addressBytes.size shr 16) and 0xFF)
        parameter.add((addressBytes.size shr 24) and 0xFF)
        addressBytes.forEach { parameter.add(it.toInt() and 0xFF) }
        
        // Encode index as u256 (32 bytes, little-endian)
        var value = index.toLong()
        for (i in 0..7) {
            parameter.add((value and 0xFF).toInt())
            value = value shr 8
        }
        for (i in 8..31) parameter.add(0)
        
        val requestBody = """{"jsonrpc":"2.0","id":1,"method":"execute_read_only_call","params":[[{"target_address":"${collection.contractAddress}","target_function":"tokenOfOwnerByIndex","parameter":$parameter,"max_gas":100000000}]]}"""
        
        val request = Request.Builder()
            .url("https://mainnet.massa.net/api/v2")
            .post(requestBody.toRequestBody("application/json".toMediaTypeOrNull()))
            .build()
        
        httpClient.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: return@withContext null
            if (response.isSuccessful && body.contains("\"Ok\"")) {
                val json = JSONObject(body)
                val resultArray = json.optJSONArray("result")
                if (resultArray != null && resultArray.length() > 0) {
                    val okArray = resultArray.getJSONObject(0)
                        .optJSONObject("result")
                        ?.optJSONArray("Ok")
                    if (okArray != null && okArray.length() > 0) {
                        return@withContext decodeU64FromJsonArray(okArray)
                    }
                }
            }
        }
        null
    }
    
    /**
     * Try tokensOfOwner function
     */
    private suspend fun tryTokensOfOwner(address: String, collection: KnownCollection): List<Int> = withContext(Dispatchers.IO) {
        try {
            val addressBytes = address.toByteArray(Charsets.UTF_8)
            val parameter = mutableListOf<Int>()
            
            parameter.add(addressBytes.size and 0xFF)
            parameter.add((addressBytes.size shr 8) and 0xFF)
            parameter.add((addressBytes.size shr 16) and 0xFF)
            parameter.add((addressBytes.size shr 24) and 0xFF)
            addressBytes.forEach { parameter.add(it.toInt() and 0xFF) }
            
            val requestBody = """{"jsonrpc":"2.0","id":1,"method":"execute_read_only_call","params":[[{"target_address":"${collection.contractAddress}","target_function":"tokensOfOwner","parameter":$parameter,"max_gas":100000000}]]}"""
            
            val request = Request.Builder()
                .url("https://mainnet.massa.net/api/v2")
                .post(requestBody.toRequestBody("application/json".toMediaTypeOrNull()))
                .build()
            
            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: return@withContext emptyList()
                if (response.isSuccessful && body.contains("\"Ok\"")) {
                    val json = JSONObject(body)
                    val okArray = json.optJSONArray("result")
                        ?.optJSONObject(0)
                        ?.optJSONObject("result")
                        ?.optJSONArray("Ok")
                    
                    if (okArray != null && okArray.length() > 0) {
                        val bytes = (0 until okArray.length()).map { okArray.getInt(it) }
                        return@withContext decodeTokenIdsFromBytes(bytes)
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.d("NFTRepository", "tokensOfOwner not available: ${e.message}")
        }
        emptyList()
    }
    
    /**
     * Smart scan: Uses BATCH RPC calls to check multiple tokens at once
     * This is dramatically faster than individual calls
     */
    private suspend fun smartScanForTokens(address: String, collection: KnownCollection, targetBalance: Int): List<Int> = withContext(Dispatchers.IO) {
        val foundTokens = java.util.concurrent.ConcurrentHashMap<Int, Boolean>()
        
        // Get totalSupply to know range
        val totalSupply = getTotalSupply(collection) ?: collection.totalSupply
        android.util.Log.d("NFTRepository", "📊 Smart scan: looking for $targetBalance tokens in range 1-$totalSupply")
        
        // Use batch RPC - check 20 tokens per RPC call (limited by JSON-RPC)
        val rpcBatchSize = 20
        // Process multiple RPC batches in parallel
        val parallelBatches = 5
        val tokensPerRound = rpcBatchSize * parallelBatches // 100 tokens per round
        
        var currentEnd = totalSupply
        
        while (currentEnd > 0 && foundTokens.size < targetBalance) {
            val roundStart = maxOf(1, currentEnd - tokensPerRound + 1)
            
            // Split into parallel batch jobs
            val batchJobs = mutableListOf<kotlinx.coroutines.Deferred<List<Int>>>()
            
            supervisorScope {
                var batchStart = currentEnd
                while (batchStart >= roundStart) {
                    val batchEnd = maxOf(roundStart, batchStart - rpcBatchSize + 1)
                    val tokenRange = (batchStart downTo batchEnd).toList()
                    
                    batchJobs.add(async {
                        checkTokenOwnersBatch(tokenRange, collection, address)
                    })
                    
                    batchStart = batchEnd - 1
                }
                
                // Wait for all batches and collect results
                try {
                    batchJobs.awaitAll().flatten().forEach { tokenId ->
                        foundTokens[tokenId] = true
                        android.util.Log.d("NFTRepository", "🎯 Found token #$tokenId")
                    }
                } catch (e: Exception) {
                    android.util.Log.w("NFTRepository", "Batch error: ${e.message}")
                }
            }
            
            // Move to next round
            currentEnd = roundStart - 1
            
            // Early exit if we found all tokens
            if (foundTokens.size >= targetBalance) {
                android.util.Log.d("NFTRepository", "✅ Found all $targetBalance tokens, stopping scan")
                break
            }
        }
        
        foundTokens.keys.toList().sortedDescending()
    }
    
    /**
     * Check multiple token owners in a single batch RPC call
     */
    private suspend fun checkTokenOwnersBatch(tokenIds: List<Int>, collection: KnownCollection, targetAddress: String): List<Int> = withContext(Dispatchers.IO) {
        val ownedTokens = mutableListOf<Int>()
        
        try {
            // Build batch request with multiple read-only calls
            val callsArray = JSONArray()
            
            for (tokenId in tokenIds) {
                // Encode tokenId as u256 bytes (32 bytes, little-endian)
                val tokenIdBytes = JSONArray()
                var value = tokenId.toLong()
                for (i in 0..7) {
                    tokenIdBytes.put((value and 0xFF).toInt())
                    value = value shr 8
                }
                for (i in 8..31) {
                    tokenIdBytes.put(0)
                }
                
                callsArray.put(JSONObject().apply {
                    put("target_address", collection.contractAddress)
                    put("target_function", "ownerOf")
                    put("parameter", tokenIdBytes)
                    put("max_gas", 50000000)
                })
            }
            
            val requestBody = JSONObject().apply {
                put("jsonrpc", "2.0")
                put("id", 1)
                put("method", "execute_read_only_call")
                put("params", JSONArray().put(callsArray))
            }.toString()
            
            val request = Request.Builder()
                .url("https://mainnet.massa.net/api/v2")
                .post(requestBody.toRequestBody("application/json".toMediaTypeOrNull()))
                .build()
            
            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: return@withContext emptyList()
                
                val json = JSONObject(body)
                val resultArray = json.optJSONArray("result") ?: return@withContext emptyList()
                
                // Process each result
                for (i in 0 until minOf(resultArray.length(), tokenIds.size)) {
                    val resultObj = resultArray.optJSONObject(i)?.optJSONObject("result")
                    val okArray = resultObj?.optJSONArray("Ok")
                    
                    if (okArray != null && okArray.length() > 0) {
                        // Decode owner address
                        val bytes = ByteArray(okArray.length())
                        for (j in 0 until okArray.length()) {
                            bytes[j] = okArray.getInt(j).toByte()
                        }
                        val owner = String(bytes, Charsets.UTF_8)
                        
                        if (owner.equals(targetAddress, ignoreCase = true)) {
                            ownedTokens.add(tokenIds[i])
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("NFTRepository", "Batch RPC error: ${e.message}")
        }
        
        ownedTokens
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
    
    private suspend fun getTotalSupply(collection: KnownCollection): Int? = withContext(Dispatchers.IO) {
        try {
            val requestBody = """{"jsonrpc":"2.0","id":1,"method":"execute_read_only_call","params":[[{"target_address":"${collection.contractAddress}","target_function":"totalSupply","parameter":[],"max_gas":100000000}]]}"""
            
            val request = Request.Builder()
                .url("https://mainnet.massa.net/api/v2")
                .post(requestBody.toRequestBody("application/json".toMediaTypeOrNull()))
                .build()
            
            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: return@withContext null
                val json = JSONObject(body)
                val okArray = json.optJSONArray("result")
                    ?.optJSONObject(0)
                    ?.optJSONObject("result")
                    ?.optJSONArray("Ok")
                
                if (okArray != null && okArray.length() >= 8) {
                    var supply = 0L
                    for (i in 0..7) {
                        supply = supply or ((okArray.getInt(i).toLong() and 0xFF) shl (i * 8))
                    }
                    return@withContext supply.toInt()
                }
            }
        } catch (e: Exception) {
            android.util.Log.d("NFTRepository", "Error getting totalSupply: ${e.message}")
        }
        null
    }
    
    private suspend fun getTokenOwner(tokenId: Int, collection: KnownCollection): String? = withContext(Dispatchers.IO) {
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
                val body = response.body?.string() ?: return@withContext null
                
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
                        return@withContext owner
                    }
                }
            }
        } catch (e: Exception) {
            // Silently ignore - token might not exist
        }
        null
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