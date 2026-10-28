#!/usr/bin/env python3
"""PT20: adds client-overrides/servers.dat (one server: councilcraft / councilcraft.modrinth.gg) to .mrpack files.

servers.dat is the vanilla client's multiplayer list: uncompressed NBT, root compound with a "servers" list of
compounds {name, ip}. client-overrides/ is installed on clients only (mrpack format), so servers never get it.
The file is written here rather than kept in pack/ because packwiz exports pack/ files to overrides/ (both sides).
Every pack update replaces the player's list with this one entry (accepted in the SPEC).

Usage:
  servers_dat.py PACK.mrpack [...]            rewrite each pack in place (idempotent)
  servers_dat.py --verify PACK.mrpack [...]   exit 0 iff each pack has the expected entry; else exit 1
"""
import os
import struct
import sys
import tempfile
import zipfile

NAME = "councilcraft"
ADDRESS = "councilcraft.modrinth.gg"
ENTRY = "client-overrides/servers.dat"


def _str(s):
    b = s.encode("utf-8")
    return struct.pack(">H", len(b)) + b


def servers_dat(servers):
    out = b"\x0a" + _str("")                                   # TAG_Compound ""
    out += b"\x09" + _str("servers") + b"\x0a" + struct.pack(">i", len(servers))  # TAG_List of compounds
    for name, ip in servers:
        out += b"\x08" + _str("name") + _str(name)
        out += b"\x08" + _str("ip") + _str(ip)
        out += b"\x00"
    return out + b"\x00"


def add(path, data):
    fd, tmp = tempfile.mkstemp(suffix=".mrpack", dir=os.path.dirname(os.path.abspath(path)))
    os.close(fd)
    with zipfile.ZipFile(path) as src, zipfile.ZipFile(tmp, "w", zipfile.ZIP_DEFLATED) as dst:
        for info in src.infolist():
            if info.filename != ENTRY:
                dst.writestr(info, src.read(info.filename))
        dst.writestr(ENTRY, data)
    os.replace(tmp, path)


def verify(path, data):
    with zipfile.ZipFile(path) as z:
        try:
            actual = z.read(ENTRY)
        except KeyError:
            print(f"{path}: missing {ENTRY}", file=sys.stderr)
            return False
    if actual != data:
        print(f"{path}: {ENTRY} content mismatch ({len(actual)} bytes, expected {len(data)})", file=sys.stderr)
        return False
    print(f"{path}: OK {ENTRY} ({NAME} / {ADDRESS})")
    return True


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
    blob = servers_dat([(NAME, ADDRESS)])
    if do_verify:
        ok = all(verify(p, blob) for p in paths)
        sys.exit(0 if ok else 1)
    for p in paths:
        add(p, blob)
        print(p, "+", ENTRY, len(blob), "bytes")
