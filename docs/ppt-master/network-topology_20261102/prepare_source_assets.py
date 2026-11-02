"""Extract source evidence and independent artwork; does not author slides."""
from pathlib import Path
from PIL import Image
from collections import defaultdict
import hashlib
import json
import math

ROOT = Path(__file__).resolve().parent
SOURCE = ROOT / 'sources' / '项目网络.jpg'
page = Image.open(SOURCE).convert('RGB')
for folder in ['analysis', 'images/source-pages', 'icons/custom']:
    (ROOT / folder).mkdir(parents=True, exist_ok=True)
canonical = ROOT / 'images/source-pages/page-01.png'
page.save(canonical)
sha = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()

regions = [
    ('government-zone', [87,132,566,297], 'graphic', None),
    ('government-cloud', [184,157,349,274], 'graphic', None),
    ('government-label', [205,202,325,236], 'text', '政务外网'),
    ('government-owner', [369,202,519,236], 'text', '江北大数据局'),
    ('government-ip', [269,304,345,323], 'text', '政务外网IP'),
    ('machine-room', [87,324,1017,946], 'graphic', None),
    ('firewall', [216,360,314,477], 'image', None),
    ('firewall-label', [327,413,438,453], 'text', '防火墙/路由器\n(一对一NAT)'),
    ('switch', [181,594,349,667], 'image', None),
    ('sip-server', [532,497,610,594], 'image', None),
    ('sip-label', [486,618,655,649], 'text', 'SIP中继服务器'),
    ('softswitch-cloud', [982,182,1152,299], 'graphic', None),
    ('softswitch-label', [993,226,1147,259], 'text', '软交换网络'),
    ('internet-cloud', [801,576,965,697], 'graphic', None),
    ('internet-label', [804,621,959,654], 'text', '互联网专线'),
    ('nvr', [458,727,681,781], 'image', None),
    ('nvr-label', [506,786,633,818], 'text', '硬盘录像机'),
    ('vpn-cloud', [182,794,350,914], 'graphic', None),
    ('vpn-label', [200,839,335,875], 'text', 'VPN专网'),
    ('machine-room-label', [642,875,790,904], 'text', '大厂街道机房'),
    ('video-platform', [1115,572,1298,697], 'graphic', None),
    ('video-platform-label', [1153,702,1255,736], 'text', '视联平台'),
    ('unified-platform', [1472,574,1647,699], 'graphic', None),
    ('unified-platform-label', [1485,704,1636,738], 'text', '一网统管平台'),
    ('branches-zone', [0,965,594,1267], 'graphic', None),
    ('camera', [59,1065,171,1148], 'image', None),
    ('camera-label', [7,1155,202,1218], 'text', '8个卡口监控、36\n个摄像头'),
    ('fire-terminal', [381,1050,448,1146], 'image', None),
    ('fire-terminal-label', [314,1152,519,1218], 'text', '消控室36台传输装\n置'),
    ('branches-label', [215,1214,290,1246], 'text', '分支点'),
    ('handwritten-two', [378,498,494,604], 'text', '2'),
    ('handwritten-one', [683,506,709,616], 'text', '1'),
]
inventory = {
    'pages': [{
        'order': 1, 'original_file': str(SOURCE),
        'normalized_page': str(canonical), 'source_frame_bbox': [0,0,1745,1280],
        'original_sha256': sha(SOURCE), 'page_sha256': sha(canonical),
        'pixel_dimensions': [1745,1280],
        'regions': [dict(id=i,bbox=b,family=f,verbatim_text=t,
                         confidence='high', boundary_completeness='visible',
                         source_sufficiency='adequate at source display size',
                         occlusion='none affecting device identity or text')
                    for i,b,f,t in regions],
        'observed_links': [
            '政务外网 → 防火墙/路由器 → 交换机 → VPN专网',
            '交换机 ← SIP中继服务器 → 软交换网络',
            'VPN专网 → 硬盘录像机 → 互联网专线 → 视联平台 → 一网统管平台',
            'VPN专网 — 分支点分叉 → 摄像头、消控室传输装置',
        ],
        'overlap_observations': [
            'SIP server icon occludes horizontal link at y≈545.',
            'Network labels are printed over blue cloud silhouettes.',
            'Small selection handles are editor chrome, not network nodes.',
        ],
        'unresolved_evidence': [
            'The red digits 1 and 2 are legible; their meaning is unspecified.',
            'Arrow direction is visible; protocol/data direction cannot be inferred.',
            'The source supplies no subnet, IP, port or new application server position.',
        ],
    }]
}
(ROOT/'analysis/reconstruction_inventory.json').write_text(
    json.dumps(inventory,ensure_ascii=False,indent=2),encoding='utf-8')

