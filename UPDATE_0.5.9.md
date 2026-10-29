# Chill Zone Homes 0.5.9 - Repeatable /server start reset

- Each `/server start` (from existing Moderation 0.3.15) resets every saved Shard balance and Homes-tracked playtime to zero, including offline entries.
- The previous one-time marker is intentionally ignored. It is left on disk for historical purposes.
- Each run makes a unique timestamp-independent backup `config/chill-zone-shards-pre-server-start-*.bak.json` first. Failure to create a backup aborts.
- The existing public method `resetFromServerStart(MinecraftServer)` is preserved so Moderation 0.3.15 still works unchanged.
- Resets the activity gate and refreshes online sidebars after reset. Existing home slots, saved homes, Shop functionality, and shard purchase limits are not reset.
- `/server preparing` and the one-hour grace timer remain controlled by Moderation and are unchanged.
- **Warning**: Each `/server start` deliberately erases earned Shards/playtime again, even if used multiple times in the same day. Back up the server before use.
- Source ZIP only; Minecraft/Fabric build and gameplay tests still required.
