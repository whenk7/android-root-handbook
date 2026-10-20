---
title: "为什么要隐藏 Root"
---

> 你 root 了手机，结果银行 App、支付 App、一些游戏全闪退。这篇讲它们怎么检测你。

---

## 🎯 为什么 App 要检测 root

### 厂商的考虑

| 类型 | 担心的事 |
|---|---|
| 银行/支付 | 你 root 后可能篡改交易记录、拦截短信验证码 |
| 游戏 | 你 root 后可能改内存、改游戏数据（外挂） |
| 视频会员 | 你 root 后可能破解会员 |
| 企业 App | 你 root 后可能窃取公司数据 |

### 法律原因

部分地区银行 App 受监管要求，必须做"环境安全检测"。

---

## 🔍 App 怎么检测 root

### 检测维度 1：文件存在性

```java
// 检查常见 root 路径是否存在
new File("/system/bin/su").exists();
new File("/system/xbin/su").exists();
new File("/data/adb/magisk").exists();
new File("/data/adb/ksu").exists();
```

**对策**：
- KSU/Magisk 默认把 su 藏在 `/data/adb/`
- susfs 在内核层隐藏这些文件

### 检测维度 2：执行命令

```java
// 尝试执行 su，看会不会成功
Runtime.getRuntime().exec("su");
Runtime.getRuntime().exec("which su");
```

**对策**：KSU 的 root 访问控制——只有授权的 App 能用 su。

### 检测维度 3：读 /proc

```java
// 读 /proc/mounts 看有没有可疑挂载
new FileReader("/proc/mounts");

// 读 /proc/kallsyms 看有没有可疑内核符号
new FileReader("/proc/kallsyms");
```

**对策**：susfs 在内核层返回假数据。

### 检测维度 4：PackageManager 查询

```java
// 列出所有已安装应用，看有没有 root 工具
getPackageManager().getInstalledApplications(0);
// 如果列表里有 me.weishu.kernelsu / org.lsposed.manager → root 了
```

**对策**：Hide My Applist 拦这个 API。

### 检测维度 5：SELinux 状态

```java
// 读 SELinux 状态
new FileReader("/sys/fs/selinux/enforce");
// 0 = Permissive（宽容）
// 1 = Enforcing（严格）
```

**对策**：永远保持 Enforcing。

### 检测维度 6：系统属性

```java
// 看 build 相关属性
System.getProperty("ro.build.type");     // user / userdebug / eng
System.getProperty("ro.debuggable");     // 0 / 1
System.getProperty("ro.secure");         // 0 / 1
System.getProperty("service.bootanim.exit"); // 启动完成
```

**对策**：KSU/Magisk 模块改这些属性。

### 检测维度 7：Zygisk/LSPosed 痕迹

```java
// 看自己进程的 maps，看有没有可疑 .so
new FileReader("/proc/self/maps");
// 如果看到 lspd.so / zygisk.so → 被 hook 了
```

**对策**：ZygiskNext v1.3.0+ 在 Zygisk 层抹掉这些（已整合 Shamiko 的大部分功能）。

### 检测维度 8：堆栈检测

```java
// 触发异常，看堆栈里有没有可疑类
throw new Exception();
// 如果堆栈里有 de.robv.android.xposed → 被 hook 了
```

**对策**：ZygiskNext v1.3.0+ / LSP 的"白名单模式"。

### 检测维度 9：签名校验

```java
// 校验自己的签名
PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), GET_SIGNATURES);
// 如果签名不对 → 被改过了
```

**对策**：不能改 APK。所以 LSPatch 路线很难过这个检测。

### 检测维度 10：Play Integrity

Google 提供的官方"设备完整性" API：

```java
// 调 Google Play Integrity API
IntegrityManagerFactory.create(context).requestIntegrityToken(...);
```

返回三个判定：
- `MEETS_DEVICE_INTEGRITY`：设备完整
- `MEETS_BASIC_INTEGRITY`：基本完整
- `MEETS_STRONG_INTEGRITY`：强完整（要硬件支持）

**对策**：Play Integrity Fix 模块伪造指纹。

