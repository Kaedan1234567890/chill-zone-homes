# Chill Zone Homes 0.4.0-alpha-shards

Minecraft 26.2 Fabric server-side homes + Shards update.

## Shards
- 1 Shard per full minute online (AFK time counts).
- Balance persists in `config/chill-zone-shards.json`.
- Clean per-player sidebar: player name + Shards.

## Home progression
- Homes 1-3 are free/default.
- Home 4 costs 100 Shards.
- Every next home costs 50 more, through Home 28 (1,300 Shards).
- Click the next locked barrier in `/home` to buy it permanently.
- Staff `/homes limit <player> <amount>` remains a free administrative override and now accepts 1-28.
- Existing home data is preserved.

## Fix 2 build cleanup
- Restores the MIT `LICENSE` file from the known-good Homes source.
- Explicitly excludes the unrelated `com/chillzone/vanish/**` package if an old copy is still sitting in the GitHub repository.
- This prevents stale Vanish source files from breaking the Homes build.

## Shard admin commands
Permission: `chillzonehomes.command.shardsadmin`

- `/shards give <player> <amount>`
- `/shards set <player> <amount>`
- `/shards take <player> <amount>`
- `/shards balance <player>`

These commands are intended for Owner/Admin testing and management. The target player must be online.

## Fix 4 — staff home-limit override
- `/homes limit <player> <amount>` now always overrides shard-purchased limits, even when the staff limit is lower.
- Example: if a player purchased 8 homes and staff sets `/homes limit Player 4`, the player is limited to 4 homes.
- `/homes limit <player> reset` removes the staff override, allowing the player's normal shard-purchased limit to apply again.
- No existing homes, shard balances, or purchased-home progress are deleted.
