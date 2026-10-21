#!/usr/bin/env python3
"""PPM49/49a/60 (0.4.6): build resourcepacks/councilcraft-blending.zip, CouncilCraft's own Continuity blending pack.

Replaces the Connected Paths mod (and its first-launch compile). Reproducible: run from the pack dir with
    tools/mkblending.py <connected-paths-fabric-1.0.2.jar> <minecraft-1.20.1.jar> <client mods dir>
Art: the Connected Paths artist's (Jacosvaldo) CC BY-NC-SA 4.0 48x48 edge sheets from the mod's templates/textures,
sliced into Continuity's 17 overlay tiles exactly like Connected Paths' ImageSlicer. Newly covered blocks (PPM49a(5),
PPM60 terracottas) get procedural recolors: a donor sheet's alpha mask and shading ratio over the block's own texture.
The output pack is CC BY-NC-SA 4.0 (see its README/LICENSE). No art is drawn by hand.

Rules (SPEC PPM49a, PPM60):
- method=overlay, connectTiles = source texture, matchBlocks = receivers. Continuity only draws an overlay on a face that
  is rendered (open to air) and only from a neighbor whose face in front is not covered by an opaque full cube, which is
  PPM49a(2); bottom edges get fringe from soft ground below (PPM49a(3)) through the same overlay pass.
- Pairings: soft ground -> later soft ground + natural stone; stone -> later stone; nothing onto logs/wood/planks/
  obsidian/ores/bricks/man-made blocks; bricks only within their own family; terracottas onto each other.
- Connected Paths' GrassFix snowy-grass blockstates are NOT shipped: LambdaBetterGrass handles snowy grass sides.
"""
import io, json, re, sys, tomllib, zipfile, glob, os
import numpy as np
from PIL import Image

CPJ, MCJ, MODS = sys.argv[1:4]
OUT = 'resourcepacks/councilcraft-blending.zip'
MC = (1, 20, 1)

# ---- groups: Connected Paths groups_def with CouncilCraft changes ----
SOFT = ['always_on_top', 'powders', 'granular_soils', 'sculks', 'grasses', 'mosses', 'wet_soils', 'crumbly_soils',
        'dry_soils', 'non_full_hitbox']
STONE = ['bedrock', 'cobblestones', 'special_rocks', 'slates', 'rocks', 'stones', 'natural_sandstones']
BRICKS = ['cut_copper_like_bricks', 'stone_bricks_like_bricks', 'mud_bricks_like_bricks', 'quartz_brciks_like_bricks',
          'sculk_grime_bricks_like_bricks', 'resin_brciks_like_bricks', 'nether_bricks_like_bricks',
          'dacite_bricks_like_bricks', 'sculk_stone_tiles_like_bricks', 'cut_gloomslate_like_bricks',
          'chalk_bricks_like_bricks', 'chiseled_mega_meteorid_brick_like_bricks', 'brimstone_bricks_like_bricks',
          'deepslate_tiles_like_bricks']
CHAIN = SOFT + STONE
# group -> groups it draws onto (besides later blocks of its own group)
RELATIONS = {g: CHAIN[i + 1:] for i, g in enumerate(SOFT)}
RELATIONS.update({g: STONE[i + 1:] for i, g in enumerate(STONE)})
SOURCE_GROUPS = set(CHAIN) | set(BRICKS) | {'terracottas'}
# PPM49a(5) + PPM60: newly covered blocks -> (group, donor template sheet per face texture)
NEW = {
    'create:limestone': ('stones', 'minecraft/stone'), 'create:scoria': ('stones', 'minecraft/stone'),
    'create:ochrum': ('stones', 'minecraft/stone'), 'create:veridium': ('stones', 'minecraft/stone'),
    'create:crimsite': ('stones', 'minecraft/stone'), 'create:asurine': ('stones', 'minecraft/stone'),
    'create:scorchia': ('stones', 'minecraft/stone'), 'naturalist:shellstone': ('stones', 'minecraft/stone'),
    'supplementaries:ashen_basalt': ('special_rocks', 'minecraft/basalt_side'),
    'farmersdelight:rich_soil': ('crumbly_soils', 'minecraft/dirt'),
}  # pale moss already has an original Connected Paths sheet (rendered through Vanilla Backport's texture)
TERRACOTTAS = ['minecraft:terracotta'] + ['minecraft:%s_terracotta' % c for c in (
    'white', 'light_gray', 'gray', 'black', 'brown', 'red', 'orange', 'yellow', 'lime', 'green', 'cyan', 'light_blue',
    'blue', 'purple', 'magenta', 'pink')]
