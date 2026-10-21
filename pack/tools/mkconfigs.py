#!/usr/bin/env python3
"""Rebuild CouncilCraft pack configs from generated mod defaults (2026-10-19 MVP rebuild).
Every key is asserted to exist in the generated default before it is changed (S248(d) rule).
Usage: mkconfigs.py <defaults-dir> <out-config-dir>"""
import json, re, sys, os, shutil
SRC, OUT = sys.argv[1], sys.argv[2]
log = []
def src(p): return os.path.join(SRC, p)
def out(p):
    o = os.path.join(OUT, p); os.makedirs(os.path.dirname(o), exist_ok=True); return o
def lit(v):
    if isinstance(v, bool): return 'true' if v else 'false'
    if isinstance(v, str): return json.dumps(v)
    if isinstance(v, list): return '[' + ', '.join(lit(x) for x in v) + ']'
    return repr(v)
def toml(path, changes):
    """changes: list of (section or None, key, value). section = dotted header text without brackets"""
    txt = open(out(path) if os.path.exists(out(path)) else src(path)).read().split('\n')
    for sec, key, val in changes:
        cur = None; hit = False
        for i, l in enumerate(txt):
            m = re.match(r'^\s*\[(.+)\]\s*$', l)
            if m: cur = m.group(1).strip(); continue
            m = re.match(r'^(\s*)(' + re.escape(key) + r'|"' + re.escape(key) + r'")\s*=', l)
            if m and (sec is None or cur == sec):
                txt[i] = f'{m.group(1)}{m.group(2)} = {lit(val)}'; hit = True; break
        assert hit, f'{path}: key {sec}.{key} not found in generated default'
        log.append(f'{path} [{sec or ""}] {key} = {lit(val)}')
    open(out(path), 'w').write('\n'.join(txt))
def js(path, changes, cfgform=False):
    p = out(path) if os.path.exists(out(path)) else src(path)
    d = json.load(open(p))
    for keypath, val in changes:
        o = d; ks = keypath.split('/')
        for k in ks[:-1]: o = o[k]
        assert ks[-1] in o, f'{path}: key {keypath} not found'
        if cfgform and isinstance(o[ks[-1]], dict) and 'input' in o[ks[-1]]: o[ks[-1]]['input'] = val
        else: o[ks[-1]] = val
        log.append(f'{path} {keypath} = {json.dumps(val)[:120]}')
    json.dump(d, open(out(path), 'w'), indent=2)
def props(path, changes):
    txt = open(src(path)).read().split('\n')
    for key, val in changes:
        found = False
        for i, l in enumerate(txt):
            if re.match(r'^#?\s*' + re.escape(key) + r'\s*=', l) and not l.startswith('#  '):
                txt[i] = f'{key}={val}'; found = True; break
        if not found:
            # key documented only as a comment (e.g. ModernFix lists defaults as "#  key=true # (default)")
            assert any(key in l for l in txt), f'{path}: {key} not documented in generated default'
            txt.append(f'{key}={val}')
        log.append(f'{path} {key}={val}')
    open(out(path), 'w').write('\n'.join(txt))

PASSIVES = json.load(open(os.path.join(os.path.dirname(__file__), 'passives.json')))

# --- S248(a) deadlock fix
props('modernfix-mixins.properties', [('mixin.perf.reduce_blockstate_cache_rebuilds', 'false')])
# --- S148/S244 G1/G2 Paragliders
toml('paraglider-server.toml', [(None,'ascendingWinds',False),(None,'paragliderDurability',384),
    ('spiritOrbs','spiritOrbLoots',False),('spiritOrbs','spawnerSpiritOrbDrops',0),('spiritOrbs','raidGivesVessel',False),
    ('spiritOrbs','enderDragonDropsVessel',False)])
toml('paraglider-server.toml', [(None,'maxStamina',1200),(None,'startingStamina',1200),(None,'maxStaminaVessels',0),
    (None,'runningAndSwimmingConsumesStamina',False)])
