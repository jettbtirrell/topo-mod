import colorsys,random,os,sys
from PIL import Image
RAW=sys.argv[1]; OUT=sys.argv[2]
N=16
def noise_levels(rng,strength=1.0):
    # tileable, lightly clustered noise quantised to levels -2..2 (Minecraft-style: mostly 0/+-1, rare extremes)
    f=[[rng.random()*2-1 for _ in range(N)] for _ in range(N)]
    g=[[0.55*f[y][x]+0.45*(f[y][(x+1)%N]+f[y][(x-1)%N]+f[(y+1)%N][x]+f[(y-1)%N][x])/4*1.6 for x in range(N)] for y in range(N)]
    flat=sorted(v for r in g for v in r); n=len(flat)
    cut=[flat[int(n*q)] for q in (0.09,0.33,0.67,0.91)]
    def lvl(v): return sum(v>c for c in cut)-2
    return [[lvl(g[y][x]) for x in range(N)] for y in range(N)]
def shade(rgb,level,step):
    h,l,s=colorsys.rgb_to_hls(*(c/255 for c in rgb))
    l=max(0,min(1,l+level*step)); h=(h+level*0.007)%1.0; s=max(0,min(1,s-level*0.02))
    return tuple(round(c*255) for c in colorsys.hls_to_rgb(h,l,s))
def pebbles(lv,rng,count,mask):
    for _ in range(count):
        x,y=rng.randrange(N),rng.randrange(N)
        horiz=rng.random()<0.5
        cells=[(x,y),((x+1)%N,y) if horiz else (x,(y+1)%N)]
        if not all(mask(cx,cy) for cx,cy in cells): continue
        for cx,cy in cells: lv[cy][cx]=-3
        hx,hy=(x-1)%N,(y-1)%N
        if mask(hx,hy) and (hx,hy) not in cells: lv[hy][hx]=3
# Retone the flat sift colours to sit with the saturated pink-coral grass top: grass lip -> the top's palette, dirt -> deeper red-coral.
CMAP={(0xbb,0x7e,0x80):(0xf6,0x7e,0x8e),(0xb9,0x76,0x82):(0xed,0x6c,0x78),(0xb9,0x6c,0x64):(0xe6,0x62,0x62),
      (0xb4,0x60,0x57):(0xb9,0x59,0x4c),(0xb5,0x57,0x58):(0xb3,0x4f,0x50),(0xb7,0x4d,0x60):(0xba,0x45,0x58)}
def process(name,kind):
    im=Image.open(f'{RAW}/{name}.png').convert('RGBA'); px=im.load()
    for yy in range(N):
        for xx in range(N):
            c=px[xx,yy]; px[xx,yy]=CMAP.get(c[:3],c[:3])+(c[3],)
    rng=random.Random({'dirt':11,'top':23,'side':37}[name])
    lv=noise_levels(rng)
    dirtc={(0xb9,0x59,0x4c),(0xb3,0x4f,0x50),(0xba,0x45,0x58)}
    isdirt=lambda x,y:name=='dirt' or (name=='side' and px[x,y][:3] in dirtc)
    if name in('dirt','side'): pebbles(lv,rng,7 if name=='dirt' else 5,isdirt)
    out=im.copy(); o=out.load()
    for y in range(N):
        for x in range(N):
            step=0.032 if isdirt(x,y) else 0.026
            L=lv[y][x]
            if name=='side' and isdirt(x,y) and y>0 and not isdirt(x,y-1): L=min(L,-1)  # shadow under the grass lip
            o[x,y]=shade(px[x,y][:3],L,step)+(px[x,y][3],)
    return out
os.makedirs(OUT,exist_ok=True)
res={}
for n in ('dirt','top','side'):
    res[n]=process(n,n); res[n].save(f'{OUT}/{n}.png')
# preview: before | after, plus 3x3 tiling of after
sheet=Image.new('RGBA',(16*10*2+16*6*3+60,16*10*3+10),(50,50,50,255))
for r,n in enumerate(('dirt','top','side')):
    b=Image.open(f'{RAW}/{n}.png').convert('RGBA'); sheet.paste(b.resize((160,160),Image.NEAREST),(0,r*160))
    sheet.paste(res[n].resize((160,160),Image.NEAREST),(170,r*160))
    big=Image.new('RGBA',(48,48))
    for i in range(3):
        for j in range(3): big.paste(res[n],(i*16,j*16))
    sheet.paste(big.resize((288,288),Image.NEAREST).crop((0,0,288,160)),(350,r*160))
sheet.save(OUT+'/preview.png')
