package com.aletropy.justlocking.events.protection

import com.aletropy.justlocking.data.LockDataManager
import net.minecraft.world.level.Level
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.event.level.PistonEvent

/** Prevents pistons from moving or crushing locked containers. */
object PistonProtectionHandler {

    /**
     * Inspects blocks affected by a piston movement. Cancels the event if any locked block is in
     * the movement path.
     */
    @SubscribeEvent
    fun onPistonPre(event: PistonEvent.Pre) {
        val level = event.level as? Level ?: return
        if (level.isClientSide) return

        val structureHelper = event.structureHelper ?: return
        structureHelper.resolve()

        // Combine both moved and destroyed blocks for comprehensive protection
        val allAffected = structureHelper.toPush + structureHelper.toDestroy

        for (pos in allAffected) {
            val state = level.getBlockState(pos)
            if (!LockDataManager.mightBeLockable(state)) continue

            val be = level.getBlockEntity(pos)
            if (LockDataManager.isLockable(state, be)) {
                if (LockDataManager.isLocked(level, pos)) {
                    // Cancel moving the entire structure to prevent bypasses
                    event.isCanceled = true
                    return
                }
            }
        }
    }
}