### 检测维度 11：挂载信息交叉验证

抽样读 `/proc/*/fdinfo` 里的 mnt_id，和挂载视图对账——被你藏掉的挂载，可能在 fdinfo 里露馅。

**对策**：换挂载一致性更好的元模块（见 [susfs 与内核层隐藏](/android-root-handbook/23-susfs与内核层隐藏/)）。

### 检测维度 12：密钥证明链校验

对 keystore 签出的证书链做几十项一致性检查（密钥用途、签名算法、补丁日期标签……），伪装总会在某一项露馅。

**对策**：密钥模块（见 [Play Integrity 过检测](/android-root-handbook/27-playintegrity过检测/)）。

---

## 🛡️ 对应防御策略（三层体系，2026 版）

```
检测维度              防御层            工具
─────────────────────────────────────────
文件存在性        →   内核层        →   susfs（SukiSU 内置）
执行命令          →   Root 访问控制  →   KSU 内置
读 /proc          →   内核层        →   susfs
PackageManager    →   App 层        →   Hide My Applist
SELinux 状态      →   内核层        →   selinux_hide（KSU v3.3.0+）
系统属性          →   prop 隐藏     →   自己写 service.sh（见 25 章）
Zygisk/LSPosed    →   Zygisk 层     →   ZygiskNext v1.3.0+（已整合 Shamiko）
堆栈检测          →   Zygisk 层     →   ZygiskNext
签名校验          →   无解（不要改 APK）
Play Integrity    →   App 层        →   Play Integrity Fix
```

详见 [22-三层防御体系](/android-root-handbook/22-三层防御体系)。

---

## 🏦 常见"严格"App 名单

### 银行类（几乎必检测）

- 工商银行
- 招商银行
- 建设银行
- 农业银行
- 中国银行
- 交通银行
- 支付宝（部分场景）

### 支付类

- 微信支付（部分场景）
- Google Pay
- Samsung Pay
- Apple Pay（iOS 不涉及）

### 游戏类

- 原神（检测外挂）
- 王者荣耀（检测外挂）
- PUBG Mobile
- 网易系游戏

### 流媒体/会员类

- Netflix
- Disney+
- 爱奇艺（部分场景）

### 其他

- 12306（部分场景）
- 部分企业 MDM App

---

## 🧪 自测：你的手机能不能过检测

### 工具 1：Play Integrity API Checker

在 Google Play 搜 "Play Integrity API Checker"，装上跑一下，看哪一项不通过。

### 工具 2：RootBeer Fresh

开源的 root 检测库，装上直接跑，看每一项检测过不过。

### 工具 3：Momo（国内常用）

第三方检测工具，专门测各种反 root 检测。

### 工具 4：高强度检测器（进阶）

Momo 之外还有检测项上百个的检测器，配合[社区实测的逐项应对方案](https://github.com/mingzun09/Chunqiu-Detector-Problem-solution)逐条对照排查——能过它，银行 App 基本就稳了。

### 实测：直接打开银行 App

最直接的测试——打开看会不会闪退/弹窗。

---

## 🎯 经典疑问

### Q1: 我卸载所有 root 相关的 App 不就完了？

**没用**。检测不只看"装了什么 App"，还看：
- 系统文件是否被改
- /proc 里的痕迹
- Zygisk 痕迹
- ...

必须做**完整三层防御**。

### Q2: 我能不能"用时再开 root，不用时关掉"？

**理论上可以**（KSU 有"卸载"功能，重启后 root 没了）。但：
- 每次切换要重启
- 银行 App 可能记住了"你以前 root 过"
- 不实用

### Q3: 检测率 100% 吗？

**不是**。猫鼠游戏：
- 检测方不停加新检测
- 防御方不停更新对抗手段
- 没有"绝对安全"，只有"目前能过"

---

## 📚 资料来源

- [Play Integrity API 官方文档](https://developer.android.com/google/play/integrity)
- [RootBeer 开源检测库](https://github.com/scottyab/rootbeer)
- [LSPosed 检测对抗讨论](https://github.com/LSPosed/LSPosed/issues)
