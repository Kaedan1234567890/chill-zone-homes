# Fix 1 — standalone build

This must be uploaded as its OWN GitHub repository/project, not copied into the existing chill-zone-homes source repository.

It contains the existing working Homes JAR in `libs/` only so Gradle can reference it without compiling Homes, Geyser, Floodgate, Vanish or LuckPerms source.

Command:
`/resetplayer <stored name>`

The command works on offline stored records, provides stored-name suggestions, resets shards/playtime to zero, and creates a backup of the JSON before editing.

Do not remove the existing Homes JAR from the Minecraft server. The built Reset Player JAR is installed beside it.

Compile status is not claimed until GitHub Actions is green.
