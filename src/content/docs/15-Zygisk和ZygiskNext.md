---
title: "Zygisk 和 ZygiskNext"
---

> Zygisk 是 Magisk 的核心功能，让代码能进 Zygote 进程。ZygiskNext 是 KSU 用户用的替代品。

---

## 🧬 Zygote 回顾

Zygote 是 Android 所有 App 进程的"妈妈"：

```
init
 └── Zygote (加载了 framework.jar、所有系统类)
      ├── fork → 微信进程
      ├── fork → B站进程
      └── fork → ...
```

**所有 App 都是 Zygote fork 出来的**。控制 Zygote = 控制所有 App。

---

## 💉 Zygisk 是什么

**Zygisk = Zygote + Magisk**。

**功能**：在 Zygote 启动时，把一段 `.so` 注入到 Zygote 里。

**效果**：Zygote fork 出新 App 进程时，注入的 `.so` 也跟着进新进程。

---

## 🏗️ Zygisk 的工作机制

### 加载流程

```
Zygote 启动
   ↓
Magisk/KSU 在 Zygote 启动前注入 libzygisk.so
   ↓
libzygisk.so 加载所有 Zygisk 模块
   ↓
Zygote 准备好，等 AMS 来 fork
   ↓
AMS: "给我 fork 一个微信进程"
   ↓
Zygote fork → 新进程里有 libzygisk.so + 所有 Zygisk 模块
   ↓
新进程 exec 微信代码 → 变成微信进程
   ↓
Zygisk 模块在微信进程里继续运行
```

### Zygisk 模块的结构

一个 Zygisk 模块是一个 `.so` 文件，实现这几个回调：

```c
// 伪代码
void on_load() {
    // Zygisk 加载我时调用
}

void pre_app_specialize() {
    // App 进程刚 fork 出来，还没 exec 时调用
    // 我可以读 App 的包名，决定要不要在这个 App 里激活
}

void post_app_specialize() {
    // App 进程已经设置好，可以 hook 了
    // LSPosed 在这里加载模块
}
```

### 常见的 Zygisk 模块

- **LSPosed**：ART hook 框架
- **ZygiskNext 自己**：在 KSU 上提供 Zygisk 能力
- **Play Integrity Fix**：过 Google 完整性检测

**注**：Shamiko 曾是常见 Zygisk 模块，但 **ZygiskNext v1.3.0+ 已整合其大部分功能**，不再需要单独安装。

---

## 🆚 ZygiskNext

### 是啥

**ZygiskNext** = 第三方做的 Zygisk 实现，让 KSU 也能用 Zygisk。

### 为啥需要它

**KSU 官方不内置 Zygisk**——KSU 作者认为 Zygisk 是"不必要的复杂性"。但社区需要 Zygisk（因为 LSPosed 等模块依赖它）。

**所以 ZygiskNext 出现了**——Dr-TSNG 写的，作为 KSU 模块提供 Zygisk 能力。

### 跟 Magisk Zygisk 的关系

| 维度 | Magisk Zygisk | ZygiskNext |
|---|---|---|
| 运行环境 | Magisk | KernelSU |
| 实现 | Magisk 内置 | 第三方模块 |
| 兼容 Zygisk 模块 | 是 | 是（大部分） |
| DenyList | 有 | 有 |

### 安装

ZygiskNext 是个**标准 KSU 模块**：
1. 下载 ZIP
2. KSU Manager 装
3. 重启

文件位置：`/data/adb/modules/zygisksu/`

---

## 🆕 ZygiskNext v1.3.0+ 重大更新

### 整合 Shamiko 大部分功能

**v1.3.0（2025-11）核心变化**：

> 整合 Shamiko 的大部分功能，排除列表增加「仅还原挂载」模式，允许在对应用加载模块的同时还原挂载变更。
>
> **安装 ZN 前无需卸载 Shamiko，Shamiko 即使安装也不会被加载。**

**意思**：你装了 Shamiko 也没用，ZN v1.3.0+ 不加载它——功能由 ZN 自己做。

### ZN 没整合的部分

- **prop 隐藏**：改系统属性（`ro.debuggable` / `ro.secure` 等）——Shamiko 靠 `service.sh` 做的，ZN 没接。详见 [25-prop隐藏](/android-root-handbook/25-prop隐藏)

### 排除列表三模式

| 模式 | 行为 | 效果 |
|---|---|---|
| `disabled` | 不启用排除列表 | 所有 App 都注入 Zygisk，不还原挂载 |
| `enabled` | 完全排除 | 列表里的 App **不注入** Zygisk 模块 |
| `just_umount` | **仅还原挂载**（v1.3.0 新增） | 列表里的 App **注入** Zygisk 模块，但**还原模块挂载** |

**详见 [24-卸载模块-KSU与ZN排除列表](/android-root-handbook/24-卸载模块-ksu与zn排除列表)**

---

## 🚫 排除列表（denylist）

ZygiskNext 自己维护一个**排除列表**（`/data/adb/zygisksu/denylist.txt`），配三种模式控制注入行为。**这是 ZN 自己的概念，不是 KSU 的**。

- KSU 用户对应的"按 App 控制"功能在 **App Profile → 卸载模块**（内核层）
- Magisk 用户对应的是 Magisk 自己的 DenyList

**详见 [24-卸载模块-KSU与ZN排除列表](/android-root-handbook/24-卸载模块-ksu与zn排除列表)**

---

## 🧪 亲手验证 Zygisk

```bash
adb shell

# 1. 看 ZygiskNext 是否安装
ls /data/adb/modules/ | grep -i zygisk

# 2. 看 Zygisk 是否在 Zygote 里
su -c "cat /proc/$(pidof zygote64)/maps | grep -i zygisk"
# 应该能看到 zygisk 相关的 .so

# 3. 看 Zygisk 是否进了某个 App
su -c "cat /proc/$(pidof com.tencent.mm)/maps | grep -i zygisk"

# 4. 看 ZN 排除列表
cat /data/adb/zygisksu/denylist.txt

# 5. 看 ZN 当前模式
su -c "/data/adb/modules/zygisksu/bin/zygiskd enforce-denylist"
```

---

## 🎯 经典疑问

### Q1: 我装了 LSPosed 但打不开 manager？

可能因为：
- ZygiskNext 没装/没启用
- LSPosed 模块没启用
- 没重启

### Q2: 我在 ZN 排除列表里加了银行 App，但它还是检测到 root？

因为 ZN 排除列表只管 Zygisk 层，**不能藏 root 文件/挂载/属性**——你需要：
- susfs 或 KSU App Profile 卸载模块（藏文件/挂载）
- prop 隐藏（改系统属性，见 [25-prop隐藏](/android-root-handbook/25-prop隐藏)）

### Q3: ZygiskNext 和 Zygisk 有什么区别？

对用户来说几乎没区别。ZygiskNext 是 KSU 版的 Zygisk 实现。

### Q4: 我能不能只用 ZygiskNext 不用 LSPosed？

可以。ZygiskNext 只提供"注入 Zygote"的能力，你爱加载啥加载啥。但没 LSPosed 你就用不了 LSP 模块。

---

## 📚 资料来源

- [ZygiskNext 仓库](https://github.com/Dr-TSNG/ZygiskNext)
- [Magisk Zygisk 文档](https://github.com/topjohnwu/Magisk/blob/master/docs/index.md)
