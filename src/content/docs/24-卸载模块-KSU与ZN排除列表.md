---
title: "卸载模块：KSU App Profile 与 ZN 排除列表"
---

> 这篇讲清楚两个**容易混淆**的概念：
> - **KSU 的"卸载模块"**（App Profile 的 Non-Root Profile）
> - **ZygiskNext 的"排除列表"**（denylist，含 v1.3.0+ 新增的 `just_umount` 模式）
>
> 它们都能让 App 看不到模块的修改，但**工作层完全不同**。

---

## 🎯 先说结论

| 维度 | KSU 卸载模块 | ZN 排除列表（just_umount） |
|---|---|---|
| **工作层** | 内核层（mount namespace） | Zygisk 层（进程内） |
| **需要内核** | 5.10+（KSU 内核直接做） | 不依赖（ZN 自己做） |
| **影响范围** | 只卸载模块挂载 | 可控制是否注入 + 是否还原挂载 |
| **配置位置** | KSU Manager → App Profile | ZygiskNext 设置 / CLI |
| **典型用法** | 让银行 App 看到干净 /system | 让银行 App 进程里不留痕迹 |

**推荐**：**两个都配**——KSU 做粗粒度，ZN 做细粒度。

---

## 1️⃣ KSU 的"卸载模块"（App Profile）

### 是什么

KSU 的 App Profile 分两块：
- **Root Profile**：给**有 root 的** App 定制权限（UID/GID/Capabilities/SELinux）
- **Non-Root Profile**：给**没 root 的** App 定制**挂载行为**

"卸载模块"就是 Non-Root Profile 的核心开关：

> **对这个 App 卸载所有模块造成的 /system 修改**——让银行 App 看到的是"未修改"的 /system。

### 工作原理（内核 5.10+）

```
App 启动
   ↓
fork
   ↓
KSU 看包名 → 查 App Profile
   ↓
"卸载模块"开关是 ON？
   ↓
是 → 在新进程的 mount namespace 里 unmount 所有模块挂载
   ↓
新进程看到的 /system 是干净的
```

**关键**：在 mount namespace 里 unmount——只影响这个 App，不影响其他。

### 配置位置

```
KSU Manager → 超级用户 → 找到目标 App → App Profile → "卸载模块"开关
```

### 全局默认开关

```
KSU Manager → 设置 → "默认卸载模块"开关
```

| 默认开关 | 含义 |
|---|---|
| **开**（默认） | 所有 App 默认看到"卸载模块"的 /system；要让某个 App 看到修改后的 /system，得单独关闭这个 App 的"卸载模块" |
| **关** | 所有 App 默认看到修改后的 /system；要让某个 App 看到干净 /system，得单独开启它的"卸载模块" |

### 两种用法

#### 白名单思维（推荐）

```
全局设置：
  默认卸载模块 = ON       ← 大部分 App 看不到修改

例外（关闭"卸载模块"的 App）：
  - Chrome              ← 让 Chrome 看到过滤后的 hosts
  - Via                 ← 让 Via 看到过滤后的 hosts
```

#### 黑名单思维

```
全局设置：
  默认卸载模块 = OFF      ← 大部分 App 看到修改

例外（开启"卸载模块"的 App）：
  - 工商银行
  - 招商银行
  - 支付宝
```

### 老内核（< 5.10）的坑

KSU 内核模块在 5.10+ 才直接做 unmount 操作。老内核上这个开关**只是配置项**——具体执行靠 ZygiskNext 等模块。

---

## 2️⃣ ZygiskNext 的"排除列表"（denylist）

### 是什么

ZygiskNext 的排除列表是一个**配置文件**，列出一堆包名。意思是：对这些 App，**控制 Zygisk 的注入行为**。

### v1.3.0+ 的三种模式

ZygiskNext v1.3.0 引入了 `just_umount` 模式，现在排除列表有**三种模式**：

| 模式 | CLI 参数 | 行为 |
|---|---|---|
| **disabled** | `disabled` | 不启用排除，所有 App 都正常注入 |
| **enabled** | `enabled` | 完全排除——名单里的 App **不注入任何 Zygisk 模块** |
| **just_umount** | `just_umount` | **仅还原挂载**——注入模块，但把挂载还原掉 ★ 新增 |

### CLI 配置

```bash
# 查看当前模式
/data/adb/modules/zygisksu/bin/zygiskd enforce-denylist

# 设置为 just_umount（推荐）
/data/adb/modules/zygisksu/bin/zygiskd enforce-denylist just_umount

# 其他模式
/data/adb/modules/zygisksu/bin/zygiskd enforce-denylist disabled
/data/adb/modules/zygisksu/bin/zygiskd enforce-denylist enabled
```

### 配置文件

```
/data/adb/zygisksu/denylist.txt
```

内容格式（每行一个包名）：

```
com.icbc
com.cmbchina
com.tencent.mm
```

### 三种模式的区别

#### disabled（默认）

- 所有 App 都注入 Zygisk 模块
- 不还原挂载
- **银行 App 会检测到**（因为 LSPosed 的 .so 在进程里）

#### enabled（完全排除）

- 名单里的 App **不注入任何 Zygisk 模块**
- LSPosed 不进去、Shamiko 也不进去
- **问题**：Shamiko 没机会擦痕迹（因为根本没注入）

#### just_umount（仅还原挂载）★ 推荐

- 名单里的 App **注入 Zygisk 模块**
- 但**还原所有挂载变更**（把 overlay 挂载 unmount 掉）
- Shamiko 能进去擦痕迹
- **最隐蔽**——进程里有模块但挂载是干净的

