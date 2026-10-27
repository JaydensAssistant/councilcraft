#!/usr/bin/env python3
"""PT46: generate CMYK Ink's flat opaque band textures (process_ink_still.png, process_ink_flow.png).

Original work: only the band colors from PT36 and a faint near-white grain (values 238-247, multiplied into each
band) are used. No pixel from Create Dragons Plus or any other mod is copied; the grain comes from a seeded random
generator in this script. Alpha is 255 everywhere. No ripples, no highlights.

Layout (unchanged from PT36): four equal vertical bands C, M, Y, K, left to right; 32 animation frames
(still 16x16 per frame, flow 32x32 per frame); the .mcmeta files are not touched, so the timing is unchanged.
The still texture is the same grain in every frame (flat). In the flowing texture the grain scrolls down one row per
frame and wraps, so the flow reads as moving without adding any highlight.

Usage: python3 pack/tools/mkink.py [--check]   (--check writes nothing and fails if the files differ)
"""
import random
import sys
from pathlib import Path

from PIL import Image

BANDS = [(0, 116, 154), (152, 10, 90), (174, 158, 17), (39, 39, 46)]  # C, M, Y, K
FRAMES = 32
GRAIN_LO, GRAIN_HI = 238, 247
OUT = Path(__file__).resolve().parents[2] / "src/main/resources/assets/councilcraft/textures/block"


def grain(size, seed):
    rng = random.Random(seed)
    return [[rng.randint(GRAIN_LO, GRAIN_HI) for _ in range(size)] for _ in range(size)]


def frame(size, g, shift):
    im = Image.new("RGBA", (size, size))
    band_w = size // len(BANDS)
    for y in range(size):
        for x in range(size):
            base = BANDS[x // band_w]
            v = g[(y - shift) % size][x]
            im.putpixel((x, y), tuple(c * v // 255 for c in base) + (255,))
    return im


def build(size, seed, scroll):
    g = grain(size, seed)
    sheet = Image.new("RGBA", (size, size * FRAMES))
    for f in range(FRAMES):
        sheet.paste(frame(size, g, f if scroll else 0), (0, f * size))
    return sheet


def main():
    check = "--check" in sys.argv
    out = {"process_ink_still.png": build(16, 4601, False), "process_ink_flow.png": build(32, 4602, True)}
    bad = 0
    for name, img in out.items():
        path = OUT / name
        if check:
            cur = Image.open(path).convert("RGBA")
            if cur.tobytes() != img.tobytes() or cur.size != img.size:
                print("DIFFERS:", name)
                bad += 1
        else:
            img.save(path, optimize=True)
            print("wrote", path, img.size)
    sys.exit(1 if bad else 0)


if __name__ == "__main__":
    main()