for t in TERRACOTTAS:
    NEW[t] = ('terracottas', 'minecraft/stone')


def norm(i):
    return i if ':' in i else 'minecraft:' + i


cpz = zipfile.ZipFile(CPJ)
groups = {k: [norm(i) for i in v['items']] for k, v in tomllib.loads(cpz.read('templates/config/groups_def.toml').decode()).items()}
render = tomllib.loads(cpz.read('templates/config/render_order.toml').decode())['order']
# ores: receiver-only, never a source or target (nothing onto ores); prismarine (monument block) leaves cobblestones
for g in list(groups):
    keep = []
    for b in groups[g]:
        if re.search(r'_ore$', b) or b == 'minecraft:prismarine':
            continue
        keep.append(b)
    groups[g] = keep
groups['natural_sandstones'] = [b for b in groups['smooth_blocks'] if b in ('minecraft:sandstone', 'minecraft:red_sandstone')
                                or re.match(r'^[a-z_]+:[a-z_]*sandstone$', b) and not re.search(r'(smooth|cut|chiseled)_', b)]
groups['smooth_blocks'] = [b for b in groups['smooth_blocks'] if b not in groups['natural_sandstones']]
for g in ('obsidians', 'raw_ores', 'gems', 'crystals'):
    groups.pop(g, None)  # never sources; they are not targets of anything either
for g in groups.values():
    for b in TERRACOTTAS:
        if b in g:
            g.remove(b)
groups['terracottas'] = list(TERRACOTTAS)
for b, (g, _) in NEW.items():
    if g != 'terracottas' and b not in groups[g]:
        groups[g].append(b)
if 'minecraft:pale_moss_block' not in groups['mosses']:
    groups['mosses'].append('minecraft:pale_moss_block')
group_of = {}
for g, items in groups.items():
    for b in items:
        group_of.setdefault(b, g)

# ---- asset index over the game + mods ----
jars = [MCJ] + sorted(glob.glob(os.path.join(MODS, '*.jar')))
index = {}
for j in jars:
    try:
        z = zipfile.ZipFile(j)
    except zipfile.BadZipFile:
        continue
    for n in z.namelist():
        if n.startswith('assets/') and n not in index:
            index[n] = z
def asset(path):
    z = index.get(path)
    return z.read(path) if z else None
def exists_block(b):
    ns, p = b.split(':')
    return f'assets/{ns}/blockstates/{p}.json' in index
def sprite_png(spr):
    ns, p = norm(spr).split(':')
    return asset(f'assets/{ns}/textures/{p}.png')


def model_textures(model, seen=0):
    ns, p = norm(model).split(':')
    raw = asset(f'assets/{ns}/models/{p}.json')
    if raw is None or seen > 8:
        return {}
    d = json.loads(raw)
    tex = model_textures(d['parent'], seen + 1) if 'parent' in d else {}
    tex.update(d.get('textures', {}))
    return tex


