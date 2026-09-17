# Chill Zone Reset Player Addon

Server-side addon for the existing Chill Zone Homes 0.4.0-alpha-shards-fix16-playtime-tracking mod.

Command:
`/resetplayer <stored-player>`

- Works on offline stored players.
- Autocompletes names currently represented in `/baltop`.
- Sets Shards to 0.
- Sets tracked playtime to 0.
- The existing `rankedBalances()` logic excludes records where both values are zero, so the reset player disappears from `/baltop`.
- Requires Minecraft gamemaster/admin command permission.
- Does not change `/shardshop`.

This is an addon because only the compiled Homes JAR was available. Keep the existing Homes JAR installed alongside the built addon JAR.

Compile status: not claimed until GitHub Actions is green.
