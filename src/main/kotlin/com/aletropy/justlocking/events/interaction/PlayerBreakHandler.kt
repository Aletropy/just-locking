package com.aletropy.justlocking.events.interaction

import com.aletropy.justlocking.data.LockDataManager
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.event.level.BlockEvent

/** Ensures locked blocks cannot be broken by unauthorized players. */
object PlayerBreakHandler {

    /** Fired when a player attempts to break a block. */
    @SubscribeEvent
    fun onBlockBreak(event: BlockEvent.BreakEvent) {
        val level = event.level
        if (level.isClientSide) return

        val serverLevel = level as ServerLevel

        // Ensure only the owner or trusted players can break the block
        if (!LockDataManager.canAccess(serverLevel, event.pos, event.player)) {
            event.player.displayClientMessage(
                    Component.literal("§cYou cannot break this locked block."),
                    true
            )
            event.isCanceled = true
        }
    }
}