def block_sprites(b):
    """All face sprites of a block's models (resolved #refs), plus the 'main' sprite for side and top."""
    ns, p = b.split(':')
    st = json.loads(asset(f'assets/{ns}/blockstates/{p}.json'))
    models = []
    for v in st.get('variants', {}).values():
        for m in (v if isinstance(v, list) else [v]):
            models.append(m['model'])
    sprites, main = [], {}
    for m in models:
        t = model_textures(m)
        def res(k):
            v = t.get(k)
            for _ in range(5):
                if isinstance(v, str) and v.startswith('#'):
                    v = t.get(v[1:])
            return norm(v) if isinstance(v, str) else None
        for k in ('all', 'side', 'end', 'top', 'texture', 'particle'):
            s = res(k)
            if s and s not in sprites and k != 'particle':
                sprites.append(s)
        main.setdefault('side', res('all') or res('side') or res('texture') or res('particle'))
        main.setdefault('top', res('all') or res('end') or res('top') or main['side'])
    return sprites, main


def ver(s):
    return tuple(int(x) for x in s.split('.')) if s else None


# ---- templates: logicalName -> chosen (png bytes, meta) for 1.20.1 ----
tmpl = {}
for n in cpz.namelist():
    m = re.match(r'^templates/textures/([a-z0-9_]+)/([a-z0-9_@.\-]+)\.png$', n)
    if not m:
        continue
    ns, base = m.groups()
    logical = base.split('@')[0]
    js = n[:-4] + '.json'
    meta = json.loads(cpz.read(js)) if js in cpz.namelist() else {}
    if not meta and '@' in base and f'templates/textures/{ns}/{logical}.json' in cpz.namelist():
        meta = json.loads(cpz.read(f'templates/textures/{ns}/{logical}.json'))
    lo, hi = ver(meta.get('minGameVersion')), ver(meta.get('maxGameVersion'))
    fits = (lo is None or lo <= MC) and (hi is None or MC <= hi)
    key = (ns, logical)
    mc = n + '.mcmeta'
    cand = (fits, '@' in base, cpz.read(n), meta, cpz.read(mc).decode() if mc in cpz.namelist() else None)
    if key not in tmpl or (cand[0], cand[1]) > (tmpl[key][0], tmpl[key][1]):
        tmpl[key] = cand

TILES = {0: [(0, 0)], 1: [(1, 0)], 2: [(2, 0)], 3: [(1, 0), (0, 1)], 4: [(1, 0), (2, 1)], 5: [(1, 0), (0, 1), (2, 1)],
         6: [(1, 0), (2, 1), (1, 2)], 7: [(0, 1)], 8: [(1, 0), (0, 1), (2, 1), (1, 2)], 9: [(2, 1)], 10: [(0, 1), (1, 2)],
         11: [(2, 1), (1, 2)], 12: [(0, 1), (1, 0), (1, 2)], 13: [(0, 1), (2, 1), (1, 2)], 14: [(0, 2)], 15: [(1, 2)],
         16: [(2, 2)]}  # Connected Paths ImageSlicer.OVERLAYS (sheet cell x,y per tile, composited SrcOver)


def slice_sheet(sheet):
    frames = sheet.height // 48
    out = {}
    for t, cells in TILES.items():
        im = Image.new('RGBA', (16, 16 * frames), (0, 0, 0, 0))
        for f in range(frames):
            for cx, cy in cells:
                cell = sheet.crop((cx * 16, f * 48 + cy * 16, cx * 16 + 16, f * 48 + cy * 16 + 16))
                im.alpha_composite(cell, (0, f * 16))
        out[t] = im
    a = np.array(sheet)[..., 3]
    return out, bool(((a > 0) & (a < 255)).any())


def recolor(donor_sheet, donor_tex, target_tex):
    """Donor sheet's alpha + shading ratio (sheet / donor's own texture) applied to the target block's texture."""
    d = np.array(donor_sheet.crop((0, 0, 48, 48))).astype(float)
    dt = np.array(donor_tex.convert('RGBA').crop((0, 0, 16, 16))).astype(float)
    tt = np.array(target_tex.convert('RGBA').crop((0, 0, 16, 16))).astype(float)
    dt3, tt3 = np.tile(dt, (3, 3, 1)), np.tile(tt, (3, 3, 1))
    ratio = np.clip(d[..., :3] / np.maximum(dt3[..., :3], 1), 0.6, 1.4)
    out = np.zeros_like(d)
    out[..., :3] = np.clip(tt3[..., :3] * ratio, 0, 255)
    out[..., 3] = d[..., 3]
    return Image.fromarray(out.round().astype(np.uint8), 'RGBA')


