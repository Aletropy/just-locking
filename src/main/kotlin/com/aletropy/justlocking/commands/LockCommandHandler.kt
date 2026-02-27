package com.aletropy.justlocking.commands

import com.aletropy.justlocking.data.LockDataManager
import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.HitResult

/** Logic handler for the base "/lock" command. */
object LockCommandHandler {

    /**
     * Executes the locking action based on where the player is looking. Includes double-chest
     * detection logic.
     */
    fun execute(context: CommandContext<CommandSourceStack>): Int {
        val source = context.source
        val player: ServerPlayer = source.playerOrException

        // Raytrace to find the block the player is looking at (max 5 blocks distance)
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
            val level = player.level()
            val state = level.getBlockState(pos)
            val be = level.getBlockEntity(pos)

            if (LockDataManager.isLockable(state, be)) {
                val owner = LockDataManager.getLockOwner(level, pos)

                if (owner.isNotEmpty()) {
                    if (owner == player.stringUUID) {
                        player.sendSystemMessage(
                                Component.literal("§eThis block is already locked by you.")
                        )
                    } else {
                        player.sendSystemMessage(
                                Component.literal(
                                        "§cThis block is already locked by another player."
                                )
                        )
                    }
                } else {
                    // Lock the block and its double-chest pair if applicable
                    if (LockDataManager.lockBlock(level, pos, player.stringUUID)) {
                        player.sendSystemMessage(Component.literal("§aBlock successfully locked!"))

                        // Play tripwire attach sound
                        level.playSound(
                                null,
                                pos,
                                net.minecraft.sounds.SoundEvents.TRIPWIRE_ATTACH,
                                net.minecraft.sounds.SoundSource.BLOCKS,
                                1.0f,
                                1.0f
                        )

                        // Spawn totem of undying particles server-side to clients
                        if (level is net.minecraft.server.level.ServerLevel) {
                            val centerX = pos.x + 0.5
                            val centerY = pos.y + 0.5
                            val centerZ = pos.z + 0.5
                            // 10 particles of TOTEM_OF_UNDYING type with small spread
                            level.sendParticles(
                                    net.minecraft.core.particles.ParticleTypes.TOTEM_OF_UNDYING,
                                    centerX,
                                    centerY,
                                    centerZ,
                                    15,
                                    0.3,
                                    0.3,
                                    0.3,
                                    0.1
                            )
                        }
                    } else {
                        player.sendSystemMessage(Component.literal("§cFailed to lock block."))
                    }
                }
            } else {
                player.sendSystemMessage(
                        Component.literal("§cYou can only lock chests, barrels, and shulker boxes!")
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
