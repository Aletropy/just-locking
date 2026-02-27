package com.aletropy.justlocking

import com.aletropy.justlocking.data.LockDataManager
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.BlockEntityType
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent
import net.neoforged.neoforge.items.IItemHandler

/**
 * Handles the interception of block capabilities to prevent Hopper interactions. This is the
 * primary mechanism for protecting locked blocks from automated item extraction/insertion.
 */
object LockingCapabilities {

    /**
     * Registers capability providers for containers. If a block is locked, it returns a dummy
     * "empty" handler that rejects all item operations.
     */
    @SubscribeEvent
    fun onRegisterCapabilities(event: RegisterCapabilitiesEvent) {
        /**
         * A custom IItemHandler that effectively disconnects the container from the world. Used to
         * block Hoppers and Minecart Hoppers.
         */
        val emptyHandler =
                object : IItemHandler {
                    override fun getSlots(): Int = 0
                    override fun getStackInSlot(slot: Int): ItemStack = ItemStack.EMPTY
                    override fun insertItem(
                            slot: Int,
                            stack: ItemStack,
                            simulate: Boolean
                    ): ItemStack = stack
                    override fun extractItem(slot: Int, amount: Int, simulate: Boolean): ItemStack =
                            ItemStack.EMPTY
                    override fun getSlotLimit(slot: Int): Int = 0
                    override fun isItemValid(slot: Int, stack: ItemStack): Boolean = false
                }

        // Apply protection to all supported container types
        val targetTypes =
                listOf(
                        BlockEntityType.CHEST,
                        BlockEntityType.TRAPPED_CHEST,
                        BlockEntityType.BARREL,
                        BlockEntityType.SHULKER_BOX
                )

        for (type in targetTypes) {
            event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, type) { be, _ ->
                // Use centralized LockDataManager for consistency and clean checks
                if (be != null && LockDataManager.isLockable(be.blockState, be)) {
                    val lockOwner =
                            LockDataManager.getLockOwner(be.level!!, be.blockPos, be.blockState, be)
                    if (lockOwner.isNotEmpty()) {
                        return@registerBlockEntity emptyHandler
                    }
                }
                null
            }
        }
    }
}
