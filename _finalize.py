import os, shutil, zipfile, subprocess, re

BUILD = r'C:\Users\28215\changhe-build'
WORK = r'D:\微信公众号\人生倒计时'
APK = os.path.join(BUILD, r'app\build\outputs\apk\release\app-release.apk')

# 从 build.gradle 读取版本号，作为交付文件名
gradle = open(os.path.join(BUILD, 'app', 'build.gradle'), encoding='utf-8').read()
vc = re.search(r'versionCode\s+(\d+)', gradle).group(1)
vn = re.search(r"versionName\s+'([^']+)'", gradle).group(1)
print('versionName', vn, 'versionCode', vc)

with zipfile.ZipFile(APK) as z:
    names = z.namelist()
    asset = z.read('assets/changhe.html').decode('utf-8')
    print('apk entries:', len(names))
    print('has classes.dex:', 'classes.dex' in names)
    print('snapshot export:', 'window.ChangheSnapshot' in asset)
    print('settings hook:', 'window.ChangheBridge.onSettingsChanged' in asset)

verify = subprocess.run(
    [r'C:\Users\28215\.local\lib\android-sdk\build-tools\34.0.0\apksigner.bat',
     'verify', APK],
    capture_output=True, text=True, encoding='utf-8', errors='replace',
    env=dict(os.environ, JAVA_HOME=r'C:\Users\28215\.local\lib\jdk17\jdk-17.0.21.0.2+1'))
print('apksigner:', (verify.stdout + verify.stderr).strip() or 'OK (verified)')

# —— 版本化命名，永不覆盖历史 ——
def unique(path):
    if not os.path.exists(path):
        return path
    base, ext = os.path.splitext(path)
    i = 1
    while os.path.exists(f'{base}_{i}{ext}'):
        i += 1
    return f'{base}_{i}{ext}'

OUT_APK = unique(os.path.join(WORK, f'光阴长河-v{vn}({vc}).apk'))
shutil.copy2(APK, OUT_APK)
print('APK ->', OUT_APK, os.path.getsize(OUT_APK), 'bytes')

OUT_SRC = unique(os.path.join(WORK, f'光阴长河Android-v{vn}'))
shutil.copytree(BUILD, OUT_SRC, ignore=shutil.ignore_patterns(
    'build', '.gradle', '.idea', '*.iml', '*.apk',
    '_finalize.py', '_verify_web.py', '_shots.py', '_webtest.html',
    '_desktop.png', '_s_*.png', '_bridge_test.html'))
print('source ->', OUT_SRC)

# 列出交付的源码关键文件
for root, dirs, files in os.walk(OUT_SRC):
    for f in files:
        print('  ', os.path.relpath(os.path.join(root, f), OUT_SRC))

print('\n历史交付物：')
for f in sorted(os.listdir(WORK)):
    if f.startswith('光阴长河') and (f.endswith('.apk') or 'Android' in f):
        p = os.path.join(WORK, f)
        tag = 'APK' if f.endswith('.apk') else 'SRC'
        print(f'  [{tag}] {f}  ({os.path.getsize(p) if os.path.isfile(p) else "dir"})')
