#!/usr/bin/env python3
"""PPM49a(6) / PPM70: LambdaBetterGrass snowy fences for the 22 Regions Unexplored fences and Vanilla Backport's pale_oak_fence.

Adds data + models + textures to resourcepacks/councilcraft-bettergrass-ru.zip. Models reference LambdaBetterGrass's
snowy_fence_{post,side} templates by name.

PPM70: the textures carry LambdaBetterGrass's own snow overlay so they match its vanilla snowy fences. The overlay is
read from LambdaBetterGrass's MIT-licensed legacy/mit branch (pinned commit below; the same files as in the 2.0.3 jar).
For each of its 10 vanilla-wood snowy fences, every opaque pixel is either the wood's planks pixel (post: x 6-9 at the
same coordinate; side: rows 1-3 and 7-9 at the same coordinate, underside rows 12-13 x 0-8 = planks[x][20 - row]) or an
overlay pixel with the same RGBA in all 10 woods. The overlay is the set of pixels that differ from the planks in all
10 woods; the script checks that this model rebuilds all 20 LambdaBetterGrass textures (known strays below excepted),
then lays the overlay on each new wood's planks. No new art. The MIT notice ships in the zip as LICENSE-LambdaBetterGrass.txt.
usage: mklbgfences.py <minecraft-1.20.1.jar> <RegionsUnexplored.jar> <VanillaBackport.jar>
"""
import io, json, sys, urllib.request, zipfile
import numpy as np
from PIL import Image

MC, RU, VB = sys.argv[1:4]
PACK = 'resourcepacks/councilcraft-bettergrass-ru.zip'
LBG = 'https://raw.githubusercontent.com/LambdAurora/LambdaBetterGrass/05428aa00e5948c1c70f8b2df383a7e1046effd2/'
LBG_FENCE = LBG + 'src/main/resources/resourcepacks/default/assets/lambdabettergrass/textures/block/fence/snowy_{}_fence_{}.png'
VANILLA_WOODS = ['oak', 'spruce', 'birch', 'jungle', 'acacia', 'dark_oak', 'mangrove', 'cherry', 'crimson', 'warped']
# Hand touches in single LambdaBetterGrass textures, not part of the shared overlay: (part, wood) -> pixels (row, x)
STRAYS = {('side', 'spruce'): {(2, 6)}, ('side', 'acacia'): {(11, x) for x in range(11, 15)},
          ('side', 'mangrove'): {(11, x) for x in range(11, 15)}}
RU_WOODS = ['baobab', 'blackwood', 'blue_bioshroom', 'brimwood', 'cobalt', 'cypress', 'dead', 'eucalyptus', 'green_bioshroom',
            'joshua', 'kapok', 'larch', 'magnolia', 'maple', 'mauve', 'palm', 'pine', 'pink_bioshroom', 'redwood', 'socotra',
            'willow', 'yellow_bioshroom']
FENCES = [('regions_unexplored', w, RU, f'assets/regions_unexplored/textures/block/{w}_planks.png') for w in RU_WOODS]
FENCES.append(('minecraft', 'pale_oak', VB, 'assets/minecraft/textures/block/pale_oak_planks.png'))


def rgba(data):
    a = np.array(Image.open(io.BytesIO(data)).convert('RGBA'))[:16, :16].astype(np.uint8)  # first frame if animated
    a[a[..., 3] == 0] = 0  # transparent pixels compare equal whatever their RGB
    return a


def img(jar, path):
    with zipfile.ZipFile(jar) as z:
        return rgba(z.read(path))


def fetch(url):
    with urllib.request.urlopen(url) as r:
        return r.read()


def wood(planks, part):
    """The planks pixels LambdaBetterGrass's snowy fence keeps, everything else transparent."""
    out = np.zeros((16, 16, 4), np.uint8)
    if part == 'post':
        out[:, 6:10] = planks[:, 6:10]
    else:
        for r in (1, 2, 3, 7, 8, 9):
            out[r] = planks[r]
        for r in (12, 13):
            for x in range(9):
                out[r, x] = planks[x, 20 - r]
    out[..., 3] = np.where(out[..., 3] > 0, 255, 0)
    return out


