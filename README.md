# Chill Zone Homes 0.3.2 Fix 8

Fix 8 focuses on reliable home access for every rank and both client editions.

- Adds LuckPerms node `chillzonehomes.command.home` to control `/home`.
- Hides the staff-only `/homes` command tree from players without `chillzonehomes.command.limit`.
- Java text entry uses the stable vanilla anvil field so non-OP/member players are not blocked by temporary sign editing.
- Bedrock text entry stays native through Floodgate/Cumulus, with a short delayed open and one retry to avoid menu-close races.
- Existing homes, limits, icons, teleports, and saved data are unchanged.

Recommended LuckPerms setup:

```
/lp group member permission set chillzonehomes.command.home true
/lp group member permission set chillzonehomes.command.limit false
/lp group mod permission set chillzonehomes.command.limit false
/lp group admin permission set chillzonehomes.command.limit false
/lp group owner permission set chillzonehomes.command.limit true
```
