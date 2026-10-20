---
title: "Root 方案有哪些（横向对比）"
---

> 市面上能拿到 root 的方案不止一种。这篇做横向对比，帮你搞清楚自己装的是哪个、为什么这么选。

---

## 🏆 主流方案

### Magisk（老大哥）

- **作者**：topjohnwu（吴泓霖，前 Google 员工）
- **首次发布**：2016 年
- **工作层**：用户态
- **核心创新**："systemless root"——不改 /system 分区
- **Zygisk**：原生内置
- **隐藏**：ZygiskNext v1.3.0+（已整合 Shamiko）

**优点**：
- 生态最成熟
- 模块最多
- Zygisk 原生内置
- 文档全

**缺点**：
- 工作在用户态，特征明显
- 容易被银行/支付 App 检测

---

### KernelSU（KSU，新王）

- **作者**：tiann（吴伟，Weishu，Epic 框架作者）
- **首次发布**：2022 年
- **工作层**：内核态
- **核心创新**：直接在内核里给 root，特征少
- **Zygisk**：不内置，要装 ZygiskNext
- **隐藏**：susfs

**优点**：
- 工作在内核态，难检测
- App Profile 精细权限控制（Magisk 没有）
- Metamodule 架构，挂载可插拔
- 模块化好

**缺点**：
- 模块数量比 Magisk 少（但在追赶）
- Zygisk 要靠 ZygiskNext 补

---

### APatch（另一条路）

- **作者**：bmax121
- **工作层**：内核补丁
- **核心创新**：不动 boot.img，直接 patch 运行中的内核
- **依赖**：KernelPatch 框架

**优点**：
- 不需要 patch boot.img（不破坏厂商签名）
- KPM 模块系统

**缺点**：
- 社区相对小
- 兼容性不如 KSU

---

## 📊 横向对比

| 维度 | Magisk | KernelSU | APatch |
|---|---|---|---|
| 工作层 | 用户态 | 内核态 | 内核态 |
| 首次发布 | 2016 | 2022 | 2023 |
| 检测难度 | 易（特征多） | 难（在内核里） | 难 |
| Zygisk | 原生 | 要装 ZygiskNext | 要装 ZygiskNext |
| 模块生态 | 最丰富 | 追赶中 | 较少 |
| 模块格式 | ZIP | ZIP（兼容 Magisk） | KPM |
| 隐藏方案 | ZygiskNext | susfs | 类似 KSU |
| App Profile | 无 | 有 | 有 |
| 维护活跃度 | topjohnwu 个人 | tiann + 社区 | bmax121 |
| 模块挂载 | 内置 Magic Mount | Metamodule 可插拔 | 类似 KSU |
| 修改 boot.img | 是 | 是 | **否** |

---

## 🌿 KSU 的家族

KernelSU 是开源的，社区出了一堆 fork：

### SukiSU（SukiSU-Ultra）

- **作者**：ShirkNeko
- **特点**：
  - 支持非 GKI 和 GKI 1.0 内核
  - 内置 KPM 支持
  - 内置 susfs 集成
  - 管理器主题增强

**推荐人群**：用老内核或想要更多内置功能的人。

### KernelSU-Next

- **作者**：KernelSU-Next 团队
- **特点**：增加更多功能，跟随 KSU 主线

### MKSU（已停更）

- **作者**：5ec1cff
- **特点**：把 Magic Mount 移植到 KSU
- **现状**：已停止维护，被 KSU 的 metamodule 取代

---

## 🎯 怎么选

### 选 Magisk 如果

- 你手机老（Android 11 以下）
- 你要用最丰富的模块生态
- 你不在乎被部分银行 App 检测

### 选 KernelSU 如果

- 你手机比较新（GKI 内核）
- 你要更好的隐藏（susfs）
- 你要精细的 App Profile 权限控制
- 你喜欢更现代的架构

### 选 APatch 如果

- 你不想改 boot.img（保留厂商签名）
- 你愿意尝试新东西

### 选 SukiSU 如果

- 你已经在用 KernelSU，想要更多内置功能
- 你的内核不是 GKI 2.0

---

## 🤔 我已经装了 XX，能换吗？

**可以，但要小心**。

### Magisk → KernelSU

1. 卸载所有 Magisk 模块
2. 还原原厂 boot.img
3. 重启确认无 root
4. 用 KSU 的流程刷入

### KernelSU → Magisk

反向操作。**注意**：Magisk 不兼容 KSU 的 App Profile，迁移后所有 root 权限都是"全开"。

### KernelSU → SukiSU

相对平滑——SukiSU 是 KSU 的 fork，模块兼容。但**Manager 签名不同**，要：
1. 卸载官方 KSU Manager
2. 装 SukiSU Manager
3. 换 SukiSU 的 .ko（用 Manager 里的"刷入 LKM"）

---

## 📚 资料来源

- [KernelSU vs Magisk 官方对比](https://github.com/tiann/KernelSU/blob/main/website/docs/guide/difference-with-magisk.md)
- [Magisk GitHub](https://github.com/topjohnwu/Magisk)
- [KernelSU GitHub](https://github.com/tiann/KernelSU)
- [APatch GitHub](https://github.com/bmax121/APatch)
