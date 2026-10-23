#!/usr/bin/env python3
"""PPM49a(6) / PPM70 / PPM90: LambdaBetterGrass snowy fences for the 22 Regions Unexplored fences and Vanilla Backport's pale_oak_fence.

PPM90 (licensing): no planks pixels are baked any more. Regions Unexplored is All Rights Reserved, so the zip ships only
LambdaBetterGrass's MIT snow overlay (2 PNGs: post and side) plus models/data/states. Each fence model references the
wood's own planks texture from the installed mod (regions_unexplored:block/<wood>_planks, minecraft:block/pale_oak_planks
from Vanilla Backport) at runtime.

How the look is kept: LambdaBetterGrass's snowy fence (same geometry as its MIT template models, pinned commit below)
samples one baked texture per wood whose opaque pixels are either the wood's planks pixel or a shared overlay pixel
(see PPM70 notes). Fences render in the solid layer, so an overlay with transparent pixels cannot simply be drawn on top.
Instead every textured face is split, texel by texel, into rectangles that come only from the planks or only from the
overlay; each rectangle becomes a flat one-face element (same plane, same cullface) sampling either #planks (at the
planks texel LambdaBetterGrass used) or #overlay (at the same texel as LambdaBetterGrass). No quads overlap, nothing is
transparent, and the snow top (#top = minecraft:block/snow) is unchanged.
usage: mklbgfences.py <minecraft-1.20.1-client.jar> <RegionsUnexplored.jar> <VanillaBackport.jar>   (run in pack/)
"""
import io, json, sys, urllib.request, zipfile
import numpy as np
from PIL import Image

MC, RU, VB = sys.argv[1:4]
PACK = 'resourcepacks/councilcraft-bettergrass-ru.zip'
LBG = 'https://raw.githubusercontent.com/LambdAurora/LambdaBetterGrass/05428aa00e5948c1c70f8b2df383a7e1046effd2/'
LBG_FENCE = LBG + 'src/main/resources/resourcepacks/default/assets/lambdabettergrass/textures/block/fence/snowy_{}_fence_{}.png'
LBG_TEMPLATE = LBG + 'src/main/resources/assets/lambdabettergrass/models/block/template/{}.json'
VANILLA_WOODS = ['oak', 'spruce', 'birch', 'jungle', 'acacia', 'dark_oak', 'mangrove', 'cherry', 'crimson', 'warped']
STRAYS = {('side', 'spruce'): {(2, 6)}, ('side', 'acacia'): {(11, x) for x in range(11, 15)},
          ('side', 'mangrove'): {(11, x) for x in range(11, 15)}}
RU_WOODS = ['baobab', 'blackwood', 'blue_bioshroom', 'brimwood', 'cobalt', 'cypress', 'dead', 'eucalyptus', 'green_bioshroom',
            'joshua', 'kapok', 'larch', 'magnolia', 'maple', 'mauve', 'palm', 'pine', 'pink_bioshroom', 'redwood', 'socotra',
            'willow', 'yellow_bioshroom']
FENCES = [('regions_unexplored', w, f'regions_unexplored:block/{w}_planks') for w in RU_WOODS]
FENCES.append(('minecraft', 'pale_oak', 'minecraft:block/pale_oak_planks'))
OVERLAY_TEX = 'councilcraft:block/fence/snowy_fence_overlay_{}'
TEMPLATE = 'councilcraft:block/fence/template/snowy_fence_{}'


def rgba(data):
    a = np.array(Image.open(io.BytesIO(data)).convert('RGBA'))[:16, :16].astype(np.uint8)
    a[a[..., 3] == 0] = 0
    return a


def fetch(url):
    with urllib.request.urlopen(url) as r:
        return r.read()


def planks_source(part):
    """16x16 array of (row, col) planks texel that LambdaBetterGrass's texture holds at each texel, or None."""
    src = {}
    if part == 'post':
        for r in range(16):
            for x in range(6, 10):
                src[(r, x)] = (r, x)
    else:
        for r in (1, 2, 3, 7, 8, 9):
            for x in range(16):
                src[(r, x)] = (r, x)
        for r in (12, 13):
            for x in range(9):
                src[(r, x)] = (x, 20 - r)
    return src


