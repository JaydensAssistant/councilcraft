#!/usr/bin/env bash
# Build CouncilCraft release artifacts for <version> into dist/<version>/ (or $OUT_DIR).
#
# Steps (in order; any failure exits non-zero):
#   1. packwiz refresh
#   2. packwiz modrinth export -> CouncilCraft-<ver>-no-cc.mrpack
#   3. ./gradlew build; copy jar; inject into CouncilCraft-<ver>.mrpack at overrides/mods/
#   4. tools/servers_dat.py on BOTH mrpacks (PT20)
#   5. tools/servers_dat.py --verify on BOTH (fails if client-overrides/servers.dat missing/wrong)
#
# Usage:
#   tools/build_release.sh <version>
#   OUT_DIR=/tmp/sdtest tools/build_release.sh <fake-version>   # dry-run; does not write dist/
#
# <version> should match gradle.properties mod_version for a real release. When OUT_DIR is set,
# a different label is allowed so a dry-run can use a fake version string without bumping the repo.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

if [[ $# -ne 1 ]]; then
  echo "usage: $0 <version>" >&2
  echo "  OUT_DIR=/path  optional; default dist/<version>" >&2
  exit 2
fi

VER="$1"
MOD_VER="$(sed -n 's/^mod_version=//p' gradle.properties | head -n1)"
if [[ -z "$MOD_VER" ]]; then
  echo "error: could not read mod_version from gradle.properties" >&2
  exit 1
fi

OUT="${OUT_DIR:-$ROOT/dist/$VER}"
if [[ -z "${OUT_DIR:-}" && "$VER" != "$MOD_VER" ]]; then
  echo "error: version '$VER' does not match gradle.properties mod_version '$MOD_VER'" >&2
  echo "       (set OUT_DIR to allow a dry-run with a different label)" >&2
  exit 1
fi

export PATH="${HOME}/go/bin:/usr/local/go/bin:${PATH}"
if ! command -v packwiz >/dev/null 2>&1; then
  echo "error: packwiz not found on PATH" >&2
  exit 1
fi

mkdir -p "$OUT"
NOCC="$OUT/CouncilCraft-${VER}-no-cc.mrpack"
FULL="$OUT/CouncilCraft-${VER}.mrpack"
JAR_NAME="councilcraft-${MOD_VER}.jar"
JAR_SRC="$ROOT/build/libs/${JAR_NAME}"

echo "==> packwiz refresh"
( cd "$ROOT/pack" && packwiz refresh )

echo "==> packwiz modrinth export -> $NOCC"
( cd "$ROOT/pack" && packwiz modrinth export -o "$NOCC" )

echo "==> ./gradlew build"
./gradlew build --no-daemon

if [[ ! -f "$JAR_SRC" ]]; then
  echo "error: expected jar missing: $JAR_SRC" >&2
  exit 1
fi
cp -f "$JAR_SRC" "$OUT/$JAR_NAME"

echo "==> inject jar into $FULL"
cp -f "$NOCC" "$FULL"
python3 - "$FULL" "$JAR_SRC" "overrides/mods/${JAR_NAME}" <<'PY'
import sys, zipfile
pack, jar, entry = sys.argv[1], sys.argv[2], sys.argv[3]
with open(jar, "rb") as f:
    data = f.read()
# Rewrite zip so a prior overrides/mods/councilcraft-*.jar is replaced cleanly.
import os, tempfile
fd, tmp = tempfile.mkstemp(suffix=".mrpack", dir=os.path.dirname(os.path.abspath(pack)))
os.close(fd)
with zipfile.ZipFile(pack) as src, zipfile.ZipFile(tmp, "w", zipfile.ZIP_DEFLATED) as dst:
    for info in src.infolist():
        if info.filename == entry:
            continue
        dst.writestr(info, src.read(info.filename))
    dst.writestr(entry, data)
os.replace(tmp, pack)
print(pack, "+", entry, len(data), "bytes")
PY

echo "==> PT20 servers.dat on both mrpacks"
python3 "$ROOT/tools/servers_dat.py" "$NOCC" "$FULL"

echo "==> verify PT20 servers.dat"
python3 "$ROOT/tools/servers_dat.py" --verify "$NOCC" "$FULL"

echo "OK: $OUT"
ls -la "$NOCC" "$FULL" "$OUT/$JAR_NAME"
