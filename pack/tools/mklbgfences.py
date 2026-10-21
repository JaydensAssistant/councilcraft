#!/usr/bin/env python3
"""PPM49a(6): LambdaBetterGrass snowy fences for the 22 Regions Unexplored fences and Vanilla Backport's pale_oak_fence.

Adds data + models + textures to resourcepacks/councilcraft-bettergrass-ru.zip. Models only reference LambdaBetterGrass's
snowy_fence_{post,side} templates by name. Each texture is procedural from the wood's own planks texture plus vanilla
minecraft:block/snow pixels (same coordinates) on a fixed cap-and-drip mask, so no LambdaBetterGrass art is copied.
usage: mklbgfences.py <minecraft-1.20.1.jar> <RegionsUnexplored.jar> <VanillaBackport.jar>
"""
import io, json, sys, zipfile
import numpy as np
from PIL import Image

MC, RU, VB = sys.argv[1:4]
PACK = 'resourcepacks/councilcraft-bettergrass-ru.zip'
RU_WOODS = ['baobab', 'blackwood', 'blue_bioshroom', 'brimwood', 'cobalt', 'cypress', 'dead', 'eucalyptus', 'green_bioshroom',
            'joshua', 'kapok', 'larch', 'magnolia', 'maple', 'mauve', 'palm', 'pine', 'pink_bioshroom', 'redwood', 'socotra',
            'willow', 'yellow_bioshroom']
FENCES = [('regions_unexplored', w, RU, f'assets/regions_unexplored/textures/block/{w}_planks.png') for w in RU_WOODS]
FENCES.append(('minecraft', 'pale_oak', VB, 'assets/minecraft/textures/block/pale_oak_planks.png'))


def img(jar, path):
    with zipfile.ZipFile(jar) as z:
        a = np.array(Image.open(io.BytesIO(z.read(path))).convert('RGBA'))
    return a[:16, :16]  # first frame if animated


SNOW = img(MC, 'assets/minecraft/textures/block/snow.png')


def drip(x, depth):
    """Fixed drip pattern under a snow cap: depth 1 on ~half the columns, depth 2 on a few."""
    return (x * 5 + 2) % 7 < 3 if depth == 1 else (x * 3 + 1) % 8 == 0


def mask(part):
    m = np.zeros((16, 16), bool)
    if part == 'post':  # post faces sample x 6-9, rows 0-15 (top at row 0); the down face samples x 0-3 and stays wood
        caps = [(0, range(6, 10))]
        m[1, 6:10] = True
    else:  # bar faces sample rows 1-3 (top bar) and 7-9 (bottom bar), x 0-15; down faces (rows 12-13) stay wood
        caps = [(1, range(16)), (7, range(16))]
    for top, xs in caps:
        for x in xs:
            m[top, x] = True
            m[top + 1, x] |= drip(x, 1)
            m[top + 2, x] |= drip(x, 1) and drip(x, 2)
    return m


def snowy(planks, part):
    out = planks.copy()
    out[..., 3] = 255
    m = mask(part)
    out[m] = SNOW[m]
    return Image.fromarray(out, 'RGBA')


def multipart(model_prefix):
    parts = [{'apply': {'model': model_prefix + '_post'}}]
    for d, rot in (('north', 0), ('east', 90), ('south', 180), ('west', 270)):
        apply = {'model': model_prefix + '_side'}
        if rot:
            apply['y'] = rot
        parts.append({'when': {d: 'true'}, 'apply': apply})
    return {'snow': {'layer': True, 'block_state': {'multipart': parts}}}


with zipfile.ZipFile(PACK) as z:
    existing = {n: z.read(n) for n in z.namelist()}
files = {n: d for n, d in existing.items() if '_fence' not in n}
for ns, wood, jar, tex in FENCES:
    planks = img(jar, tex)
    base = f'snowy_{ns}_{wood}_fence'
    for p in ('post', 'side'):
        buf = io.BytesIO(); snowy(planks, p).save(buf, 'PNG')
        files[f'assets/councilcraft/textures/block/fence/{base}_{p}.png'] = buf.getvalue()
        files[f'assets/councilcraft/models/block/fence/{base}_{p}.json'] = json.dumps(
            {'parent': f'lambdabettergrass:block/template/snowy_fence_{p}',
             'textures': {'texture': f'councilcraft:block/fence/{base}_{p}'}}, indent=2).encode()
    files[f'assets/{ns}/bettergrass/data/{wood}_fence.json'] = json.dumps(
        multipart(f'councilcraft:block/fence/{base}'), indent=2).encode()
files['README.txt'] = (b'CouncilCraft LambdaBetterGrass data for Regions Unexplored.\n'
                       b'Grass/path data loads textures from Regions Unexplored, Minecraft and LambdaBetterGrass.\n'
                       b'Snowy fences (22 RU fences + pale_oak_fence, tools/mklbgfences.py): each wood\'s planks texture with\n'
                       b'vanilla snow pixels on a fixed cap-and-drip mask. Models reference LambdaBetterGrass (LambdAurora)\n'
                       b'snowy fence templates by name; no LambdaBetterGrass files are included.\n')
with zipfile.ZipFile(PACK, 'w', zipfile.ZIP_DEFLATED) as z:
    for n in sorted(files):
        zi = zipfile.ZipInfo(n, (2026, 10, 21, 0, 0, 0)); zi.compress_type = zipfile.ZIP_DEFLATED
        z.writestr(zi, files[n])
print(len(FENCES), 'fences;', len(files), 'files')
