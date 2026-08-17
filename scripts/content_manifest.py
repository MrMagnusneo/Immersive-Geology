#!/usr/bin/env python3
"""Capture and compare the production surface of Immersive Geology.

The port is expected to edit many Java and JSON files.  Consequently the
preservation check treats paths and registry identifiers as the compatibility
contract, while retaining hashes for strict same-branch/reproducibility checks.
"""

import argparse
from collections import Counter
import hashlib
import json
import re
import sys
from pathlib import Path


LIST_CATEGORIES = (
    "materials",
    "blocks",
    "items",
    "fluids",
    "fluid_types",
    "block_entities",
    "entities",
    "menus",
    "particles",
    "recipe_types",
    "recipe_serializers",
    "multiblocks",
    "sounds",
    "creative_tabs",
    "loot_entries",
    "biome_modifiers",
    "features",
    "placements",
    "structures",
    "config_keys",
    "network_messages",
    "integrations",
    "resource_paths",
)
HASH_CATEGORIES = ("java_sources", "resource_hashes")
DUPLICATE_REGISTRATION_CATEGORIES = (
    "blocks", "items", "fluids", "fluid_types", "block_entities", "entities",
    "menus", "particles", "recipe_types", "recipe_serializers", "multiblocks",
    "creative_tabs", "loot_entries",
)


class ManifestError(RuntimeError):
    pass


def _sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def _matches(pattern: str, text: str) -> list[str]:
    return re.findall(pattern, text, flags=re.MULTILINE | re.DOTALL)


def _enum_materials(path: Path, text: str) -> list[str]:
    if not path.name.endswith("Enum.java") or "implements MaterialInterface" not in text:
        return []
    declaration = re.search(r"\benum\s+(\w+)\b[^\{]*\{(.*?);", text, re.DOTALL)
    if not declaration:
        return []
    enum_name, body = declaration.groups()
    constants = re.findall(r"(?:^|,)\s*([A-Za-z][A-Za-z0-9_]*)\s*\(\s*new\s+Material", body)
    return [f"{enum_name}.{constant}" for constant in constants]


def collect_manifest(root: Path) -> dict:
    root = Path(root)
    values = {category: set() for category in LIST_CATEGORIES}
    explicit = {category: [] for category in LIST_CATEGORIES}
    java_sources: dict[str, str] = {}
    resource_hashes: dict[str, str] = {}
    resource_occurrences: list[str] = []
    java_root = root / "src/main/java"

    def record(category: str, identifiers: list[str]) -> None:
        explicit[category].extend(identifiers)
        values[category].update(identifiers)

    for path in sorted(java_root.rglob("*.java")) if java_root.exists() else []:
        text = path.read_text(encoding="utf-8")
        relative = path.relative_to(java_root).as_posix()
        java_sources[path.relative_to(root).as_posix()] = _sha256(path)

        record("materials", _enum_materials(path, text))
        record("items", _matches(r"\bregisterItem\s*\(\s*\"([a-z0-9_]+)\"", text))
        record("blocks", _matches(r"\bregisterBlock(?:AndItem)?\s*\(\s*\"([a-z0-9_]+)\"", text))
        record("fluids", _matches(r"\bregisterFluid\s*\(\s*\"([a-z0-9_]+)\"", text))
        record("fluid_types", _matches(r"\bregisterFluidType\s*\(\s*\"([a-z0-9_]+)\"", text))
        record("multiblocks", _matches(r"\bregisterMultiblock\s*\(\s*\"([a-z0-9_]+)\"", text))

        if path.name == "IGRegistrationHolder.java":
            record("block_entities", _matches(r"\bTE_REGISTER\.register\s*\(\s*\"([a-z0-9_]+)\"", text))
            record("creative_tabs", _matches(r"\bTAB_REGISTER\.register\s*\(\s*\"([a-z0-9_]+)\"", text))
            record("loot_entries", _matches(r"\bLOOT_SERIALIZER_REGISTER\.register\s*\(\s*\"([a-z0-9_]+)\"", text))
        elif path.name == "IGRecipeTypes.java":
            record("recipe_types", _matches(r"=\s*register\s*\(\s*\"([a-z0-9_]+)\"", text))
        elif path.name == "IGRecipeSerializers.java":
            record("recipe_serializers", _matches(r"RECIPE_SERIALIZERS\.register\s*\(\s*\"([a-z0-9_]+)\"", text))
        elif path.name == "IGParticles.java":
            record("particles", _matches(r"PARTICLES\.register\s*\(\s*\"([a-z0-9_]+)\"", text))
        elif path.name == "IGMenuTypes.java":
            record("menus", _matches(r"register(?:Simple|Multiblock|Arg)?\s*\(\s*\"([a-z0-9_]+)\"", text))
        elif path.name == "IGLib.java":
            record("menus", _matches(r"GUIID_\w+\s*=\s*\"([a-z0-9_]+)\"", text))
        elif path.name == "IGMultiblockProvider.java":
            record("multiblocks", _matches(r"(?:registerMetalMultiblock|metal_skinnable|stone_skinnable|stone|mirroredStone)\s*\([^;]*?\"([a-z0-9_]+)\"", text))

        if path.name.endswith("Config.java"):
            record("config_keys", _matches(r"\.(?:define|defineInRange|defineEnum|defineListAllowEmpty)\s*\(\s*\"([A-Za-z0-9_.-]+)\"", text))
        if "/common/network/" in f"/{relative}" and "/msg/" in f"/{relative}":
            values["network_messages"].add(relative.removesuffix(".java"))
        if "/common/integration/" in f"/{relative}":
            values["integrations"].add(relative.removesuffix(".java"))
        if "/common/world/structure/" in f"/{relative}" and path.name != "package-info.java":
            values["structures"].add(path.stem)
        if "/common/world/features/" in f"/{relative}":
            values["features"].add(path.stem)
        if "/common/world/placements/" in f"/{relative}":
            values["placements"].add(path.stem)
        if "/common/world/modifiers/" in f"/{relative}":
            values["biome_modifiers"].add(path.stem)

    duplicates = []
    for category in DUPLICATE_REGISTRATION_CATEGORIES:
        duplicates.extend(
            f"{category}:{identifier}"
            for identifier, count in Counter(explicit[category]).items()
            if count > 1
        )
    if duplicates:
        raise ManifestError("Duplicate explicit registrations: " + ", ".join(sorted(duplicates)))

    for source_root in (root / "src/main/resources", root / "src/generated/resources"):
        if not source_root.exists():
            continue
        for path in sorted(source_root.rglob("*")):
            relative = path.relative_to(source_root)
            if not path.is_file() or ".cache" in relative.parts:
                continue
            jar_path = relative.as_posix()
            resource_occurrences.append(jar_path)
            values["resource_paths"].add(jar_path)
            resource_hashes[jar_path] = _sha256(path)

            blockstate_prefix = "assets/immersivegeology/blockstates/"
            item_model_prefix = "assets/immersivegeology/models/item/"
            fluid_model_prefix = "assets/immersivegeology/models/block/fluid/"
            if jar_path.startswith(blockstate_prefix) and jar_path.endswith(".json"):
                values["blocks"].add(jar_path[len(blockstate_prefix):-5])
            if jar_path.startswith(item_model_prefix) and jar_path.endswith(".json"):
                values["items"].add(jar_path[len(item_model_prefix):-5])
            if jar_path.startswith(fluid_model_prefix) and jar_path.endswith(".json"):
                fluid_id = jar_path[len(fluid_model_prefix):-5]
                values["fluids"].add(fluid_id)
                values["fluid_types"].add(fluid_id)

    duplicate_resources = sorted(
        path for path, count in Counter(resource_occurrences).items() if count > 1
    )
    if duplicate_resources:
        raise ManifestError("Duplicate resource paths: " + ", ".join(duplicate_resources))

    manifest = {category: sorted(values[category]) for category in LIST_CATEGORIES}
    manifest["java_sources"] = dict(sorted(java_sources.items()))
    manifest["resource_hashes"] = dict(sorted(resource_hashes.items()))
    return manifest


