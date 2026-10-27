#!/usr/bin/env python3
"""PT35 (+ amendments 1-3), PT37 and PT42 (0.4.18): advancement overrides for councilcraft-data.

Usage (from the pack dir): tools/mkadvancements.py <server mods dir> <vanilla server jar> <vanilla client jar>
(the client jar is read only for item models, to check PT42 icon IDs)
Reads every data/*/advancements/*.json in the mod jars (nested jars included) and the vanilla jar, then writes:
  - REMOVE: an always-false fabric:load_conditions file at each removed ID's path (PT35 mechanism).
  - REPARENT / FOLD: a full copy of the jar JSON plus the new "parent" (PT35 re-parents, PT37 folded tab roots).
  - create:root retitled "Engineering" (PT37), voluntary_exile restored from vanilla with a new description (PT35 am. 2).
  - PT42: the 11 CouncilCraft advancements and their entity-type tags.
It deletes the S244 G7 impossible+hidden files it replaces, then checks that every surviving advancement's parent
exists and that the displayed tab roots are exactly PT35's set. Lang keys live in the councilcraft mod's en_us.json.
"""
import glob, io, json, os, re, sys, zipfile

PACK = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DP = os.path.join(PACK, 'moonlight-global-datapacks', 'councilcraft-data')
DATA = os.path.join(DP, 'data')
FALSE = {"fabric:load_conditions": [{"condition": "fabric:not", "value": {
    "condition": "fabric:all_mods_loaded", "values": ["minecraft"]}}]}

BETTEREND = ['root', 'enter_end', 'all_the_biomes', 'village', 'portal', 'portal_on', 'portal_travel', 'infusion',
             'infusion_finished', 'hammer', 'all_elytras', 'all_the_templates',
             'aeternium_anvil', 'aeternium_armor', 'aeternium_hammer', 'aeternium_hammer_head', 'aeternium_plate',
             'aeternium_tool', 'aeternium_tool_head',
             'terminite_anvil', 'terminite_armor', 'terminite_plate', 'terminite_tool', 'terminite_tool_head',
             'thallasium_anvil', 'thallasium_armor', 'thallasium_plate', 'thallasium_tool', 'thallasium_tool_head']
