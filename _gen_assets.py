import os, math
from PIL import Image, ImageDraw, ImageFont

ROOT = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.join(ROOT, '长河.html')

html = open(SRC, encoding='utf-8').read()
old = 'function save(){ try{ localStorage.setItem(KEY, JSON.stringify(S)); }catch(e){} }'
new = (
    'function save(){ try{ localStorage.setItem(KEY, JSON.stringify(S)); }catch(e){} '
    'try{ if(window.ChangheBridge && window.ChangheBridge.onSettingsChanged) '
    'window.ChangheBridge.onSettingsChanged(); }catch(e){} }\n'
    'window.ChangheSnapshot = function(){'
    'try{'
    'var out={birth:S.birth,horizon:S.horizon,marks:[]};'
    'var y=new Date().getFullYear();'
    'function add(n,d){var s=(d instanceof Date)?iso(d):\'\';if(n&&s)out.marks.push({n:n,d:s});}'
    'S.events.forEach(function(e){add(e.name,pDate(e.date));});'
    'buildAges().forEach(function(e){add(e.name,e.date);});'
    '[0,1,2].forEach(function(k){var base=new Date(y+k,0,1);'
    'buildFestivals(base).forEach(function(e){add(e.name,e.date);});'
    'buildMemorials(base).forEach(function(e){add(e.name,e.date);});});'
    'return JSON.stringify(out);'
    '}catch(e){return JSON.stringify({error:String(e)});}'
    '};'
)
assert old in html, 'save() pattern not found'
html = html.replace(old, new, 1)
assert 'window.ChangheBridge.onSettingsChanged' in html
assert 'window.ChangheSnapshot' in html
open(os.path.join(ROOT, 'app', 'src', 'main', 'assets', 'changhe.html'), 'w', encoding='utf-8').write(html)
print('assets html chars:', len(html))

JADE = (60, 106, 96)
JADE_SOFT = (127, 167, 158)
PAPER = (244, 245, 241)
INK2 = (92, 108, 103)
INK3 = (151, 167, 161)
LINE = (228, 235, 231)
INK = (34, 48, 45)   # 墨 #22302D


def icon(size):
    # 年轮 / 涟漪：深墨绿底 + 米白同心不闭合圆环（粗细不一）+ 中心实心白点
    S = 4
    W = size * S
    im = Image.new('RGB', (W, W), INK)
    d = ImageDraw.Draw(im)
    cx = cy = W // 2
    rings = [
        (0.45, 0.018),   # (半径比例, 线宽比例) 粗细不一
        (0.37, 0.010),
        (0.29, 0.026),
        (0.21, 0.012),
        (0.13, 0.020),
    ]
    for i, (rf, wf) in enumerate(rings):
        R = W * rf
        w = max(1, int(W * wf))
        gap = 46 + i * 10                      # 不闭合，留缺口
        start = (15 + i * 47) % 360             # 缺口错位，似年轮 / 水波
        d.arc([cx - R, cy - R, cx + R, cy + R], start=start,
              end=start + 360 - gap, fill=PAPER, width=w)
    rd = int(W * 0.030)
    d.ellipse([cx - rd, cy - rd, cx + rd, cy + rd], fill=PAPER)   # 中心白点
    return im.resize((size, size), Image.LANCZOS)


for folder, size in [('mipmap-mdpi', 48), ('mipmap-hdpi', 72),
                     ('mipmap-xhdpi', 96), ('mipmap-xxhdpi', 144),
                     ('mipmap-xxxhdpi', 192)]:
    icon(size).save(os.path.join(ROOT, 'app', 'src', 'main', 'res', folder, 'ic_launcher.png'))
    print('icon', folder, size)


def font(path, size):
    try:
        return ImageFont.truetype(path, size)
    except Exception:
        return None


F = None
for p in [r'C:\Windows\Fonts\msyh.ttc', r'C:\Windows\Fonts\simsun.ttc', r'C:\Windows\Fonts\simhei.ttf']:
    F = font(p, 22)
    if F:
        break

def preview(path, W, H, compact):
    # 系统小组件选择器缩略图：严格还原 App「小组件」页演示卡的 2×2 / 4×2 布局与文案
    im = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    d.rounded_rectangle([6, 6, W - 6, H - 6], radius=24,
                        fill=(244, 245, 241, 255),
                        outline=(228, 235, 231, 255), width=2)
    if not F:
        d.line([(26, H // 2), (W - 26, H // 2)], fill=(228, 235, 231, 255), width=2)
        im.save(path); return
    f_lbl = font(r'C:\Windows\Fonts\msyh.ttc', 12) or F
    f_big = font(r'C:\Windows\Fonts\msyh.ttc', 30) or F
    f_mid = font(r'C:\Windows\Fonts\msyh.ttc', 15) or F
    f_small = font(r'C:\Windows\Fonts\msyh.ttc', 13) or F
    d.text((20, 18), '岁月长河', font=f_small, fill=INK3 + (255,))
    d.text((20, 48), '已在此间', font=f_lbl, fill=INK2 + (255,))
    d.text((W - 96, 48), '前方尚有 55 年', font=f_lbl, fill=INK2 + (255,))
    d.text((20, 70), '24 个年头', font=f_big, fill=JADE + (255,))
    if not compact:   # 4×2：右侧补「零 4 个月 29 天」
        d.text((200, 78), '零 4 个月 29 天', font=f_mid, fill=INK2 + (255,))
    line_y = 150 if compact else 104
    d.line([(20, line_y), (W - 20, line_y)], fill=(228, 235, 231, 255), width=2)
    by = 162 if compact else 116
    d.text((20, by), '最近 · 元旦', font=f_mid, fill=INK2 + (255,))
    d.text((W - 60, by - 2), '48 天', font=f_mid, fill=JADE + (255,))
    im.save(path)


DRAW = os.path.join(ROOT, 'app', 'src', 'main', 'res', 'drawable')
preview(os.path.join(DRAW, 'widget_preview_small.png'), 250, 250, True)
preview(os.path.join(DRAW, 'widget_preview_wide.png'), 420, 210, False)
print('widget previews (small 2x2 / wide 4x2) ok, font:', bool(F))
