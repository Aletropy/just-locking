package com.aletropy.justlocking.events.interaction

import com.aletropy.justlocking.data.LockDataManager
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent

/** Manages player interactions (opening/locking) with blocks. */
object PlayerInteractionHandler {

    /**
     * Fired when a player right-clicks a block. Prevents unauthorized players from opening locked
     * containers.
     */
    @SubscribeEvent
    fun onRightClickBlock(event: PlayerInteractEvent.RightClickBlock) {
        val level = event.level
        if (level.isClientSide) return

        val serverLevel = level as ServerLevel

        // Use LockDataManager's optimized access check
        if (!LockDataManager.canAccess(serverLevel, event.pos, event.entity)) {
            event.entity.displayClientMessage(
                    Component.literal("§cThis block is locked by its owner."),
                    true
            )
            // Play a locking sound effect as feedback
            serverLevel.playSound(
                    null,
                    event.pos,
                    net.minecraft.sounds.SoundEvents.CHEST_LOCKED,
                    net.minecraft.sounds.SoundSource.BLOCKS,
                    1.0f,
                    1.0f
            )
            event.isCanceled = true
        }
    }
}
