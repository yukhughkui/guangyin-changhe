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

def preview(path, W, H, compact):
    # 系统小组件选择器缩略图：与原生 widget 实际渲染对齐
    # —— 标签（已在此间/前方尚有/最近）jade 加粗为视觉重点，数值墨色退为次一级
    im = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    d.rounded_rectangle([6, 6, W - 6, H - 6], radius=24,
                        fill=(244, 245, 241, 255),
                        outline=(228, 235, 231, 255), width=2)
    if not F:
        d.line([(26, H // 2), (W - 26, H // 2)], fill=(228, 235, 231, 255), width=2)
        im.save(path); return
    sz = 12 if compact else 14
    fb_label = font(r'C:\Windows\Fonts\msyh.ttc', sz, index=1) or font(r'C:\Windows\Fonts\msyh.ttc', sz) or F
    f_val = font(r'C:\Windows\Fonts\msyh.ttc', sz) or F
    f_mark = font(r'C:\Windows\Fonts\msyh.ttc', 12 if compact else 13) or F
    x = 16
    y0 = 20 if compact else 18
    lh = 24 if compact else 28

    def row(yy, lab, val):
        d.text((x, yy), lab, font=fb_label, fill=JADE + (255,))
        lw = d.textlength(lab, font=fb_label)
        d.text((x + lw + 8, yy), val, font=f_val, fill=INK2 + (255,))

    row(y0, '已在此间', '24 年 零 4 个月 30 天')
    row(y0 + lh, '前方尚有', '55 年 零 7 个月 1 天')
    line_y = y0 + lh * 2 + 12
    d.line([(x, line_y), (W - 16, line_y)], fill=(228, 235, 231, 255), width=2)
    by = line_y + 12
    d.text((x, by), '最近', font=fb_label, fill=JADE + (255,))
    nlw = d.textlength('最近', font=fb_label)
    d.text((x + nlw + 8, by), '元旦', font=f_mark, fill=INK2 + (255,))
    d.text((W - 56, by - 1), '47 天', font=f_mark, fill=JADE + (255,))
    im.save(path)


DRAW = os.path.join(ROOT, 'app', 'src', 'main', 'res', 'drawable')
preview(os.path.join(DRAW, 'widget_preview_small.png'), 250, 250, True)
preview(os.path.join(DRAW, 'widget_preview_wide.png'), 420, 210, False)
print('widget previews (small 2x2 / wide 4x2) ok, font:', bool(F))
