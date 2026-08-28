#!/usr/bin/env python3
"""Reject silent data/runtime failures even when Gradle exits successfully."""

import argparse
import re
import sys
from pathlib import Path


FAILURE = re.compile(
    r"\[(?:[^\]\n]*/)?(?:ERROR|FATAL)\]"
    r"|Not all defined tags for registry"
    r"|Failed to build Recipe Method"
    r"|Failed Recipe for"
)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--kind", choices=("datagen", "gametest"), required=True)
    parser.add_argument("log", type=Path)
    args = parser.parse_args()
    try:
        log = args.log.read_text(encoding="utf-8")
    except (OSError, UnicodeError) as error:
        print(f"Cannot read runtime log: {error}", file=sys.stderr)
        return 1

    failures = [line for line in log.splitlines() if FAILURE.search(line)]
    if "BUILD SUCCESSFUL" not in log:
        failures.append("Missing successful Gradle completion")
    if args.kind == "gametest":
        if "Started game test server" not in log:
            failures.append("Missing GameTest server startup")
        passed = re.search(r"All ([1-9][0-9]*) required tests passed", log)
        if not passed:
            failures.append("Missing successful nonempty GameTest result")

    if failures:
        print("Runtime validation failed:\n" + "\n".join(failures), file=sys.stderr)
        return 1
    print(f"Validated {args.kind} log: no data or runtime errors")
    return 0


if __name__ == "__main__":
    sys.exit(main())
