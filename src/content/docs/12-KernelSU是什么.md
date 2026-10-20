---
title: "KernelSU 是什么"
---

> KernelSU（KSU）是当前最流行的内核态 root 方案。这篇讲它的核心原理。

---

## 🧬 核心思想：在内核里给 root

### Magisk 的做法（用户态）

```
App 跑 su
   ↓
su 进程启动
   ↓
su 跟 magiskd 通信（socket）
   ↓
magiskd 决定给不给 root
   ↓
给 → su 切到 UID 0
```

### KSU 的做法（内核态）

```
App 跑 su
   ↓
su 进程启动
   ↓
exec 系统调用
   ↓
【内核拦截 exec】← KSU 内核模块在这
   ↓
内核直接决定给不给 root
   ↓
给 → 进程直接拿到 root capabilities
```

**区别**：KSU 把决策**从用户态搬到了内核态**。

---

## 🏗️ KSU 的组件

```
┌─────────────────────────────────────┐
│ 内核态                                │
│ ┌─────────────────────────────┐    │
│ │ kernelsu.ko（KSU 内核模块）   │    │
│ │ - hook execve 系统调用        │    │
│ │ - 决定给不给 root             │    │
│ │ - 提供 uapi 给用户态调用      │    │
│ └─────────────────────────────┘    │
└─────────────────────────────────────┘
              ↕ (通过 uapi 通信)
┌─────────────────────────────────────┐
│ 用户态                                │
│ ┌─────────────────────────────┐    │
│ │ ksud（守护进程）              │    │
│ │ - 挂载模块                    │    │
│ │ - 跑启动脚本                  │    │
│ │ - 管理 SELinux 规则           │    │
│ └─────────────────────────────┘    │
│ ┌─────────────────────────────┐    │
│ │ Manager APK                  │    │
│ │ - 用户界面                    │    │
│ │ - 模块管理                    │    │
│ │ - 给 App 授权 root            │    │
│ └─────────────────────────────┘    │
└─────────────────────────────────────┘
```

---

## 🔧 两种部署模式

### GKI 模式（编译进内核）

**做法**：把 KSU 代码**编译进内核**，生成新的 boot.img。

**优点**：
- 稳定
- 能进 preinit 阶段
- 性能最好

**缺点**：
- 要为你的设备**专门编译**内核
- 厂商签名问题（要解锁 BL）

### LKM 模式（动态加载）

**做法**：不动 boot.img，**开机后把 kernelsu.ko 加载到内核**。

**优点**：
- 通用，一个 .ko 适配一类内核
- 不用重编内核

**缺点**：
- 依赖内核版本匹配（KMI 要一致）
- 加载时机晚于 GKI

---

## 📐 KMI（Kernel Module Interface）

### 是啥

Google 给每组"Android 大版本 + 内核大版本"定义的接口代号：

```
android12-5.10     ← Android 12 + Linux 5.10
android13-5.15     ← Android 13 + Linux 5.15
android14-6.1      ← Android 14 + Linux 6.1
android15-6.6      ← Android 15 + Linux 6.6
android16-6.12     ← Android 16 + Linux 6.12
android17-6.18     ← Android 17 + Linux 6.18
```

**LKM 是按 KMI 编译的**——`android14-6.1-aarch64_kernelsu.ko` 只能在"Android 14 + Linux 6.1 + ARM64"的设备上跑。

### 看你的 KMI

```bash
adb shell uname -r
# 输出示例：6.1.138-android14-11-g0c3d559bcd85-ab14529422
# 拆解：6.1 = 内核大版本
#       android14 = Android 大版本
#       合起来 KMI = android14-6.1
```

---

## 🎭 KSU 的核心特性

### 1. Root 访问控制

只有**明确允许的 App**能用 `su`。其他 App 根本看不到 `su`。

```
App A（允许）→ 跑 su → 拿到 root
App B（不允许）→ 跑 su → 报"command not found"
```

### 2. App Profile（独有）

给每个 root App 单独定制权限：
- 改 UID/GID/Groups
- 改 Capabilities
- 改 SELinux context

详见 [41-47 章节]。

### 3. Metamodule 系统

挂载实现可插拔。详见 [64-KSU元模块](/android-root-handbook/64-ksu元模块)。

### 4. 模块系统

兼容 Magisk 模块格式。详见 [31-KSU模块是什么](/android-root-handbook/31-ksu模块是什么)。

### 5. WebUI

模块可以有自己的 Web 配置界面。详见 [65-KSU模块WebUI](/android-root-handbook/65-ksu模块webui)。

---

## 🆚 KSU vs Magisk 在 root 授予上的差异

| 维度 | Magisk | KernelSU |
|---|---|---|
| 决策位置 | magiskd（用户态 daemon） | 内核模块 |
| 拦截点 | su 通过 socket 跟 magiskd 通信 | 内核 hook execve |
| 可定制粒度 | 给 / 不给 | 给 + UID/GID/Caps/SELinux |
| 检测特征 | magiskd 进程、socket | 内核符号 |

---

## 🧪 亲手验证 KSU

```bash
adb shell
su

# 1. 看内核态是否加载
lsmod | grep -i ksu
# 或
cat /proc/kallsyms | grep -i ksu | head -3

# 2. 看 ksud 进程
ps -ef | grep ksud

# 3. 看 KSU 数据目录
ls /data/adb/ksu/

# 4. 看版本
ksud --version
```

---

## 🎯 经典疑问

### Q1: KSU 是不是绝对安全？

**不是**。内核态意味着：
- 特征更少，更难检测
- 但**一旦 KSU 自己有漏洞，影响更大**（内核态代码跑在最高权限）

### Q2: 我能不能只用 KSU 不用 Manager？

**技术上可以**——root 决策是内核做的。但你会失去：
- 图形界面管理 root 授权
- 模块管理
- 各种便利功能

**不推荐**。

### Q3: KSU 会不会被检测到？

任何 root 方案都会被检测，只是难易程度。KSU 在内核态，**特征比 Magisk 少**，配合 susfs + ZygiskNext v1.3.0+ 能过大部分检测。

---

## 📚 资料来源

- [KernelSU 官方文档 - What is KernelSU](https://github.com/tiann/KernelSU/blob/main/website/docs/guide/what-is-kernelsu.md)
- [KernelSU GitHub](https://github.com/tiann/KernelSU)
