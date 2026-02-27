# Just Locking 🔒

A high-performance, strictly server-side block protection mod for **NeoForge 1.21.1**. 

Designed as a modern, heavily optimized alternative to legacy plugins like Lockette, **Just Locking** provides bulletproof chest and barrel protection without sacrificing server MSPT (Milliseconds Per Tick). Built specifically to handle large player counts (100+ concurrent users) without event-listener lag.

## ✨ Key Features

* **Exploit-Proof Architecture:** Unlike basic locking mods, Just Locking intercepts deep block-update and capability events to prevent all known bypasses:
  * **Hopper & Minecart Extraction:** Blocks items from being pulled out from underneath or pushed into locked containers via hoppers or hopper minecarts.
  * **Explosion Resistance:** Locked blocks dynamically remove themselves from the affected block lists of Creepers, TNT, Ghasts, and other explosions.
  * **Piston Displacement:** Prevents pistons from pushing locked blocks outside of their secure locations.
  * **Support Block Breaking:** Prevents players from bypassing the lock by breaking the block the chest is resting upon.
* **UUID-Based Trust System:** Uses modern UUIDs instead of raw strings. If a trusted player changes their Minecraft username, they won't lose access.
* **Zero-Tick Overhead:** Event listeners are heavily optimized to ensure 0 impact on server TPS, even under heavy load.
* **Server-Side Only:** Clients do not need to install this mod to join your server.

## 🛠️ Commands & Usage

Players can easily secure their containers and manage access.

* `/lock` - Locks the target block you are looking at.
* `/lock trust <player>` - Grants access to a specific player for all your locked containers.
* `/lock untrust <player>` - Revokes access from a specific player.
* `/lock info` - Displays the owner and trusted members of the target container.

*(Note: Server administrators with operator permissions automatically bypass locks for moderation purposes).*

## 📥 Installation

Since this is a server-side mod, simply drop the `.jar` file into your server's `mods` folder. 
* **Modloader:** NeoForge 1.21.1
* **Client Requirement:** None. Vanilla clients can connect flawlessly.

## 📜 License

This project is licensed under the [MIT License](LICENSE).