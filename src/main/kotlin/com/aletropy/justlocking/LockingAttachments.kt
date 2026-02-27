package com.aletropy.justlocking

import com.aletropy.justlocking.data.LockData
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import java.util.function.Supplier
import net.neoforged.neoforge.attachment.AttachmentType
import net.neoforged.neoforge.registries.DeferredRegister
import net.neoforged.neoforge.registries.NeoForgeRegistries

/**
 * Handles registration of NeoForge Data Attachments. Data Attachments allow us to persist metadata
 * (like lock ownership) directly on BlockEntities.
 */
object LockingAttachments {
    /** Registry for attachment types associated with this mod. */
    val ATTACHMENT_TYPES: DeferredRegister<AttachmentType<*>> =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, JustLocking.MOD_ID)

    /**
     * Codec for serializing/deserializing [LockData] to NBT. RecordCodecBuilder ensures a stable
     * and efficient conversion process.
     */
    private val LOCK_DATA_CODEC: Codec<LockData> =
            RecordCodecBuilder.create { instance ->
                instance.group(Codec.STRING.fieldOf("owner").forGetter { it.owner }).apply(
                                instance
                        ) { owner -> LockData(owner) }
            }

    /**
     * The specific attachment type for locking data. Configured to serialize and have a default
     * empty state.
     */
    val LOCK_DATA =
            ATTACHMENT_TYPES.register(
                    "lock_data",
                    Supplier {
                        AttachmentType.builder(Supplier { LockData() })
                                .serialize(LOCK_DATA_CODEC)
                                .build()
                    }
            )
}
