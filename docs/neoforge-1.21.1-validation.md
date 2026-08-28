# NeoForge 1.21.1 port validation

## Scope and platform

Follow-up to PR #1, based on `18e773cdbe6fb0ce57a796966a9e4bd9cb8d0550`.
The Forge baseline remains `cfa6707983bbdac2bb5c2a740d2bff8942b1cb28`.
Changes belong to `codex/neoforge-1.21.1-port`; this work does not merge the PR or alter `forge-1.20.1`.

Minecraft 1.21.1, Java 21, NeoForge 21.1.234, Immersive Engineering 1.21.1-12.4.2-194, JEI 19.27.0.343, bundled DualCodecs 0.1.2. Gameplay build: `0.8.3-b4650-alpha`.

## Defects in the previous validation

The previous successful [CI run](https://github.com/MrMagnusneo/Immersive-Geology/actions/runs/32761276679) was not sufficient evidence of a complete port. Its logs contained:

- Failed mineral recipe decoding due to legacy Forge conditions and tags.
- Missing biome tags for ruined structures.
- Missing skin configuration during multiblock formation/reset.
- Failed generation of chemical/centrifuge recipes with empty optional fluid outputs.

Twelve ordinary processing recipes were incorrectly included in the optional-TFC deletion exception. A successful Gradle exit and passing formation tests did not detect these losses. The follow-up makes runtime log validation and gameplay JAR inspection explicit gates.

## Verification procedure

1. Run the Python contract/regression suite.
2. Compare source content with the immutable Forge checkout, allowing documented identifier/path replacements and migrated resource contents.
3. Run `clean build gametestClasses` independently on three CI workers.
4. Inspect each gameplay JAR, then require identical SHA-256 hashes across all three builds.
5. Run `runData`, reject logged recipe errors, and compare generated resources. Only actual optional TFC data may disappear when TFC is absent.
6. Start `runGameTestServer`, exercise multiblock formation/skins and recipe output checks, and reject logged runtime/data errors even when the test runner exits successfully.

The local environment cannot download Gradle because of its network restrictions. Full Java builds and runtime checks therefore run in GitHub Actions; local Python checks and source/content review are separate evidence.

## Follow-up checkpoint

[Run 33094248000](https://github.com/MrMagnusneo/Immersive-Geology/actions/runs/33094248000) built three byte-identical gameplay JARs, completed data generation without logged errors, and passed 19 required server GameTests. The new log gate nevertheless rejected three malformed mineral condition IDs. This is a failed checkpoint, not a release validation result.

Its generated-resource snapshot restored the twelve ordinary processing recipes: empty optional fluid outputs are encoded as `{}`. Those files were synchronized without deleting optional TFC resources from source. Subsequent fixes correct the condition IDs and preserve native deferred item-tag outputs across recipe construction.

The current PR description records the final commit/run evidence and artifact hash. Use the checks attached to that exact commit; this checkpoint does not validate later changes.

## Remaining interactive checks

Automated server checks do not certify these behaviors. Before treating this alpha as a production release, verify:

- Client startup, creative inventory subgroups, models/textures, GUI rendering, JEI and the IE manual.
- Processing, sided item/fluid/energy IO, save/reload and chunk reload for each machine family.
- A real client connecting to a dedicated server and network interactions.
- Optional integrations with matching TFC/Ad Astra versions installed.
- Existing-world upgrades from Forge 1.20.1 using a backed-up copy.

No successful manual execution of these checks is claimed by this report.
