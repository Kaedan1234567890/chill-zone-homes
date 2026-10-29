# /baltop LuckPerms gate

This patch adds `.requires(LuckPermsPermissions::canUseBaltop)` to the existing `/baltop` command. All other Homes and Shards functions are intended to remain unchanged.

Default: players without `chillzonehomes.command.baltop` cannot see or execute `/baltop`. Console allowed.

After installing the compiled 0.5.10 JAR and ensuring the previous Homes JAR is removed:

- To block **everyone** (including Owner through group inheritance), remove any existing individual grants to the node and run in console:
  `lp group member permission set chillzonehomes.command.baltop false`
  `lp group mod permission set chillzonehomes.command.baltop false`
  `lp group admin permission set chillzonehomes.command.baltop false`
  `lp group owner permission set chillzonehomes.command.baltop false`
- To bring it back later for everyone, set `true` on `member` and remove explicit `false` nodes on mod/admin/owner.

This is *source only*. Not compiled or game-tested. `/homes` remains separately registered and unaffected.
