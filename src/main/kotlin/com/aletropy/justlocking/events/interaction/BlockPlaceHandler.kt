package com.aletropy.justlocking.events.interaction

import com.aletropy.justlocking.data.LockDataManager
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.ChestBlock
import net.minecraft.world.level.block.state.properties.ChestType
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.event.level.BlockEvent

/** Prevents players from placing chests next to locked chests to bypass security. */
object BlockPlaceHandler {

    @SubscribeEvent
    fun onBlockPlace(event: BlockEvent.EntityPlaceEvent) {
        val level = event.level as? ServerLevel ?: return
        if (level.isClientSide) return

        val state = event.placedBlock
        val pos = event.pos
        val player = event.entity

        if (state.block is ChestBlock) {
            if (state.hasProperty(ChestBlock.TYPE)) {
                val type = state.getValue(ChestBlock.TYPE)
                if (type != ChestType.SINGLE) {
                    val side = ChestBlock.getConnectedDirection(state)
                    val otherPos = pos.relative(side)

                    val otherOwner = LockDataManager.getLockOwner(level, otherPos)
                    if (otherOwner.isNotEmpty()) {
                        val isOwnerOrOp =
                                player != null &&
                                        (player.stringUUID == otherOwner ||
                                                player.hasPermissions(2))
                        if (!isOwnerOrOp) {
                            player?.sendSystemMessage(
                                    Component.literal(
                                            "§cYou cannot place a chest next to a locked chest owned by someone else!"
                                    )
                            )
                            event.isCanceled = true
                        } else {
                            LockDataManager.lockBlock(level, pos, otherOwner)
                        }
                    }
                }
            }
        }
    }
}
