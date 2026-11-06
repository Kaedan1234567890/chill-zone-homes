# Homes 0.5.14 — Bedrock Form button compile fix

Fixes `FormImage is not a functional interface` errors in `BedrockHomesMenu.java` from 0.5.13.

The API in the build environment does not accept `form.button(label, response -> ...)`. This version uses `button(label)` and one `validResultHandler` per form, dispatching through `clickedButtonId()`.

Preserves the Java Homes GUI, existing save storage, Geyser/Floodgate Bedrock menu, creation, teleporting, renaming, deletion, icon changes, and home-slot unlocking.

Status: source fix prepared and statically inspected; not compiled with full Fabric 26.3 / JDK 25 dependencies or tested on a Bedrock client. Run your normal GitHub Actions build before deployment.
