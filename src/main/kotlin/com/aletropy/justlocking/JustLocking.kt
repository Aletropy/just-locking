package com.aletropy.justlocking

import net.neoforged.api.distmarker.Dist
import net.neoforged.fml.common.Mod
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import thedarkcolour.kotlinforforge.neoforge.forge.runForDist

@Mod(JustLocking.MOD_ID, dist = [Dist.DEDICATED_SERVER])
object JustLocking
{
    const val MOD_ID = "justlocking"
    val LOGGER: Logger = LogManager.getLogger(MOD_ID)

    init
    {
        LOGGER.info("JustLocking is loading...")
    }
}