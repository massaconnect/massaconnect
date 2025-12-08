package com.massapay.android.core.model

import java.io.Serializable

/**
 * Staking/Rolls information for a Massa address
 * 
 * Note on roll counts:
 * - finalRolls: Confirmed rolls you own
 * - candidateRolls: Rolls pending confirmation (buying/selling in progress)
 * 
 * The actual roll count is the MAX of final and candidate, not the sum,
 * because candidate represents the future state, not additional rolls.
 */
data class StakingInfo(
    val address: String,
    val candidateRolls: Int = 0,      // Rolls pending (future state after confirmation)
    val finalRolls: Int = 0,          // Confirmed rolls (current confirmed state)
    val activeRolls: Int = 0,         // Rolls actively staking (linked to node)
    val balance: String = "0",        // Available MAS balance
    val deferredCredits: String = "0" // MAS from sold rolls (takes 3 cycles)
) : Serializable {
    
    /**
     * Total rolls = max of final and candidate
     * When buying: candidate increases first, then final catches up
     * When selling: candidate decreases first, then final catches up
     */
    val totalRolls: Int
        get() = maxOf(finalRolls, candidateRolls)
    
    /**
     * Check if there's a pending roll operation
     */
    val hasPendingOperation: Boolean
        get() = finalRolls != candidateRolls
    
    val rollsValueInMas: Double
        get() = totalRolls * ROLL_PRICE_MAS
    
    companion object {
        const val ROLL_PRICE_MAS = 100.0  // 1 Roll = 100 MAS
    }
}

/**
 * Request to buy rolls
 */
data class BuyRollsRequest(
    val rollCount: Int,
    val fee: String = "0.01"
) {
    val totalCost: Double
        get() = rollCount * StakingInfo.ROLL_PRICE_MAS + fee.toDoubleOrNull().let { it ?: 0.01 }
}

/**
 * Request to sell rolls
 */
data class SellRollsRequest(
    val rollCount: Int,
    val fee: String = "0.01"
) {
    val masToReceive: Double
        get() = rollCount * StakingInfo.ROLL_PRICE_MAS
}

/**
 * Result of a staking operation
 */
data class StakingOperationResult(
    val success: Boolean,
    val operationId: String? = null,
    val error: String? = null
)
