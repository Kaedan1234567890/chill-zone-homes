# Owner-only home inspection — source candidate

Command: `/homes goto <player> <home>`; suggestions use the same saved-player and saved-home registries as `/homes remove`. Allows offline players already in that registry. Enforces LuckPerms primary group `owner` (fail-closed). Switches viewer to Spectator before teleporting directly to home coordinates, logs inspection. Does not alter target home data or their inventory. Existing `/homes remove` unchanged.

Important: Spectator still loads nearby chunks; it does not guarantee zero chunk loads. Use an existing staff return command or manually restore your original position and gamemode afterwards. No automatic return was added. Uncompiled/not tested in game; back up server data first.
