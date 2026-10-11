# Valkery

Valkery is a Forge-first starter equipment mod for Minecraft 1.20.1. This repository currently builds one loader target: Forge. The common gameplay classes use the Minecraft 1.20.1 API and are separated from Forge registration and event handling so other loader modules can be added later.

## Included

- A selectable Valkyrie origin in Origins' standard origin layer.
- The origin grants Icarus' white feathered wings on selection and automatically equips them in the back slot. Players can choose Icarus' feathered, dragon, mechanical, and light wings in any of 16 colors, or one of its unique styles, using `/valkery wings set <type> <color>` or `/valkery wings unique <style>`. The selected wings stay equipped and are restored if removed.
- Flight consumes a configurable, reduced hunger cost (default 0.025 exhaustion/second) and requires at least 6 food points (three hunger icons). Initiates fly for 60 seconds, then recover for 30 seconds on the ground; heavy hits (6 post-mitigation damage) briefly disrupt flight. Forced descents keep refreshing Slow Falling until landing.
- Flight mastery is earned only while actually moving during equipped-wing flight, persists across sessions and death, and displays with `/valkery mastery`. Windborne (600 XP) extends endurance to 90 seconds, cuts hunger use by 25%, and reduces recovery to 24 seconds; Skywarden (1,800 XP) extends endurance to 120 seconds, cuts hunger use by 45%, and reduces recovery to 18 seconds. Flight values and XP thresholds are configurable.
- Valkyries have four extra hearts, deal 25% more damage with any bow, and deal 25% less melee damage. Valkyrie bow draws and shots add divine chime tones. Fully charged shots from any bow have a configurable, provisional 10% chance to trigger Divine Smite: a stronger arrow that pierces up to three targets and loses 35% damage after each hit.
- A Valkery advancement tab tracks first flight, completing an endurance cycle, Divine Smite, the spear's lightning, and reaching Skywarden.
- Any bow can be used; arrows are still required and are consumed normally.
- The origin also grants Origins' aerial-launch power.
- The Spear of the Oath has 5 total attack damage, a slower attack speed, and a 16-block aimed lightning strike on right-click with a 30-second cooldown. The strike adds attributed lightning, electric sparks, end-rod glints, thunder audio, and in-game ability/cooldown tooltips.
- The Spear of the Oath appears in the Combat creative tab.

The selected wings are automatically equipped into Icarus' Curios back slot and restored if removed. Slow Falling is applied only during a Valkyrie safe descent, not as a permanent origin power. The removed Valkyrie armor set is not part of this version. Custom Valkery wing rendering, aerial dash, and a separate temporary divine blessing are deferred. Minecraft 1.20.1 does not expose the later vanilla player interaction-range attribute, so this starter does not claim to add spear reach.

**Origins is optional; Icarus and Curios are required.** Valkery loads without Origins, but the Valkyrie Origin selection, its starting wings, four-heart bonus, and Origins-provided aerial powers are available only when Origins is installed. Without Origins, obtain Icarus wings separately; Valkery's wing, flight, bow, spear, and mastery mechanics activate for players with Icarus wings. Forge checks for Icarus mod ID `icarus` version `2.11.0` or later and Curios mod ID `curios` version `5.2.0` or later at load time.

[Origins `1.10.2+mc.1.20.1`](https://modrinth.com/mod/origins/version/S03WNOkQ) is listed for Minecraft 1.20.1 and Forge, but is a Fabric Origins release running through Sinytra Connector rather than a native Forge API build. Its published metadata requires Connector, Connector Extras, and Forgified Fabric API; install compatible 1.20.1 Forge builds of that dependency chain if you want the Origin. [Icarus `2.14.1`](https://modrinth.com/mod/icarus/version/RXhgOzWN) is published for Forge and NeoForge on Minecraft 1.20.1; it requires Common Network, Resourceful Config, Caelus API, and Curios API (Figura is optional). Valkery uses Curios' Forge API to keep Icarus wings equipped in its back slot.

The Forge common config is generated at `config\valkery-common.toml`. It contains flight hunger/endurance/recovery and Divine Smite tuning. Bow hits grant persistent Bow XP; `/valkery bow` shows progress and `/valkery bow upgrade` spends XP on Divine Smite levels. Each upgrade increases Smite chance and damage. The default Smite chance is provisional and should be adjusted after multiplayer gameplay testing.

## Developing Valkery

See [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) for the codebase map, how to add content, configuration and progression details, testing guidance, and release packaging notes.

## Build

Install Java 17, then run in PowerShell:

```powershell
.\gradlew.bat build
```

The Forge jar is written to `build\libs\valkery-0.1.7.jar`. To launch the development client:

```powershell
.\gradlew.bat runClient
```

The project pins Minecraft 1.20.1, Forge 47.4.26, ForgeGradle 6.0.54, official Mojang mappings, and Gradle 8.8.

## Download

Players can download the current **beta** Forge jar from the [Valkery GitHub Releases](https://github.com/Nyxcore-dev/Valkery/releases) page. Choose the `valkery-0.1.7.jar` asset for Minecraft 1.20.1 and Forge; install Icarus and Curios as required dependencies. This is a beta build, not a full/stable release. Origins is only needed for the Valkyrie Origin selection and its Origin-provided powers.

## Test in game

With Origins installed, choose Valkyrie from the standard Origin selection screen when creating a world; without Origins, obtain Icarus wings separately. Wings equip automatically in Icarus' back slot and are restored if removed. Choose a color and type with `/valkery wings set dragon purple`, or a unique style with `/valkery wings unique flandres`; `/valkery wings` reports the current selection and usage. Double-tap jump to fly. Slow Falling is granted only during protected descents. Craft or find any bow and arrows; Valkyrie bow damage and Divine Smite apply to any bow. Successful bow hits grant bow XP; use `/valkery bow` to view the skill and `/valkery bow upgrade` to buy a Divine Smite upgrade. Check flight rank and progress with `/valkery mastery`. For balancing, test hunger use, the 60-second Initiate fatigue, grounded recovery, low-food cutoff, and heavy-hit safe descent. Flight XP should only accrue while moving. At higher flight ranks, verify endurance, hunger efficiency, and recovery changes. Use fully charged bow shots against multiple targets to test Divine Smite damage falloff, and test its configured chance over many shots. Verify all five advancements and the spear lightning/cooldown in-game.

Build validation checks Java compilation, JSON syntax, advancement/power links, and jar packaging. Gameplay, multiplayer synchronization, hunger pacing, and proc balance still require an actual Minecraft 1.20.1 instance with Forge and Icarus installed; Origins and its Connector dependencies are needed only to test the Origin selection and powers.

Forge is the only implemented loader target. Fabric and NeoForge registration, loader-specific testing, and shared packaging remain future work.
