# Android Root 与 LSPosed 实战手册

> 给半新手的一本"看得懂、用得上"的玩机指南。从 KernelSU / SukiSU / LSPosed / Xposed / Zygisk / susfs / ZygiskNext / HMA 一路讲清。

---

## 🎯 这本手册适合谁

- 已经解锁 BL、刷好 KernelSU / SukiSU 的玩家
- 装过几个模块但概念模糊（分不清 Zygisk 和 LSPosed 是啥关系）
- 想自己写简单模块但不知道从何下手
- 撞坑之后不知道去哪查（银行 App 闪退、OTA 后 root 没了、模块不工作...）

## ⚠️ 不适合谁

- 还没解锁 BL 的（先看厂商解锁教程）
- 完全没碰过 root 的纯小白（先随便刷一次再回来看）
- 想看内核源码级深度原理的（本手册点到为止）

---

## 🚀 30 秒决策树（新手先看这）

### 我该用 KSU 还是 LSP 改东西？

```
要改的东西在哪一层？
│
├─ 改文件系统（删预装 App、改 hosts、改字体、改 build.prop）
│     → 用 KSU 模块
│
├─ 改内核 / 加载 .ko / 改 SELinux / 改 init.rc
│     → 用 KSU 模块
│
├─ 改某个 App 内部的 Java 方法（去广告、防撤回、解锁会员）
│     → 用 LSP 模块
│
├─ 要 hook 多个 App 的同一种方法
│     → 用 LSP 模块（一个模块勾选多个作用域）
│
└─ 在 App 启动前拦截（改 ClassLoader、绕签名校验）
      → 用 LSP 模块
```

**口诀**：KSU 改"环境"（文件、内核、init），LSP 改"行为"（Java 方法）。

### 我该装哪些模块？（2026 版）

| 需求 | 推荐 |
|---|---|
| 只用 root 改系统 | KSU + meta-overlayfs |
| 还要 hook App | + ZygiskNext v1.3.0+ + LSPosed |
| 还要过银行 App 检测 | + susfs（SukiSU 已集成）+ prop 隐藏（见 25 章） |
| 还要防应用列表被扫 | + Hide My Applist |
| 还要过 Google Play 完整性 | + Play Integrity Fix |

---

## 📖 章节导航（按学习路径排）

### 第一阶段：基础概念（**必读**，刷完机先把这七篇看了）

| # | 标题 | 讲什么 |
|---|---|---|
| 01 | [名词宇宙](01-名词宇宙.md) | 所有缩写一张表（KSU/SukiSU/LSP/Xposed/Zygisk/susfs/ZygiskNext/HMA...） |
| 02 | [我是怎么跑起来的](02-我是怎么跑起来的.md) | Android 启动流程 + KSU/Zygisk 在哪个时点介入 |
| 03 | [手机里的目录都是啥](03-手机里的目录都是啥.md) | /system /data /sdcard /storage/emulated/0 /dev/block |
| 04 | [分区那些事儿](04-分区那些事儿.md) | boot / init_boot / super / A/B 槽 |
| 05 | [挂载与覆盖](05-挂载与覆盖.md) | mount 是什么 / OverlayFS / whiteout / mknod |
| 06 | [权限与 SELinux](06-权限与SELinux.md) | UID/GID / Capabilities / SELinux 严格宽容 |
| 07 | [进程与注入](07-进程与注入.md) | 进程隔离 / Zygisk 怎么进 App 进程 |

### 第二阶段：方案（搞清楚自己装的是啥）

| # | 标题 | 讲什么 |
|---|---|---|
| 11 | [Root 方案有哪些](11-Root方案有哪些.md) | KSU / Magisk / APatch 横向对比 |
| 12 | [KernelSU 是什么](12-KernelSU是什么.md) | 内核态 root 原理 |
| 13 | [SukiSU 和其他分支](13-SukiSU和其他分支.md) | SukiSU / MKSU / KernelSU-Next，LKM/KMI/GKI |
| 14 | [Hook 框架演化史](14-Hook框架演化史.md) | Xposed → EdXposed → LSPosed → Vector |
| 15 | [Zygisk 和 ZygiskNext](15-Zygisk和ZygiskNext.md) | 注入 Zygote 的原理 + v1.3.0 整合 Shamiko |
| 16 | [LSPosed 是什么](16-LSPosed是什么.md) | LSPlant、ART hook 原理 |
| 17 | [LSPatch 无 Root 方案](17-LSPatch无Root方案.md) | 不想 root 也能用 Xposed |

### 第三阶段：隐藏 Root（被银行 App 检测再看）

| # | 标题 | 讲什么 |
|---|---|---|
| 21 | [为什么要隐藏 Root](21-为什么要隐藏Root.md) | App 检测手段概览 |
| 22 | [三层防御体系](22-三层防御体系.md) | 内核层 + Zygisk 层 + App 层（2026 版） |
| 23 | [susfs 与内核层隐藏](23-susfs与内核层隐藏.md) | susfs 原理 + SukiSU 集成 + KSU selinux_hide |
| 24 | [卸载模块：KSU App Profile 与 ZN 排除列表](24-卸载模块-KSU与ZN排除列表.md) | KSU 内核层卸载 + ZN just_umount 模式 |
| 25 | [prop 隐藏](25-prop隐藏.md) | 改系统属性（ro.debuggable 等），ZN v1.3.0+ 没接的活 |
| 26 | [Hide My Applist 藏应用](26-HideMyApplist藏应用.md) | HMA 用法 + 模板配置 |
| 27 | [Play Integrity 过检测](27-PlayIntegrity过检测.md) | PIF 模块 |

