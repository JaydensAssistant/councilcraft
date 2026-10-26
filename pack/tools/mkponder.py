#!/usr/bin/env python3
"""PT36 (d). Writes the CMYK Ink Ponder structure (src/main/resources/assets/councilcraft/ponder/cmyk_ink.nbt).

Same format as Create Dragons Plus's dye_fluids.nbt and CEI's printer.nbt (DataVersion 3955, 5x5 snow/white
concrete base plate). Layout: basin (1,1,3) under a mixer (1,3,3); pump (2,1,3) -> pipes (3,1..3,3) -> printer
(3,3,2) facing west over a depot (3,1,2). Positions (0..2,1,0..1) are left empty for the scene's ink pool.
"""
import os
import nbtlib
from nbtlib import Compound, List, Int, String, Float, Byte

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, '..', '..', 'src', 'main', 'resources', 'assets', 'councilcraft', 'ponder', 'cmyk_ink.nbt')

palette = []


def state(name, **props):
    entry = {'Name': String(name)}
    if props:
        entry['Properties'] = Compound({k: String(v) for k, v in props.items()})
    entry = Compound(entry)
    if entry not in palette:
        palette.append(entry)
    return palette.index(entry)


def tank():
    return Compound({'Level': Compound({'Speed': Float(0.25), 'Target': Float(0.0), 'Value': Float(0.0)}),
                     'TankContent': Compound({})})


def pipe(**sides):
    props = {d: 'true' if d in sides else 'false' for d in ('north', 'south', 'east', 'west', 'up', 'down')}
    props['waterlogged'] = 'false'
    nbt = {d: Compound({}) for d in sides}
    nbt['id'] = String('create:fluid_pipe')
    return state('create:fluid_pipe', **props), Compound(nbt)


blocks = {}
for x in range(5):
    for z in range(5):
        blocks[(x, 0, z)] = (state('minecraft:snow_block' if (x + z) % 2 == 0 else 'minecraft:white_concrete'), None)

blocks[(1, 1, 3)] = (state('create:basin', facing='down'), Compound({
    'id': String('create:basin'), 'InputItems': Compound({'Size': Int(9), 'Items': List([])}),
    'OutputItems': Compound({'Size': Int(9), 'Items': List([])}), 'Filter': Compound({}),
    'DisabledSpoutput': List([]), 'FilterAmount': Int(64), 'InputTanks': List[Compound]([tank(), tank()]),
    'OutputTanks': List[Compound]([tank(), tank()]), 'UpTo': Byte(1), 'Overflow': List([]),
    'FluidOverflow': List([])}))
blocks[(1, 3, 3)] = (state('create:mechanical_mixer'), Compound({
    'id': String('create:mechanical_mixer'), 'Speed': Float(0.0), 'Ticks': Int(0), 'Running': Byte(0),
    'NeedsUpdate': Byte(0)}))
blocks[(2, 1, 3)] = (state('create:mechanical_pump', facing='east', waterlogged='false'), Compound({
    'id': String('create:mechanical_pump'), 'Speed': Float(0.0), 'east': Compound({}), 'west': Compound({})}))
blocks[(3, 1, 3)] = pipe(west=1, up=1)
blocks[(3, 2, 3)] = pipe(up=1, down=1)
blocks[(3, 3, 3)] = pipe(down=1, north=1)
blocks[(3, 1, 2)] = (state('create:depot', waterlogged='false'), Compound({
    'id': String('create:depot'), 'OutputBuffer': Compound({'Size': Int(8), 'Items': List([])})}))
template = Compound({'id': String('exposure:photograph'), 'Count': Byte(1),
                     'tag': Compound({'photograph_type': String('color')})})
blocks[(3, 3, 2)] = (state('create_enchantment_industry:printer', facing='west'), Compound({
    'id': String('create_enchantment_industry:printer'), 'Tanks': List[Compound]([tank()]),
    'PrintingTemplate': template, 'ProcessingTicks': Int(-1)}))

air = state('minecraft:air')
for x in range(5):
    for y in range(1, 5):
        for z in range(5):
            blocks.setdefault((x, y, z), (air, None))

out = []
for (x, y, z), (s, nbt) in sorted(blocks.items(), key=lambda kv: (kv[1][1] is not None, kv[0][1], kv[0][0], kv[0][2])):
    b = {'pos': List[Int]([Int(x), Int(y), Int(z)]), 'state': Int(s)}
    if nbt is not None:
        b['nbt'] = nbt
    out.append(Compound(b))

root = Compound({'size': List[Int]([Int(5), Int(5), Int(5)]), 'entities': List([]), 'blocks': List[Compound](out),
                 'palette': List[Compound](palette), 'DataVersion': Int(3955)})
os.makedirs(os.path.dirname(OUT), exist_ok=True)
nbtlib.File(root).save(OUT, gzipped=True)
print('wrote', os.path.normpath(OUT), len(out), 'blocks', len(palette), 'states')
