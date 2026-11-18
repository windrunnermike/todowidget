# 待办组件 TodoWidget

小米 15 / HyperOS 上的桌面待办小组件：一个 4×6 的大组件显示「每日待办 + 长期待办 + DDL」，再加一个 2×2 的最近 DDL 倒计时组件。

零第三方依赖：纯 Android RemoteViews + Kotlin，不需要 AndroidX，数据存在 SharedPreferences。

## 功能

- **每日待办**：勾选即完成，已完成自动划线置灰并沉到列表底部
- **长期待办**：左侧色条 + 进度徽章（`6/12 章`、`31%`、`1/3`）
- **DDL**：顶部「最近 DDL」条 + 每条任务的截止时间小字 + 右侧倒计时徽章
  - 已逾期 = 深红底白字，≤24 小时 = 红，≤3 天 = 橙，≤14 天 = 琥珀，其余 = 灰蓝
  - 排序：逾期 → 24 小时内 → 3 天内 → 按日期升序 → 无 DDL 沉底
- **点一下就完成**：点组件里的任意一行即可切换完成状态，立即重绘
- **两个小组件**：4×6 大组件、2×2 倒计时组件（大数字 `3 天后` / `5 小时前逾期`）
- **提醒**：DDL 前一小时通知；每天 9:00 汇总通知

## 怎么拿到 APK（GitHub Actions 云端编译）

这台电脑没装 JDK / Android SDK，所以走 GitHub 云端编译，不需要本地装任何东西。

1. 在 GitHub 新建一个**空仓库**（不要勾选 README，私有仓库也行）
2. 把本目录推到仓库：

```bash
cd TodoWidget
git init
git add .
git commit -m "init: todo widget"
git branch -M main
git remote add origin https://github.com/<你的用户名>/<仓库名>.git
git push -u origin main
```

3. 打开仓库的 **Actions** 页 → 等 `Build APK` 跑完（约 2–4 分钟）→ 点进这次运行 → 页面底部 **Artifacts** 下载 `TodoWidget-debug.zip` → 解压得到 `app-debug.apk`
4. 把 APK 传到手机安装（HyperOS 会提示「未知来源」，允许即可）

> 以后想改代码，改完 push 一次，就会自动重新编译出新 APK。

## 手机上怎么用

1. 安装后先打开一次 App（否则通知权限不会被触发）
2. 回到桌面 → 长按空白处 → **添加小部件** → 找到「待办组件」
   - `每日待办 · 大组件` → 拖到桌面，可以拉伸到 4×6 装满一屏
   - `最近 DDL · 倒计时` → 2×2 放在旁边
3. 点组件底部「＋ 快速添加」进 App 新建待办；点任意一行可以直接勾选完成
4. 长按某一行 → 编辑 / 设截止时间 / 删除
5. 想让它更透明：HyperOS 的桌面设置里可以调小部件透明度，或者用自带的「模糊」效果

## 项目结构

```
TodoWidget/
├─ .github/workflows/build.yml   CI：push 即编译出 APK
├─ build.gradle.kts              AGP 8.5.2 + Kotlin 1.9.24
├─ settings.gradle.kts
├─ gradle.properties
└─ app/
   ├─ build.gradle.kts           compileSdk 34 / minSdk 26 / targetSdk 34
   └─ src/main/
      ├─ AndroidManifest.xml
      ├─ java/com/mike/todowidget/
      │  ├─ Task.kt              数据模型 + JSON 序列化
      │  ├─ Store.kt             SharedPreferences 读写
      │  ├─ Fmt.kt               日期格式、倒计时文案、颜色分级、排序
      │  ├─ WidgetRenderer.kt    两个组件的 RemoteViews 渲染
      │  ├─ WidgetProviders.kt   AppWidgetProvider
      │  ├─ ToggleReceiver.kt    点行切换完成 + 开机重排
      │  ├─ Reminder.kt          通知 + 闹钟
      │  └─ MainActivity.kt      列表 / 新建 / 编辑
      └─ res/
         ├─ layout/widget_todo.xml   4×6 大组件（8 个今日槽位 + 8 个长期槽位）
         ├─ layout/widget_ddl.xml    2×2 倒计时
         ├─ xml/widget_*_info.xml    组件声明
         ├─ drawable/                圆角卡片、徽章、进度条、勾选图标
         └─ values/                  colors / strings / themes
```

## 设计说明

- **自适应行数**：按组件实际高度算能放几行（`((高dp - 150) / 40).coerceIn(2, 8)`），今日约占 55%，其余给长期待办；拉伸组件时自动增减
- **数据格式**：`[{"id","title","type","deadline","done","doneAt","counter","progress","createdAt"}]`，`type` 为 `today` / `long`，`deadline=0` 表示没有截止时间
- **刷新时机**：系统每 30 分钟一次（`updatePeriodMillis`）、点行切换、打开 App、开机、组件尺寸变化、闹钟触发

## 已知边界

- 桌面壁纸不是真毛玻璃：组件用半透明白（`#E9FFFFFF`）+ 描边模拟毛玻璃，真模糊需要 Android 12 的 `RenderEffect`，小部件里用不了
- 提醒是「一分钟级」精度，Doze 模式下可能晚几分钟；没有 `SCHEDULE_EXACT_ALARM` 授权时会退化成普通闹钟
- 数据只存在本机（SharedPreferences），没有云同步、没有备份导出
