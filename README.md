# Chill Zone Homes 0.3.2 Alpha Fix 7

Fix 7 separates Java and Bedrock text input so a Bedrock workaround can no longer break the working Java sign editor.

- Java: restored exactly to the known-good Fix 4 sign flow.
- Bedrock/Floodgate: uses a native Bedrock CustomForm input field through Floodgate/Cumulus.
- Create Home, Rename Home, and Icon Search all route through the same cross-play input manager.
- Bedrock no longer depends on Geyser translating Java's Open Sign Editor packet.
- Closing/cancelling returns an empty result safely.
- Text is capped at 32 characters.
- Existing homes, permissions, icons, limits, and stored data are unchanged.

This build has a compile-only Floodgate API dependency; Floodgate remains provided by the server at runtime.
