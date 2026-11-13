import os, shutil, re

# 版本化交付助手：从 app/build.gradle 读取版本号，把 release APK 复制为
#   dist/岁月长河-v{versionName}({versionCode}).apk
# 与项目约定一致（同名自动追加 _i，不覆盖历史）。
# 运行前请先：gradle assembleRelease

ROOT = os.path.dirname(os.path.abspath(__file__))
APK = os.path.join(ROOT, 'app', 'build', 'outputs', 'apk', 'release', 'app-release.apk')
DIST = os.path.join(ROOT, 'dist')
os.makedirs(DIST, exist_ok=True)

gradle = open(os.path.join(ROOT, 'app', 'build.gradle'), encoding='utf-8').read()
vc = re.search(r'versionCode\s+(\d+)', gradle).group(1)
vn = re.search(r"versionName\s+'([^']+)'", gradle).group(1)
print('versionName', vn, 'versionCode', vc)

if not os.path.exists(APK):
    raise SystemExit('APK 不存在，请先运行 gradle assembleRelease')

def unique(path):
    if not os.path.exists(path):
        return path
    base, ext = os.path.splitext(path)
    i = 1
    while os.path.exists(f'{base}_{i}{ext}'):
        i += 1
    return f'{base}_{i}{ext}'

out_apk = unique(os.path.join(DIST, f'岁月长河-v{vn}({vc}).apk'))
shutil.copy2(APK, out_apk)
print('APK ->', out_apk, os.path.getsize(out_apk), 'bytes')
print('可复制到 apk/ 目录提交，或直接分发。')
