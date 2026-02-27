package com.aletropy.justlocking.commands

import com.aletropy.justlocking.data.TrustData
import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel

/** Handles Global Trust subcommands. */
object TrustCommandHandler {

    /**
     * Entry point for trust/untrust logic.
     *
     * @param isTrusting True for 'trust', false for 'untrust'.
     */
    fun handle(context: CommandContext<CommandSourceStack>, isTrusting: Boolean): Int {
        val source = context.source
        val player = source.playerOrException
        val targetPlayer = EntityArgument.getPlayer(context, "player")

        val ownerUUID = player.stringUUID
        val targetUUID = targetPlayer.stringUUID

        if (ownerUUID == targetUUID) {
            player.sendSystemMessage(Component.literal("§cYou cannot trust yourself."))
            return 0
        }

        val level = player.level() as? ServerLevel ?: return 0
        val trustData = TrustData.get(level)

        if (isTrusting) {
            if (trustData.trustPlayer(ownerUUID, targetUUID)) {
                player.sendSystemMessage(
                        Component.literal(
                                "§aAdded ${targetPlayer.name.string} to your trusted players."
                        )
                )

                // Send feedback to the target player if they are online
                targetPlayer.sendSystemMessage(
                        Component.literal("§aYou have been trusted by ${player.name.string}.")
                )
            } else {
                player.sendSystemMessage(
                        Component.literal("§e${targetPlayer.name.string} is already trusted.")
                )
            }
        } else {
            if (trustData.untrustPlayer(ownerUUID, targetUUID)) {
                player.sendSystemMessage(
                        Component.literal(
                                "§aRemoved ${targetPlayer.name.string} from your trusted players."
                        )
                )

                // Also tell the target player if they were untrusted
                targetPlayer.sendSystemMessage(
                        Component.literal(
                                "§cYou have been removed from ${player.name.string}'s trusted players."
                        )
                )
            } else {
                player.sendSystemMessage(
                        Component.literal(
                                "§e${targetPlayer.name.string} is not in your trusted players."
                        )
                )
            }
        }
        return 1
    }
}
