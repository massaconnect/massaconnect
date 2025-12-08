package com.massapay.android.network.repository

import com.massapay.android.core.model.BuyRollsRequest
import com.massapay.android.core.model.SellRollsRequest
import com.massapay.android.core.model.StakingInfo
import com.massapay.android.core.model.StakingOperationResult
import com.massapay.android.core.util.Result
import com.massapay.android.network.api.MassaApi
import com.massapay.android.network.model.JsonRpcRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StakingRepository @Inject constructor(
    private val massaApi: MassaApi,
    private val massaRepository: MassaRepository
) {
    
    /**
     * Get staking info for an address (rolls, balance, etc.)
     */
    /**
     * Get staking info for an address (rolls, balance, etc.)
     * Note: Massa API returns balance in MAS (not nanoMAS) for get_addresses
     */
    fun getStakingInfo(address: String): Flow<Result<StakingInfo>> = flow {
        try {
            emit(Result.Loading)
            
            val request = JsonRpcRequest(
                method = "get_addresses",
                params = listOf(listOf(address))
            )
            
            val response = massaApi.getAddresses(request)
            
            android.util.Log.d("StakingRepository", "get_addresses response: ${response.result}")
            
            if (response.error != null) {
                emit(Result.Error(Exception(response.error.message)))
                return@flow
            }
            
            val addressInfo = response.result?.firstOrNull()
            
            if (addressInfo == null) {
                // No info found, return empty staking info
                emit(Result.Success(StakingInfo(address = address)))
                return@flow
            }
            
            // Balance is already in MAS from this API
            val balanceMas = addressInfo.candidateBalance 
                ?: addressInfo.finalBalance 
                ?: addressInfo.balance 
                ?: "0"
            
            android.util.Log.d("StakingRepository", "Balance: $balanceMas MAS")
            android.util.Log.d("StakingRepository", "Rolls: final=${addressInfo.finalRollCount}, candidate=${addressInfo.candidateRollCount}")
            android.util.Log.d("StakingRepository", "Deferred credits raw: ${addressInfo.deferredCredits}")
            
            // Calculate total deferred credits (these are also in MAS)
            val totalDeferred = addressInfo.deferredCredits
                ?.mapNotNull { credit -> 
                    android.util.Log.d("StakingRepository", "Credit amount: ${credit.amount}")
                    credit.amount?.toBigDecimalOrNull() 
                }
                ?.fold(java.math.BigDecimal.ZERO) { acc, amount -> acc.add(amount) }
                ?.stripTrailingZeros()
                ?.toPlainString() ?: "0"
            
            android.util.Log.d("StakingRepository", "Total deferred: $totalDeferred MAS")
            
            val stakingInfo = StakingInfo(
                address = address,
                candidateRolls = addressInfo.candidateRollCount ?: 0,
                finalRolls = addressInfo.finalRollCount ?: 0,
                activeRolls = 0, // Not directly available from API
                balance = balanceMas,
                deferredCredits = totalDeferred
            )
            
            android.util.Log.d("StakingRepository", "StakingInfo: rolls=${stakingInfo.totalRolls}, balance=${stakingInfo.balance}")
            
            emit(Result.Success(stakingInfo))
            
        } catch (e: Exception) {
            android.util.Log.e("StakingRepository", "Error getting staking info", e)
            emit(Result.Error(e))
        }
    }
    
    /**
     * Buy rolls with MAS
     * 1 Roll = 100 MAS
     * Now delegates to MassaRepository for proper signing
     */
    suspend fun buyRolls(
        address: String,
        request: BuyRollsRequest,
        privateKey: String,
        publicKey: String
    ): StakingOperationResult {
        return try {
            android.util.Log.d("StakingRepository", "Buying ${request.rollCount} rolls for address $address")
            
            val result = massaRepository.buyRolls(
                from = address,
                rollCount = request.rollCount,
                fee = request.fee,
                privateKey = privateKey,
                publicKey = publicKey
            )
            
            when (result) {
                is Result.Success -> {
                    android.util.Log.i("StakingRepository", "✅ Buy rolls successful: ${result.data}")
                    StakingOperationResult(
                        success = true,
                        operationId = result.data
                    )
                }
                is Result.Error -> {
                    android.util.Log.e("StakingRepository", "Buy rolls failed: ${result.exception.message}")
                    StakingOperationResult(
                        success = false,
                        error = result.exception.message ?: "Unknown error"
                    )
                }
                is Result.Loading -> {
                    StakingOperationResult(
                        success = false,
                        error = "Unexpected loading state"
                    )
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("StakingRepository", "Error buying rolls", e)
            StakingOperationResult(
                success = false,
                error = e.message ?: "Unknown error"
            )
        }
    }
    
    /**
     * Sell rolls to get MAS back
     * Note: There's a delay of ~3 cycles before MAS is available
     * Now delegates to MassaRepository for proper signing
     */
    suspend fun sellRolls(
        address: String,
        request: SellRollsRequest,
        privateKey: String,
        publicKey: String
    ): StakingOperationResult {
        return try {
            android.util.Log.d("StakingRepository", "Selling ${request.rollCount} rolls for address $address")
            
            val result = massaRepository.sellRolls(
                from = address,
                rollCount = request.rollCount,
                fee = request.fee,
                privateKey = privateKey,
                publicKey = publicKey
            )
            
            when (result) {
                is Result.Success -> {
                    android.util.Log.i("StakingRepository", "✅ Sell rolls successful: ${result.data}")
                    StakingOperationResult(
                        success = true,
                        operationId = result.data
                    )
                }
                is Result.Error -> {
                    android.util.Log.e("StakingRepository", "Sell rolls failed: ${result.exception.message}")
                    StakingOperationResult(
                        success = false,
                        error = result.exception.message ?: "Unknown error"
                    )
                }
                is Result.Loading -> {
                    StakingOperationResult(
                        success = false,
                        error = "Unexpected loading state"
                    )
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("StakingRepository", "Error selling rolls", e)
            StakingOperationResult(
                success = false,
                error = e.message ?: "Unknown error"
            )
        }
    }
}
