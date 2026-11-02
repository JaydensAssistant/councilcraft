#!/usr/bin/env python3
"""CC-10A: inject client-only Distant Horizons CPU Load = Aggressive into .mrpack files.

DistantHorizons.toml in pack/config is exported to overrides/ (client AND server). Setting
threadPresetSetting=AGGRESSIVE there would make DH's ThreadPresetConfigEventHandler rewrite
common.multiThreading.numberOfThreads to ~75% of cores on the dedicated server too (verified
in DH 3.3.3 ThreadPresetConfigEventHandler / AbstractPresetConfigEventHandler).

This script copies overrides/config/DistantHorizons.toml into
client-overrides/config/DistantHorizons.toml with threadPresetSetting = "AGGRESSIVE" under
[client]. Modrinth client-overrides are client-only, so servers keep overrides/ with
numberOfThreads = 4 unchanged.

Usage:
  cc10a_dh_client_preset.py PACK.mrpack [...]
  cc10a_dh_client_preset.py --verify PACK.mrpack [...]
"""
from __future__ import annotations

import os
import re
import sys
import tempfile
import zipfile

SRC = "overrides/config/DistantHorizons.toml"
ENTRY = "client-overrides/config/DistantHorizons.toml"
PRESET_LINE = 'threadPresetSetting = "AGGRESSIVE"'
COMMENT = (
    "\t#\n"
    "\t# CC-10A: CPU Load preset Aggressive (EDhApiThreadPreset.AGGRESSIVE). Client-only "
    "copy — see tools/cc10a_dh_client_preset.py.\n"
    "\t# DH applies linked numberOfThreads / threadRunTimeRatio from this preset at runtime.\n"
)


def with_aggressive(toml: str) -> str:
    if PRESET_LINE in toml and "CC-10A" in toml:
        return toml
    # Strip a prior injection so re-runs stay idempotent.
    toml = re.sub(
        r"\n\t#\n\t# CC-10A:.*?threadPresetSetting = \"AGGRESSIVE\"\n",
        "\n",
        toml,
        count=1,
        flags=re.S,
    )
    toml = re.sub(r"\n\tthreadPresetSetting = \"[^\"]+\"\n", "\n", toml)
    marker = "[client]\n"
    idx = toml.find(marker)
    if idx < 0:
        raise SystemExit("DistantHorizons.toml missing [client] section")
    insert_at = idx + len(marker)
    return toml[:insert_at] + COMMENT + "\t" + PRESET_LINE + "\n" + toml[insert_at:]


def inject(path: str) -> None:
    with zipfile.ZipFile(path) as src:
        try:
            raw = src.read(SRC).decode("utf-8")
        except KeyError as e:
            raise SystemExit(f"{path}: missing {SRC}") from e
        body = with_aggressive(raw).encode("utf-8")
        fd, tmp = tempfile.mkstemp(suffix=".mrpack", dir=os.path.dirname(os.path.abspath(path)))
        os.close(fd)
        with zipfile.ZipFile(tmp, "w", zipfile.ZIP_DEFLATED) as dst:
            for info in src.infolist():
                if info.filename == ENTRY:
                    continue
                dst.writestr(info, src.read(info.filename))
            dst.writestr(ENTRY, body)
    os.replace(tmp, path)
    print(path, "+", ENTRY, len(body), "bytes")


def verify(path: str) -> bool:
    with zipfile.ZipFile(path) as z:
        try:
            client = z.read(ENTRY).decode("utf-8")
        except KeyError:
            print(f"{path}: missing {ENTRY}", file=sys.stderr)
            return False
        try:
            shared = z.read(SRC).decode("utf-8")
        except KeyError:
            print(f"{path}: missing {SRC}", file=sys.stderr)
            return False
    ok = True
    if PRESET_LINE not in client:
        print(f"{path}: {ENTRY} missing {PRESET_LINE}", file=sys.stderr)
        ok = False
    if PRESET_LINE in shared:
        print(
            f"{path}: {SRC} must NOT set AGGRESSIVE (server shares this file)",
            file=sys.stderr,
        )
        ok = False
    if 'numberOfThreads = 4' not in shared:
        print(f"{path}: {SRC} expected numberOfThreads = 4 for server", file=sys.stderr)
        ok = False
    if ok:
        print(f"{path}: OK {ENTRY} (AGGRESSIVE) ; {SRC} keeps numberOfThreads=4")
    return ok


if __name__ == "__main__":
    args = sys.argv[1:]
    if not args:
        print(__doc__.strip(), file=sys.stderr)
        sys.exit(2)
    do_verify = args[0] == "--verify"
    paths = args[1:] if do_verify else args
    if not paths:
        print(__doc__.strip(), file=sys.stderr)
        sys.exit(2)
    if do_verify:
        sys.exit(0 if all(verify(p) for p in paths) else 1)
    for p in paths:
        inject(p)