toml('paraglider-common.toml', [(None,'heartContainers',False),(None,'spiritOrbGens',False),(None,'staminaVessels',False)])
# --- S163/S177 CEI
toml('create_enchantment_industry-server.toml', [(None,'blazeEnchanterMaxLevelExtension',0),(None,'blazeForgerMaxLevelExtension',0),(None,'ignoreEnchantmentCompatibility',False)])
# --- S84/S143/S150/S242 Small Ships (MaxSpeed mapped linearly 25..35 -> 78..90; accel + rotation accel x3; wind off; vanilla boats 1.0)
ss = []
for ship, d in {'Cog':27,'Brigg':30,'Galley':35,'Drakkar':28,'Galleon':25,'Dhow':32,'Caravel':32}.items():
    ss.append((None, f'shipAttribute{ship}MaxSpeed', round(78 + (d-25)*1.2, 1)))
txt = open(src('smallships-server.toml')).read()
for ship in ['Cog','Brigg','Galley','Drakkar','Galleon','Dhow','Caravel']:
    for k in ['Acceleration','RotationAcceleration']:
        m = re.search(rf'shipAttribute{ship}{k}\s*=\s*([0-9.]+)', txt); ss.append((None, f'shipAttribute{ship}{k}', round(float(m.group(1))*3, 4)))
ss += [(None,'windEnable',False),(None,'vanillaBoatSpeedFactor',1.0)]
toml('smallships-server.toml', ss)
# --- S248(f) Tectonic companion values from the v4 README (pending Jayden)
js('tectonic.json', [('global_terrain/elevation_boost',14.0),('global_terrain/vertical_scale',1.255),('global_terrain/max_y',2000),
    ('global_terrain/ultrasmooth',True),('continents/flat_terrain_skew',0.6)])
# --- S84/S135 Friends & Foes
js('friendsandfoes.json', [('enableIllusioner',False),('enableIllusionerSpawn',False),('enableIllusionerInRaids',False),
    ('generateIllusionerShackStructure',False),('generateIllusionerTrainingGroundsStructure',False),
    ('enableBeekeeperVillagerProfession',False),('generateBeekeeperAreaStructureInVillages',False)])
# --- S82/S157/S158 Illager Invasion
toml('illagerinvasion-server.toml', [('general','platinum_trim_effects',False),('sorcerer','participate_in_raids',False),('necromancer','participate_in_raids',False)])
# --- S166 Mutant Monsters
toml('mutantmonsters-server.toml', [(None,'mutant_x_conversions',['minecraft:snow_golem,mutantmonsters:mutant_snow_golem'])])
# --- S84/S98/S129 RpgDifficulty
js('rpgdifficulty.json', [('increasingDistance',250),('distanceFactor',0.1),('timeFactor',0.0),('heightFactor',0.0),
    ('maxFactorHealth',4.0),('maxFactorDamage',4.0),('maxFactorProtection',1.5),('bossTimeFactor',0.0),('dynamicBossModificator',0.3),
    ('excludeDistanceInOtherDimension',False),('excludedEntity',PASSIVES)])
# --- S83/S98/S242 Nameplate
d = json.load(open(src('nameplate.json')))
js('nameplate.json', [('excludedEntities', d['excludedEntities'] + [p for p in PASSIVES if p not in d['excludedEntities']])])
# --- S85/S86 Improved Mobs
im = json.load(open(src('improvedmobs/common.json')))
ec = im['Entity Configs']['input'] + ['mutantmonsters', 'illagerinvasion']
incr = [k for k in im if k.endswith(' Increase') and k != 'Difficulty Increase']
js('improvedmobs/common.json', [('Enable difficulty scaling',False),('Item Equip Chance',0.25),('Guardian Chance',0.05),('Phantom Chance',0.05),
    ('Weapon Chance',0.2),('Flag Blacklist',['BLOCKBREAK','STEAL']),('Breaker Chance',0.0),('Stealer Chance',0.0),
    ('Neutral Aggressive Chance',0.0),('Entity Configs',ec)] + [(k,0.0) for k in incr], cfgform=True)