def wood(planks, part):
    out = np.zeros((16, 16, 4), np.uint8)
    for (r, x), (pr, px) in planks_source(part).items():
        out[r, x] = planks[pr, px]
    out[..., 3] = np.where(out[..., 3] > 0, 255, 0)
    return out


def overlay(part):
    lbg = {w: rgba(fetch(LBG_FENCE.format(w, part))) for w in VANILLA_WOODS}
    with zipfile.ZipFile(MC) as z:
        plain = {w: wood(rgba(z.read(f'assets/minecraft/textures/block/{w}_planks.png')), part) for w in VANILLA_WOODS}
    mask = np.all([np.any(plain[w] != lbg[w], axis=2) for w in VANILLA_WOODS], axis=0)
    first = lbg[VANILLA_WOODS[0]][mask]
    assert all(np.array_equal(lbg[w][mask], first) for w in VANILLA_WOODS), part + ': overlay differs between woods'
    for w in VANILLA_WOODS:
        rebuilt = plain[w].copy()
        rebuilt[mask] = first
        bad = {tuple(map(int, p)) for p in np.argwhere(np.any(rebuilt != lbg[w], axis=2))}
        assert bad == STRAYS.get((part, w), set()), f'{part} {w}: model does not rebuild LambdaBetterGrass: {sorted(bad)}'
    tex = np.zeros((16, 16, 4), np.uint8)
    tex[mask] = first
    assert np.all(tex[mask][:, 3] == 255), part + ': overlay has non-opaque pixels'
    return mask, tex


# --- Minecraft face geometry (FaceInfo / BlockFaceUV, 1.20.1) ---
def corners(d, f, t):
    x0, y0, z0 = f; x1, y1, z1 = t
    return {'down': [(x0, y0, z1), (x0, y0, z0), (x1, y0, z0), (x1, y0, z1)],
            'up': [(x0, y1, z0), (x0, y1, z1), (x1, y1, z1), (x1, y1, z0)],
            'north': [(x1, y1, z0), (x1, y0, z0), (x0, y0, z0), (x0, y1, z0)],
            'south': [(x0, y1, z1), (x0, y0, z1), (x1, y0, z1), (x1, y1, z1)],
            'west': [(x0, y1, z0), (x0, y0, z0), (x0, y0, z1), (x0, y1, z1)],
            'east': [(x1, y1, z1), (x1, y0, z1), (x1, y0, z0), (x1, y1, z0)]}[d]


