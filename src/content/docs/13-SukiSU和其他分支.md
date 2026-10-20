---
title: "SukiSU 和其他分支"
---

> KernelSU 是开源的，社区出了一堆 fork。这篇讲主流的 fork 以及它们的差异。

---

## 🌿 KSU 的家族树

```
                    KernelSU (tiann)
                          │
        ┌─────────────────┼─────────────────┐
        │                 │                 │
   SukiSU-Ultra    KernelSU-Next          MKSU
   (ShirkNeko)      ( rifatxuan )        (5ec1cff, 已停)
        │
   BakaSU / ReSukiSU 等更下游 fork
```

---

## 🐱 SukiSU（SukiSU-Ultra）

- **作者**：ShirkNeko
- **仓库**：`SukiSU-Ultra/SukiSU-Ultra`
- **当前状态**：活跃维护

### 相比官方 KSU 多了什么

1. **支持非 GKI 和 GKI 1.0 内核**
   - 官方 KSU 主要支持 GKI 2.0（Android 13+）
   - SukiSU 移植到更老的内核

2. **KPM 支持**
   - KPM = KernelPatch Module
   - 可以加载 APatch 风格的内核模块
   - 兼容部分 APatch 生态

3. **susfs 集成**
   - 官方 KSU 要自己装 susfs
   - SukiSU 编译时开 `CONFIG_KSU_SUSFS` 即可内置，Manager 里有专门 susfs 管理界面
   - Manager 里有专门的 susfs 管理界面

4. **管理器增强**
   - 主题美化
   - 更多设置选项

### 适合谁

- 用老内核的设备（GKI 1.0 / 非 GKI）
- 想要 susfs 开箱即用
- 喜欢更丰富功能的人

---

## 🆕 KernelSU-Next

- **作者**：KernelSU-Next 团队
- **仓库**：`KernelSU-Next/KernelSU-Next`
- **当前状态**：活跃维护

### 特点

- 跟随官方 KSU 主线
- 加入一些社区功能
- 改进某些实现细节

---

## 👻 MKSU（已停更）

- **作者**：5ec1cff
- **现状**：停止维护
- **历史意义**：把 Magisk 的 Magic Mount 移植到 KSU，后来被 KSU 官方的 metamodule 系统取代

---

## 🛠️ BakaSU / ReSukiSU

更下游的 fork，在 SukiSU 基础上继续改。一般用户不需要关注。

---

## 🔑 关键概念：LKM / KMI / GKI

### LKM（Loadable Kernel Module）

**内核模块**是 Linux 的概念：一段代码（`.ko` 文件），可以**动态加载到正在运行的内核里**。

```
正常内核：一个静态二进制（Image.gz / Image）
加载 LKM 后：内核 + mymodule.ko（融合到一起跑）
```

**.ko = Kernel Object**，类似 Windows 的 `.sys` 驱动。

### KMI（Kernel Module Interface）

Google 给每组"Android 大版本 + 内核大版本"定义的接口代号：

```
android12-5.10     ← Android 12 + Linux 5.10
android13-5.15     ← Android 13 + Linux 5.15
android14-6.1      ← Android 14 + Linux 6.1
android15-6.6      ← Android 15 + Linux 6.6
android16-6.12     ← Android 16 + Linux 6.12
```

**LKM 是按 KMI 编译的**——`android14-6.1-aarch64_kernelsu.ko` 只能在"Android 14 + Linux 6.1 + ARM64"的设备上跑。

### GKI（Generic Kernel Image）

**背景**：以前每个 Android 手机厂商都改自己的内核，碎片化严重。

**Google 的解决方案（Android 11+）**：推出 **GKI**——一个**标准化的内核**：
- 所有厂商必须用**同一个版本**的 Linux 内核（5.10、5.15、6.1、6.6...）
- 厂商的定制放到 **GKI 模块**（`.ko`）里，不动主内核

**GKI 1.0 / GKI 2.0** 是版本：
- **GKI 1.0** = Android 11/12 时代的内核（5.4 / 5.10）
- **GKI 2.0** = Android 13+ 时代的内核（5.15+）

### KSU 的两种部署方式

| 方式 | 干啥 | 优点 | 缺点 |
|---|---|---|---|
| **GKI 模式** | 把 KSU 代码**编译进内核**（生成新的 boot.img） | 稳定、能进 preinit | 要为你的设备**专门编译**内核 |
| **LKM 模式** | 不动 boot.img，**开机后把 kernelsu.ko 加载到内核** | 通用，一个 .ko 适配一类内核 | 依赖内核版本匹配（KMI 要一致） |

**SukiSU 的优势**：支持**非 GKI** 设备（官方 KSU 主要支持 GKI）。

---

## 🧩 第三方 Fork 的信任问题

### 风险

第三方 fork 可能：
- 加入恶意代码（偷数据、留后门）
- 不跟进上游安全修复
- 构建过程不透明

### 怎么验证

1. **看源码是否完整公开**
   - 完整 fork 仓库 + 完整构建脚本 = 可信
   - 只发二进制不发源码 = 警惕

2. **看构建方式**
   - CI 自动构建（GitHub Actions 公开记录）= 可信
   - 本地手动构建 = 不可验证

3. **对比上游**
   - 看 fork 跟上游的 diff
   - 改动越少越好

4. **看社区反馈**
   - 有没有人用
   - 有没有人报问题
   - 作者是否活跃

---

## 🧪 亲手看看你的 KSU 是哪个版本

```bash
adb shell
su

# 看内核版本
uname -r
# 输出示例：6.1.138-android14-11-g0c3d559bcd85-ab14529422

# 看 KSU 内核态版本
cat /proc/kallsyms | grep ksu | head -3

# 看 ksud 版本
ksud --version

# 看模块目录
ls /data/adb/modules/
```

---

## 🎯 推荐选择

### 新手机（Android 13+，GKI 2.0）

**官方 KernelSU**——最稳，跟着上游走。

### 老手机 / 特殊内核

**SukiSU**——支持非 GKI，集成 susfs。

### 想试新特性

**KernelSU-Next** 或 **SukiSU**——跟随上游加新功能。

### 不推荐

**不知名的第三方 fork**——除非你完全信任作者。

---

## 📚 资料来源

- [SukiSU-Ultra GitHub](https://github.com/SukiSU-Ultra/SukiSU-Ultra)
- [KernelSU-Next GitHub](https://github.com/KernelSU-Next/KernelSU-Next)
- [KernelSU 官方安装文档](https://github.com/tiann/KernelSU/blob/main/website/docs/guide/installation.md)
