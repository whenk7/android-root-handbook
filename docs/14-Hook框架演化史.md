# 14 · Hook 框架演化史（Xposed → EdXposed → LSPosed → Vector）

> Hook 框架的历史就是一部"对抗 Android 收紧"的历史。这篇讲清楚各个框架的关系。

---

## 📜 时间线

```
2014 ── Xposed 诞生（rovo89）
            │
2017 ── Xposed 停更（Android 8 之后适配困难）
            │
2019 ── EdXposed 出现（基于 Riru）
            │
2020 ── LSPosed 出现（EdXposed 的继任者）
            │
2021 ── Riru 停更（作者 Rikka 弃坑）
            │
2022 ── LSPosed 切换到 Zygisk
            │
2023 ── Vector 出现（LSPosed 的现代 fork）
            │
2024 ── LSPosed 主线缓慢维护，Vector 活跃
```

---

## 🥇 Xposed（祖师爷，已停更）

- **作者**：rovo89
- **年份**：2014 - 2017
- **工作方式**：**替换 app_process**（Zygote 的启动二进制）

### 原理

Xposed 直接改 `/system/bin/app_process`，在 Zygote 启动时加载 `XposedBridge.jar`。

**优点**：简单粗暴  
**缺点**：要改 /system 分区，破坏系统完整性

### 为啥停更

Android 8 引入了：
- **ART 大改**：AOT 编译、JIT 混合
- **dm-verity**：系统完整性校验
- **Treble**：vendor 和 system 分离

Xposed 跟不上这些变化，rovo89 弃坑。

### 遗产

**XposedBridge API** 成为事实标准——后续所有框架（EdXposed、LSPosed）都兼容这套 API。

---

## 🥈 EdXposed（已停更）

- **作者**：ElderDrivers 团队
- **年份**：2019 - 2021
- **工作方式**：基于 Riru

### 原理

不替换 app_process，而是用 **Riru**（一个 Zygote 注入框架）把代码注入 Zygote。

**优点**：不改 /system，systemless  
**缺点**：依赖 Riru

### 为啥停更

2021 年 Riru 作者 Rikka 宣布停更，EdXposed 失去根基。

### 分支

- **SandHook** 分支：用 SandHook 做 ART hook
- **YAHFA** 分支：用 YAHFA 做 ART hook

两个分支后来合流到 LSPosed。

---

## 🥉 LSPosed（当前主流）

- **作者**：LSPosed 团队（LoveSy、yujincheng08 等）
- **年份**：2020 - 至今
- **工作方式**：基于 Zygisk
- **hook 引擎**：LSPlant

### 原理

```
Zygote 启动
   ↓
Zygisk 注入 liblspd.so
   ↓
LSPosed 初始化
   ↓
用 LSPlant hook ART 方法
   ↓
加载启用的模块
```

**优点**：
- systemless（不改 /system）
- 作用域机制（模块只对勾选的 App 生效）
- 活跃维护

**缺点**：
- 依赖 Magisk/KSU + Zygisk

### 作用域（Scope）

LSPosed 的核心创新：每个模块声明它作用于哪些 App，LSPosed 只在那些 App 进程里加载它。

**意义**：
- 减少全局污染
- 提升稳定性（一个模块崩了不影响其他 App）
- 对抗检测（银行 App 看不到 hook 框架）

---

## 🎨 Vector（LSPosed 的现代 fork）

- **作者**：JingMatrix
- **年份**：2023 - 至今
- **仓库**：`JingMatrix/Vector`

### 改进

- 支持**最新 Android 版本**（LSPosed 主线跟进慢）
- 现代化代码
- 修了一些 LSPosed 的 bug

### 兼容

跟 LSPosed API 完全兼容。你可以理解为"LSPosed 的活跃分支"。

---

## 🔧 LSPatch（无 root 方案）

- **作者**：LSPosed 团队
- **年份**：2022 - 至今
- **仓库**：`LSPosed/LSPatch`

### 原理

把 LSPosed 的代码**直接打进目标 APK**：

```
原 APK
  ↓ LSPatch 处理
塞入 LSPosed 代码 + 你选的模块
  ↓ 重新打包 + 签名
新 APK（装到手机直接用，不需要 root）
```

### 优点

- 不需要 root
- 不需要 LSPosed 框架

### 缺点

- 每个 App 要单独打包
- 签名变了（部分 App 会检测）
- 模块更新要重新打包

### 适用场景

- 不想 root
- 只需要 hook 一两个 App
- 临时测试

---

## 🆚 横向对比

| 维度 | Xposed | EdXposed | LSPosed | Vector | LSPatch |
|---|---|---|---|---|---|
| 状态 | 停更 | 停更 | 主线缓慢 | 活跃 | 活跃 |
| 改 /system | 是 | 否 | 否 | 否 | 否 |
| 需要 root | 是 | 是 | 是 | 是 | **否** |
| 需要 Magisk/KSU | 否 | 是 | 是 | 是 | 否 |
| 作用域机制 | 无 | 无 | 有 | 有 | 有 |
| API 兼容 | XposedBridge | XposedBridge | XposedBridge | XposedBridge | XposedBridge |

---

## 🎯 怎么选

### 有 root（Magisk/KSU）

→ **LSPosed** 或 **Vector**

### 不想 root

→ **LSPatch**

### 想试新特性

→ **Vector**

---

## 🧬 模块兼容性

**所有 Xposed 模块理论上都能跑在 LSPosed/Vector 上**——因为 API 是同一套 XposedBridge。

**但实际上**：
- 老模块（Xposed 时代）可能用了已废弃的 API
- 部分模块对 LSPosed 的作用域机制不适配
- 混淆的 App 类名变了，hook 不到

---

## 📚 资料来源

- [Xposed 官方仓库](https://github.com/rovo89/XposedBridge)
- [EdXposed 仓库](https://github.com/ElderDrivers/EdXposed)
- [LSPosed 仓库](https://github.com/LSPosed/LSPosed)
- [Vector 仓库](https://github.com/JingMatrix/Vector)
- [LSPatch 仓库](https://github.com/LSPosed/LSPatch)
