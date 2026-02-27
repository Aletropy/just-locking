package com.aletropy.justlocking.commands

import net.minecraft.commands.Commands
import net.minecraft.commands.arguments.EntityArgument
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.event.RegisterCommandsEvent

/**
 * Registration entry point for the "/lock" command tree. Adheres to SRP by delegating execution
 * logic to specialized handlers.
 */
object CommandLock {

        @SubscribeEvent
        fun onRegisterCommands(event: RegisterCommandsEvent) {
                val dispatcher = event.dispatcher

                dispatcher.register(
                        Commands.literal("lock")
                                // Subcommand: /lock trust <player>
                                .then(
                                        Commands.literal("trust")
                                                .then(
                                                        Commands.argument(
                                                                        "player",
                                                                        EntityArgument.player()
                                                                )
                                                                .executes { context ->
                                                                        TrustCommandHandler.handle(
                                                                                context,
                                                                                true
                                                                        )
                                                                }
                                                )
                                )
                                // Subcommand: /lock untrust <player>
                                .then(
                                        Commands.literal("untrust")
                                                .then(
                                                        Commands.argument(
                                                                        "player",
                                                                        EntityArgument.player()
                                                                )
                                                                .executes { context ->
                                                                        TrustCommandHandler.handle(
                                                                                context,
                                                                                false
                                                                        )
                                                                }
                                                )
                                )
                                // Default: /lock (locks block being looked at)
                                .executes { context -> LockCommandHandler.execute(context) }
                )

                // Register the independent /unlock command
                dispatcher.register(
                        Commands.literal("unlock").executes { context ->
                                UnlockCommandHandler.execute(context)
                        }
                )
        }
}