REMOVE = {
    # PT35: End is disabled
    **{f'minecraft:end/{a}': 'PT35 End disabled' for a in ['root', 'dragon_breath', 'dragon_egg', 'elytra', 'enter_end_gateway',
                                                            'find_end_city', 'kill_dragon', 'levitate', 'respawn_dragon']},
    'minecraft:story/enter_the_end': 'PT35 End disabled',
    'nullscape:root': 'PT35 End disabled',
    'minecraft:adventure/spyglass_at_dragon': 'PT35 End disabled (ender dragon)',
    'minecraft:adventure/kill_all_mobs': 'PT35 End disabled (shulker, ender dragon)',
    'minecraft:nether/all_effects': 'PT35 End disabled (levitation)',
    'minecraft:adventure/trim_with_all_exclusive_armor_patterns': 'PT35 End disabled (spire template)',
    'minecraft:husbandry/balanced_diet': 'PT35 End disabled (chorus fruit)',
    'farmersdelight:main/plant_all_crops': 'PT35 End disabled (chorus flower)',
    'exposure:adventure/void': 'PT35 End disabled',
    **{f'betterend:{a}': 'PT35 End disabled (BetterEnd display set)' for a in BETTEREND},
    # PT35: no villagers, villages or raids
    'minecraft:adventure/trade': 'PT35 no villagers',
    'minecraft:adventure/trade_at_world_height': 'PT35 no villagers',
    'minecraft:adventure/lightning_rod_with_villager_no_fire': 'PT35 no villagers',
    'minecraft:adventure/very_very_frightening': 'PT35 no villagers',
    'minecraft:story/cure_zombie_villager': 'PT35 no villagers',
    'exposure:adventure/exposed_paparazzi': 'PT35 no villagers',
    'nova_structures:adventure/find_swamp_village': 'PT35 no swamp village',
    'nova_structures:adventure/find_all_illager_structures': 'PT35 needs swamp village',
    'supplementaries:adventure/way_sign': 'PT35 no way signs',
    'minecraft:adventure/hero_of_the_village': 'PT35 raids off',
    # PT35: removed items, blocks and mobs
    'minecraft:story/enchant_item': 'PT35 no enchanting table (replaced by PT42 Read the Fine Print)',
    'create_enchantment_industry:blaze_enchantery': 'PT35 Blaze Enchanter removed',
    'create_enchantment_industry:blazing_enchantment': 'PT35 Blaze Enchanter removed',
    'create_enchantment_industry:sigil_forging': 'PT35 Blaze Enchanter removed',
    'create_enchantment_industry:thousand_runes': 'PT35 Blaze Enchanter removed',
    'create_enchantment_industry:probability_spike': 'PT35 Blaze Enchanter only',
    'create_enchantment_industry:paradox_fusion': 'PT35 am.1 incompatible enchants impossible',
    'mutantmonsters:hulk_smash': 'PT35 Hulk Hammer removed',
    'mutantmonsters:you_da_bomby': 'PT35 creeper minions removed',
    'exposure:adventure/piercing_gaze': 'PT35 Interplanar Projector removed',
    'friendsandfoes:adventure/kill_illusioner': 'PT35 illusioner off',
    'friendsandfoes:adventure/the_magicians': 'PT35 illusioner off',
    'paraglider:statues_bargain': 'PT35 statues emptied',
    'paraglider:pray_to_the_goddess': 'PT35 statues emptied',
    'paraglider:all_vessels': 'PT35 statues emptied',
    'minecraft:niftycarts/hand_cart_one_k': 'PT35 hand carts removed',
    'minecraft:husbandry/place_dried_ghast_in_water': 'PT35 dried ghasts off',
    # PT35 amendment 1
    'minecraft:nether/fast_travel': 'PT35 am.1 (replaced by PT42 No Shortcuts)',
    'minecraft:adventure/adventuring_time': 'PT35 am.1 biome checklist',
    'minecraft:husbandry/whole_pack': 'PT35 am.1 wolf-variant checklist',
    'regions_unexplored:pioneer': 'PT35 am.1 biome checklist',
    'regions_unexplored:regions_explored': 'PT35 am.1 biome checklist',
    'regions_unexplored:spelunker': 'PT35 am.1 biome checklist',
    'regions_unexplored:eternal_expedition': 'PT35 am.1 biome checklist',
    # PT35 amendment 3
    'create:train_portal': 'PT35 am.3 trains do not cross Immersive Portals',
}
REPARENT = {
    'minecraft:adventure/summon_iron_golem': 'minecraft:adventure/root',
    'supplementaries:adventure/globe': 'minecraft:adventure/root',
    'exposure:adventure/unforeseen_consequences': 'exposure:adventure/lights_up',
    **{f'nova_structures:adventure/{a}': 'minecraft:adventure/root' for a in
       ['find_illager_mansion', 'find_illager_manor', 'find_pillager_outpost', 'find_witch_villa', 'loot_illager_hideout',
        'conquer_miner_outpost']},
    'create_enchantment_industry:transcendent_overclock': 'create_enchantment_industry:lightning_catalysis',
    'create_enchantment_industry:omni_enchanter': 'create_enchantment_industry:transcendent_overclock',
    **{f'regions_unexplored:{a}': 'regions_unexplored:parent' for a in
       ['every_bit_of_the_rainbow', 'from_the_tree_tops', 'light_as_a_frog', 'blind_as_a_bat', 'downer', 'light_snack']},
}
FOLD = {
    'create_enchantment_industry:root': 'create:root', 'numismatics:root': 'create:root', 'railways:root': 'create:root',
    'minecraft:niftycarts/root': 'create:root',
    'mutantmonsters:root': 'minecraft:adventure/root', 'betterdungeons:root': 'minecraft:adventure/root',
    'betterdeserttemples:root': 'minecraft:adventure/root', 'regions_unexplored:parent': 'minecraft:adventure/root',
    'paraglider:root': 'minecraft:adventure/root',
    'farmersdelight:main/root': 'minecraft:husbandry/root',
}
TAB_ROOTS = {'minecraft:story/root', 'minecraft:adventure/root', 'minecraft:husbandry/root', 'minecraft:nether/root', 'create:root'}


def milestone(name):
    return {"trigger": "councilcraft:milestone", "conditions": {"milestone": name}}


