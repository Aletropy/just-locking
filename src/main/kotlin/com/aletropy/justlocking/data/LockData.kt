package com.aletropy.justlocking.data

/**
 * Data class representing the lock information attached to a block entity.
 *
 * @property owner The UUID string of the player who owns the lock.
 * ```
 *                 An empty string indicates the block is not locked.
 * ```
 */
data class LockData(var owner: String = "") {
    /**
     * Checks if the block is currently locked.
     *
     * @return True if the block has an owner assigned.
     */
    fun isLocked(): Boolean = owner.isNotEmpty()
}
