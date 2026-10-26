"""PT14: builds src/main/resources/assets/councilcraft/textures/gui/album_blank_page.png
from Exposure 1.9.22's own assets/exposure/textures/gui/album.png (MIT, Copyright (c) mortuusars).

For each page of the spread (left at x0, right at x140, 149x188 each):
1) note rules (rows 140/149/158, x27..131) are replaced by the clean paper row directly above;
2) the photo frame area (x24..133, y20..130) is filled by harmonic (Laplace) interpolation from the
   surrounding paper, then every pixel is snapped to the page's own clean-paper palette.
No new art. Usage: python3 tools/album_blank_page.py <path/to/exposure/album.png> <out.png>
"""
import sys
import numpy as np
from PIL import Image


def blank_page(album, xo):
    page = album.crop((xo, 0, xo + 149, 188)).copy()
    for ry in (140, 149, 158):
        page.paste(page.crop((27, ry - 1, 132, ry)), (27, ry))
    a = np.asarray(page.convert('RGB')).astype(float)
    mask = np.zeros(a.shape[:2], bool)
    mask[20:131, 24:134] = True
    sub = a[22:160, 12:146].reshape(-1, 3)
    m2 = mask[22:160, 12:146].reshape(-1)
    pal = np.unique(sub[(~m2) & (sub.sum(1) >= 725)], axis=0)
    f = a.copy()
    f[mask] = a[~mask][(a[~mask].sum(1) >= 725)].mean(0)
    for _ in range(4000):
        n = (np.roll(f, 1, 0) + np.roll(f, -1, 0) + np.roll(f, 1, 1) + np.roll(f, -1, 1)) / 4
        f[mask] = n[mask]
    pts = f[mask]
    idx = ((pts[:, None, :] - pal[None, :, :]) ** 2).sum(2).argmin(1)
    out = a.copy()
    out[mask] = pal[idx]
    res = Image.fromarray(out.astype('uint8'), 'RGB').convert('RGBA')
    res.putalpha(page.split()[3])
    return res


def main(src, dst):
    album = Image.open(src).convert('RGBA')
    spread = album.crop((0, 0, 298, 188)).copy()
    spread.paste(blank_page(album, 0), (0, 0))
    right = blank_page(album, 140)
    # only the right page's own area (x149..) so the left page stays intact at the spine
    spread.paste(right.crop((9, 0, 149, 188)), (149, 0))
    spread.save(dst)


if __name__ == '__main__':
    main(sys.argv[1], sys.argv[2])
