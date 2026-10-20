# 90 · FAQ（高频问题汇总）

> 收集最常被问到的问题。

---

## 🎯 概念类

### Q: KernelSU 和 LSPosed 是啥关系？

**完全独立的两件事**：
- KernelSU 是 root 方案（拿到 root 权限）
- LSPosed 是 hook 框架（hook Java 方法）

**关系**：LSPosed 跑在 KernelSU 提供的环境里（通过 ZygiskNext）。

### Q: Xposed 和 LSPosed 啥关系？

LSPosed 是 Xposed 的**继任者**。Xposed 已停更，LSPosed 是当前事实标准。

### Q: SukiSU 和 KernelSU 啥关系？

SukiSU 是 KernelSU 的**社区 fork**，加了非 GKI 支持、KPM、susfs 集成。

### Q: Zygisk 和 ZygiskNext 啥关系？

- **Zygisk** 是 Magisk 内置的功能
- **ZygiskNext** 是第三方实现，让 KSU 也能用 Zygisk

---

## 🔧 安装类

### Q: 我该装哪个？Magisk / KernelSU / SukiSU？

- **新手机（GKI 2.0）**：KernelSU 或 SukiSU
- **老手机**：SukiSU（支持非 GKI）
- **要最丰富模块**：Magisk
- **不想改 boot.img**：APatch

### Q: 我能不能同时装 Magisk 和 KernelSU？

**不能**——它们都改 init_boot，会冲突。

### Q: 我能不能从 Magisk 切到 KernelSU？

**能**，但要：
1. 卸载所有 Magisk 模块
2. 还原原厂 boot.img
3. 重启确认无 root
4. 走 KSU 安装流程

---

## 🛡️ 隐藏 Root 类

### Q: 银行 App 闪退怎么办？

按顺序检查：
1. SELinux 是不是 Enforcing
2. 装了 susfs 没（SukiSU 已集成）
3. ZygiskNext v1.3.0+ 装了吗（整合 Shamiko）
4. ZN 排除列表配了吗（just_umount 模式）
5. prop 隐藏做了吗（resetprop 或 Shamiko service.sh）
6. 装了 HMA 没
7. Play Integrity 过了吗

详见 [73-App闪退排查](73-App闪退排查.md)。

### Q: 我能不能只用 ZygiskNext 不用 susfs？

**不够**——ZN 藏进程，susfs 藏文件。两个都要。

### Q: DenyList 和 ZygiskNext 排除列表啥关系？

- **DenyList** 是 Magisk 的概念，KSU 里没有
- **ZygiskNext 排除列表** 是 ZN 自己的功能（三模式：disabled / enabled / just_umount）
- 详见 [24-卸载模块-KSU与ZN排除列表](24-卸载模块-KSU与ZN排除列表.md)

---

## 🛠️ 模块类

### Q: KSU 模块和 LSP 模块的区别？

| | KSU 模块 | LSP 模块 |
|---|---|---|
| 格式 | ZIP | APK |
| 改文件系统 | ✅ | ❌ |
| Hook Java 方法 | ❌ | ✅ |
| 作用域 | 全局 | 按 App |

详见 [33-KSU还是LSP决策树](33-KSU还是LSP决策树.md)。

### Q: 模块装了不生效？

按顺序排查：
1. 重启了吗
2. 启用了吗
3. metamodule 装了吗
4. SELinux 拦了吗

详见 [72-模块不工作排查](72-模块不工作排查.md)。

### Q: 我能用 Magisk 模块刷到 KSU 吗？

**大部分能**——格式兼容。但注意：
- `.replace` 文件不支持
- Recovery 模式刷入不支持
- 内置 Zygisk 不支持

---

## 🔄 OTA / 升级类

### Q: OTA 后 root 没了咋办？

按 [75-OTA后Root丢失](75-OTA后Root丢失.md) 找回。

### Q: 怎么 OTA 不丢 root？

OTA 完成后**先别重启**，在 KSU Manager 里点"安装到另一槽位"。

详见 [52-保Root升级](52-保Root升级.md)。

### Q: 我能不能直接从 Android 14 升到 16？

**全量包可以**——直接刷最新版全量包。

**增量 OTA 不行**——必须按顺序。

---

## 🔐 安全类

### Q: 我装的第三方 fork 安全吗？

按 [81-第三方Fork的风险](81-第三方Fork的风险.md) 排查。

### Q: 我该给所有 App 都配 App Profile 吗？

**不用**——默认配置已经够安全。只对**不信任的 App** 配。

### Q: 我能给所有 App 都禁用 root 吗？

**能，但 App 就 root 不了了**——本末倒置。

---

## 🐛 故障类

### Q: 开不了机咋办？

按 [71-开不了机怎么办](71-开不了机怎么办.md) 救砖。

### Q: 模块导致开机循环？

**安全模式**：
```
开机时按住音量-
→ KSU 跳过所有模块
→ 进系统卸载问题模块
→ 重启
```

### Q: SELinux 是啥？为啥要 Enforcing？

SELinux 是强制访问控制，详见 [06-权限与SELinux](06-权限与SELinux.md)。

**Enforcing 是必须的**——Permissive 会被银行 App 检测。

---

## 🎓 学习类

### Q: 怎么自己写模块？

- KSU 模块：从 [61-KSU模块开发入门](61-KSU模块开发入门.md) 开始
- LSP 模块：从 [66-LSP模块开发入门](66-LSP模块开发入门.md) 开始

### Q: 我能不能只用一个模块？

**能**——只装你需要的。

### Q: 推荐的入门组合？

```
最小配置（能用）：
- KernelSU / SukiSU
- meta-overlayfs（要改 /system 才装）

中等配置（hook App）：
+ ZygiskNext
+ LSPosed

完整配置（过检测）：
+ susfs（SukiSU 已带）
+ ZygiskNext v1.3.0+（整合 Shamiko）
+ prop 隐藏（resetprop 或 Shamiko service.sh）
+ Hide My Applist
+ Play Integrity Fix
```

---

## 📚 资料来源

本手册各章节。