# PT42: (id, parent, icon, criterion)
PT42 = [
    ('not_on_my_watch', 'minecraft:adventure/root', 'minecraft:golden_apple', milestone('not_on_my_watch')),
    ('here_comes_the_airplane', 'minecraft:husbandry/root', 'minecraft:mushroom_stew', milestone('here_comes_the_airplane')),
    ('read_the_fine_print', 'minecraft:story/mine_diamond', 'minecraft:enchanted_book',
     {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": ["minecraft:enchanted_book"]}]}}),
    ('no_shortcuts', 'minecraft:nether/root', 'minecraft:compass',
     {"trigger": "minecraft:nether_travel", "conditions": {"distance": {"horizontal": {"min": 1000.0}}}}),
    ('keep_the_change', 'numismatics:root', 'numismatics:spur', milestone('keep_the_change')),
    ('here_be_mutants', 'mutantmonsters:root', 'mutantmonsters:mutant_skeleton_skull', milestone('here_be_mutants')),
    ('shared_horizons', 'minecraft:adventure/root', 'councilcraft:antique_atlas', milestone('shared_horizons')),
    ('set_sail', 'minecraft:adventure/root', 'smallships:oak_cog',
     {"trigger": "minecraft:started_riding", "conditions": {"player": [{"condition": "minecraft:entity_properties",
      "entity": "this", "predicate": {"vehicle": {"type": "#councilcraft:dockyard_ships"}}}]}}),
    ('coffee_table_book', 'create_enchantment_industry:root', 'exposure:signed_album',
     {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": ["exposure:signed_album"],
      "nbt": "{\"councilcraft:printed\":1b}"}]}}),
    ('double_saddle', 'minecraft:husbandry/root', 'minecraft:saddle', milestone('double_saddle')),
    ('testing_testing', 'minecraft:story/root', 'minecraft:note_block', milestone('testing_testing')),
]
ENTITY_TAGS = {
    'dockyard_ships': ['smallships:cog', 'smallships:brigg', 'smallships:galley', 'smallships:dhow', 'smallships:drakkar',
                       'smallships:galleon', 'smallships:caravel'],
    'double_saddle': ['minecraft:horse', 'minecraft:donkey', 'minecraft:mule', 'minecraft:skeleton_horse',
                      'minecraft:zombie_horse'],
}


def index(mods_dir, vanilla, client):
    idx, items = {}, set()

    def scan(z, name):
        for n in z.namelist():
            if n.startswith(('META-INF/jars/', 'META-INF/versions/')) and n.endswith('.jar'):
                try:
                    scan(zipfile.ZipFile(io.BytesIO(z.read(n))), name + '!' + n)
                except zipfile.BadZipFile:
                    pass
            m = re.match(r'data/([^/]+)/advancements/(.+)\.json$', n)
            if m:
                idx.setdefault(m.group(1) + ':' + m.group(2), []).append((name, json.loads(z.read(n))))
            m = re.match(r'assets/([^/]+)/models/item/([a-z0-9_/]+)\.json$', n)
            if m:
                items.add(m.group(1) + ':' + m.group(2))
    for jp in sorted(glob.glob(os.path.join(mods_dir, '*.jar'))) + [vanilla]:
        scan(zipfile.ZipFile(jp), os.path.basename(jp))
    with zipfile.ZipFile(client) as z:
        items.update('minecraft:' + m.group(1) for n in z.namelist()
                     for m in [re.match(r'assets/minecraft/models/item/([a-z0-9_]+)\.json$', n)] if m)
    return idx, items


def path_of(aid):
    ns, p = aid.split(':', 1)
    return os.path.join(DATA, ns, 'advancements', p + '.json')


def write(aid, d):
    p = path_of(aid)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, 'w') as f:
        json.dump(d, f, indent=2)
        f.write('\n')


def jar_copy(idx, aid):
    entries = idx.get(aid)
    assert entries, f'{aid} not in any jar'
    bodies = {json.dumps(e[1], sort_keys=True) for e in entries}
    assert len(bodies) == 1, f'{aid} has {len(bodies)} different jar copies: {[e[0] for e in entries]}'
    return json.loads(json.dumps(entries[0][1]))


def conditional(d):
    """True if the jar JSON carries its own load condition (may be absent at runtime, e.g. CEI legacy_ignition,
    supplementaries story/unenchanter); skipped by the static checks and proven by the server log instead."""
    return 'fabric:load_conditions' in d or 'forge:conditions' in d or 'conditions' in d


