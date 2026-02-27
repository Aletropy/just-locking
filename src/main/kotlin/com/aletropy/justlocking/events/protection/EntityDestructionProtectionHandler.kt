package com.aletropy.justlocking.events.protection

import com.aletropy.justlocking.data.LockDataManager
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.event.entity.living.LivingDestroyBlockEvent

/**
 * Specifically prevents Withers and Ender Dragons from destroying locked blocks via their physical
 * movement path (Boss Griefing).
 */
object EntityDestructionProtectionHandler {

    /**
     * Fired when an entity (like the Wither or Dragon) attempts to break a block via movement. We
     * cancel this to protect the integrity of locked blocks.
     */
    @SubscribeEvent
    fun onLivingDestroyBlock(event: LivingDestroyBlockEvent) {
        val level = event.entity.level()
        if (level.isClientSide) return

        val pos = event.pos
        val state = level.getBlockState(pos)
        val be = level.getBlockEntity(pos)

        if (LockDataManager.isLockable(state, be)) {
            if (LockDataManager.isLocked(level, pos, state, be)) {
                // Cancel the block destruction
                event.isCanceled = true
            }
        }
    }
}
