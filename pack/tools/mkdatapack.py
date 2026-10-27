#!/usr/bin/env python3
"""Builds the councilcraft-data folder datapack (Moonlight global datapack, S248(g)) from the spec.
Inputs are read from the extracted mod jars (pack-work/x) and the vanilla 1.20.1 data (pack-work/vanilla).
Usage: mkdatapack.py <x-dir> <vanilla-dir> <out-dir>"""
import json, os, sys, glob, shutil, copy
X, VAN, OUT = sys.argv[1], sys.argv[2], sys.argv[3]
shutil.rmtree(OUT, ignore_errors=True)
notes = []
def jar(prefix):
    m = [d for d in os.listdir(X) if d.startswith(prefix)]; assert len(m) == 1, prefix; return os.path.join(X, m[0])
def rd(p): return json.load(open(p))
def w(rel, obj, why):
    p = os.path.join(OUT, 'data', rel); os.makedirs(os.path.dirname(p), exist_ok=True)
    json.dump(obj, open(p, 'w'), indent=2); notes.append(f'{rel}  # {why}')
def tag(vals, replace=False): return {'replace': replace, 'values': vals}
json.dump({'pack': {'pack_format': 15, 'description': 'CouncilCraft data (village removal, loot, cohesion, CEI rules) - MVP 0.2.0'}},
          open(os.path.join(os.makedirs(OUT, exist_ok=True) or OUT, 'pack.mcmeta'), 'w'), indent=2)

RU = jar('RegionsUnexploredFabric'); DT = jar('dungeons-and-taverns'); PG = jar('Paraglider'); II = jar('IllagerInvasion')
MM = jar('MutantMonsters'); FD = jar('FarmersDelight'); FF = jar('friendsandfoes'); NAT = jar('naturalist')
def ru_tag(ns, name):
    p = f'{RU}/data/{ns}/tags/worldgen/biome/{name}.json'
    return [v if isinstance(v, str) else v['id'] for v in rd(p)['values']] if os.path.exists(p) else []
def expand(vals, ns_dirs):
    out = []
    for v in vals:
        if v.startswith('#'):
            ns, n = v[1:].split(':'); 
            for d in ns_dirs:
                p = f'{d}/data/{ns}/tags/worldgen/biome/{n}.json'
                if os.path.exists(p): out += expand([x if isinstance(x, str) else x['id'] for x in rd(p)['values']], ns_dirs)
        else: out.append(v)
    return out

# ---------- RU overworld biomes and their analog vanilla biome (S248(j): analog mapping, pending Jayden)
RU_OVER = sorted(set(b for b in expand(ru_tag('minecraft', 'is_overworld'), [RU]) if b.startswith('regions_unexplored:')))
RU_NETHER = set(b for b in expand(ru_tag('minecraft', 'is_nether'), [RU]))
ctags = {}
for f in glob.glob(f'{RU}/data/c/tags/worldgen/biome/*.json') + glob.glob(f'{RU}/data/minecraft/tags/worldgen/biome/*.json'):
    n = os.path.splitext(os.path.basename(f))[0]
    for b in expand([v if isinstance(v, str) else v['id'] for v in rd(f)['values']], [RU]): ctags.setdefault(b, set()).add(n)
def analog(b):
    t = ctags.get(b, set()); n = b.split(':')[1]
    if 'cave' in t or 'caves' in t or 'underground' in t: return None
    if 'is_ocean' in t or 'ocean' in t or 'is_deep_ocean' in t: return 'minecraft:ocean'
    if 'is_beach' in t or 'beach' in t or 'shore' in t: return 'minecraft:beach'
    if 'is_river' in t or 'river' in t: return 'minecraft:river'
    if 'mushroom' in t: return 'minecraft:mushroom_fields'
    if 'swamp' in t: return 'minecraft:swamp'
    if 'desert' in t or 'deserts' in t or n in ('outback',): return 'minecraft:desert'
    if 'savanna' in t or 'is_savanna' in t: return 'minecraft:savanna'
    if 'is_jungle' in t or 'tree_jungle' in t: return 'minecraft:jungle'
    if ('snowy' in t or 'icy' in t) and ('is_taiga' in t or 'taiga' in t or 'coniferous' in t): return 'minecraft:snowy_taiga'
    if 'snowy' in t or 'icy' in t: return 'minecraft:snowy_plains'
    if 'is_taiga' in t or 'taiga' in t or 'coniferous' in t: return 'minecraft:taiga'
    if 'mountain_peak' in t or 'mountain_peaks' in t: return 'minecraft:stony_peaks'
    if 'is_mountain' in t or 'mountain' in t or 'mountain_slope' in t: return 'minecraft:windswept_hills'
    if 'spooky' in t or 'dark' in n: return 'minecraft:dark_forest'
    if 'meadow' in n: return 'minecraft:meadow'
    if 'is_forest' in t: return 'minecraft:forest'
    return 'minecraft:plains'
