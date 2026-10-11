# Valkery development guide

This guide documents the current Minecraft 1.20.1 Forge implementation and the patterns to follow when extending it. Valkery is an actively evolving prototype; verify mechanics in a game before treating balance or compatibility as final.

## Development environment

- Minecraft 1.20.1
- Minecraft Forge 47.4.26
- Java 17
- Gradle 8.8 through the checked-in wrapper
- ForgeGradle 6.0.54
- Mojang official mappings

Use the wrapper rather than a globally installed Gradle:

```powershell
.\gradlew.bat build --console=plain
```

The distributable is `build\libs\valkery-<version>.jar`. `runClient` launches the Forge development client:

```powershell
.\gradlew.bat runClient
```

Useful checks:

```powershell
.\gradlew.bat clean build --console=plain
.\gradlew.bat test --console=plain
```

There are currently no automated Java tests. A successful build checks Java compilation, resource processing, and jar remapping; it does not prove that a feature behaves correctly in a live world or multiplayer.

## Project layout

| Path | Responsibility |
|---|---|
| `src/main/java/dev/valkery/common/` | Shared balance constants and player progression storage/logic. This is a package boundary, not yet a separately built loader-neutral module. |
| `src/main/java/dev/valkery/common/item/` | Item behavior that is currently implemented against the Minecraft API. |
| `src/main/java/dev/valkery/forge/` | Forge mod entry point, config, commands, item registration, and advancement helpers. |
| `src/main/java/dev/valkery/forge/event/` | Forge event subscribers for gameplay, player lifecycle, and creative tabs. |
| `src/main/resources/META-INF/mods.toml` | Forge identity, version expansion, Minecraft/Forge range, and runtime dependency declarations. |
| `src/main/resources/assets/valkery/` | Item models/textures and English names, messages, and tooltips. |
| `src/main/resources/data/valkery/advancements/` | Advancement JSON tree. |
| `src/main/resources/data/valkery/origins/` and `powers/` | Optional Origins-compatible Origin and power definitions. |
| `src/main/resources/data/origins/origin_layers/` | Adds the Valkyrie choice to Origins' standard origin layer. |
| `src/main/resources/pack.mcmeta` | Minecraft data/resource pack format metadata. |
| `build.gradle`, `gradle.properties`, `settings.gradle` | Build plugins, dependencies, target versions, and mod version. |

Forge is the only implemented loader. The `common` package is a place to keep reusable decisions and state logic, but it still compiles within this Forge project and some classes directly use Minecraft APIs. A real multi-loader release will need separate loader projects/adapters and loader-specific validation.

## Existing gameplay and where it lives

- `ValkeryItems` registers `spear_of_the_oath`; its item model and texture use the matching `assets/valkery/models/item/` and `textures/item/` paths.
- `ValkyrieSpearItem` implements the lightning activation. `ValkyrieBalance` holds its attack modifiers, range, and cooldown.
- `ValkeryGameplayEvents` handles Icarus wing recognition/equipping, flight endurance and safe descents, flight hunger, flight mastery awards, bow damage and Divine Smite, and related advancement triggers.
- `ValkyrieFlightMastery` and `ValkyrieBowMastery` store progression in player persistent NBT. `ValkeryPlayerEvents` copies progression when a player entity is replaced.
- `ValkeryConfig` defines server common-config settings in the `flight` and `archery` groups. Its TOML is generated under the instance's `config` directory on first launch.
- `ValkeryCommands` defines `/valkery mastery`, `/valkery bow`, `/valkery bow upgrade`, and wing style/status commands.
- `ValkeryAdvancements` awards advancement criteria from the real gameplay event paths; the matching JSON files describe the tree.
- All player-facing names and messages belong in `assets/valkery/lang/en_us.json`; refer to them from Java with translation keys instead of hard-coded UI strings.

The Origin definition references Origins/Apoli power types and the Origin layer resource uses Origins' namespace. Origins is optional in `mods.toml`: without it, Forge can load Valkery's standalone mod features, but there is no Origin selection, Origin-applied four-heart bonus, Origins starting-wing grant, or Origins aerial powers. Icarus and Curios are still mandatory for wing integration. The documented Forge Origins release uses Sinytra Connector and its required compatibility libraries; this path should be tested with the exact modpack before claiming it works in-game.

## Adding a weapon

For a straightforward new weapon, use the spear as the reference implementation:

