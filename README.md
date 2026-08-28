<p align="center"><img src="https://github.com/Immersive-Geology-Team/Immersive-Geology/blob/forge-1.20.1/.github/images/logo.png?raw=true"></p>

<p align="center">
Immersive Geology is a mod in development for Minecraft 1.21.1 / NeoForge<br/>
The mod requires Immersive Engineering for the appropriate Minecraft Version<br/>
</p>
<hr>

[![NeoForge verification](https://github.com/MrMagnusneo/Immersive-Geology/actions/workflows/neoforge-1.21.1-port.yml/badge.svg?branch=codex%2Fneoforge-1.21.1-port)](https://github.com/MrMagnusneo/Immersive-Geology/actions/workflows/neoforge-1.21.1-port.yml)
<img src="https://img.shields.io/discord/610912351142674434?logo=discord&logoColor=white"
            alt="Chat on Discord"></a>
<a href="https://github.com/Immersive-Geology-Team/Immersive-Geology/pulse" alt="Activity">
        <img src="https://img.shields.io/github/commit-activity/m/Immersive-Geology-Team/Immersive-Geology/forge-1.20.1" /></a>
 
**This mod is in development and not all features have been implemented**

### NeoForge 1.21.1 port

The port is developed on `codex/neoforge-1.21.1-port` in [PR #1](https://github.com/MrMagnusneo/Immersive-Geology/pull/1), targeting `neoforge-1.21.1`. The Forge 1.20.1 branch is unchanged. Until that PR is merged, use the PR branch rather than the target branch to build this port.

| Component | Tested version |
| --- | --- |
| Minecraft | 1.21.1 |
| Java | 21 |
| NeoForge | 21.1.234 |
| Immersive Engineering (required) | 1.21.1-12.4.2-194 |
| JEI (optional in installed games) | 19.27.0.343 |
| DualCodecs (bundled) | 0.1.2 |

Install the gameplay `ImmersiveGeology-*.jar` alongside the matching Immersive Engineering version. Do not install the `-sources` or `-datagen` JARs as mods. Back up worlds before testing the port; cross-version world migration is not certified.

Build and validate from a Java 21 environment:

```sh
python3 -m unittest discover -s scripts/tests -v
./gradlew --no-daemon clean build gametestClasses
./gradlew --no-daemon runData
./gradlew --no-daemon runGameTestServer
```

CI additionally checks runtime logs, compares three independent gameplay JARs, and audits content against the immutable Forge baseline. `runData` without TFC removes optional TFC generated files from the working tree; the precise expected list is in `scripts/optional_tfc_generated_resources.txt`. Do not commit those removals. TFC/Ad Astra compatibility with the corresponding mods installed and interactive client behavior still require separate verification.

If you want to help us out, you can donate with Liberapay!<br>
<noscript><a href="https://liberapay.com/Immersive-Geology/donate"><img alt="Donate using Liberapay" src="https://liberapay.com/assets/widgets/donate.svg"></a></noscript>

**All IG asset files (textures, models) are ALL RIGHTS RESERVED**<br>
**Code files are under the GNU LESSER GENERAL PUBLIC LICENSE**

### Mod Authors and Contributors ###
> [CrimsonTwilight](https://www.curseforge.com/members/crimsontwilight): Team Immersive Geology Founder, Texture Artist and Developer<br/>
> [Muddykats](https://www.curseforge.com/members/muddykats): Lead Immersive Geology Developer <br/>
> [UnSchtalch](https://github.com/UnSchtalch): Developer and 3D Model Artist <br/>
> [Peter](https://github.com/Cordyceps22): Multiblock 3D-Modellierer und Künstler<br/>
> SteelBlue8: Item and Block Texture Artist<br/>