def compare_manifests(
    baseline: dict,
    current: dict,
    allow_content_changes: bool = False,
    replacements: dict | None = None,
) -> dict:
    replacements = replacements or {}
    differences = {}
    for category in sorted(set(baseline) | set(current)):
        before = baseline.get(category, {})
        after = current.get(category, {})
        if isinstance(before, dict) or isinstance(after, dict):
            before = before if isinstance(before, dict) else {}
            after = after if isinstance(after, dict) else {}
            category_replacements = replacements.get(category, {})
            removed = sorted(
                key for key in set(before) - set(after)
                if category_replacements.get(key) not in after
            )
            added = sorted(set(after) - set(before))
            changed = sorted(key for key in set(before) & set(after) if before[key] != after[key])
            result = {}
            if removed:
                result["removed"] = removed
            if added and not allow_content_changes:
                result["added"] = added
            if changed and not allow_content_changes:
                result["changed"] = changed
            if result:
                differences[category] = result
        else:
            category_replacements = replacements.get(category, {})
            removed = sorted(
                key for key in set(before) - set(after)
                if category_replacements.get(key) not in after
            )
            added = sorted(set(after) - set(before))
            result = {}
            if removed:
                result["removed"] = removed
            if added and not allow_content_changes:
                result["added"] = added
            if result:
                differences[category] = result
    return differences


def main() -> int:
    parser = argparse.ArgumentParser(description="Generate or verify the Immersive Geology content manifest")
    mode = parser.add_mutually_exclusive_group(required=True)
    mode.add_argument("--write", type=Path, metavar="PATH")
    mode.add_argument("--check", type=Path, metavar="PATH")
    mode.add_argument("--compare-root", type=Path, metavar="BASELINE_ROOT")
    parser.add_argument("--allow-content-changes", action="store_true")
    parser.add_argument("--replacements", type=Path, metavar="PATH")
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[1])
    args = parser.parse_args()

    try:
        current = collect_manifest(args.root)
    except ManifestError as error:
        print(error, file=sys.stderr)
        return 1

    target = args.write or args.check
    if args.write:
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(json.dumps(current, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
        return 0

    if args.compare_root:
        baseline = collect_manifest(args.compare_root)
    else:
        baseline = json.loads(target.read_text(encoding="utf-8"))
    replacements = {}
    if args.replacements:
        replacements = json.loads(args.replacements.read_text(encoding="utf-8"))
    differences = compare_manifests(
        baseline,
        current,
        args.allow_content_changes,
        replacements,
    )
    if differences:
        print(json.dumps(differences, indent=2, ensure_ascii=False), file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
