package com.aletropy.justlocking

import com.aletropy.justlocking.commands.CommandLock
import com.aletropy.justlocking.events.interaction.PlayerBreakHandler
import com.aletropy.justlocking.events.interaction.PlayerInteractionHandler
import com.aletropy.justlocking.events.protection.EntityDestructionProtectionHandler
import com.aletropy.justlocking.events.protection.ExplosionProtectionHandler
import com.aletropy.justlocking.events.protection.PistonProtectionHandler
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.common.Mod
import net.neoforged.neoforge.common.NeoForge
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

/**
 * Main entry point for the JustLocking mod.
 *
 * This mod provides a server-side only locking mechanism for containers. Architecture: Clean,
 * modular handlers with single responsibilities. Performance: Optimized event early-exits for
 * high-player-count stability.
 */
@Mod(JustLocking.MOD_ID)
class JustLocking(modEventBus: IEventBus) {

    init {
        LOGGER.info("JustLocking is initializing...")

        // Register Data Attachments (Mod Bus)
        LockingAttachments.ATTACHMENT_TYPES.register(modEventBus)

        // Register Capability Handlers (Mod Bus)
        modEventBus.register(LockingCapabilities)

        // Register Command Handlers (Forge Bus)
        NeoForge.EVENT_BUS.register(CommandLock)

        // Register Interaction Handlers (Forge Bus)
        NeoForge.EVENT_BUS.register(PlayerInteractionHandler)
        NeoForge.EVENT_BUS.register(PlayerBreakHandler)

        // Register Protection Handlers (Forge Bus)
        NeoForge.EVENT_BUS.register(ExplosionProtectionHandler)
        NeoForge.EVENT_BUS.register(PistonProtectionHandler)
        NeoForge.EVENT_BUS.register(EntityDestructionProtectionHandler)

        LOGGER.info("JustLocking initialized successfully.")
    }

    companion object {
        const val MOD_ID = "justlocking"
        /** Shared logger for debugging and audit logs. */
        val LOGGER: Logger = LogManager.getLogger(MOD_ID)
    }
}