def receivers(b, g):
    items = groups[g]
    r = items[items.index(b) + 1:] if b in items else []
    for rg in RELATIONS.get(g, []):
        r += groups.get(rg, [])
    seen, outl = set(), []
    for x in r:
        if x not in seen and exists_block(x):
            seen.add(x); outl.append(x)
    return outl


def order_index(ns, tile):
    key = f'{ns}/{tile}'
    return len(render) - 1 - render.index(key) if key in render else 9999


files = {}
report = []
def emit(folder, sheet, emissive_sheet, mcmeta, props_fn):
    """Two property files per source: top/bottom faces get the full sheet; side faces get it with the top row
    (fringe arriving from the block above) cleared, so nothing seeps down walls and snow never doubles on block
    sides, while fringe from below and from the sides stays (PPM49a(2)/(3))."""
    side_sheet = blank_top(sheet)
    tiles, tr = slice_sheet(sheet)
    emit_one(folder, tiles, mcmeta, props_fn(tr) + 'faces=top bottom\n',
             slice_sheet(emissive_sheet)[0] if emissive_sheet else None)
    tiles, tr = slice_sheet(side_sheet)
    emit_one(folder + '_sides', tiles, mcmeta, props_fn(tr) + 'faces=sides\n',
             slice_sheet(blank_top(emissive_sheet))[0] if emissive_sheet else None)


