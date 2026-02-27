package com.aletropy.justlocking.events.protection

import com.aletropy.justlocking.data.LockDataManager
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.event.level.ExplosionEvent

/** Optimized explosion handler focused on preventing destruction of locked containers. */
object ExplosionProtectionHandler {

    /**
     * Filters affected blocks from an explosion if they are locked.
     *
     * Performance Design:
     * 1. Iterates through the list of affected blocks.
     * 2. Only performs a BlockEntity lookup if the block is a container type.
     * 3. Removes locked blocks from the explosion's damage list.
     */
    @SubscribeEvent
    fun onExplosionDetonate(event: ExplosionEvent.Detonate) {
        val level = event.level
        if (level.isClientSide) return

        val iterator = event.affectedBlocks.iterator()
        while (iterator.hasNext()) {
            val pos = iterator.next()

            // Early Exit Optimization: Check if the block has an entry in the chunk's tile entity
            // map.
            // This is significantly faster than level.getBlockEntity() as it avoids creating or
            // loading
            // unnecessary data structures for non-tile-entities (stone, air, etc.).
            val state = level.getBlockState(pos)
            if (!LockDataManager.mightBeLockable(state)) continue

            val be = level.getBlockEntity(pos)
            if (LockDataManager.isLockable(state, be)) {
                if (LockDataManager.isLocked(level, pos, state, be)) {
                    iterator.remove()
                }
            }
        }
    }
}