1. Add behavior in `common/item/` if it can be expressed with the Minecraft API and does not import Forge-only classes. Put tunable shared values in `ValkyrieBalance`; put settings intended for pack owners in `ValkeryConfig`.
2. Register the item in `forge/item/ValkeryItems.java` with `DeferredRegister` and a stable registry path such as `oath_blade`. Keep the registry ID lowercase and do not change it after publishing; changing an ID can invalidate saved item stacks.
3. If it has an activated ability, implement server-authoritative behavior in the item or a Forge event handler. Validate target/range/cooldown server-side, and send sounds, particles, and messages through the appropriate level/player APIs. Avoid triggering world-changing effects only on the client.
4. Add its inventory icon under `assets/valkery/textures/item/<id>.png`, the model at `assets/valkery/models/item/<id>.json`, and localized display/tooltip keys in `assets/valkery/lang/en_us.json`.
5. Add it to an appropriate creative tab through `ValkeryCreativeTabEvents` if it should be obtainable in Creative.
6. If it unlocks an advancement, add a criterion to a JSON advancement and call the helper from the successful server-side action, not merely from an attempted use.
7. Build, inspect the jar, and test in a Forge 1.20.1 client and dedicated server. Check multiplayer cooldowns, ownership/attribution, persistence, and edge cases such as unloaded targets.

When adding armor, item registration is only one part: plan armor attributes/material, equipment slot behavior, item textures, and an actual armor model/rendering strategy. Do not assume vanilla item models render a custom worn armor appearance.

## Configuration and balance

The current configurable values are in `ValkeryConfig`:

- `flight.exhaustionPerSecond`, `minimumFoodLevel`
- `initiateFlightSeconds`, `windborneFlightSeconds`, `skywardenFlightSeconds`
- corresponding `*RecoverySeconds`
- `windborneHungerMultiplier`, `skywardenHungerMultiplier`
- `windborneExperience`, `skywardenExperience`
- `archery.divineSmiteChance`, `divineSmiteDamageMultiplier`, `divineSmitePierceTargets`, `divineSmiteDamageFalloff`

Other mechanics live in `ValkyrieBalance`: attack damage multipliers, heavy-hit interruption threshold and duration, movement threshold for flight XP, and lightning spear range/cooldown. The bow mastery level cap and XP costs are currently constants in `ValkyrieBowMastery`. When adding a balance value, choose config for values modpack authors should tune and a constant for implementation invariants; document the unit and expected range.

Treat player persistent-data key names as save data. Keep them stable after release. If a data format must change, include a migration/fallback path, and ensure player clone/death handling copies the intended fields. Do not assume client-side state is authoritative for rewards or combat.

## Assets, IDs, and data resources

Resource paths are identifiers. For registry item `valkery:oath_blade`, the typical item model path is `assets/valkery/models/item/oath_blade.json`; the model usually points to `valkery:item/oath_blade`. Translation keys must match the registered item ID, for example `item.valkery.oath_blade`.

Advancement JSON IDs are file paths beneath `data/valkery/advancements/`; keep parent IDs and runtime criterion names in sync with `ValkeryAdvancements`. Origins resources are consumed by Origins/Apoli, not by Forge's Java registration system. Avoid adding Origins API classes to mandatory Forge code when keeping Origins optional.

## Dependency and loader policy

Dependencies used directly by Java must be on the build classpath and correctly declared in `mods.toml`. Runtime-only integrations should be isolated so an optional mod is not referenced by class signatures or eagerly loaded classes when absent. Icarus and Curios are currently direct required runtime dependencies. Origins is optional but only its Origin-specific content depends on it.

Do not label the mod as Fabric- or NeoForge-compatible based on shared-looking code or a dependency's loader support. Each loader needs its own metadata, registration/event bridge, dependency versions, build, and runtime tests. Check published dependency versions for Minecraft 1.20.1 and the exact loader before updating ranges or documentation.

## Release and CurseForge checklist

1. Update `mod_version` in `gradle.properties` using the project's version sequence and ensure README artifact examples are current.
2. Run `.\gradlew.bat clean build --console=plain` with Java 17.
3. Test the built jar in a clean Minecraft 1.20.1 Forge instance with Icarus and Curios, then separately test Origins plus the required Connector dependency set. Test a dedicated server if gameplay changes affect server state.
4. Publish a GitHub Release with a version tag such as `v<version>` and attach only `build\libs\valkery-<version>.jar`. Mark alpha/beta builds as **Pre-release** and label their title and notes clearly; do not present a test build as stable. For example, after committing and pushing the release source, `gh release create v0.1.7 build\libs\valkery-0.1.7.jar --title "Valkery 0.1.7 Beta" --prerelease --notes "Beta build for Minecraft 1.20.1 Forge; not a stable release."`. Do not attach `*-sources.jar`, development runs, `.gradle`, or the whole `build` directory.
5. Upload the same jar to the matching CurseForge project file release if distributing there. Set Minecraft and Forge metadata correctly. Declare required Icarus/Curios dependencies and explain that Origins is optional for non-Origin gameplay but needed for the Valkyrie Origin choice.
6. Include release notes, known limitations, and the tested game/loader/dependency versions. A successful Gradle build is not an in-game compatibility test.
