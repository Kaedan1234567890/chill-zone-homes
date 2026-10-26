# Chill Zone Homes 0.5.5 (source candidate)

## Changes
- /homesseasonreset now accepts server console or players with LuckPerms node `chillzonehomes.seasonreset`. The command will not be visible to normal players without the node.
- Give ONLY the Owner group: `/lp group owner permission set chillzonehomes.seasonreset true`.
- The reset is still once-only, creates a copy of the Shards JSON, and resets recorded Shards and Homes-tracked playtime for online/offline records. Do not run on real data before the new season.
- Anti-AFK activity gate no longer awards Shards to a freshly connected, totally stationary player merely because their first five-minute payout happens before timeout. Requires actual movement before first reward. Standing still for five minutes subsequently stops rewards.
- Does not change normal playtime counting or homes. Does not modify Moderation `/server start`.

Build and test in a temporary test-server copy first. This ZIP is source code, not a compiled/tested JAR. Do not install old and new Homes JAR simultaneously.

Note: a permission node granted to the Owner group is necessary but insufficient if other group negations or permissions conflict; test with `/lp user <player> permission check chillzonehomes.seasonreset`.