---

## 3️⃣ 两者的关系

### 完整流程（just_umount 模式）

```
银行 App 启动
   ↓
【KSU 层】
KSU 看包名 → App Profile "卸载模块" = ON
   ↓
在新进程的 mount namespace 里 unmount 所有模块挂载
   ↓
【Zygisk 层】
ZygiskNext 看排除列表 → just_umount 模式
   ↓
注入 Zygisk 模块（LSPosed、Shamiko）
   ↓
在进程内还原挂载变更
   ↓
Shamiko 擦除进程内痕迹
   ↓
银行 App 跑起来
   ↓
银行 App 读 /proc/mounts → 看不到 overlay（KSU 做了）
   ↓
银行 App 读 /proc/self/maps → 看不到 lspd.so（Shamiko 做了）
   ↓
银行 App 觉得"手机是干净的"
```

### 谁做什么

| 检测点 | 谁负责 |
|---|---|
| `/proc/mounts` 里的 overlay | KSU 卸载模块（内核层） |
| `/proc/self/maps` 里的 lspd.so | ZN 排除列表 + Shamiko（Zygisk 层） |
| 系统属性 `ro.debuggable` | prop 隐藏（见 [25-prop隐藏](/android-root-handbook/25-prop隐藏)） |
| 应用列表里的 KSU Manager | Hide My Applist（见 [26-HideMyApplist藏应用](/android-root-handbook/26-hidemyapplist藏应用)） |

---

## 🛠️ 实操配置

### 第 1 步：配置 KSU 卸载模块

```
KSU Manager → 超级用户 → 找到目标 App（如工商银行）→ App Profile → 开启"卸载模块"
```

或全局默认开：

```
KSU Manager → 设置 → "默认卸载模块" = ON
```

### 第 2 步：配置 ZN 排除列表

#### 方法 1：WebUI

```
ZygiskNext → 设置 → 排除列表 → 添加 App → 选择模式（推荐 just_umount）
```

#### 方法 2：CLI

```bash
adb shell

# 添加包名到排除列表
su -c "echo 'com.icbc' >> /data/adb/zygisksu/denylist.txt"

# 设置模式为 just_umount
su -c "/data/adb/modules/zygisksu/bin/zygiskd enforce-denylist just_umount"
```

### 第 3 步：验证

```bash
adb shell

# 1. 看 KSU 卸载模块是否生效
su -c "cat /proc/$(pidof com.icbc)/mounts | grep overlay"
# 应该返回空

# 2. 看 ZN 排除列表
su -c "cat /data/adb/zygisksu/denylist.txt"
# 应该有 com.icbc

# 3. 看 ZN 模式
su -c "/data/adb/modules/zygisksu/bin/zygiskd enforce-denylist"
# 应该返回 just_umount

# 4. 看银行 App 进程是否有 LSPosed 痕迹（应该没有）
su -c "cat /proc/$(pidof com.icbc)/maps | grep -i lsp"
# 应该返回空

# 5. 银行 App 打开试试
```

---

## ⚠️ 注意事项

### 1. KSU 卸载模块只影响模块挂载

这个开关**只影响模块通过 OverlayFS 挂载到 /system 的内容**。

**不影响**：
- 模块的 service.sh（已经在跑）
- 模块改的 system properties（已经生效）
- 模块改的 SELinux 规则（已经生效）

### 2. ZN just_umount 模式需要 v1.3.0+

老版本 ZN 只有 `disabled` 和 `enabled` 两种模式。`just_umount` 是 v1.3.0 新增的。

### 3. 两者都要配

- 只配 KSU 卸载模块 → 银行 App 读 `/proc/self/maps` 能看到 lspd.so
- 只配 ZN 排除列表 → 银行 App 读 `/proc/mounts` 能看到 overlay

**两个都配才能完整隐藏**。

### 4. v1.5.0+ 的变化

ZygiskNext v1.5.0 调整了 WebView 子进程 umount 方式，**交由 Root 方案统一管理**。意思是：

- 如果 KSU 内核层已经做了卸载模块，ZN 就不再重复做
- 避免重复操作导致的问题

---

## 🎯 经典疑问

### Q1: 我把所有 App 都开"卸载模块"，那我的模块还有意义吗？

**只对"不想让它看到修改"的 App 开**。

**例子**：
- 广告过滤模块改了 hosts
- 银行 App 开"卸载模块" → 看到原版 hosts
- 浏览器不开 → 看到过滤后 hosts

### Q2: 卸载模块会让 App 看到"原版"系统吗？

**只针对模块挂载**——只看不到模块改的。系统出厂自带的修改（厂商定制）**还能看到**。

### Q3: ZN 的排除列表和 Magisk 的 DenyList 一样吗？

**类似但不完全一样**：
- Magisk DenyList：只有"启用/禁用"两种
- ZN v1.3.0+：三种模式（disabled / enabled / just_umount）

### Q4: 我该用哪种模式？

**推荐 `just_umount`**：
- 注入模块（Shamiko 能进去擦痕迹）
- 还原挂载（App 看不到 overlay）
- 最隐蔽

---

## 📚 资料来源

- [KernelSU app-profile.md](https://github.com/tiann/KernelSU/blob/main/website/docs/zh_CN/guide/app-profile.md)
- [ZygiskNext v1.3.0 release](https://github.com/Dr-TSNG/ZygiskNext/releases/tag/v1.3.0)
- [ZygiskNext v1.5.0 release](https://github.com/Dr-TSNG/ZygiskNext/releases/tag/v1.5.0)