def main():
    mods_dir, vanilla, client = sys.argv[1], sys.argv[2], sys.argv[3]
    idx, items = index(mods_dir, vanilla, client)
    items.add('councilcraft:antique_atlas')  # registered in code (AntiqueAtlasItem); model checked below
    assert os.path.exists(os.path.join(PACK, '..', 'src/main/resources/assets/councilcraft/models/item/antique_atlas.json'))
    for aid in list(REMOVE) + list(REPARENT) + list(FOLD) + ['create:root', 'minecraft:adventure/voluntary_exile']:
        assert aid in idx, f'{aid} not in any jar'

    # This script owns every advancement file in councilcraft-data: drop them all (including the S244 G7
    # impossible+hidden overrides that PT35 replaces) and write the set below.
    removed_old = 0
    for p in glob.glob(os.path.join(DATA, '*', 'advancements', '**', '*.json'), recursive=True):
        os.remove(p)
        removed_old += 1

    final = {aid: es[0][1] for aid, es in idx.items()}
    notes = []
    for aid, why in REMOVE.items():
        write(aid, FALSE)
        final.pop(aid)
        notes.append(f'{os.path.relpath(path_of(aid), DATA)}  # REMOVE: {why}')
    for table, why in ((REPARENT, 'PT35 re-parent'), (FOLD, 'PT37 tab fold')):
        for aid, parent in table.items():
            d = jar_copy(idx, aid)
            d['parent'] = parent
            write(aid, d)
            final[aid] = d
            notes.append(f'{os.path.relpath(path_of(aid), DATA)}  # {why} -> {parent}')
    d = jar_copy(idx, 'create:root')
    d['display']['title'] = {'translate': 'advancements.councilcraft.engineering.title'}
    write('create:root', d)
    final['create:root'] = d
    notes.append('create/advancements/root.json  # PT37 Engineering tab title')
    d = jar_copy(idx, 'minecraft:adventure/voluntary_exile')
    d['display']['description'] = {'translate': 'advancements.councilcraft.voluntary_exile.description'}
    d['display']['hidden'] = False  # vanilla has hidden=true; PT35 am.2 says visible, not hidden
    write('minecraft:adventure/voluntary_exile', d)
    final['minecraft:adventure/voluntary_exile'] = d
    notes.append('minecraft/advancements/adventure/voluntary_exile.json  # PT35 am.2 vanilla copy, patrol-captain description')

    for name, values in ENTITY_TAGS.items():
        p = os.path.join(DATA, 'councilcraft', 'tags', 'entity_types', name + '.json')
        os.makedirs(os.path.dirname(p), exist_ok=True)
        json.dump({'replace': False, 'values': values}, open(p, 'w'), indent=2)
        notes.append(f'councilcraft/tags/entity_types/{name}.json  # PT42')
    for name, parent, icon, crit in PT42:
        assert icon in items, f'{icon} is not a real item'
        aid = 'councilcraft:' + name
        d = {'parent': parent, 'display': {
            'icon': {'item': icon},
            'title': {'translate': f'advancements.councilcraft.{name}.title'},
            'description': {'translate': f'advancements.councilcraft.{name}.description'},
            'frame': 'task', 'show_toast': True, 'announce_to_chat': True, 'hidden': False},
            'criteria': {name: crit}, 'requirements': [[name]]}
        write(aid, d)
        final[aid] = d
        notes.append(f'councilcraft/advancements/{name}.json  # PT42')

    # Checks: every parent exists; no child points at a removed ID; tab roots are exactly PT35's set.
    problems = []
    for aid, d in final.items():
        par = d.get('parent')
        if par and par not in final and not conditional(d):
            problems.append(f'{aid} -> missing parent {par}' + (' (REMOVED)' if par in REMOVE else ''))
    roots = {aid for aid, d in final.items() if 'display' in d and 'parent' not in d and not conditional(d)}
    if roots != TAB_ROOTS:
        problems.append(f'tab roots {sorted(roots)} != {sorted(TAB_ROOTS)}')
    if problems:
        raise SystemExit('\n'.join(problems))

    lines = open(os.path.join(DP, 'CONTENTS.md')).read().split('\n')
    lines = [l for l in lines if 'S244 G7' not in l]
    marker = '## Advancements (generated by pack/tools/mkadvancements.py)'
    if marker in lines:
        lines = lines[:lines.index(marker)]
    while lines and lines[-1] == '':
        lines.pop()
    open(os.path.join(DP, 'CONTENTS.md'), 'w').write('\n'.join(lines) + '\n\n' + marker + '\n\n' +
                                                     '\n'.join('- ' + n for n in notes) + '\n')
    print(f'removed {removed_old} old overrides; wrote {len(REMOVE)} removals, {len(REPARENT)} re-parents, {len(FOLD)} folds, '
          f'{len(PT42)} PT42; tab roots {sorted(roots)}')


if __name__ == '__main__':
    main()