ANALOG = {b: analog(b) for b in RU_OVER}
w('councilcraft/tags/worldgen/biome/none.json', tag([]), 'empty biome tag used to switch structures off (S248(c))')
json.dump(ANALOG, open(os.path.join(OUT, 'ru-analog-map.json'), 'w'), indent=1)
def ru_for(vanilla_members):
    vm = set(vanilla_members)
    return sorted(b for b, a in ANALOG.items() if a in vm)

# ---------- S206/S244 B2/S248(c): village removal
for v in ['plains', 'desert', 'savanna', 'snowy', 'taiga']:
    w(f'minecraft/tags/worldgen/biome/has_structure/village_{v}.json', tag([], True), 'S206 empty village tag')
for rel in ['minecraft/worldgen/structure/village_taiga.json', 'nova_structures/worldgen/structure/village_jungle.json', 'nova_structures/worldgen/structure/village_swamp.json']:
    s = rd(f'{DT}/data/{rel}'); s['biomes'] = '#councilcraft:none'; w(rel, s, 'S244 B2/S248(c) D&T village copy with no biomes')
# ---------- S244 B1: Paragliders statues
for s in ['underground_horned_statue', 'nether_horned_statue', 'tarrey_town_goddess_statue']:
    w(f'paraglider/tags/worldgen/biome/has_structure/{s}.json', tag([], True), 'S244 B1 statue structures off')
# ---------- S157/S158: Illager Invasion pools
p = rd(f'{II}/data/illagerinvasion/worldgen/template_pool/mobs/sorcerer.json'); p['elements'] = []
w('illagerinvasion/worldgen/template_pool/mobs/sorcerer.json', p, 'S157 sorcerer pool emptied')
p = rd(f'{II}/data/illagerinvasion/worldgen/template_pool/mobs/necromancer.json'); p['elements'][0]['element']['location'] = 'illagerinvasion:mobs/vindicator'
w('illagerinvasion/worldgen/template_pool/mobs/necromancer.json', p, 'S158 necromancer -> vindicator')

# ---------- S156/S177/S242 mutant loot (player kill; above-max rewards as books)
KP = [{'condition': 'minecraft:killed_by_player'}]
def item(i, lo=None, hi=None, extra=None):
    e = {'type': 'minecraft:item', 'name': i, 'functions': []}
    if lo is not None: e['functions'].append({'function': 'minecraft:set_count', 'count': lo if hi is None else {'type': 'minecraft:uniform', 'min': lo, 'max': hi}})
    if extra: e['functions'] += extra
    if not e['functions']: del e['functions']
    return e
def pool(e, chance=None, cond=KP):
    c = list(cond) + ([{'condition': 'minecraft:random_chance', 'chance': chance}] if chance else [])
    return {'rolls': 1, 'entries': [e], 'conditions': c}
def book(ench, lvl): return item('minecraft:book', extra=[{'function': 'minecraft:set_nbt', 'tag': '{StoredEnchantments:[{id:"%s",lvl:%ds}]}' % (ench, lvl)}, {'function': 'minecraft:set_item', 'item': 'minecraft:enchanted_book'}] if False else
    [{'function': 'minecraft:set_enchantments', 'enchantments': {ench: lvl}}])
def ent(rel, pools, why): w(f'{rel}', {'type': 'minecraft:entity', 'pools': pools}, why)
ent('mutantmonsters/loot_tables/entities/mutant_zombie.json', [pool(item('minecraft:iron_block', 2, 4)), pool(item('minecraft:rotten_flesh', 8, 16)),
    pool(item('illagerinvasion:unusual_dust', 1, 2), 0.30), pool(book('minecraft:sharpness', 6), 0.05)], 'S156/S177 mutant zombie (no Hulk Hammer)')