def blank_top(sheet):
    a = np.array(sheet)
    for f in range(sheet.height // 48):
        a[f * 48:f * 48 + 16, :, 3] = 0
    return Image.fromarray(a, 'RGBA')


def emit_one(folder, tiles, mcmeta, props, emissive_tiles=None):
    base = f'assets/minecraft/optifine/ctm/_overlay/{folder}/'
    for t, im in tiles.items():
        buf = io.BytesIO(); im.save(buf, 'PNG'); files[base + f'{t}.png'] = buf.getvalue()
        if mcmeta:
            files[base + f'{t}.png.mcmeta'] = mcmeta.encode()
    for t, im in (emissive_tiles or {}).items():
        buf = io.BytesIO(); im.save(buf, 'PNG'); files[base + f'{t}_e.png'] = buf.getvalue()
        if mcmeta:
            files[base + f'{t}_e.png.mcmeta'] = mcmeta.encode()
    files[base + folder.split('_', 2)[2] + '.properties'] = props.encode()
    if all(np.array(im)[..., 3].max() == 0 for im in tiles.values()):
        raise SystemExit('empty tiles in ' + folder)


def props_text(recv, connect, translucent, meta):
    s = 'matchBlocks=%s\nmethod=overlay\nlayer=%s\ntiles=0-16\nconnectTiles=%s\n' % (
        ' '.join(recv), 'translucent' if translucent else 'cutout', ' '.join(connect))
    if meta.get('tintIndex') is not None:
        s += 'tintIndex=%s\n' % meta['tintIndex']
    if meta.get('tintBlock'):
        s += 'tintBlock=%s\n' % meta['tintBlock']
    return s


def as_list(v):
    return [] if v is None else (v if isinstance(v, list) else [v])


# 1) Connected Paths template sources (not emissive; emissive siblings ride along)
for (ns, logical), (fits, _, png, meta, mcmeta) in sorted(tmpl.items()):
    if logical.endswith('_e'):
        continue
    block = norm(meta.get('blockID') or f'{ns}:{logical}')
    g = group_of.get(block)
    if g not in SOURCE_GROUPS or block in NEW or not exists_block(block):
        continue
    if sprite_png(f'{ns}:block/{logical}') is None:
        continue
    recv = receivers(block, g)
    if not recv:
        continue
    connect = as_list(meta.get('connectTiles')) or [f'{ns}:{logical}']
    connect += as_list(meta.get('connectTilesAppend'))
    connect = [c for c in connect if sprite_png(c if '/' in c else c.replace(':', ':block/', 1)) is not None]
    sheet = Image.open(io.BytesIO(png)).convert('RGBA')
    emis = tmpl.get((ns, logical + '_e'))
    esheet = Image.open(io.BytesIO(emis[2])).convert('RGBA') if emis else None
    recv += [b for b in as_list(meta.get('matchBlocksAppend')) if exists_block(norm(b)) and norm(b) not in recv]
    folder = '%04d_%s_%s' % (order_index(ns, logical), ns, logical)
    emit(folder, sheet, esheet, mcmeta, lambda tr, recv=recv, connect=connect, meta=meta: props_text(recv, connect, tr, meta))
    report.append((g, block, logical, len(recv)))

# 2) Newly covered blocks: recolor the donor sheet onto each face texture of the block
for block, (g, donor) in sorted(NEW.items()):
    if not exists_block(block):
        print('missing block', block); continue
    dns, dname = donor.split('/')
    dpng = tmpl[(dns, dname)][2]
    dtex = Image.open(io.BytesIO(sprite_png(f'{dns}:block/{dname}')))
    sprites, main = block_sprites(block)
    recv = receivers(block, g)
    if not recv:
        continue
    faces = {'side': main['side']}
    if main['top'] and main['top'] != main['side']:
        faces['top'] = main['top']
    for face, spr in faces.items():
        donor_sheet = Image.open(io.BytesIO(dpng)).convert('RGBA')
        if face == 'top' and (dns, dname.replace('_side', '_top')) in tmpl:
            donor_sheet = Image.open(io.BytesIO(tmpl[(dns, dname.replace('_side', '_top'))][2])).convert('RGBA')
            dtex = Image.open(io.BytesIO(sprite_png(f'{dns}:block/' + dname.replace('_side', '_top'))))
        sheet = recolor(donor_sheet, dtex, Image.open(io.BytesIO(sprite_png(spr))))
        connect = [spr] if face == 'top' else [s for s in sprites if s != main['top'] or main['top'] == main['side']]
        ns, path = block.split(':')
        folder = '%04d_%s_%s' % (9999, 'councilcraft', f'{ns}_{path}' + ('_top' if face == 'top' else ''))
        emit(folder, sheet, None, None, lambda tr, recv=recv, connect=connect: props_text(recv, connect, tr, {}))
    report.append((g, block, 'recolor:' + donor, len(recv)))

files['assets/minecraft/optifine/emissive.properties'] = b'suffix.emissive=_e\n'
files['pack.mcmeta'] = json.dumps({'pack': {'pack_format': 15, 'description':
                                   'CouncilCraft blending (Continuity) - CC BY-NC-SA 4.0'}}, indent=2).encode()
files['README.txt'] = ('CouncilCraft blending pack (PPM49/49a/60), rendered by Continuity. Generated by tools/mkblending.py.\n'
                       'Overlay art: Connected Paths by Jacosvaldo, CC BY-NC-SA 4.0 (https://modrinth.com/mod/connected-paths-mod).\n'
                       'Changes: our own group pairings; tiles sliced from the original sheets; Create stones, Naturalist shellstone,\n'
                       'Supplementaries ashen basalt, Farmer\'s Delight rich soil, pale moss and terracottas are procedural recolors\n'
                       'of the original sheets onto each block\'s own texture. This pack is licensed CC BY-NC-SA 4.0:\n'
                       'https://creativecommons.org/licenses/by-nc-sa/4.0/\n').encode()
with zipfile.ZipFile(OUT, 'w', zipfile.ZIP_DEFLATED) as z:
    for n in sorted(files):
        zi = zipfile.ZipInfo(n, (2026, 10, 21, 0, 0, 0)); zi.compress_type = zipfile.ZIP_DEFLATED
        z.writestr(zi, files[n])
for r in report:
    print(*r)
print(len(report), 'sources;', len(files), 'files ->', OUT)
