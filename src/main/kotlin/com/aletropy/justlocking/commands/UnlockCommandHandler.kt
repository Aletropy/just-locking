package com.aletropy.justlocking.commands

import com.aletropy.justlocking.data.LockDataManager
import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.HitResult

/** Logic handler for the "/unlock" command. */
object UnlockCommandHandler {

    /**
     * Executes the unlocking action based on where the player is looking. Checks if the player owns
     * the block or has OP bypass.
     */
    fun execute(context: CommandContext<CommandSourceStack>): Int {
        val source = context.source
        val player: ServerPlayer = source.playerOrException

        // Raytrace to find the block the player is looking at
        val start = player.eyePosition
        val look = player.lookAngle
        val end = start.add(look.x * 5.0, look.y * 5.0, look.z * 5.0)
        val hitResult =
                player.level()
                        .clip(
                                ClipContext(
                                        start,
                                        end,
                                        ClipContext.Block.OUTLINE,
                                        ClipContext.Fluid.NONE,
                                        player
                                )
                        )

        if (hitResult.type == HitResult.Type.BLOCK) {
            val pos = hitResult.blockPos
            val level = player.level() as? ServerLevel ?: return 0
            val state = level.getBlockState(pos)
            val be = level.getBlockEntity(pos)

            if (LockDataManager.isLockable(state, be)) {
                val owner = LockDataManager.getLockOwner(level, pos)
                if (owner.isNotEmpty()) {
                    if (owner == player.stringUUID || player.hasPermissions(2)) {
                        if (LockDataManager.unlockBlock(level, pos)) {
                            player.sendSystemMessage(
                                    Component.literal("§aBlock successfully unlocked!")
                            )
                        } else {
                            player.sendSystemMessage(Component.literal("§cFailed to unlock block."))
                        }
                    } else {
                        player.sendSystemMessage(
                                Component.literal("§cYou don't own this locked block.")
                        )
                    }
                } else {
                    player.sendSystemMessage(Component.literal("§eThis block is not locked."))
                }
            } else {
                player.sendSystemMessage(
                        Component.literal(
                                "§cYou can only unlock chests, barrels, shulker boxes, and doors!"
                        )
                )
            }
        } else {
            player.sendSystemMessage(
                    Component.literal("§cYou must be looking at a lockable block.")
            )
        }
        return 1
    }
}
