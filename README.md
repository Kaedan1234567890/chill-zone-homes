# Chill Zone Homes — 0.2.0 Alpha

Custom server-side Fabric `/home` system for Minecraft 26.2, inspired by the interaction flow shown in the DonutSMP reference video.

## Planned/implemented behavior

- `/home` opens a vanilla chest GUI.
- Saved homes are clickable.
- Empty unlocked slots show **New Home**.
- Slots above the player's allowance show **Locked**.
- **Show More** pages through additional visible slots.
- Clicking a saved home opens **Teleport / Change Icon / Rename / Delete / Back**.
- Homes are stored by player UUID in the world folder.
- Default home allowance comes from `config/chill-zone-homes.json`.
- Optional per-player/per-group LuckPerms override uses meta key `homes-max`.
- Cross-dimension homes are supported.
- Server-side only: players do not install the mod client-side.

## Default limit

The generated config defaults to:

```json
{
  "defaultHomes": 3,
  "maximumVisibleSlots": 18,
  "luckPermsMetaKey": "homes-max",
  "showLockedSlots": true,
  "allowCrossDimensionTeleport": true
}
```

A LuckPerms override can later be assigned with:

```text
/lp user PLAYERNAME meta set homes-max 5
```

or to a group:

```text
/lp group booster meta set homes-max 5
```

Removing the meta value returns the player to the configured default.

## Build target

- Minecraft 26.2
- Fabric Loader 0.19.3
- Fabric API 0.154.2+26.2
- Fabric Loom 1.17-SNAPSHOT
- Java 25

A GitHub Actions workflow is included at `.github/workflows/build.yml`. It provisions Java 25 and Gradle 9.7 automatically and uploads the compiled server JAR as a workflow artifact.

## Important

This is still an **alpha source build** until the GitHub Actions build succeeds and the resulting JAR is tested on a Minecraft 26.2 Fabric server. Do not upload the source ZIP to Shockbyte.