ent('mutantmonsters/loot_tables/entities/mutant_skeleton.json', [pool(item('mutantmonsters:mutant_skeleton_skull'), 0.50), pool(item('minecraft:bone_block', 3, 6)),
    pool(item('minecraft:arrow', 8, 16)), pool(book('minecraft:power', 6), 0.05)], 'S154/S156 mutant skeleton')
for part in ['limb', 'pelvis', 'rib', 'shoulder_pad']:
    ent(f'mutantmonsters/loot_tables/entities/mutant_skeleton/{part}.json', [], 'S154 body-part table emptied (skull only)')
ent('mutantmonsters/loot_tables/entities/mutant_creeper.json', [pool(item('minecraft:gunpowder', 27, 54)), pool(item('smallships:cannon_ball', 4, 8), 0.40),
    pool(item('friendsandfoes:music_disc_around_the_corner'), 0.10), pool(book('minecraft:blast_protection', 5), 0.05)], 'S156 mutant creeper (no minion eggs/shard)')
ent('mutantmonsters/loot_tables/entities/mutant_enderman.json', [pool(item('minecraft:ender_pearl', 8, 16)), pool(item('minecraft:ender_eye'), 0.50),
    pool(item('illagerinvasion:illusionary_dust', 1, 3), 0.35), pool(item('illagerinvasion:hallowed_gem'), 0.05), pool(book('minecraft:protection', 5), 0.05)], 'S156 mutant enderman (no Endersoul Hand)')
ent('mutantmonsters/loot_tables/entities/mutant_enderman_continuous.json', [], 'S156 endersoul fragments removed')
ent('mutantmonsters/loot_tables/entities/mutant_snow_golem.json', [pool(item('minecraft:snowball', 8, 16)), pool(item('friendsandfoes:penguin_feather', 1, 2), 0.30),
    pool(item('naturalist:fur', 1, 2), 0.20)], 'S156/S242 mutant snow golem')
inv = rd(f'{II}/data/illagerinvasion/loot_tables/entities/invoker.json')
s = json.dumps(inv); assert 'primal_essence' in s
for pl in inv['pools']: pl['entries'] = [e for e in pl['entries'] if 'primal_essence' not in json.dumps(e)]
inv['pools'] = [pl for pl in inv['pools'] if pl['entries']]
w('illagerinvasion/loot_tables/entities/invoker.json', inv, 'S156 Primal Essence out of Invoker drops')
lab = rd(f'{II}/data/illagerinvasion/loot_tables/chests/labyrinth_map.json'); assert 'lost_candle' in json.dumps(lab)
for pl in lab['pools']: pl['entries'] = [e for e in pl['entries'] if 'lost_candle' not in json.dumps(e)]
lab['pools'] = [pl for pl in lab['pools'] if pl['entries']]
w('illagerinvasion/loot_tables/chests/labyrinth_map.json', lab, 'S82 Lost Candle removed')

