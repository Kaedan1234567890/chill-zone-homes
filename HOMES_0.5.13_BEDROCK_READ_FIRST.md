# Chill Zone Homes 0.5.13 — Bedrock Homes menu

Based on the user-supplied BALTOP_LAYOUT_FIX_NOTES(2).zip / Homes 0.5.12 source.

Changes:
- `/home` opens a **Floodgate/Cumulus SimpleForm** menu for Bedrock players, not a Java-only 54-slot chest interface.
- Bedrock menu supports creating, viewing, teleporting, renaming, deleting (with confirmation), choosing common home icons, and buying the next home slot with Shards.
- Existing sign/anvil-based Java player home UI stays unchanged.
- Bedrock uses the previously established native text-entry manager for naming/renaming.
- Preserves home JSON storage, purchases, permission checks, and teleport implementation. Native form handlers are routed onto server thread, with a short send delay to avoid Geyser close/open conflicts.
- Bedrock menus require working Geyser AND Floodgate server installation. Players need existing `chillzonehomes.command.home` permission.

Test `/home` with a Bedrock player (new home, list, teleport, rename, delete, unlock) and Java player (no behavior change). Back up `chill_zone_homes.json` and Shards data before deploying.

Source-only delivery, not an installable JAR. The local environment has Java 21 and no project Gradle toolchain/dependency cache, so full Fabric 26.3 compilation was NOT performed. Build in the normal GitHub Actions Java 25 workflow and do not replace live mod until tested.
