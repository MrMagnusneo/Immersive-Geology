#!/usr/bin/env python3
"""Validate that a packaged NeoForge gameplay JAR contains its runtime contract."""

import argparse
import json
import struct
import sys
import tomllib
import zipfile
from pathlib import Path


ENTRYPOINT = "com/igteam/immersivegeology/ImmersiveGeology.class"
NEOFORGE_METADATA = "META-INF/neoforge.mods.toml"
FORGE_METADATA = "META-INF/mods.toml"
JAVA_21_CLASS_VERSION = 65


def source_resource_paths(roots):
    paths = set()
    for root in roots:
        if not root.is_dir():
            raise ValueError(f"Source resource root does not exist: {root}")
        for path in root.rglob("*"):
            if path.is_file() and ".cache" not in path.relative_to(root).parts:
                paths.add(path.relative_to(root).as_posix())
    return paths


def validate_metadata(contents, errors):
    try:
        metadata = contents[NEOFORGE_METADATA].decode("utf-8")
    except UnicodeDecodeError as error:
        errors.append(f"Cannot decode {NEOFORGE_METADATA}: {error}")
        return
    if "${" in metadata:
        errors.append(f"Found unexpanded placeholder in {NEOFORGE_METADATA}")
        return
    try:
        parsed = tomllib.loads(metadata)
    except tomllib.TOMLDecodeError as error:
        errors.append(f"Invalid {NEOFORGE_METADATA}: {error}")
        return
    if parsed.get("modLoader") != "javafml":
        errors.append(f"{NEOFORGE_METADATA} must declare modLoader=\"javafml\"")
    mods = parsed.get("mods", [])
    if not isinstance(mods, list) or not any(
        isinstance(mod, dict) and mod.get("modId") == "immersivegeology" for mod in mods
    ):
        errors.append(f"{NEOFORGE_METADATA} must declare modId=\"immersivegeology\"")


def validate_entrypoint(contents, errors):
    classfile = contents.get(ENTRYPOINT)
    if classfile is None:
        errors.append(f"Missing gameplay entrypoint: {ENTRYPOINT}")
        return
    if len(classfile) < 8 or classfile[:4] != b"\xca\xfe\xba\xbe":
        errors.append(f"Gameplay entrypoint has invalid class header: {ENTRYPOINT}")
        return
    _, major = struct.unpack(">HH", classfile[4:8])
    if major != JAVA_21_CLASS_VERSION:
        errors.append(
            f"Gameplay entrypoint must target Java 21 (class version {JAVA_21_CLASS_VERSION}), found {major}"
        )


def validate_archive(jar, source_roots):
    errors = []
    name = jar.name.lower()
    if name.endswith("-sources.jar"):
        errors.append("sources JAR cannot be validated as a gameplay JAR")
    if name.endswith("-datagen.jar"):
        errors.append("datagen JAR cannot be validated as a gameplay JAR")
    try:
        with zipfile.ZipFile(jar) as archive:
            names = {info.filename for info in archive.infolist() if not info.is_dir()}
            contents = {name: archive.read(name) for name in names}
    except (OSError, zipfile.BadZipFile) as error:
        return [f"Cannot read gameplay JAR: {error}"]

    if FORGE_METADATA in names:
        errors.append(f"Forge mods.toml is not allowed in a NeoForge gameplay JAR: {FORGE_METADATA}")
    if NEOFORGE_METADATA not in names:
        errors.append(f"Missing NeoForge metadata: {NEOFORGE_METADATA}")
    else:
        validate_metadata(contents, errors)

    for path in sorted(source_resource_paths(source_roots) - names):
        errors.append(f"Missing resource: {path}")

    for path in sorted(name for name in names if name.endswith((".json", ".mcmeta"))):
        try:
            json.loads(contents[path].decode("utf-8"))
        except (UnicodeDecodeError, json.JSONDecodeError) as error:
            errors.append(f"invalid JSON in {path}: {error}")

    validate_entrypoint(contents, errors)
    return errors


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("jar", type=Path)
    parser.add_argument("--source-root", type=Path, action="append", required=True)
    args = parser.parse_args()
    try:
        errors = validate_archive(args.jar, args.source_root)
    except ValueError as error:
        errors = [str(error)]
    if errors:
        print("Gameplay JAR validation failed:\n" + "\n".join(errors), file=sys.stderr)
        return 1
    print(f"Validated gameplay JAR: {args.jar}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
