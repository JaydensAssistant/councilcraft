#!/usr/bin/env python3
"""Offline check: IPN 1.10.20 InitWidgets has reHint + sort/move fields PT90 hooks."""
import struct
import sys
import zipfile
from pathlib import Path

JAR = Path(sys.argv[1] if len(sys.argv) > 1 else
           "/workspace/mrpack-audit-8/dl/mods__InventoryProfilesNext-fabric-1.20-1.10.20.jar")
TARGET = "org/anti_ad/mc/ipnext/gui/inject/SortingButtonCollectionWidget$InitWidgets.class"
NEED_FIELDS = {"sortButton", "moveAllToContainer", "moveAllToPlayer", "this$0"}
NEED_METHODS = {"reHint", "<init>"}


def read_u2(b, i):
    return struct.unpack_from(">H", b, i)[0], i + 2


def read_u4(b, i):
    return struct.unpack_from(">I", b, i)[0], i + 4


def parse_cp(data):
    assert data[:4] == b"\xca\xfe\xba\xbe"
    i = 8
    cp_count, i = read_u2(data, i)
    cp = [None]
    j = 1
    while j < cp_count:
        tag = data[i]
        i += 1
        if tag == 1:
            ln, i = read_u2(data, i)
            s = data[i : i + ln]
            i += ln
            cp.append((1, s))
        elif tag in (7, 8, 16, 19, 20):
            v, i = read_u2(data, i)
            cp.append((tag, v))
        elif tag in (3, 4):
            v, i = read_u4(data, i)
            cp.append((tag, v))
        elif tag in (9, 10, 11, 12, 17, 18):
            a, i = read_u2(data, i)
            b, i = read_u2(data, i)
            cp.append((tag, a, b))
        elif tag in (5, 6):
            v, i = read_u4(data, i)
            v2, i = read_u4(data, i)
            cp.append((tag, v, v2))
            j += 1
            cp.append(None)
        elif tag == 15:
            k = data[i]
            i += 1
            v, i = read_u2(data, i)
            cp.append((tag, k, v))
        else:
            raise SystemExit(f"unknown cp tag {tag}")
        j += 1
    return cp, i


def utf8(cp, idx):
    tag, data = cp[idx][0], cp[idx][1]
    assert tag == 1
    return data.decode("utf-8")


def main():
    if not JAR.is_file():
        raise SystemExit(f"missing jar: {JAR}")
    with zipfile.ZipFile(JAR) as z:
        data = z.read(TARGET)
    cp, i = parse_cp(data)
    # access + this + super
    i += 6
    # interfaces
    icount, i = read_u2(data, i)
    i += 2 * icount
    # fields
    fcount, i = read_u2(data, i)
    fields = set()
    for _ in range(fcount):
        _acc, i = read_u2(data, i)
        name_i, i = read_u2(data, i)
        _desc_i, i = read_u2(data, i)
        fields.add(utf8(cp, name_i))
        acount, i = read_u2(data, i)
        for _ in range(acount):
            _ai, i = read_u2(data, i)
            alen, i = read_u4(data, i)
            i += alen
    # methods
    mcount, i = read_u2(data, i)
    methods = set()
    for _ in range(mcount):
        _acc, i = read_u2(data, i)
        name_i, i = read_u2(data, i)
        _desc_i, i = read_u2(data, i)
        methods.add(utf8(cp, name_i))
        acount, i = read_u2(data, i)
        for _ in range(acount):
            _ai, i = read_u2(data, i)
            alen, i = read_u4(data, i)
            i += alen

    missing_f = NEED_FIELDS - fields
    missing_m = NEED_METHODS - methods
    if missing_f or missing_m:
        raise SystemExit(f"FAIL missing fields={missing_f} methods={missing_m}")
    print(f"OK {JAR.name}: InitWidgets has reHint + {sorted(NEED_FIELDS)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
