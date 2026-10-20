# 26 · Play Integrity 过检测

> Play Integrity 是 Google 官方的"设备完整性"检测。Netflix、Google Pay、部分游戏用它。这篇讲怎么过。

---

## 🎯 是什么

**Play Integrity API** 是 Google 提供的官方"设备完整性"验证服务。

### 三种判定

| 判定 | 含义 |
|---|---|
| **MEETS_DEVICE_INTEGRITY** | 设备完整（基本） |
| **MEETS_BASIC_INTEGRITY** | 基本完整性（要未 root） |
| **MEETS_STRONG_INTEGRITY** | 强完整性（要硬件级密钥） |

### 跟 SafetyNet 的关系

- **SafetyNet**：老的检测 API，已废弃
- **Play Integrity**：新的，SafetyNet 的继任者

---

## 🔍 Play Integrity 怎么检测

### 服务端验证

```
App 调 Play Integrity API
   ↓
Google Play Services 收集设备信息：
   - 设备型号
   - Android 版本
   - 安全补丁级别
   - Bootloader 状态
   - SELinux 状态
   - 签名证书指纹
   - ...
   ↓
打包成 token，发给 Google 服务器
   ↓
Google 服务器返回"是否完整"
   ↓
App 根据结果决定要不要放行
```

**关键**：判定在 Google 服务端做，**不在本地**。

---

## 🛡️ Play Integrity Fix (PIF)

### 是啥

PIF 是一个 **Zygisk 模块**，**伪造设备指纹**，让 Google 以为你的手机是"原厂未 root"的。

**作者**：chiteroman（主流），还有 osm0sis 的 fork 等。

### 原理

```
Google Play Services 收集设备信息
   ↓
PIF 拦截，替换关键字段：
   - ro.product.model  → 改成 Pixel 系列
   - ro.product.device → 改成 Pixel 设备代号
   - ro.build.fingerprint → 改成官方指纹
   - ro.boot.locked → 改成 true
   - ...
   ↓
Google 服务器看到伪造的信息 → 判定"设备完整"
```

### 装法

1. 下载 PIF ZIP
2. 在 KSU/Magisk Manager 里装
3. 重启
4. 在 PIF 里配置（选个 Pixel 指纹）

---

## 🛠️ 配置

### 选指纹

PIF 需要"设备指纹"——一个真实存在的、未 root 的 Pixel 设备的指纹。

**指纹来源**：
- PIF 内置一些
- 社区维护的指纹列表

**配置方式**：

新版 PIF 一般是自动选指纹，老版要手动改 `pif.json`：

```json
{
  "PRODUCT": "panther",
  "DEVICE": "panther",
  "MODEL": "Pixel 7",
  "FINGERPRINT": "google/panther/panther:14/UQ1A.240205.002/11224170:user/release-keys",
  "SECURITY_PATCH": "2024-02-05",
  ...
}
```

---

## 🧪 测试

### 工具 1：Play Integrity API Checker

在 Google Play 搜，装上跑：

```
Play Integrity API Checker
   ↓
点击 Check
   ↓
看三个判定：
- MEETS_DEVICE_INTEGRITY: ✅/❌
- MEETS_BASIC_INTEGRITY: ✅/❌
- MEETS_STRONG_INTEGRITY: ✅/❌
```

**目标**：前两个 ✅，第三个看硬件。

### 工具 2：TB Checker / 国内类似工具

直接显示 Play Integrity 状态。

### 实测：打开 Netflix / Google Pay

最直接的测试。

---

## ⚠️ 注意事项

### 1. PIF 不是一劳永逸

Google 会**不断更新检测**：
- 新版可能识别"假指纹"
- 新版可能加新的检测维度

**PIF 作者会持续更新**——你要跟进。

### 2. 设备指纹要"真实存在"

乱编一个指纹会被识别。要用真实 Pixel 的指纹。

### 3. STRONG_INTEGRITY 难

`MEETS_STRONG_INTEGRITY` 要求**硬件级密钥**——root 后基本拿不到。

**好消息**：大部分 App 不要求 STRONG，只要求 BASIC 或 DEVICE。

### 4. 配合 ZygiskNext + HMA

PIF 只解决"Play Integrity 检测"，**不解决其他检测**：
- 文件系统检测 → susfs
- 进程检测 → ZygiskNext
- 应用列表检测 → HMA

要完整过检测，**全套都要装**。

---

## 🌐 国内用户怎么办

### 问题

国内手机**没有 Google Play Services**，Play Integrity 用不了。

### 影响

- 用不到 Google Pay
- 用不到 Netflix 官方 App
- 国内银行 App **不依赖 Play Integrity**（用自己的检测）

### 结论

**国内用户基本不用管 PIF**——把 susfs + ZygiskNext + HMA 配好就行。

---

## 🎯 经典疑问

### Q1: 我装了 PIF，为啥 Play Integrity 还是不过？

可能原因：
- 指纹过期了
- 设备指纹被 Google 标记了
- 其他检测暴露了（susfs/ZygiskNext 没配好）

**排查**：
1. 更新 PIF 到最新
2. 换指纹
3. 检查 susfs/ZygiskNext

### Q2: 为啥 Pixel 指纹这么重要？

因为 Google 最信任自家设备——Pixel 系列被认为"是干净的"。所以 PIF 都伪装成 Pixel。

### Q3: 我用的是小米，PIF 假装 Pixel 会不会有副作用？

**一般不会**——PIF 只改"上报给 Google 的指纹"，不改系统其他部分。但：
- 部分 App 可能用 fingerprint 决定要不要启用某些功能
- 极少数情况会有兼容问题

---

## 📚 资料来源

- [Play Integrity API 官方文档](https://developer.android.com/google/play/integrity)
- [PlayIntegrityFix 仓库](https://github.com/chiteroman/PlayIntegrityFix)
- [Play Integrity Fork（osm0sis）](https://github.com/osm0sis/PlayIntegrityFork)