asset_boxes = {
    'firewall': (216,360,314,477),
    'switch': (181,594,349,667),
    'sip-server': (533,498,608,594),
    'nvr': (459,727,681,781),
    'camera': (59,1065,171,1148),
    'fire-terminal': (381,1050,448,1146),
    'video-platform-cloud': (1115,572,1298,697),
    'unified-platform-cloud': (1472,574,1647,699),
}
evidence=[]
for name,box in asset_boxes.items():
    dest=ROOT/'images'/f'{name}.png'
    page.crop(box).save(dest)
    evidence.append(dict(id=name,source_region=box,source_page=str(canonical),
                         source_sha256=sha(canonical),method='exact source crop',
                         output=str(dest),output_sha256=sha(dest),status='Existing',
                         registration_group='source-page-01'))

def rdp(points,epsilon):
    if len(points)<3:return points
    x1,y1=points[0];x2,y2=points[-1]
    norm=math.hypot(x2-x1,y2-y1)
    distances=[abs((x2-x1)*(y1-y)-(x1-x)*(y2-y1))/norm if norm else math.hypot(x-x1,y-y1) for x,y in points]
    k=max(range(len(points)),key=distances.__getitem__)
    if distances[k]<=epsilon:return [points[0],points[-1]]
    return rdp(points[:k+1],epsilon)[:-1]+rdp(points[k:],epsilon)

def trace_blue(box):
    crop=page.crop(box)
    w,h=crop.size
    pixels=crop.load()
    inside=set()
    for y in range(h):
        for x in range(w):
            r,g,b=pixels[x,y]
            if r<100 and g>105 and b>140 and b-r>80:
                inside.add((x,y))
    edges=defaultdict(list)
    for x,y in inside:
        if (x,y-1) not in inside:edges[(x,y)].append((x+1,y))
        if (x+1,y) not in inside:edges[(x+1,y)].append((x+1,y+1))
        if (x,y+1) not in inside:edges[(x+1,y+1)].append((x,y+1))
        if (x-1,y) not in inside:edges[(x,y+1)].append((x,y))
    contours=[]
    while edges:
        start=next(iter(edges));p=start;contour=[start]
        for _ in range(100000):
            choices=edges.get(p)
            if not choices:break
            q=choices.pop()
            if not choices:del edges[p]
            contour.append(q);p=q
            if p==start:break
        contours.append(contour)
    def area(poly):return abs(sum(x1*y2-x2*y1 for (x1,y1),(x2,y2) in zip(poly,poly[1:])))
    outer=max(contours,key=area)
    result=rdp(outer,0.65)
    return w,h,result

for name,box in {
    'government-cloud':(181,155,353,278),
    'softswitch-cloud':(979,179,1156,302),
    'internet-cloud':(798,574,970,701),
    'vpn-cloud':(178,790,354,919),
}.items():
    w,h,points=trace_blue(box)
    d='M '+' L '.join(f'{x} {y}' for x,y in points)+' Z'
    dest=ROOT/'icons/custom'/f'{name}.svg'
    dest.write_text(f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {w} {h}"><path d="{d}" fill="#00AADE"/></svg>\n',encoding='utf-8')
    evidence.append(dict(id=name,source_region=box,source_page=str(canonical),
                         source_sha256=sha(canonical),method='deterministic external contour traced from source color; editable vector',
                         max_simplification_error_px=0.65,output=str(dest),
                         output_sha256=sha(dest),status='Existing',
                         registration_group='source-page-01'))
(ROOT/'analysis/reconstruction_layers.json').write_text(json.dumps(evidence,ensure_ascii=False,indent=2),encoding='utf-8')
print(f'Canonical page archived; {len(regions)} observed regions; {len(evidence)} independent assets prepared.')