# --- S74/S75 Forgotten Graves
js('forgottengraves.json', [('experience/percentage',50),('server/graveRobbing',True)])
# --- S168 Hardcore Revival
toml('hardcorerevival-common.toml', [(None,'continueTimerWhileOffline',True)])
# --- S107/S139 Immersive Portals
js('immersive_portals.json', [('enableAlternateDimensions',False)])
# --- S60/S134/S172 Vanilla Backport (common)
toml('vanillabackport-common.toml', [(None,'dried_ghasts',False),(None,'do_sulfur_cubes_explode',False),(None,'do_geysers_erupt',True)])
# --- S205 Farmer's Delight
toml('farmersdelight-common.toml', [(None,'enableVanillaCropCrates',True),(None,'enableVanillaSoupExtraEffects',False),(None,'enableRabbitStewJumpBoost',False),(None,'generateFDChestLoot',True)])
# --- S167/S242 Create trains (~36 / 48 powered)
toml('create-server.toml', [(None,'trainTopSpeed',36.0),(None,'poweredTrainTopSpeed',48.0)])
# --- S93 Horseman
toml('horseman-common.toml', [(None,'increase_horse_airborne_speed',False)])
# --- S183 SVC (stock value, locked explicitly)
props('voicechat/voicechat-server.properties', [('allow_recording','true')])
# --- S178 Surveyor
toml('surveyor.toml', [('networking','globalSharing',False),('networking','waypoints','SOLO'),('networking','positions','SOLO'),
    ('networking','terrain','SOLO'),('networking','structures','SOLO'),('networking','landmarks','SOLO')])
# --- S192..S244 Supplementaries
js('supplementaries-common.json', [
    ('tools/altimeter',False),('tools/bomb/enabled',False),('functional/safe/enabled',False),('redstone/lock_block',False),
    ('tweaks/traders_open_doors/enabled',False),('general/red_merchant_spawn_multiplier',0.0),
    ('tools/wrench/enabled',False),('redstone/cog_block',False),('redstone/pulley_block/enabled',False),
    ('tools/bubble_blower/enabled',False),('functional/lumisene/enabled',False),('redstone/spring_launcher/enabled',False),
    ('tweaks/noteblocks_scare/enabled',False),('tweaks/slimed_effect/enabled',False),('tweaks/slimed_effect/throwable_slimeballs',False),
    ('functional/cannon/enabled',False),('functional/cannon/cannonball/enabled',False),('functional/cannon/music_disc_heave_ho',True),
    ('tweaks/map_tweaks/random_adventurer_maps',False),('functional/bamboo_spikes/tipped_spikes',False),
    ('tweaks/dragon_banner_pattern/enabled',False),('building/netherite_doors/door',False),('building/netherite_doors/trapdoor',False),
    ('tools/slingshot/enabled',True),('tools/slingshot/allow_enderpearls',False),('tools/slingshot/allow_fire_charges',True),
    ('tools/slingshot/allow_splash_potions',True),('tools/slingshot/allow_snowballs',True),('tools/slingshot/allow_bombs',False),
    ('functional/sack/enabled',True),('functional/sack/sack_penalty',True),('functional/sack/sack_increment',2),('functional/sack/slots',9),
    ('tweaks/throwable_bricks/enabled',True),('tweaks/placeable_gunpowder/enabled',True),('building/ash/enabled',True),
    ('functional/flax/enabled',True),('functional/fodder',True),('tools/flute/enabled',True),('tools/quiver/enabled',True),
    ('tools/lunch_basket/enabled',True),('tools/rope_arrow/enabled',True),('building/pancake',True),('tools/candy',True),
    ('building/sugar_cube/enabled',True),('tools/antique_ink/enabled',True),('functional/soap/enabled',True),('tools/confetti_popper',True),
    ('functional/urn/enabled',True),('tools/slice_map/enabled',True)])