def vertex_uvs(uv, rot):
    u1, v1, u2, v2 = uv
    out = []
    for i in range(4):
        j = (i + rot // 90) % 4
        out.append((u1 if j in (0, 1) else u2, v1 if j in (0, 3) else v2))
    return out


def solve_uv(vuvs):
    """(uv, rotation) giving these 4 vertex UVs, if any."""
    for rot in (0, 90, 180, 270):
        g = {}
        ok = True
        for i, (u, v) in enumerate(vuvs):
            j = (i + rot // 90) % 4
            for key, val in ((('u', j in (0, 1)), u), (('v', j in (0, 3)), v)):
                if g.setdefault(key, val) != val:
                    ok = False
        if ok:
            return [g[('u', True)], g[('v', True)], g[('u', False)], g[('v', False)]], rot
    raise ValueError(vuvs)


def affine(d, f, t, uv, rot):
    """uv -> 3D point on the face and back, from vertices 0, 1, 3."""
    P = [np.array(p, float) for p in corners(d, f, t)]
    U = [np.array(q, float) for q in vertex_uvs(uv, rot)]
    A = np.column_stack([U[1] - U[0], U[3] - U[0]])
    B = np.column_stack([P[1] - P[0], P[3] - P[0]])
    M = B @ np.linalg.pinv(A)  # 3x2, exact for axis-aligned 90-degree mappings
    to3 = lambda q: P[0] + M @ (np.array(q, float) - U[0])
    Minv = A @ np.linalg.pinv(B)
    to2 = lambda p: U[0] + Minv @ (np.array(p, float) - P[0])
    return to3, to2


def rects(cls):
    """Greedy split of a 2D label grid (rows=v, cols=u) into same-label rectangles."""
    h, w = cls.shape
    used = np.zeros_like(cls, bool)
    out = []
    for r in range(h):
        for c in range(w):
            if used[r, c]:
                continue
            lab = cls[r, c]
            cw = 1
            while c + cw < w and not used[r, c + cw] and cls[r, c + cw] == lab:
                cw += 1
            rh = 1
            while r + rh < h and all(not used[r + rh, c + k] and cls[r + rh, c + k] == lab for k in range(cw)):
                rh += 1
            used[r:r + rh, c:c + cw] = True
            out.append((lab, r, c, rh, cw))
    return out


def num(x):
    x = round(float(x), 4)
    return int(x) if x == int(x) else x


def build_template(part, mask):
    name = 'fence_post' if part == 'post' else 'snowy_fence_side'
    tpl = json.loads(fetch(LBG_TEMPLATE.format(name)))
    src = planks_source(part)
    elements = []
    for el in tpl['elements']:
        f, t = el['from'], el['to']
        keep = {}
        for d, face in el['faces'].items():
            if face['texture'] != '#texture':
                keep[d] = face
                continue
            uv, rot = face['uv'], face.get('rotation', 0)
            u1, v1, u2, v2 = uv
            assert u1 < u2 and v1 < v2
            cls = np.zeros((v2 - v1, u2 - u1), object)
            for r in range(v1, v2):
                for c in range(u1, u2):
                    cls[r - v1, c - u1] = 'O' if mask[r, c] else ('P' if (r, c) in src else 'X')
            if (cls == 'X').all():
                # LambdaBetterGrass leaves this face transparent (only the post underside, always culled: the snow layer
                # needs a solid block below). Use the planks at vanilla's default UV instead of nothing.
                assert d == 'down' and face.get('cullface') == 'down', (part, d)
                fc = {'uv': {'down': [f[0], 16 - t[2], t[0], 16 - f[2]]}[d], 'texture': '#planks', 'cullface': 'down'}
                keep[d] = fc
                continue
            assert not (cls == 'X').any(), f'{part} {d}: texel neither planks nor overlay'
            to3, to2 = affine(d, f, t, uv, rot)
            for lab, r, c, rh, cw in rects(cls):
                sub = (u1 + c, v1 + r, u1 + c + cw, v1 + r + rh)
                pts = [to3(q) for q in ((sub[0], sub[1]), (sub[2], sub[1]), (sub[0], sub[3]), (sub[2], sub[3]))]
                lo, hi = np.min(pts, axis=0), np.max(pts, axis=0)
                vu = [to2(p) for p in corners(d, lo, hi)]
                if lab == 'P':
                    vu = [PLANKS_MAP[part](*q) for q in vu]
                    tex = '#planks'
                else:
                    tex = '#overlay'
                nuv, nrot = solve_uv([(num(a), num(b)) for a, b in vu])
                fc = {'uv': nuv, 'texture': tex}
                if nrot:
                    fc['rotation'] = nrot
                if 'cullface' in face:
                    fc['cullface'] = face['cullface']
                elements.append({'from': [num(x) for x in lo], 'to': [num(x) for x in hi], 'faces': {d: fc}})
        if keep:
            elements.insert(0, {'name': el.get('name'), 'from': f, 'to': t, 'faces': keep})
    return {'textures': {'particle': '#planks', 'overlay': OVERLAY_TEX.format(part), 'top': 'minecraft:block/snow'},
            'elements': elements}


# Continuous form of planks_source: baked (u, v) -> planks (u, v). Post and side bars: identity. Side underside
# (rows 12-13, cols 0-8): baked texel (row r, col x) = planks texel (row x, col 20 - r), i.e. u' = 21 - v, v' = u.
def side_map(u, v):
    return (21 - v, u) if 12 <= v <= 14 and u <= 9 else (u, v)


PLANKS_MAP = {'post': lambda u, v: (u, v), 'side': side_map}


def multipart(model_prefix):
    parts = [{'apply': {'model': model_prefix + '_post'}}]
    for d, rot in (('north', 0), ('east', 90), ('south', 180), ('west', 270)):
        apply = {'model': model_prefix + '_side'}
        if rot:
            apply['y'] = rot
        parts.append({'when': {d: 'true'}, 'apply': apply})
    return {'snow': {'layer': True, 'block_state': {'multipart': parts}}}


if __name__ == '__main__':
    with zipfile.ZipFile(PACK) as z:
        existing = {n: z.read(n) for n in z.namelist()}
    files = {n: d for n, d in existing.items() if '_fence' not in n and '/fence/' not in n}
    for part in ('post', 'side'):
        mask, tex = overlay(part)
        buf = io.BytesIO(); Image.fromarray(tex, 'RGBA').save(buf, 'PNG')
        files[f'assets/councilcraft/textures/block/fence/snowy_fence_overlay_{part}.png'] = buf.getvalue()
        files[f'assets/councilcraft/models/block/fence/template/snowy_fence_{part}.json'] = json.dumps(
            build_template(part, mask), indent=2).encode()
    for ns, w, planks in FENCES:
        base = f'snowy_{ns}_{w}_fence'
        for p in ('post', 'side'):
            files[f'assets/councilcraft/models/block/fence/{base}_{p}.json'] = json.dumps(
                {'parent': TEMPLATE.format(p), 'textures': {'planks': planks}}, indent=2).encode()
        files[f'assets/{ns}/bettergrass/data/{w}_fence.json'] = json.dumps(
            multipart(f'councilcraft:block/fence/{base}'), indent=2).encode()
        files[f'assets/{ns}/bettergrass/states/{w}_fence.json'] = json.dumps(
            {'type': 'layer', 'variants': {'waterlogged=false': {'data': f'{ns}:bettergrass/data/{w}_fence'}}}, indent=2).encode()
    files['LICENSE-LambdaBetterGrass.txt'] = fetch(LBG + 'LICENSE')
    files['README.txt'] = (b'CouncilCraft LambdaBetterGrass data for Regions Unexplored.\n'
                           b'Grass/path data loads textures from Regions Unexplored, Minecraft and LambdaBetterGrass.\n'
                           b'Snowy fences (22 RU fences + pale_oak_fence, states + data, tools/mklbgfences.py): models reference each\n'
                           b'wood\'s own planks texture from the installed mod (no Regions Unexplored or Vanilla Backport pixels are\n'
                           b'shipped here) and lay the snow overlay of LambdaBetterGrass\'s snowy fence textures by LambdAurora on top,\n'
                           b'from its MIT-licensed legacy/mit branch (https://github.com/LambdAurora/LambdaBetterGrass/tree/legacy/mit,\n'
                           b'commit 05428aa). Shipped textures: snowy_fence_overlay_post.png and snowy_fence_overlay_side.png only\n'
                           b'(the overlay pixels, extracted from LambdaBetterGrass; modified: separated from the vanilla planks).\n'
                           b'Model geometry follows LambdaBetterGrass\'s MIT snowy fence templates. MIT notice: LICENSE-LambdaBetterGrass.txt.\n')
    with zipfile.ZipFile(PACK, 'w', zipfile.ZIP_DEFLATED) as z:
        for n in sorted(files):
            zi = zipfile.ZipInfo(n, (2026, 10, 22, 0, 0, 0)); zi.compress_type = zipfile.ZIP_DEFLATED
            z.writestr(zi, files[n])
    print(len(FENCES), 'fences;', len(files), 'files')
