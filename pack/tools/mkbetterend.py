#!/usr/bin/env python3
"""PM7a-c (0.4.6): BetterEnd 4.0.12 datapack overrides for councilcraft-data.
Usage (from the pack dir): tools/mkbetterend.py <better-end-4.0.12.jar>
Writes into moonlight-global-datapacks/councilcraft-data/data/betterend/:
  - loot_tables/chests/*.json: BetterEnd chest loot minus every cut item (CUT below), minecraft:elytra and end crystals (PPM43)
  - (advancements: superseded in 0.4.18 by PT35, which removes BetterEnd's whole display set; see tools/mkadvancements.py)
Keep CUT in sync with kubejs/server_scripts/councilcraft_end.js (CC_END_CUT) and the EMI hide file."""
import json, re, sys, zipfile, os
CUT = re.compile(r'^betterend:(aeternium_[a-z_]+|[a-z_]+_hammer|[a-z_]+_anvil|[a-z_]+_pedestal|guide_book|respawn_obelisk|'
                 r'eternal_crystal|flavolite_runed(_eternal)?|elytra_armored|elytra_crystalite|crystalite_(helmet|chestplate|leggings|boots)|'
                 r'enchanted_(membrane|petal)|leather_wrapped_stick|[a-z_]+_smithing_template|'
                 r'(thallasium|terminite)_(axe_head|hoe_head|pickaxe_head|shovel_head|sword_blade|sword_handle|forged_plate))$|^minecraft:elytra$')
IMPOSSIBLE_PRE_0418 = ['aeternium_anvil', 'aeternium_armor', 'aeternium_hammer', 'aeternium_hammer_head', 'aeternium_plate', 'aeternium_tool',
              'aeternium_tool_head', 'all_elytras', 'all_the_templates', 'hammer', 'infusion', 'infusion_finished', 'portal', 'portal_on',
              'portal_travel', 'thallasium_anvil', 'terminite_anvil', 'thallasium_plate', 'terminite_plate', 'thallasium_tool_head',
              'terminite_tool_head']
REPARENT_PRE_0418 = ['thallasium_armor', 'thallasium_tool', 'terminite_armor', 'terminite_tool']
# PPM43: end crystals have no recipe (no dragon re-summon), so BetterEnd chests must not hand them out either.
LOOT_EXTRA = {'minecraft:end_crystal'}
OUT = 'moonlight-global-datapacks/councilcraft-data/data/betterend'

def strip(node):
    """Drop loot entries naming a cut item; drop pools left empty."""
    pools = []
    for p in node.get('pools', []):
        p['entries'] = [e for e in p.get('entries', []) if not (CUT.match(e.get('name', '')) or e.get('name') in LOOT_EXTRA)]
        if p['entries']:
            pools.append(p)
    node['pools'] = pools
    return node

z = zipfile.ZipFile(sys.argv[1])
n_loot = 0
for name in z.namelist():
    if name.startswith('data/betterend/loot_tables/chests/') and name.endswith('.json'):
        raw = z.read(name).decode()
        if not any(CUT.match(i) or i in LOOT_EXTRA for i in re.findall(r'"name"\s*:\s*"([^"]+)"', raw)):
            continue
        d = strip(json.loads(raw))
        dst = os.path.join(OUT, name[len('data/betterend/'):])
        os.makedirs(os.path.dirname(dst), exist_ok=True)
        json.dump(d, open(dst, 'w'), indent=2)
        n_loot += 1
IMPOSSIBLE, REPARENT = [], []  # PT35 (0.4.18): tools/mkadvancements.py owns every advancement file
for a in IMPOSSIBLE + REPARENT:
    d = json.loads(z.read('data/betterend/advancements/%s.json' % a))
    if a in IMPOSSIBLE:
        d['criteria'] = {'impossible': {'trigger': 'minecraft:impossible'}}
        d['requirements'] = [['impossible']]
        d.pop('rewards', None)
        d.setdefault('display', {}).update({'hidden': True, 'show_toast': False, 'announce_to_chat': False})
    else:
        d['parent'] = 'betterend:enter_end'
    dst = os.path.join(OUT, 'advancements', a + '.json')
    os.makedirs(os.path.dirname(dst), exist_ok=True)
    json.dump(d, open(dst, 'w'), indent=2)
print('loot tables', n_loot, 'advancements', len(IMPOSSIBLE) + len(REPARENT))
