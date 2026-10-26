#!/usr/bin/env python3
"""PT12 (0.4.15). Build pack/shaderpacks/E-LITE shaders 5.1.3 CouncilCraft.zip, the Medium preset's shader pack:
stock E-LITE shaders 5.1.3 from Modrinth (LGPL-3.0-or-later, by Entokito, based on MakeUp by Javier Garduno)
+ docs/patches/elite-5.1.3-councilcraft-taa.diff (TAA history rejection + CHANGES-CouncilCraft.txt notice).
Stock entries keep their order and timestamps; changed/added entries get a fixed date, so the output is
reproducible. Needs Python 3 and GNU patch.
Usage (from the repo root): python3 pack/tools/mkelite.py"""
import hashlib, os, subprocess, sys, tempfile, urllib.request, zipfile

URL = 'https://cdn.modrinth.com/data/LTvf5Tji/versions/ChU59bMQ/%C2%A7lE-LITE%20shaders%205.1.3.zip'
SHA512 = ('42d37790d19cf78a9e44e690d2b135ab6f0d5bb806b8570c6973115e336781fd'
          '3f80c1057682c02207341cd9cc446d36e4248d361501f403c72bcabe9689b973')
PATCH = 'docs/patches/elite-5.1.3-councilcraft-taa.diff'
OUT = 'pack/shaderpacks/E-LITE shaders 5.1.3 CouncilCraft.zip'
DATE = (2026, 10, 25, 0, 0, 0)

stock = urllib.request.urlopen(URL).read()
assert hashlib.sha512(stock).hexdigest() == SHA512, 'stock E-LITE 5.1.3 zip hash mismatch'
with tempfile.TemporaryDirectory() as tmp:
    src = os.path.join(tmp, 'stock.zip')
    open(src, 'wb').write(stock)
    tree = os.path.join(tmp, 'tree')
    with zipfile.ZipFile(src) as z:
        z.extractall(tree)
        infos = z.infolist()
    subprocess.run(['patch', '-p1', '-s', '-d', tree, '-i', os.path.abspath(PATCH)], check=True)
    patched = [l.split()[1][2:] for l in open(PATCH, encoding='utf-8') if l.startswith('+++ ')]
    with zipfile.ZipFile(src) as zi, zipfile.ZipFile(OUT, 'w', zipfile.ZIP_DEFLATED) as zo:
        for info in infos:
            if info.filename in patched:
                ni = zipfile.ZipInfo(info.filename, DATE)
                ni.compress_type = zipfile.ZIP_DEFLATED
                zo.writestr(ni, open(os.path.join(tree, info.filename), 'rb').read())
            else:
                zo.writestr(info, zi.read(info),
                            compress_type=zipfile.ZIP_STORED if info.is_dir() else zipfile.ZIP_DEFLATED)
        names = {i.filename for i in infos}
        for name in patched:
            if name not in names:
                ni = zipfile.ZipInfo(name, DATE)
                ni.compress_type = zipfile.ZIP_DEFLATED
                zo.writestr(ni, open(os.path.join(tree, name), 'rb').read())
print('wrote', OUT, os.path.getsize(OUT), 'bytes; patched:', ', '.join(patched))
