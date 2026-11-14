import os, math
from PIL import Image, ImageDraw, ImageFont

ROOT = os.path.dirname(os.path.abspath(__file__))
# 优先用同目录的网页源（公开仓库克隆后可独立构建）；本地开发时回退到工作区源文件
_SRC_LOCAL = os.path.join(ROOT, '长河.html')
_SRC_WORK = r'D:\微信公众号\人生倒计时\长河.html'
SRC = _SRC_LOCAL if os.path.exists(_SRC_LOCAL) else _SRC_WORK

html = open(SRC, encoding='utf-8').read()
old = 'function save(){ try{ localStorage.setItem(KEY, JSON.stringify(S)); }catch(e){} }'
new = (
    'function save(){ try{ localStorage.setItem(KEY, JSON.stringify(S)); }catch(e){} '
    'try{ if(window.ChangheBridge && window.ChangheBridge.onSettingsChanged) '
    'window.ChangheBridge.onSettingsChanged(); }catch(e){} }\n'
    'window.ChangheSnapshot = function(){'
    'try{'
    'var out={birth:S.birth,horizon:S.horizon,notify:!!S.notify,notifyTime:S.notifyTime||"07:30",marks:[]};'
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
    # 系统小组件选择器缩略图：与桌面真实三行布局、字号层级一致
    im = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    d.rounded_rectangle([6, 6, W - 6, H - 6], radius=24,
                        fill=(252, 252, 250, 255),
                        outline=(228, 235, 231, 255), width=2)
    if not F:
        im.save(path); return

    k = 1.0 if compact else 1.15
    def f(sz, bold=False):
        return font(r'C:\Windows\Fonts\msyh.ttc', int(sz * k),
                    index=1 if bold else 0) or F
    # 标签词是主角；数值退为小字；2×2 只到年
    sz_lbl = 17 if compact else 20
    sz_val = 12 if compact else 14
    sz_mark = 12 if compact else 13
    sz_days = 14 if compact else 15
    f_lbl = f(sz_lbl, True)
    f_val = f(sz_val)
    f_mark = f(sz_mark)
    f_days = f(sz_days)

    JADE = (60, 106, 96, 255)
    INK = (34, 48, 45, 255)
    INK2 = (92, 108, 103, 255)
    LINE = (228, 235, 231, 255)

    x = 18 if compact else 24
    lw = 72 if compact else 100
    y1 = 40 if compact else 24
    y2 = 110 if compact else 76
    ly = 170 if compact else 128
    y3 = 190 if compact else 142
    txt_lived = '24 年' if compact else '24 年 零 4 个月 30 天'
    txt_ahead = '55 年' if compact else '55 年 零 7 个月'

    def put(px, py, txt, fnt, color, sz, maxsz):
        d.text((px, py + (maxsz - sz) * k), txt, font=fnt, fill=color)

    put(x, y1, '已在此间', f_lbl, JADE, sz_lbl, sz_lbl)
    put(x + lw, y1, txt_lived, f_val, INK, sz_val, sz_lbl)
    put(x, y2, '前方尚有', f_lbl, JADE, sz_lbl, sz_lbl)
    put(x + lw, y2, txt_ahead, f_val, INK, sz_val, sz_lbl)
    d.line([(x, ly), (W - x, ly)], fill=LINE, width=2)
    put(x, y3, '最近', f_lbl, JADE, sz_lbl, sz_lbl)
    put(x + (40 if compact else 50), y3, '元旦', f_mark, INK2, sz_mark, sz_lbl)
    try:
        tw = d.textlength('47 天', font=f_days)
    except Exception:
        tw = f_days.getsize('47 天')[0]
    put(W - x - tw, y3, '47 天', f_days, JADE, sz_days, sz_lbl)
    im.save(path)


DRAW = os.path.join(ROOT, 'app', 'src', 'main', 'res', 'drawable')
preview(os.path.join(DRAW, 'widget_preview_small.png'), 250, 250, True)
preview(os.path.join(DRAW, 'widget_preview_wide.png'), 420, 210, False)
print('widget assets (previews) ok, font:', bool(F))