# ---------- S244 C4 / S248(b) / PM43 1c: Heave Ho disc at 0.5% (was 2%) in simple_dungeon + YUNG's Better Dungeons chest tables
disc = {'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'supplementaries:music_disc_heave_ho'}], 'conditions': [{'condition': 'minecraft:random_chance', 'chance': 0.005}]}
t = rd(f'{VAN}/data/minecraft/loot_tables/chests/simple_dungeon.json'); t['pools'].append(disc); w('minecraft/loot_tables/chests/simple_dungeon.json', t, 'S248(b)/PM43 1c Heave Ho 0.5%')
BD = jar('YungsBetterDungeons')
for f in sorted(glob.glob(f'{BD}/data/betterdungeons/loot_tables/*/chests/*.json')):
    if 'nether' in f: continue
    t = rd(f); t['pools'].append(copy.deepcopy(disc)); w(f.split('/data/')[1], t, 'S248(b)/PM43 1c Heave Ho 0.5%')

# ---------- PM2a: loot-only enchanting
# fishing treasure without the enchanted book entry (the enchanted bow/rod stay)
t = rd(f'{VAN}/data/minecraft/loot_tables/gameplay/fishing/treasure.json'); n0 = len(t['pools'][0]['entries'])
t['pools'][0]['entries'] = [e for e in t['pools'][0]['entries'] if e.get('name') != 'minecraft:book']; assert len(t['pools'][0]['entries']) == n0 - 1
w('minecraft/loot_tables/gameplay/fishing/treasure.json', t, 'PM2a no enchanted books from fishing')
# D&T illager manor libraries: enchanting tables become bookshelves. D&T is ARR, so instead of shipping edited structure NBTs
# we override the processor list those pieces already use (copied from D&T 3.0.3, one rule prepended).
pl = rd(f'{DT}/data/minecraft/worldgen/processor_list/illager_manor_generic_degradation.json')
pl['processors'].insert(0, {'processor_type': 'minecraft:rule', 'rules': [{'input_predicate': {'predicate_type': 'minecraft:block_match', 'block': 'minecraft:enchanting_table'},
    'location_predicate': {'predicate_type': 'minecraft:always_true'}, 'output_state': {'Name': 'minecraft:bookshelf'}}]})
w('minecraft/worldgen/processor_list/illager_manor_generic_degradation.json', pl, 'PM2a D&T manor library enchanting tables -> bookshelves')
# ---------- S227/S245: FD rope -> Supplementaries rope in FD chest loot
for f in glob.glob(f'{FD}/data/farmersdelight/loot_tables/chests/*.json'):
    s = open(f).read()
    if 'farmersdelight:rope' in s: w(f.split('/data/')[1], json.loads(s.replace('farmersdelight:rope', 'supplementaries:rope')), 'S245 rope loot swap')
# ---------- S229: knife bonus drops (porting_lib GLMs, same form as FD's own)
GLM = []
for g in ['medium_grass', 'steppe_grass', 'steppe_tall_grass', 'bladed_grass', 'bladed_tall_grass', 'sandy_grass', 'sandy_tall_grass', 'windswept_grass', 'frozen_grass']:
    w(f'councilcraft/loot_modifiers/straw_from_{g}.json', {'type': 'farmersdelight:add_item', 'conditions': [
        {'condition': 'minecraft:match_tool', 'predicate': {'tag': 'farmersdelight:straw_harvesters'}},
        {'condition': 'minecraft:random_chance', 'chance': 0.2},
        {'condition': 'minecraft:block_state_property', 'block': f'regions_unexplored:{g}'}], 'item': 'farmersdelight:straw'}, 'S229 RU grass straw')
    GLM.append(f'councilcraft:straw_from_{g}')
def knife(mob, it):
    return {'type': 'farmersdelight:add_item', 'conditions': [
        {'condition': 'minecraft:entity_properties', 'entity': 'killer', 'predicate': {'equipment': {'mainhand': {'tag': 'farmersdelight:tools/knives'}}}},
        {'condition': 'minecraft:entity_properties', 'entity': 'this', 'predicate': {'type': mob}}], 'item': it}
w('councilcraft/loot_modifiers/scavenging_feather_from_duck.json', knife('naturalist:duck', 'minecraft:feather'), 'S229 duck -> feather'); GLM.append('councilcraft:scavenging_feather_from_duck')
w('councilcraft/loot_modifiers/scavenging_ham_from_boar.json', knife('naturalist:boar', 'farmersdelight:ham'), 'S229 boar -> ham'); GLM.append('councilcraft:scavenging_ham_from_boar')
w('forge/loot_modifiers/global_loot_modifiers.json', {'replace': False, 'entries': GLM}, 'S229 GLM registration (porting_lib)')
notes.append('S229/S248(e): deer already drops leather in naturalist:entities/deer, no knife leather added')

# ---------- S204: RU climate tags into FD wild-crop tags (overworld only)
def ru_list(name): return sorted(b for b in expand(ru_tag('c', name), [RU]) if b in RU_OVER)
w('farmersdelight/tags/worldgen/biome/wild_tomatoes_whitelist.json', tag(ru_list('hot')), 'S204 c:hot -> wild tomatoes')
w('farmersdelight/tags/worldgen/biome/wild_rice_whitelist.json', tag(ru_list('wet')), 'S204 c:wet -> wild rice')
mush = ru_list('mushroom')
w('farmersdelight/tags/worldgen/biome/has_brown_mushroom_colony.json', tag(mush), 'S204 c:mushroom -> mushroom colonies')
w('farmersdelight/tags/worldgen/biome/has_red_mushroom_colony.json', tag(mush), 'S204 c:mushroom -> mushroom colonies')

# ---------- S138/S145 structure cohesion: RU biomes into YUNG's / Illager Invasion / F&F tags
def add_ru_to(tagfile_dir, rel, mode, why):
    src = f'{tagfile_dir}/data/{rel}'
    members = expand([v if isinstance(v, str) else v['id'] for v in rd(src)['values']], [tagfile_dir, RU])
    have = set(members)
    if mode == 'all': add = [b for b in RU_OVER if ANALOG[b] and b not in have] if 'dungeon' not in rel and 'stronghold' not in rel else [b for b in RU_OVER if b not in have]
    else: add = [b for b in ru_for(members) if b not in have]
    if add: w(rel, tag(add), why + f' (+{len(add)} RU)')
for m, rel in [('YungsBetterDungeons', f'betterdungeons/tags/worldgen/biome/has_structure/{n}.json') for n in ['small_dungeon', 'skeleton_dungeon', 'spider_dungeon', 'zombie_dungeon']] + \
              [('YungsBetterStrongholds', 'betterstrongholds/tags/worldgen/biome/has_structure/better_stronghold.json')]:
    add_ru_to(jar(m), rel, 'all', 'S145 YUNG all RU overworld')
for m, rel in [('YungsBetterDesertTemples', 'betterdeserttemples/tags/worldgen/biome/has_structure/better_desert_temple.json'),
               ('YungsBetterJungleTemples', 'betterjungletemples/tags/worldgen/biome/has_structure/better_jungle_temple.json'),
               ('YungsBetterWitchHuts', 'betterwitchhuts/tags/worldgen/biome/has_structure/better_witch_hut.json'),
               ('YungsBetterOceanMonuments', 'betteroceanmonuments/tags/worldgen/biome/has_structure/better_ocean_monument.json')]:
    add_ru_to(jar(m), rel, 'analog', 'S145 YUNG thematic RU match')
# mineshaft variants: each RU biome missing from every variant goes to the variant of its analog vanilla biome
BM = jar('YungsBetterMineshafts'); vt = {}
for f in glob.glob(f'{BM}/data/bettermineshafts/tags/worldgen/biome/has_structure/*.json'):
    vt[os.path.basename(f)] = set(expand([v if isinstance(v, str) else v['id'] for v in rd(f)['values']], [BM, RU]))
allm = set().union(*vt.values()); addm = {}
for b in RU_OVER:
    if b in allm or not ANALOG[b]: continue
    a = ANALOG[b]
    var = next((k for k, s in vt.items() if a in s), 'better_mineshaft_oak.json')
    addm.setdefault(var, []).append(b)
for var, bs in addm.items(): w(f'bettermineshafts/tags/worldgen/biome/has_structure/{var}', tag(sorted(bs)), f'S145 mineshaft variant (+{len(bs)} RU)')
for f in glob.glob(f'{II}/data/illagerinvasion/tags/worldgen/biome/has_structure/*.json'):
    add_ru_to(II, f.split('/data/')[1], 'analog', 'S138 Illager Invasion structure RU match')
for f in glob.glob(f'{FF}/data/friendsandfoes/tags/worldgen/biome/has_*.json'):
    add_ru_to(FF, f.split('/data/')[1], 'analog', 'S138 F&F mob spawn RU match')

# ---------- S242/S244 spawn additions (Lithostitched biome modifiers, S231 rule)
def spawns(name, biomes, sp, why):
    w(f'councilcraft/lithostitched/worldgen_modifier/{name}.json', {'type': 'lithostitched:add_biome_spawns', 'biomes': biomes, 'spawners': sp}, why)
catb = ['minecraft:plains', 'minecraft:sunflower_plains', 'minecraft:meadow', 'minecraft:forest', 'minecraft:flower_forest', 'minecraft:birch_forest'] + \
       sorted(b for b, a in ANALOG.items() if a in ('minecraft:plains', 'minecraft:forest', 'minecraft:meadow'))
spawns('cats_rare', catb, [{'type': 'minecraft:cat', 'weight': 1, 'minCount': 1, 'maxCount': 1}], 'S242 rare natural cats')
# PPM94: camels_rare removed; Vanilla Backport's camel_spawns=true is the only natural camel source
# PPM96: pale garden stays empty. Naturalist 5.0pre4 Fabric never reads its blacklist/* biome tags (defined, unused) and adds
# its spawns through Fabric BiomeModifications at MinecraftServer.<init>; Lithostitched removals run later (initServer, before
# loadLevel), so strip every Naturalist spawn plus Naturalist's is_forest rabbit/fox from minecraft:pale_garden here.
NAT_SPAWNS = ['bear', 'deer', 'snail', 'firefly', 'butterfly', 'snake', 'rattlesnake', 'coral_snake', 'bluejay', 'canary', 'cardinal', 'robin', 'finch', 'sparrow', 'rhino', 'lion', 'elephant', 'zebra', 'giraffe', 'hippo', 'vulture', 'boar', 'dragonfly', 'catfish', 'alligator', 'bass', 'lizard', 'tortoise', 'duck']
w('councilcraft/lithostitched/worldgen_modifier/pale_garden_no_naturalist.json', {'type': 'lithostitched:remove_biome_spawns', 'biomes': 'minecraft:pale_garden',
  'mobs': ['naturalist:' + n for n in NAT_SPAWNS] + ['minecraft:rabbit', 'minecraft:fox']}, 'PPM96 pale garden: no Naturalist fauna (vanilla pale garden has no creatures)')
spawns('turtles_beach', '#minecraft:is_beach', [{'type': 'minecraft:turtle', 'weight': 5, 'minCount': 2, 'maxCount': 5}], 'S244 F2 beach turtles')
spawns('evokers_natural', ['minecraft:dark_forest', 'regions_unexplored:blackwood_taiga', 'regions_unexplored:ashen_woodland', 'regions_unexplored:mauve_hills'],
       [{'type': 'minecraft:evoker', 'weight': 2, 'minCount': 1, 'maxCount': 1}], 'S119/S121/S122 natural evokers weight 2')
notes.append('S242 patrol evoker: implemented in the councilcraft Java mod (PatrolEvokerMixin), so batch20 renewable-totems mcfunctions are NOT shipped')

# ---------- S244 G7 advancements: superseded in 0.4.18 by PT35/PT37/PT42, generated by tools/mkadvancements.py
# (run it after this script; it appends its own section to CONTENTS.md).

# ---------- S177 CEI data maps
CEI = jar('create-enchantment-industry')
rules = rd(f'{CEI}/data/create_enchantment_industry/data_maps/enchantment/enchantment_processing/rules.json')
for e in ['minecraft:sharpness', 'minecraft:power', 'minecraft:blast_protection', 'minecraft:protection']:
    rules['values'][e] = {'level_extension': {'blaze_enchanter': 0, 'blaze_forger': 1}}
w('create_enchantment_industry/data_maps/enchantment/enchantment_processing/rules.json', rules, 'S177 forger +1 for 4 enchants; Infinity/Mending rules kept')
w('create_enchantment_industry/data_maps/enchantment/printing/enchanted_book/custom_cost.json', {'values': {
    'minecraft:sharpness': [{'level': 6, 'value': 1000000}], 'minecraft:power': [{'level': 6, 'value': 1000000}],
    'minecraft:blast_protection': [{'level': 5, 'value': 1000000}], 'minecraft:protection': [{'level': 5, 'value': 1000000}]}}, 'S177 printer cannot copy above-max books')

# ---------- S245 tags: alligator/tortoise eggs out of #c:eggs and #forge:eggs is done in KubeJS (tag event) to keep removal semantics
open(os.path.join(OUT, 'CONTENTS.md'), 'w').write('# councilcraft-data contents (generated by pack/tools/mkdatapack.py)\n\n' + '\n'.join('- ' + n for n in notes) + '\n')
print(len(notes), 'files/notes'); print('RU overworld biomes', len(RU_OVER))