def overlay(part):
    lbg = {w: rgba(fetch(LBG_FENCE.format(w, part))) for w in VANILLA_WOODS}
    plain = {w: wood(img(MC, f'assets/minecraft/textures/block/{w}_planks.png'), part) for w in VANILLA_WOODS}
    mask = np.all([np.any(plain[w] != lbg[w], axis=2) for w in VANILLA_WOODS], axis=0)
    first = lbg[VANILLA_WOODS[0]][mask]
    assert all(np.array_equal(lbg[w][mask], first) for w in VANILLA_WOODS), part + ': overlay differs between woods'
    for w in VANILLA_WOODS:
        rebuilt = plain[w].copy()
        rebuilt[mask] = first
        bad = {tuple(map(int, p)) for p in np.argwhere(np.any(rebuilt != lbg[w], axis=2))}
        assert bad == STRAYS.get((part, w), set()), f'{part} {w}: model does not rebuild LambdaBetterGrass: {sorted(bad)}'
    return mask, first


OVERLAY = {p: overlay(p) for p in ('post', 'side')}


def snowy(planks, part):
    out = wood(planks, part)
    mask, pixels = OVERLAY[part]
    out[mask] = pixels
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
for ns, w, jar, tex in FENCES:
    planks = img(jar, tex)
    base = f'snowy_{ns}_{w}_fence'
    for p in ('post', 'side'):
        buf = io.BytesIO(); snowy(planks, p).save(buf, 'PNG')
        files[f'assets/councilcraft/textures/block/fence/{base}_{p}.png'] = buf.getvalue()
        files[f'assets/councilcraft/models/block/fence/{base}_{p}.json'] = json.dumps(
            {'parent': f'lambdabettergrass:block/template/snowy_fence_{p}',
             'textures': {'texture': f'councilcraft:block/fence/{base}_{p}'}}, indent=2).encode()
    files[f'assets/{ns}/bettergrass/data/{w}_fence.json'] = json.dumps(
        multipart(f'councilcraft:block/fence/{base}'), indent=2).encode()
    # PPM67: LambdaBetterGrass only reads data for blocks that have a states file (same shape as its own oak_fence.json)
    files[f'assets/{ns}/bettergrass/states/{w}_fence.json'] = json.dumps(
        {'type': 'layer', 'variants': {'waterlogged=false': {'data': f'{ns}:bettergrass/data/{w}_fence'}}}, indent=2).encode()
files['LICENSE-LambdaBetterGrass.txt'] = fetch(LBG + 'LICENSE')
files['README.txt'] = (b'CouncilCraft LambdaBetterGrass data for Regions Unexplored.\n'
                       b'Grass/path data loads textures from Regions Unexplored, Minecraft and LambdaBetterGrass.\n'
                       b'Snowy fences (22 RU fences + pale_oak_fence, states + data, tools/mklbgfences.py): each wood\'s planks texture\n'
                       b'under the snow overlay of LambdaBetterGrass\'s snowy fence textures by LambdAurora, from its MIT-licensed\n'
                       b'legacy/mit branch (https://github.com/LambdAurora/LambdaBetterGrass/tree/legacy/mit, commit 05428aa).\n'
                       b'Modified: overlay laid on other woods\' planks. MIT notice: LICENSE-LambdaBetterGrass.txt.\n'
                       b'Models reference the LambdaBetterGrass snowy fence templates by name.\n')
with zipfile.ZipFile(PACK, 'w', zipfile.ZIP_DEFLATED) as z:
    for n in sorted(files):
        zi = zipfile.ZipInfo(n, (2026, 10, 22, 0, 0, 0)); zi.compress_type = zipfile.ZIP_DEFLATED
        z.writestr(zi, files[n])
print(len(FENCES), 'fences;', len(files), 'files;', 'overlay px', {p: int(m.sum()) for p, (m, _) in OVERLAY.items()})
