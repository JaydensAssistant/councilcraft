#!/usr/bin/env python3
"""Offline check: IPN 1.10.20 AreaTypes.fillSlots reuses param slots (locals 1/2).
A RETURN @Inject that reloads args from those locals is invalid; @WrapMethod is not.
"""
import struct, sys, zipfile
from pathlib import Path

JAR = Path(sys.argv[1] if len(sys.argv) > 1 else
           "/workspace/mrpack-audit-8/dl/mods__InventoryProfilesNext-fabric-1.20-1.10.20.jar")

def read_u2(b, i): return struct.unpack_from(">H", b, i)[0], i + 2
def read_u4(b, i): return struct.unpack_from(">I", b, i)[0], i + 4

def utf8_from_cp(cp, idx):
    tag, data = cp[idx]
    assert tag == 1, tag
    return data.decode("utf-8")

def parse_class(data: bytes):
    assert data[:4] == b"\xca\xfe\xba\xbe"
    i = 8
    cp_count, i = read_u2(data, i)
    cp = [None]
    j = 1
    while j < cp_count:
        tag = data[i]; i += 1
        if tag == 1:  # Utf8
            ln, i = read_u2(data, i); s = data[i:i+ln]; i += ln; cp.append((1, s))
        elif tag in (7, 8, 16, 19, 20):
            v, i = read_u2(data, i); cp.append((tag, v))
        elif tag in (3, 4):
            v, i = read_u4(data, i); cp.append((tag, v))
        elif tag in (9, 10, 11, 12, 17, 18):
            a, i = read_u2(data, i); b, i = read_u2(data, i); cp.append((tag, a, b))
        elif tag == 5 or tag == 6:
            v, i = read_u4(data, i); v2, i = read_u4(data, i); cp.append((tag, v, v2)); j += 1; cp.append(None)
        elif tag == 15:
            k = data[i]; i += 1; v, i = read_u2(data, i); cp.append((tag, k, v))
        else:
            raise SystemExit(f"unknown cp tag {tag} at {i}")
        j += 1
    # skip access, this, super
    _, i = read_u2(data, i); _, i = read_u2(data, i); _, i = read_u2(data, i)
    ifc, i = read_u2(data, i); i += 2 * ifc
    fields, i = read_u2(data, i)
    for _ in range(fields):
        i += 6
        ac, i = read_u2(data, i)
        for _ in range(ac):
            i += 2; ln, i = read_u4(data, i); i += ln
    methods, i = read_u2(data, i)
    found = None
    for _ in range(methods):
        access, i = read_u2(data, i)
        name_i, i = read_u2(data, i)
        desc_i, i = read_u2(data, i)
        name = utf8_from_cp(cp, name_i)
        desc = utf8_from_cp(cp, desc_i)
        ac, i = read_u2(data, i)
        code = None
        for _ in range(ac):
            an, i = read_u2(data, i); ln, i = read_u4(data, i); attr = data[i:i+ln]; i += ln
            if utf8_from_cp(cp, an) == "Code":
                code = attr
        if name == "fillSlots" and desc == "(Ljava/util/Set;Ljava/util/List;Ljava/util/List;)V":
            found = (access, code)
    return found

with zipfile.ZipFile(JAR) as z:
    raw = z.read("org/anti_ad/mc/ipnext/inventory/AreaTypes.class")
access, code = parse_class(raw)
assert code is not None, "fillSlots not found"
# Code attr: max_stack u2, max_locals u2, code_length u4, code[...]
ms = struct.unpack_from(">H", code, 0)[0]
ml = struct.unpack_from(">H", code, 2)[0]
clen = struct.unpack_from(">I", code, 4)[0]
bytecode = code[8:8+clen]
# Look for istore_1 (0x3c) and astore_2 (0x4d) early — param slot reuse.
# Also istore 1 wide form is rare; Kotlin uses istore_1 / astore_2.
has_istore_1 = 0x3c in bytecode
has_astore_2 = 0x4d in bytecode
# First ops: aload_1 (0x2b) then later istore_1
first20 = list(bytecode[:40])
print(f"fillSlots max_locals={ml} code_len={clen}")
print(f"bytecode starts: {[hex(b) for b in first20[:16]]}")
print(f"contains istore_1 (0x3c): {has_istore_1}")
print(f"contains astore_2 (0x4d): {has_astore_2}")
# CFR-equivalent: after aload_1 / invoke contains, the next store to local 1 is int.
# Prove first istore_1 appears after first aload_1.
i_aload1 = bytecode.index(0x2b)
i_istore1 = bytecode.index(0x3c)
i_astore2 = bytecode.index(0x4d)
print(f"first aload_1 @{i_aload1}, first istore_1 @{i_istore1}, first astore_2 @{i_astore2}")
assert i_istore1 > i_aload1, "expected param local 1 reused as int after first read"
assert has_astore_2, "expected local 2 reused as Iterator (astore_2)"
print("OK: fillSlots reuses locals 1 and 2; WrapMethod (Java args) is required, RETURN inject is unsafe.")
