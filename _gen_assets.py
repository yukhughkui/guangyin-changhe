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


def font(path, size, index=0):
    try:
        return ImageFont.truetype(path, size, index=index)
    except Exception:
        return None


F = None
for p in [r'C:\Windows\Fonts\msyh.ttc', r'C:\Windows\Fonts\simsun.ttc', r'C:\Windows\Fonts\simhei.ttf']:
    F = font(p, 22)
    if F:
        break

def ripple(path, color=(203, 213, 208, 255)):
    # 浅色主题的「水纹」分隔：透明底上一道写意正弦波
    import math
    W, H = 600, 40
    im = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    y0 = H // 2
    amp = 5
    wl = 130.0
    pts = [(x, y0 + amp * math.sin(2 * math.pi * x / wl)) for x in range(W)]
    d.line(pts, fill=color, width=3)
    im.save(path)


def preview(path, W, H, compact):
    # 系统小组件选择器缩略图：与原生 widget 实际渲染对齐（素白浅色版）
    im = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    d.rounded_rectangle([6, 6, W - 6, H - 6], radius=24,
                        fill=(252, 252, 250, 255),
                        outline=(228, 235, 231, 255), width=2)
    if not F:
        d.line([(26, H // 2), (W - 26, H // 2)], fill=(228, 235, 231, 255), width=2)
        im.save(path); return

    f_lbl = font(r'C:\Windows\Fonts\msyh.ttc', 12) or F
    f_hero = font(r'C:\Windows\Fonts\msyh.ttc', 44, index=1) or F
    f_unit = font(r'C:\Windows\Fonts\msyh.ttc', 18) or F
    f_sub = font(r'C:\Windows\Fonts\msyh.ttc', 16) or F
    f_mark = font(r'C:\Windows\Fonts\msyh.ttc', 15) or F
    f_days = font(r'C:\Windows\Fonts\msyh.ttc', 22, index=1) or F

    LABEL = INK3 + (255,)
    HERO = INK + (255,)
    SUB = INK3 + (255,)
    MARK = INK2 + (255,)
    RIPPLE = (203, 213, 208, 255)

    import math
    if compact:
        # 2×2 小尺寸：竖向堆叠
        x = 18
        d.text((x, 22), '已在此间', font=f_lbl, fill=LABEL)
        d.text((x, 44), '24', font=f_hero, fill=HERO)
        d.text((x + 52, 70), '年', font=f_unit, fill=SUB)
        d.text((x, 92), '4 个月 30 天', font=f_sub, fill=SUB)
        ry = 122
        pts = [(x + i, ry + 4 * math.sin(2 * math.pi * (x + i) / 60)) for i in range(W - 2 * x)]
        d.line(pts, fill=RIPPLE, width=3)
        d.text((x, 140), '最近', font=f_lbl, fill=LABEL)
        d.text((x + 38, 140), '元旦', font=f_mark, fill=MARK)
        d.text((W - 70, 138), '47 天', font=f_days, fill=HERO)
    else:
        # 4×2 中尺寸：顶行 已在此间 / 前方尚有；大数 + 次行同一行
        x = 22
        d.text((x, 20), '已在此间', font=f_lbl, fill=LABEL)
        d.text((W - 210, 20), '前方尚有 55 年 零 7 个月', font=f_mark, fill=MARK)
        d.text((x, 44), '24', font=f_hero, fill=HERO)
        d.text((x + 56, 70), '年', font=f_unit, fill=SUB)
        d.text((x + 92, 70), '4 个月 30 天', font=f_sub, fill=SUB)
        ry = 112
        pts = [(x + i, ry + 4 * math.sin(2 * math.pi * (x + i) / 70)) for i in range(W - 2 * x)]
        d.line(pts, fill=RIPPLE, width=3)
        d.text((x, 130), '最近', font=f_lbl, fill=LABEL)
        d.text((x + 42, 130), '元旦', font=f_mark, fill=MARK)
        d.text((W - 84, 128), '47 天', font=f_days, fill=HERO)
    im.save(path)


DRAW = os.path.join(ROOT, 'app', 'src', 'main', 'res', 'drawable')
ripple(os.path.join(DRAW, 'widget_ripple.png'))
preview(os.path.join(DRAW, 'widget_preview_small.png'), 250, 250, True)
preview(os.path.join(DRAW, 'widget_preview_wide.png'), 420, 210, False)
print('widget assets (ripple + previews) ok, font:', bool(F))
