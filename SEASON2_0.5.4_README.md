# Chill Zone Homes 0.5.4 — Season 2 source candidate

Based on the actual 0.5.3 Minecraft 26.3 source JAR supplied with `chill-zone-homes (31).zip`, and the GitHub project scaffolding from `build(9).zip`.

## Implemented
- Existing Homes 0.5.3 commands, storage, and interfaces retained.
- Players still accrue ordinary Homes playtime ticks, but new passive Shard awards require tracked activity.
- Idle threshold 5 minutes; sampling each second; unchanged position/holding against a wall does not refresh activity.
- Basic short repeated path/direction detection; this is heuristic, not an anti-cheat and needs live verification.
- Console-only `/homesseasonreset` backs up `config/chill-zone-shards.json` as `config/chill-zone-shards-pre-season-reset.bak.json`, resets all stored Shard and Homes playtime entries, writes marker `config/chill-zone-shards-season-reset-v1.marker` to prevent re-running, and refreshes player sidebars.

## Crucial limitations
- **NOT yet wired automatically to Moderation `/server start`.** Run `/homesseasonreset` from the CONSOLE after a full backup, once, immediately before the actual Season 2 `/server start`. This avoids erasing balances on routine launch tests. Do not delete marker without a planned next-season migration.
- Player Minecraft vanilla statistics/advancements are separate from Homes playtime: this command resets only Homes-managed playtime and Shards.
- Minecraft startup and full compilation NOT VERIFIED. Java 25 and Gradle dependency resolution required.
- The current activity heuristic may mistakenly reward some sophisticated macros and may withhold some legitimate activities. It never removes Shards already earned. Observe carefully and iterate.
- Keep only ONE `chillzonehomes` mod JAR; remove old one after backup.

## Build
Upload this project to the existing Homes GitHub repository (no separate mod). Trigger GitHub Actions. It uploads `chill-zone-homes-build-log` on build failure and normal JARs on success.
