# Chill Zone Homes

Server-side Fabric homes mod for Chill Zone SMP.

## 0.3.0 alpha

- `/home` opens the 6x9 Chill Zone Homes GUI.
- `/home <home name>` teleports directly to a saved home.
- `/home ` autocomplete suggests the player's saved home names.
- Exact approved 6x9 layout with glass border, barrier-locked slots, and centred book.
- 3 homes by default; up to 24 accessible through the configured/LuckPerms limit.
- Existing 0.2.0 home data remains compatible.
- Home management: Teleport, Change Icon, Rename, Delete, Back.
- Change Icon opens a 6x9 multi-page picker containing Minecraft items/blocks.
- Search sign in the icon picker filters icons by name; clear search restores the full list.
- Rename/search text entry is configured to cost 0 XP.
- `/homes limit <player> <amount>` provides a shorter owner command and writes the same LuckPerms `homes-max` meta through LuckPerms' command.

## LuckPerms

The mod reads the meta key `homes-max`.

Examples:

```text
/lp user PlayerName meta set homes-max 5
/lp user PlayerName meta unset homes-max
```

The shorter Chill Zone command is:

```text
/homes limit PlayerName 5
```

## Data

Homes are saved in the world root as `chill_zone_homes.json`.
Configuration is stored in `config/chill-zone-homes.json`.

## Requirements

- Minecraft 26.2
- Fabric Loader 0.19+
- Fabric API
- Java 25
- LuckPerms recommended for rank/player limits
