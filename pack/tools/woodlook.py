# PPM73: closest vanilla wood by mean CIELAB plank colour (first animation frame, opaque pixels).
# Usage: put vanilla planks textures in van/<wood>_planks.png and RU/pale oak ones in ru/<wood>_planks.png, run in that folder.
# NC = Nifty Carts (11 woods), SS = Small Ships (9 boat woods). Output feeds dev.councilcraft.rules.WoodLookMap.
import glob,os,math
from PIL import Image
def lab(rgb):
    def f(c):
        c/=255; return ((c+0.055)/1.055)**2.4 if c>0.04045 else c/12.92
    r,g,b=map(f,rgb)
    X=(r*.4124+g*.3576+b*.1805)/.95047;Y=r*.2126+g*.7152+b*.0722;Z=(r*.0193+g*.1192+b*.9505)/1.08883
    h=lambda t:t**(1/3) if t>0.008856 else 7.787*t+16/116
    return (116*h(Y)-16,500*(h(X)-h(Y)),200*(h(Y)-h(Z)))
def avg(p):
    im=Image.open(p).convert('RGBA'); w=im.size[0]; im=im.crop((0,0,w,w))  # first frame
    px=[q for q in im.getdata() if q[3]>0]; n=len(px)
    # average in linear-ish: mean of Lab per pixel
    L=[lab(q[:3]) for q in px]; return tuple(sum(x[i] for x in L)/n for i in range(3)), tuple(round(sum(q[i] for q in px)/n) for i in range(3))
def de(a,b): return math.dist(a,b)
van={os.path.basename(p)[:-11]:avg(p) for p in glob.glob('van/*.png')}
nc=list(van); ss=[k for k in van if k not in('crimson','warped')]
for p in sorted(glob.glob('ru/*.png')):
    n=os.path.basename(p)[:-11]; a,rgb=avg(p)
    r1=sorted(nc,key=lambda k:de(a,van[k][0])); r2=sorted(ss,key=lambda k:de(a,van[k][0]))
    print(f"{n:18} #{rgb[0]:02x}{rgb[1]:02x}{rgb[2]:02x} NC:{r1[0]}({de(a,van[r1[0]][0]):.0f}) 2nd:{r1[1]}({de(a,van[r1[1]][0]):.0f}) SS:{r2[0]}({de(a,van[r2[0]][0]):.0f}) 2nd:{r2[1]}")
print({k:'#%02x%02x%02x'%v[1] for k,v in van.items()})