# --- S193/S197/S201/S211 Amendments (common)
js('amendments-common.json', [('fireball/gravity',False),('fireball/deflectable',False),('fireball/fire_charges_throwable',False),
    ('fireball/fire_charges_dispenser_behavior',False),('fireball/improved_explosions',False),('snowball/freeze_ticks',0),
    ('torch/torch_fire',False),('lantern/falling_lanterns','OFF')])
# --- S136/S164 Carry On
co = json.load(open(src('carryon-common.json')))
ns = ['railways','create_enchantment_industry','create_dragons_plus','numismatics','interiors','createrailwaysnavigator','forgottengraves','immersive_portals','smallships']
bl = co['blacklist']
ft = bl['forbiddenTiles'] + [n+':*' for n in ns if n+':*' not in bl['forbiddenTiles']]
fe = bl['forbiddenEntities'] + [n+':*' for n in ns if n+':*' not in bl['forbiddenEntities']]
js('carryon-common.json', [('settings/pickupPlayers',False),('blacklist/forbiddenTiles',ft),('blacklist/forbiddenEntities',fe)])
# --- S123 Numismatics defaultCoin (key lives in common, audit/config-review.md)
toml('numismatics-common.toml', [(None,'defaultCoin','SPUR')])
# --- S246 Ponderer lockdown (server)
toml('ponderer-server.toml', [(None,'enableBlueprintItem',False),(None,'enableProjector',False)])

# --- S170 updaters/telemetry off; DCIntegration Discord /stop and /kill removed
toml('Discord-Integration.toml', [('general','enableUpdateChecker',False),('bstats','sendAddonStats',False)])
L = open(out('Discord-Integration.toml')).read().split('\n'); res=[]; i=0; dropped=0
while i < len(L):
    if L[i].strip()=='[[commands.customCommands]]' and re.match(r'\s*name = "(stop|kill)"', L[i+1]):
        i += 1; dropped += 1
        while i < len(L) and L[i].strip()!='[[commands.customCommands]]' and not re.match(r'^[#\[]', L[i]): i += 1
        continue
    res.append(L[i]); i += 1
assert dropped == 2
open(out('Discord-Integration.toml'), 'w').write('\n'.join(res)); log.append('Discord-Integration.toml: removed customCommands stop, kill')
js('bstats.json', [('enabled', False)])
toml('DistantHorizons.toml', [('client.advanced.autoUpdater','enableAutoUpdater',False)])
# --- S185/S212 Mod Whitelist: BLACKLIST mode
BLOCK = ['meteor-client','wurst','liquidbounce','bleachhack','thunderhack','meteoriteclient','nicewurst','aristois','impact','inertia','coffeeclient','coffee','future',
 'baritone','baritone-meteor','freecam','freecam_by_jasonzli','legacyfreecam','cameratweaks','tweakeroo','litematica_printer','xtoxray','xray-orevision','xray','advanced-xray','xraymod',
 'autoclicker-fabric','targetespmod','chestesp','autoelytra','elytrafly','fullbright','gammautils']
