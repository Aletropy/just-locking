package com.aletropy.justlocking.data

import com.aletropy.justlocking.LockingAttachments
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.tags.BlockTags
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.ChestBlock
import net.minecraft.world.level.block.entity.BarrelBlockEntity
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.ChestBlockEntity
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.ChestType
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf

/**
 * High-performance coordinator for managing locks on block entities and doors. Centralizes logic
 * for double chests, doors, and lock access.
 */
object LockDataManager {

    /** Determines if a block is a valid target for locking. */
    fun isLockable(state: BlockState, be: BlockEntity?): Boolean {
        if (be is ChestBlockEntity || be is BarrelBlockEntity || be is ShulkerBoxBlockEntity)
                return true
        if (state.`is`(BlockTags.DOORS)) return true
        return false
    }

    /**
     * Determines if a BlockState has the potential to be a locked container or door. Used as a fast
     * pre-filter for high-performance loops (explosions/pistons).
     */
    fun mightBeLockable(state: BlockState): Boolean {
        return state.hasBlockEntity() || state.`is`(BlockTags.DOORS)
    }

    private fun getDoorLowerPos(state: BlockState, pos: BlockPos): BlockPos {
        if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF) &&
                        state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) ==
                                DoubleBlockHalf.UPPER
        ) {
            return pos.below()
        }
        return pos
    }

    /** Retrieves the Owner UUID String of the locked block. Empty if not locked. */
    fun getLockOwner(level: Level, pos: BlockPos): String {
        return getLockOwner(level, pos, level.getBlockState(pos), level.getBlockEntity(pos))
    }

    /** Optimized overload for when BlockState and BlockEntity are already known. */
    fun getLockOwner(level: Level, pos: BlockPos, state: BlockState, be: BlockEntity?): String {
        if (be != null &&
                        (be is ChestBlockEntity ||
                                be is BarrelBlockEntity ||
                                be is ShulkerBoxBlockEntity)
        ) {
            val lockData = be.getData(LockingAttachments.LOCK_DATA)
            return lockData.owner // Note: We don't check neighbors here for ultimate TPS
            // protection; lock/unlock logic ensures both halves are mirrored.
        }

        if (state.`is`(BlockTags.DOORS) && level is ServerLevel) {
            val dimData = DimensionLockData.get(level)
            val lowerPos = getDoorLowerPos(state, pos)
            return dimData.getLock(lowerPos).owner
        }

        return ""
    }

    fun isLocked(level: Level, pos: BlockPos): Boolean {
        return getLockOwner(level, pos).isNotEmpty()
    }

    fun isLocked(level: Level, pos: BlockPos, state: BlockState, be: BlockEntity?): Boolean {
        return getLockOwner(level, pos, state, be).isNotEmpty()
    }

    /** Applies a lock to a block and handles double block linking. */
    fun lockBlock(level: Level, pos: BlockPos, ownerUuid: String): Boolean {
        val state = level.getBlockState(pos)
        val be = level.getBlockEntity(pos)

        if (!isLockable(state, be)) return false

        if (be != null &&
                        (be is ChestBlockEntity ||
                                be is BarrelBlockEntity ||
                                be is ShulkerBoxBlockEntity)
        ) {
            applyBELock(be, ownerUuid)

            // Special handling for Double Chests
            if (be is ChestBlockEntity) {
                if (state.hasProperty(ChestBlock.TYPE)) {
                    val type = state.getValue(ChestBlock.TYPE)
                    if (type != ChestType.SINGLE) {
                        val side = ChestBlock.getConnectedDirection(state)
                        val otherPos = pos.relative(side)
                        val otherBe = level.getBlockEntity(otherPos)
                        if (otherBe is ChestBlockEntity) {
                            applyBELock(otherBe, ownerUuid)
                        }
                    }
                }
            }
            return true
        }

        if (state.`is`(BlockTags.DOORS) && level is ServerLevel) {
            val dimData = DimensionLockData.get(level)
            val lowerPos = getDoorLowerPos(state, pos)
            dimData.setLock(lowerPos, LockData(ownerUuid))
            return true
        }

        return false
    }

    /** Unlocks a block and handles double block linking. */
    fun unlockBlock(level: Level, pos: BlockPos): Boolean {
        val state = level.getBlockState(pos)
        val be = level.getBlockEntity(pos)

        // Basically same logic as lockBlock but with empty owner
        if (!isLockable(state, be)) return false

        if (be != null &&
                        (be is ChestBlockEntity ||
                                be is BarrelBlockEntity ||
                                be is ShulkerBoxBlockEntity)
        ) {
            applyBELock(be, "")

            if (be is ChestBlockEntity) {
                if (state.hasProperty(ChestBlock.TYPE)) {
                    val type = state.getValue(ChestBlock.TYPE)
                    if (type != ChestType.SINGLE) {
                        val side = ChestBlock.getConnectedDirection(state)
                        val otherPos = pos.relative(side)
                        val otherBe = level.getBlockEntity(otherPos)
                        if (otherBe is ChestBlockEntity) {
                            applyBELock(otherBe, "")
                        }
                    }
                }
            }
            return true
        }

        if (state.`is`(BlockTags.DOORS) && level is ServerLevel) {
            val dimData = DimensionLockData.get(level)
            val lowerPos = getDoorLowerPos(state, pos)
            dimData.setLock(lowerPos, LockData(""))
            return true
        }

        return false
    }

    private fun applyBELock(be: BlockEntity, ownerUuid: String) {
        val data = LockData(ownerUuid)
        be.setData(LockingAttachments.LOCK_DATA, data)
        be.setChanged()
    }

    /**
     * Optimized check to see if a player can access a block. Includes OP (permission level 2)
     * bypass logic.
     */
    fun canAccess(level: ServerLevel, pos: BlockPos, player: Player): Boolean {
        return canAccess(level, pos, level.getBlockState(pos), level.getBlockEntity(pos), player)
    }

    /** Maximum optimization overload. */
    fun canAccess(
            level: ServerLevel,
            pos: BlockPos,
            state: BlockState,
            be: BlockEntity?,
            player: Player
    ): Boolean {
        // OP bypass
        if (player.hasPermissions(2)) return true

        val owner = getLockOwner(level, pos, state, be)
        if (owner.isEmpty()) return true

        val trustData = TrustData.get(level)
        return trustData.isTrusted(owner, player.stringUUID)
    }
}
