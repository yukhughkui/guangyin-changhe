# 光阴长河（GuangYin ChangHe）

> 已在此间，前路尚有。

一款**本地安卓应用**：以写意河流与「X 年 零 X 个月 X 天」记录你已走过的时光，与按预期寿命推算的剩余岁月；并提供 2×2 / 4×2 桌面小组件，不必打开 App 也能看见。不联网、不上传，所有数据只存在你自己的手机里。

![光阴长河图标](docs/icon.png)

## 下载与在线体验

- **安卓安装包（最新版）**：[点此下载 APK](https://github.com/yukhughkui/guangyin-changhe/releases/latest/download/guangyin-changhe.apk)
  下载后传到安卓手机安装即可（首次需允许「安装未知应用」）。该链接永远指向最新版本。
- **网页版（免安装，打开即体验）**：[在线打开光阴长河](https://yukhughkui.github.io/guangyin-changhe/)
  纯静态单页，直接当网页用；数据只存于你浏览器本地，关页即清。若自动跳转没生效，可直连 [长河.html](https://yukhughkui.github.io/guangyin-changhe/%E9%95%BF%E6%B2%B3.html)。

## 功能

- **首页写意河流**：上游收束、下游开阔的写意河，标「归去来兮 / 前路 / 归处 / 此刻」，把人生拉成一条河。
- **已在此间 / 前方尚有**：以「X 年 零 X 个月 X 天」呈现活过的时光，以及按预期寿命推算的剩余。
- **四类刻度**：人生节点、年岁刻度、时节、纪念日；每组默认只显示最近一张，其余可展开。
- **桌面小组件**：2×2（已在此间 + 最近）与 4×2（含前路尚有），随系统日/夜主题自动切换（浅色壁纸素白·水纹，深色壁纸夜航墨绿），点击回 App。
- **夜间模式**：跟随系统，连 Android 状态栏也一并变暗。
- **本地优先**：所有设置存于本机 localStorage / SharedPreferences，不上传任何数据。

## 安装

下载上方「安卓安装包」链接中的最新 APK，传到手机安装即可。历史版本亦可在仓库 `apk/` 目录取得。

## 构建

- 用 Android Studio 打开本仓库根目录。
- 网页源 `长河.html` 经 `_gen_assets.py` 注入桥接代码后生成 `app/src/main/assets/changhe.html`，由 WebView 加载。改动网页后运行 `python _gen_assets.py` 再构建。
- 如需构建**签名** release：复制 `local.properties.example` 为 `local.properties`，填入你的 `changhe.keystore` 与密码（公开仓库不含密钥文件，也不含任何密码明文）。无密钥时 `assembleRelease` 不会签名，可正常运行 debug 版。

## 图标

深墨绿底 + 米白同心不闭合圆环（年轮 / 涟漪）+ 中心白点，呼应「长河」。

## 许可

个人使用；未声明开源许可。