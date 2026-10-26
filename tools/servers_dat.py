#!/usr/bin/env python3
"""PT20: adds client-overrides/servers.dat (one server: councilcraft / councilcraft.modrinth.gg) to .mrpack files.

servers.dat is the vanilla client's multiplayer list: uncompressed NBT, root compound with a "servers" list of
compounds {name, ip}. client-overrides/ is installed on clients only (mrpack format), so servers never get it.
The file is written here rather than kept in pack/ because packwiz exports pack/ files to overrides/ (both sides).
Every pack update replaces the player's list with this one entry (accepted in the SPEC).

Usage: servers_dat.py PACK.mrpack [...]   (rewrites each pack in place; idempotent)
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


if __name__ == "__main__":
    blob = servers_dat([(NAME, ADDRESS)])
    for p in sys.argv[1:]:
        add(p, blob)
        print(p, "+", ENTRY, len(blob), "bytes")
