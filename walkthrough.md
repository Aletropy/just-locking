# 🔒 Just Locking: Server-Side Container Security Walkthrough

Welcome to the technical overview of the **Just Locking** mod. This architecture has been designed from the ground up to provide **bulletproof, high-performance container and door security** specifically tailored for large-scale NeoForge 1.21.1 servers (100+ concurrent players). 

This document serves as a comprehensive guide to understanding the module's implementation, the extensive edge cases mitigated, and exactly how we achieved maximum security with minimal performance overhead.

---

## ⚡ Overview & Performance

Most locking plugins rely on expensive, constant entity ticking or distance checks which cripple Server TPS (Ticks Per Second) at scale. **Just Locking completely breaks from this legacy approach.**

*   **Zero-TPS Impact Strategy**: The system is entirely **event-driven**. There are absolutely no ticking entities (`TickingBlockEntity`) running in the background. The server only computes logic precisely when a player interacts with a block or an event triggers (like an explosion).
*   **Purely Server-Side**: This mod operates exclusively on the server, meaning clients do not need to download the mod to play on the server.
*   **Optimized Data Attachments**: Lock ownership is not saved into a bloated global database file that has to be queried constantly. Instead, it utilizes native Data Attachments (`LockingAttachments.LOCK_DATA`) directly on the `BlockEntity` or chunk dimensions, ensuring [O(1)](file:///home/gabriel/Projects/just-locking/src/main/kotlin/com/aletropy/justlocking/data/LockDataManager.kt#51-55) memory lookup times.
*   **Pre-filtering Optimization**: For intensive server events like explosions or piston movements involving hundreds of blocks, the mod utilizes a highly optimized [mightBeLockable()](file:///home/gabriel/Projects/just-locking/src/main/kotlin/com/aletropy/justlocking/data/LockDataManager.kt#33-40) pre-filter that checks standard BlockStates before attempting expensive `BlockEntity` reflection lookups.

---

## 🛠️ Core Usage & Commands

The plugin relies on an intuitive command structure mapped to the Forge Command Dispatcher. 

*   **`/lock`**: By looking at a supported container or door and executing this command, ownership is instantaneously bound to the player.
*   **`/unlock`**: Removes the lock from the targeted block, opening it back up to the public.
*   **`/lock trust <player>`**: Grants another player global access to open, break, and interact with your locked containers.
*   **`/lock untrust <player>`**: Revokes a player's access.

> [!TIP] 
> **Why we use UUIDs instead of Usernames:** 
> All ownership and trust data is strictly bound to the player's unique `UUID`. If a player changes their Minecraft username via Mojang, their locks and trusted friend lists remain perfectly intact. String-based username locks are highly susceptible to spoofing and data loss; UUIDs guarantee absolute cryptographic ownership.

---

## 🛡️ The 'Bulletproof' Edge Cases

A locking mod is only as good as its weakest exploit. In a high-population server environment, players will attempt every conceivable strategy to bypass container security. Here is exactly what we protected against at the lowest capability levels:

### 1. Deep Capability Blocking (Automated Extraction)
We didn't just "block players from opening the chest" or intercept vanilla `HopperBlock` ticks. **We intercepted the core `IItemHandler` capabilities deeply at the NeoForge framework level ([LockingCapabilities.kt](file:///home/gabriel/Projects/just-locking/src/main/kotlin/com/aletropy/justlocking/LockingCapabilities.kt)).**
*   When a block is locked, the mod dynamically replaces its capability provider with a "dummy" `IItemHandler` that simulates a 0-slot inventory.
*   **What this stops:** Vanilla hoppers beneath the chest, Hopper Minecarts running past the chest, and crucially, **every single modded pipe, fluid router, or import/export bus**. The modded tech ecosystem literally cannot see the items inside the locked container.

### 2. Explosion & Griefing Immunity
Tossing TNT next to a locked container to burst its contents is a classic bypass.
*   **TNT & Creepers**: The `ExplosionProtectionHandler` intercepts the `ExplosionEvent.Detonate` phase. It maps over the explosion's damage path and cleanly removes any locked blocks from the destruction list before the server processes the blast. The landscape will crater, but the locked chest will float flawlessly in the air.
*   **Boss Griefing**: Through the `EntityDestructionProtectionHandler`, we cancel the `LivingDestroyBlockEvent`. This means the `Wither` and the `Ender Dragon` cannot bulldoze through locked containers during their physical charge paths.

### 3. Block Updates, Pistons, & Breaking
Players often try to break the block *underneath* the chest to force it to drop as an item, or use pistons to push it out of protected territory.
*   **Player Breaking**: `PlayerBreakHandler` ensures a player cannot mine the locked container unless they own it (or have OP Level 2 permissions).
*   **Piston Crushing**: The `PistonProtectionHandler` calculates the entire multi-block structure a Piston intends to push. If *any* block in that trajectory is locked, the entire piston extension event is hard-canceled to prevent structure displacement.
*   Gravity and support updates will not cause a block entity to drop its contents if locked.

### 4. Smart Double Chest Sync
Double chests are notorious for causing lock desyncs (locking the left half, but the right half remains vulnerable).
*   **Automatic Merging**: If a player places a chest next to a chest they already locked, the `BlockPlaceHandler` detects the `ChestType` merge and automatically inherits the exact UUID lock onto the new half. 
*   **Placement Prevention**: If a malicious player attempts to place a generic chest adjacent to *your* locked single chest to artificially create a double chest and gain access, the event is entirely canceled, issuing them a prompt warning.

### 5. Trapped Chests, Barrels, & Shulkers
The architecture is inherently modular. Protection natively encompasses `BlockEntityType.CHEST`, `BARREL`, `TRAPPED_CHEST`, `SHULKER_BOX`, and `BlockTags.DOORS`. Because we anchor data via capabilities and block states rather than hardcoded item IDs, any modded chest that extends standard BlockEntities inherently benefits from this robust security wrapper.

---

*Designed and engineered for maximum stability. Your players' stashes are secure.*
