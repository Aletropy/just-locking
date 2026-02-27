package com.aletropy.justlocking.data

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.saveddata.SavedData

/**
 * Stores lock data for blocks that do not have their own BlockEntity (e.g. Doors). Persists
 * directly into the world's 'data' folder per dimension.
 */
class DimensionLockData : SavedData() {
    private val blockLocks = mutableMapOf<BlockPos, LockData>()

    fun getLock(pos: BlockPos): LockData {
        return blockLocks[pos] ?: LockData("")
    }

    fun setLock(pos: BlockPos, lockData: LockData) {
        if (lockData.owner.isEmpty()) {
            blockLocks.remove(pos)
        } else {
            blockLocks[pos] = lockData
        }
        setDirty()
    }

    override fun save(
            tag: CompoundTag,
            provider: net.minecraft.core.HolderLookup.Provider
    ): CompoundTag {
        val list = ListTag()
        for ((pos, data) in blockLocks) {
            val entry = CompoundTag()
            entry.putLong("pos", pos.asLong())
            entry.putString("owner", data.owner)
            list.add(entry)
        }
        tag.put("locks", list)
        return tag
    }

    companion object {
        private const val DATA_ID = "justlocking_dimension_locks"

        fun load(
                tag: CompoundTag,
                provider: net.minecraft.core.HolderLookup.Provider
        ): DimensionLockData {
            val data = DimensionLockData()
            if (tag.contains("locks", Tag.TAG_LIST.toInt())) {
                val list = tag.getList("locks", Tag.TAG_COMPOUND.toInt())
                for (i in 0 until list.size) {
                    val entry = list.getCompound(i)
                    val pos = BlockPos.of(entry.getLong("pos"))
                    val owner = entry.getString("owner")
                    data.blockLocks[pos] = LockData(owner)
                }
            }
            return data
        }

        fun get(level: ServerLevel): DimensionLockData {
            return level.dataStorage.computeIfAbsent(
                    SavedData.Factory(
                            { DimensionLockData() },
                            { tag, provider -> load(tag, provider) }
                    ),
                    DATA_ID
            )
        }
    }
}