### 第四阶段：用现成模块

| # | 标题 | 讲什么 |
|---|---|---|
| 31 | [KSU 模块是什么](31-KSU模块是什么.md) | 格式、生命周期、能干什么 |
| 32 | [LSP 模块是什么](32-LSP模块是什么.md) | 格式、作用域、能干什么 |
| 33 | [KSU 还是 LSP 决策树](33-KSU还是LSP决策树.md) | 一个功能该用哪个 |
| 34 | [模块从哪找](34-模块从哪找.md) | 官方仓库、第三方仓库、安全注意 |

### 第五阶段：App Profile（KSU 独有的精细控制）

| # | 标题 | 讲什么 |
|---|---|---|
| 41 | [App Profile 是什么](41-AppProfile是什么.md) | 把 root 关进笼子 |
| 42 | [用户身份控制](42-用户身份控制.md) | UID / GID / Groups |
| 43 | [权能 Capabilities](43-权能Capabilities.md) | Linux capabilities 详解 |
| 44 | [SELinux 域定制](44-SELinux域定制.md) | 自定义 context + 规则 |
| 45 | [防逃逸](45-防逃逸.md) | NO_NEW_PRIVS 与逃逸案例 |
| 47 | [实战配置例子](47-实战配置例子.md) | 终端/防火墙/银行/广告过滤 4 个完整例子 |

### 第六阶段：OTA 与备份（保住成果）

| # | 标题 | 讲什么 |
|---|---|---|
| 51 | [AB 槽与无缝更新](51-AB槽与无缝更新.md) | A/B 分区原理 |
| 52 | [保 Root 升级](52-保Root升级.md) | OTA 流程 + "安装到另一槽位" |
| 53 | [该备份什么](53-该备份什么.md) | boot / init_boot / vbmeta / dtbo |
| 54 | [降级解锁升级链路](54-降级解锁升级链路.md) | 高版本不能解锁怎么办 |

### 第七阶段：写模块（自己动手）

| # | 标题 | 讲什么 |
|---|---|---|
| 61 | [KSU 模块开发入门](61-KSU模块开发入门.md) | module.prop / 目录结构 / 打包 |
| 62 | [KSU 启动脚本](62-KSU启动脚本.md) | post-fs-data / service / boot-completed / preinit |
| 63 | [KSU 改系统文件](63-KSU改系统文件.md) | system/ 目录 / REMOVE / REPLACE / whiteout |
| 64 | [KSU 元模块](64-KSU元模块.md) | Metamodule 架构 |
| 65 | [KSU 模块 WebUI](65-KSU模块WebUI.md) | WebUI + 配置系统 |
| 66 | [LSP 模块开发入门](66-LSP模块开发入门.md) | AndroidManifest / xposed_init / 入口类 |
| 67 | [LSP 核心 API](67-LSP核心API.md) | IXposedHookLoadPackage / XC_MethodHook / XposedHelpers |
| 68 | [LSP 进阶技巧](68-LSP进阶技巧.md) | 构造函数 hook / 方法替换 / XSharedPreferences |
| 69 | [写模块的踩坑指南](69-写模块的踩坑指南.md) | 混淆 / ClassLoader / 作用域 |

### 第八阶段：救砖与排查（撞坑再来）

| # | 标题 | 讲什么 |
|---|---|---|
| 71 | [开不了机怎么办](71-开不了机怎么办.md) | 救砖手册（安全模式 / adb / fastboot） |
| 72 | [模块不工作排查](72-模块不工作排查.md) | SELinux / 日志 / 签名 / 顺序 |
| 73 | [App 闪退排查](73-App闪退排查.md) | 银行 App 检测列表 + 反检测排查 |
| 74 | [SELinux 拒绝怎么看](74-SELinux拒绝怎么看.md) | avc 日志 / audit2allow |
| 75 | [OTA 后 Root 丢失](75-OTA后Root丢失.md) | 找回 root |

### 第九阶段：安全与信任

| # | 标题 | 讲什么 |
|---|---|---|
| 81 | [第三方 Fork 的风险](81-第三方Fork的风险.md) | 怎么验证 .ko / APK 不带毒 |
| 82 | [adb 安全](82-adb安全.md) | 默认 root 的坑 |
| 83 | [模块可信度判断](83-模块可信度判断.md) | 看哪些信号 |

### 收尾

| # | 标题 | 讲什么 |
|---|---|---|
| 90 | [FAQ](90-FAQ.md) | 高频问题汇总 |

---

## ⚡ 速查表

放在 `cheatsheets/` 目录下，可以存到手机随时翻：

- [常用路径](cheatsheets/常用路径.md)
- [常用命令](cheatsheets/常用命令.md)
- [救砖速查](cheatsheets/救砖速查.md)
- [名词速查](cheatsheets/名词速查.md)
- [决策树](cheatsheets/决策树.md)

---

## 📚 参考资料

- [KernelSU GitHub](https://github.com/tiann/KernelSU)
- [SukiSU-Ultra GitHub](https://github.com/SukiSU-Ultra/SukiSU-Ultra)
- [LSPosed GitHub](https://github.com/LSPosed/LSPosed)
- [ZygiskNext GitHub](https://github.com/Dr-TSNG/ZygiskNext)
- [Hide My Applist GitHub](https://github.com/Dr-TSNG/Hide-My-Applist)
- [Play Integrity Fix GitHub](https://github.com/chiteroman/PlayIntegrityFix)

---

## 📄 许可证

MIT License

## 🤝 贡献

欢迎提 Issue / PR。
