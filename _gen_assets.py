import os, math
from PIL import Image, ImageDraw, ImageFont

ROOT = os.path.dirname(os.path.abspath(__file__))
SRC = r'D:\微信公众号\人生倒计时\长河.html'

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
    # 系统小组件选择器缩略图：还原原生 widget 新版面（品牌 + 黛青短横 + 涟漪角标）
    im = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    d.rounded_rectangle([6, 6, W - 6, H - 6], radius=24,
                        fill=(244, 245, 241, 255),
                        outline=(228, 235, 231, 255), width=2)
    if not F:
        d.line([(26, H // 2), (W - 26, H // 2)], fill=(228, 235, 231, 255), width=2)
        im.save(path); return
    f_brand = font(r'C:\Windows\Fonts\msyh.ttc', 12 if compact else 13) or F
    sz = 13 if compact else 15
    f_line = font(r'C:\Windows\Fonts\msyh.ttc', sz) or F
    f_mark = font(r'C:\Windows\Fonts\msyh.ttc', 13 if compact else 14) or F
    # 品牌 + 黛青短横
    d.text((16, 16), '光阴长河', font=f_brand, fill=INK3 + (255,))
    ly = 34 if compact else 35
    d.line([(16, ly), (16 + (22 if compact else 26), ly)], fill=JADE + (255,), width=2)
    lived = '已在此间 24 年 零 4 个月 30 天'
    ahead = '前方尚有 55 年 零 7 个月 1 天'
    y1 = 54 if compact else 52
    dy = 44 if compact else 48
    d.text((16, y1), lived, font=f_line, fill=JADE + (255,))
    d.text((16, y1 + dy), ahead, font=f_line, fill=INK2 + (255,))
    line_y = y1 + dy * 2 + 10
    d.line([(16, line_y), (W - 16, line_y)], fill=(228, 235, 231, 255), width=2)
    by = line_y + 14
    d.text((16, by), '最近 · 元旦', font=f_mark, fill=INK2 + (255,))
    d.text((W - 58, by - 2), '47 天', font=f_mark, fill=JADE + (255,))
    # 右下角涟漪（同心椭圆描边）
    cx, cy = W - 28, H - 24
    for r in (20, 13):
        ox = int(r * (0.82 if compact else 0.86))
        d.ellipse([cx - ox, cy - r, cx + ox, cy + r], outline=JADE + (int(255 * 0.5),), width=2)
    im.save(path)


DRAW = os.path.join(ROOT, 'app', 'src', 'main', 'res', 'drawable')
preview(os.path.join(DRAW, 'widget_preview_small.png'), 250, 250, True)
preview(os.path.join(DRAW, 'widget_preview_wide.png'), 420, 210, False)
print('widget previews (small 2x2 / wide 4x2) ok, font:', bool(F))