js('mod_whitelist-config.json', [('USE_WHITELIST_ONLY', False), ('CLIENT_MOD_BLACKLIST', BLOCK)])
# === 0.4.2 (2026-10-20): post-MVP LOCKED items. Applied in place to the shipped configs (SRC = OUT = config). ===
# --- PM4 / S165: DH generation capped to the 20k pregen (Chebyshev radius 10,000 blocks = 625 chunks around 0,0)
toml('DistantHorizons.toml', [('common.worldGenerator','generationMaxChunkRadius',625)])
# --- PM13: Chunky on the client blocklist (DH 3.3.3 + Chunky crash; pregen uses /dh pregen)
mw = json.load(open(out('mod_whitelist-config.json') if os.path.exists(out('mod_whitelist-config.json')) else src('mod_whitelist-config.json')))
js('mod_whitelist-config.json', [('CLIENT_MOD_BLACKLIST', mw['CLIENT_MOD_BLACKLIST'] + [m for m in ['chunky'] if m not in mw['CLIENT_MOD_BLACKLIST']])])
# --- PM20 / PM20a: lumisene bottle off, crank off, cauldron dye water off, no lead slot when hitching
js('supplementaries-common.json', [('functional/lumisene/lumisene_bottle/enabled',False),('redstone/crank',False)])
js('amendments-common.json', [('cauldron/dye_water',False)])
toml('horseman-common.toml', [('hitching','lead_slot',False)])
# --- PM20a (4) Improved Mobs vanilla-only gear; PM27 spiders hold no items (HELDITEMS only)
im2 = json.load(open(out('improvedmobs/common.json') if os.path.exists(out('improvedmobs/common.json')) else src('improvedmobs/common.json')))
ec2 = im2['Entity Configs']['input'] + [e for e in ['minecraft:spider|HELDITEMS','minecraft:cave_spider|HELDITEMS'] if e not in im2['Entity Configs']['input']]
js('improvedmobs/common.json', [('Item Blacklist',['minecraft']),('Item Whitelist',True),('Entity Configs',ec2)], cfgform=True)
# --- PM36 / PM39: Tectonic back to Jayden's older test profile, elevation_boost stays 14
js('tectonic.json', [('biomes/temperature_scale',0.08),('biomes/vegetation_scale',0.08),('continents/continents_scale',0.06),
    ('continents/erosion_scale',0.12),('continents/ridge_scale',0.15),('continents/ocean_offset',-0.46),('continents/flat_terrain_skew',0.1),
    ('islands/noise_scale',0.06),('oceans/ocean_depth',-0.3),('oceans/deep_ocean_depth',-0.55),('oceans/monument_offset',-40),
    ('general/snow_start_offset',256),('global_terrain/elevation_boost',14.0)])
# --- PM44 / PM47: every Nifty Carts entity on the Carry On blacklist (hand cart included)
co2 = json.load(open(out('carryon-common.json')))
js('carryon-common.json', [('blacklist/forbiddenEntities', co2['blacklist']['forbiddenEntities'] + [e for e in ['niftycarts:*'] if e not in co2['blacklist']['forbiddenEntities']])])
# --- PM47: no Nifty Cart is player-pulled; every saddleable mount can pull every cart (the stock empty list also allows players)
NC_PULLERS = ['minecraft:horse','minecraft:donkey','minecraft:mule','minecraft:skeleton_horse','minecraft:zombie_horse','minecraft:camel','naturalist:zebra']  # PPM56: spider pig is not a cart-puller
toml('niftycarts-common.toml', [(f'carts.{c}','pull_animals',NC_PULLERS) for c in ['supply_cart','handCart','animal_cart','plow','seed_drill','reaper','wagon']])
# --- PM23a / PM26: Exposure server: no projecting, Lightroom gives no XP
toml('exposure-server.toml', [('capture','projecting_enabled',False),('lightroom','experience_black_and_white',0),('lightroom','experience_color',0),('lightroom','experience_chromatic',0)])
# --- PM23a: photographs render in item frames (client option, first-install default via yosbr)
toml('yosbr/config/exposure-client.toml', [('render','photograph_renders_in_item_frame',True)])
# --- PM43 1d: passives list (spider_pig out; creeper_minion, player_illusion in) feeds RpgDifficulty + Nameplate exclusions
rd2 = json.load(open(out('rpgdifficulty.json')))
js('rpgdifficulty.json', [('excludedEntity', PASSIVES)])
np2 = json.load(open(out('nameplate.json')))
js('nameplate.json', [('excludedEntities', [e for e in np2['excludedEntities'] if e not in ('mutantmonsters:spider_pig', 'minecraft:ender_dragon', 'minecraft:wither')]  # PPM57: dragon + wither get plates + [p for p in PASSIVES if p not in np2['excludedEntities']])])

print('\n'.join(log)); print(len(log), 'settings')
