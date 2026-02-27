package com.aletropy.justlocking.data

import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.StringTag
import net.minecraft.nbt.Tag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.saveddata.SavedData

/**
 * Manages global trust relationships between players. This is stored in the overworld's data
 * storage to ensure persistent global trust regardless of which dimension the player is in.
 *
 * Performance Note:1 This uses a Map of Sets for O(1) trust lookups. Since trusted lists are
 * typically small, memory overhead is minimal.
 */
class TrustData : SavedData() {
    private val ownerToTrusted = mutableMapOf<String, MutableSet<String>>()

    /**
     * Adds a player to another player's trusted list.
     *
     * @param owner The UUID string of the owner.
     * @param trusted The UUID string of the player to trust.
     * @return True if the player was newly added, false if already trusted.
     */
    fun trustPlayer(owner: String, trusted: String): Boolean {
        val trustedSet = ownerToTrusted.getOrPut(owner) { mutableSetOf() }
        if (trustedSet.add(trusted)) {
            setDirty()
            return true
        }
        return false
    }

    /**
     * Removes a player from another player's trusted list.
     *
     * @param owner The UUID string of the owner.
     * @param untrusted The UUID string of the player to untrust.
     * @return True if the player was removed, false if not in the list.
     */
    fun untrustPlayer(owner: String, untrusted: String): Boolean {
        val trustedSet = ownerToTrusted[owner]
        if (trustedSet != null && trustedSet.remove(untrusted)) {
            if (trustedSet.isEmpty()) {
                ownerToTrusted.remove(owner)
            }
            setDirty()
            return true
        }
        return false
    }

    /**
     * Checks if a player is trusted by an owner.
     *
     * @param owner The UUID string of the block owner.
     * @param player The UUID string of the player attempting access.
     * @return True if the player is the owner or explicitly trusted.
     */
    fun isTrusted(owner: String, player: String): Boolean {
        if (owner == player) return true
        return ownerToTrusted[owner]?.contains(player) == true
    }

    override fun save(
            tag: CompoundTag,
            provider: net.minecraft.core.HolderLookup.Provider
    ): CompoundTag {
        val ownersList = ListTag()
        for ((owner, trustedSet) in ownerToTrusted) {
            val ownerTag = CompoundTag()
            ownerTag.putString("owner", owner)

            val trustedList = ListTag()
            for (trusted in trustedSet) {
                trustedList.add(StringTag.valueOf(trusted))
            }
            ownerTag.put("trusted", trustedList)

            ownersList.add(ownerTag)
        }
        tag.put("trust_data", ownersList)
        return tag
    }

    companion object {
        private const val DATA_ID = "justlocking_trust"

        /** Loads TrustData from NBT. */
        fun load(tag: CompoundTag, provider: net.minecraft.core.HolderLookup.Provider): TrustData {
            val data = TrustData()
            if (tag.contains("trust_data", Tag.TAG_LIST.toInt())) {
                val ownersList = tag.getList("trust_data", Tag.TAG_COMPOUND.toInt())
                for (i in 0 until ownersList.size) {
                    val ownerTag = ownersList.getCompound(i)
                    val owner = ownerTag.getString("owner")
                    val trustedList = ownerTag.getList("trusted", Tag.TAG_STRING.toInt())

                    val trustedSet = mutableSetOf<String>()
                    for (j in 0 until trustedList.size) {
                        trustedSet.add(trustedList.getString(j))
                    }
                    data.ownerToTrusted[owner] = trustedSet
                }
            }
            return data
        }

        /**
         * Accesses the global TrustData instance from the level.
         *
         * @param level The server level (usually overworld).
         * @return The singleton-like TrustData instance for this world.
         */
        fun get(level: ServerLevel): TrustData {
            val dataStorage = level.server.overworld().dataStorage
            return dataStorage.computeIfAbsent(
                    SavedData.Factory({ TrustData() }, { tag, provider -> load(tag, provider) }),
                    DATA_ID
            )
        }
    }
}
