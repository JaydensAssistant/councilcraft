#!/usr/bin/env python3
"""PPM79 (0.4.9): build resourcepacks/councilcraft-book-priority.zip, a CIT Resewn pack of .properties files only.
Bibliophilia (an unmodified Modrinth ref) gives every enchanted book one rule per enchantment, all at weight 0, so a
multi-enchant book would show whichever rule sorts first. This pack adds one weight-5 rule per enchantment in our pack,
pointing at Bibliophilia's own texture, named pNNN_<enchant> so CIT Resewn's tie-break (properties id ascending) gives
our priority order: the first listed enchantment the book has wins. The printed-copy rule (weight 10) still wins.
No art is copied. Usage: mkbookpriority.py <Bibliophilia zip> <out zip>"""
import sys, zipfile

PRIORITY = [
    'mending', 'sharpness', 'protection', 'efficiency', 'fortune', 'silk_touch', 'looting', 'infinity', 'power',
    'feather_falling', 'swift_sneak', 'soul_speed', 'depth_strider', 'frost_walker', 'riptide', 'loyalty', 'channeling',
    'multishot', 'piercing', 'quick_charge', 'smite', 'bane_of_arthropods', 'fire_aspect', 'sweeping', 'knockback',
    'flame', 'punch', 'impaling', 'blast_protection', 'fire_protection', 'projectile_protection', 'respiration',
    'aqua_affinity', 'thorns', 'luck_of_the_sea', 'lure',
    'create/capacity', 'create/potato_recovery', 'inventorio/deep_pockets', 'supplementaries/stasis',
    'betterend/end_veil', 'farmersdelight/backstabbing',
    'unbreaking', 'binding_curse', 'vanishing_curse',
]
CIT = 'assets/minecraft/optifine/cit/'


def props(text):
    out = {}
    for line in text.splitlines():
        if '=' in line and not line.lstrip().startswith('#'):
            k, v = line.split('=', 1)
            out[k.strip()] = v.strip()
    return out


def main(bib, dst):
    src = zipfile.ZipFile(bib)
    names = set(src.namelist())
    rules = []
    for i, entry in enumerate(PRIORITY):
        folder, ench = entry.split('/') if '/' in entry else ('vanilla', entry)
        base = CIT + folder + '/' + ench
        p = props(src.read(base + '.properties').decode('utf-8'))
        enchant = p.get('enchantments') or p['enchantmentIDs']
        texture = p.get('texture') or p['tile']
        tex_path = CIT + folder + '/' + texture + '.png'
        assert tex_path in names, tex_path  # the referenced Bibliophilia texture must exist
        rules.append((f'p{i:03d}_{ench}', 'type=item\nitems=enchanted_book\nenchantments=' + enchant
                      + '\ntexture=minecraft:optifine/cit/' + folder + '/' + texture + '.png\nweight=5\n'))
    with zipfile.ZipFile(dst, 'w', zipfile.ZIP_DEFLATED) as z:
        def put(name, data):
            info = zipfile.ZipInfo(name, (2026, 10, 22, 0, 0, 0))
            info.compress_type = zipfile.ZIP_DEFLATED
            z.writestr(info, data)
        put('pack.mcmeta', '{"pack":{"pack_format":15,"description":"CouncilCraft: enchanted book cover priority (needs Bibliophilia)"}}\n')
        for name, text in rules:
            put('assets/minecraft/citresewn/cit/councilcraft_book_priority/' + name + '.properties', text)
    print(len(rules), 'rules ->', dst)


if __name__ == '__main__':
    main(sys.argv[1], sys.argv[2])
