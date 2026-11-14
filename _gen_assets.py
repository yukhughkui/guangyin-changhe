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
    # 系统小组件选择器缩略图：与 v1.19 真实三行布局一致（标签 + 数值 + 分隔细线 + 最近）
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
    f_lbl = f(11, True)
    f_val = f(11 if compact else 13)
    f_mark = f(11 if compact else 12)
    f_days = f(13 if compact else 16, True)

    JADE = (60, 106, 96, 255)
    INK2 = (92, 108, 103, 255)
    LINE = (228, 235, 231, 255)

    x = 20 if compact else 26
    lw = 52 if compact else 68
    y1 = 46 if compact else 40
    y2 = y1 + (34 if compact else 40)
    ly = y2 + (32 if compact else 38)
    y3 = ly + (22 if compact else 26)

    d.text((x, y1), '已在此间', font=f_lbl, fill=JADE)
    d.text((x + lw, y1), '24 年 零 4 个月 30 天', font=f_val, fill=INK2)
    d.text((x, y2), '前方尚有', font=f_lbl, fill=JADE)
    d.text((x + lw, y2), '55 年 零 7 个月', font=f_val, fill=INK2)
    d.line([(x, ly), (W - x, ly)], fill=LINE, width=2)
    d.text((x, y3), '最近', font=f_lbl, fill=JADE)
    d.text((x + lw, y3), '元旦', font=f_mark, fill=INK2)
    d.text((W - x - (54 if compact else 66), y3 - 2), '47 天',
           font=f_days, fill=JADE)
    im.save(path)


DRAW = os.path.join(ROOT, 'app', 'src', 'main', 'res', 'drawable')
preview(os.path.join(DRAW, 'widget_preview_small.png'), 250, 250, True)
preview(os.path.join(DRAW, 'widget_preview_wide.png'), 420, 210, False)
print('widget assets (previews) ok, font:', bool(F))
