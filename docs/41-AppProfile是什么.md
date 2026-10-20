# 41 · App Profile 是什么

> App Profile 是 KernelSU 独有的"按 App 定制 root 权限"的机制——**把 root 关进笼子**。Magisk 没有的能力。

---

## 🎯 一句话定义

> **App Profile 让你给每个 App 单独定制 root 权限**：这个 App 拿到 `su` 后是 UID 几？能干啥？不能干啥？

---

## 🆚 跟 Magisk 的对比

### Magisk 的 root 授权

**二元**：
- 允许 → 拿到完整 root
- 拒绝 → 拿不到

### KernelSU 的 App Profile

**精细**：
- 允许 → 拿到 root，但你可以**限制**它能干啥

---

## 🏗️ 两块内容

App Profile 分两块：

### A. Root Profile（针对**有 root 的** App）

定制这个 App 拿到 `su` 后的：
- **UID / GID / Groups**（用户和组）
- **Capabilities**（Linux 权能）
- **SELinux context**（SELinux 域）

### B. Non-Root Profile（针对**没 root 的** App）

定制 KSU 对这个 App 的**挂载行为**：
- **是否卸载模块挂载**（让目标 App 看到的是干净的 /system）

**注意**：Non-Root Profile 本质上是**反检测手段**，详细用法见 [24-卸载模块-KSU与ZN排除列表](24-卸载模块-KSU与ZN排除列表.md)。

---

## 🧬 三层防护

```
第 1 层：UID/GID/Groups     ← "你是谁"（身份证）
第 2 层：Capabilities         ← "你能干啥"（技能清单）
第 3 层：SELinux Context      ← "你被允许碰什么"（行为边界）
```

三层都收紧 → root 才真正被"关进笼子"。

---

## 🎬 快速上手

### 操作位置

```
KSU Manager 
  → 底部导航"超级用户"
  → 找到目标 App
  → 点 App 图标进入详情
  → 切换"允许 Root"
  → 展开"App Profile"
  → 改 UID/GID/Groups/Capabilities/SELinux
  → 保存
```

### 命令行

```bash
# 看当前所有 App Profile
adb shell su -c "ksud profile list"

# 看某个 App 的 profile
adb shell su -c "ksud profile show com.termux"
```

不同版本的 KSU/SukiSU 命令可能不一样，跑 `ksud --help` 看具体子命令。

---

## 🎯 什么时候用 App Profile

### 场景 1：不完全信任的 App 要 root

比如某个文件管理器要 root，但你不知道它会干啥：
- 移除它的 `inet` 组 → 它拿到 root 后**不能联网**
- 移除 `CAP_SYS_MODULE` → 它**不能加载内核模块**

### 场景 2：限制功能类 App

防火墙 App 只需要配置 iptables，不需要读你的私密文件：
- 保留 `CAP_NET_ADMIN`
- 移除 `CAP_DAC_OVERRIDE`

### 场景 3：让银行 App 看不到 root

银行 App 在 Non-Root Profile 里开"卸载模块"：
- 银行 App 看到的 /system 是干净的
- 银行 App 觉得你手机没 root

**详见 [24-卸载模块-KSU与ZN排除列表](24-卸载模块-KSU与ZN排除列表.md)**

---

## 🛡️ 逃逸问题（重要）

### 什么是逃逸

**配置不当，App 能"绕开"你的限制**。

### 经典逃逸场景

```
你给 ADB shell（UID 2000）允许 root
给某个 App 的 root profile 设 UID = 2000

App 干的事：
1. 第一次 su → KSU 拦截，把它切到 UID 2000（你以为安全了）
2. 但 UID 2000 现在有 shell 权限，它再跑 su
3. KSU 看到 UID 2000 在允许 root 的名单里 → 给完整 root
4. App 拿到了完整 root！
```

### 防御：NO_NEW_PRIVS 标志

Linux 的 `NO_NEW_PRIVS`：**这个进程之后不能通过任何方式提权**。

在 App Profile 里启用这个标志，App 拿到 su 之后**没法再切身份**。

**注意**：NO_NEW_PRIVS 不是万能的，**配置 Root Profile 还是要谨慎**。

详见 [45-防逃逸](45-防逃逸.md)。

---

## 🎯 推荐配置策略

### 默认所有 root App 都开 NO_NEW_PRIVS

除非你知道某个 App 必须再切身份。

### 能移除的 Capabilities 全移除

只加确实需要的。

### 银行 App 不授予 root + 开启卸载模块

双保险。

### 不确定就别瞎改

默认配置已经够安全，乱改反而引入逃逸。

---

## 📚 详细文档

后面几篇会详细讲每个维度：

- [42-用户身份控制](42-用户身份控制.md)：UID/GID/Groups
- [43-权能Capabilities](43-权能Capabilities.md)：Linux capabilities 详解
- [44-SELinux域定制](44-SELinux域定制.md)：自定义 SELinux context
- [45-防逃逸](45-防逃逸.md)：NO_NEW_PRIVS 与逃逸案例
- [47-实战配置例子](47-实战配置例子.md)：4 个完整例子

**关于"卸载模块"**：Non-Root Profile 的"卸载模块"开关本质是**反检测**手段，已单独成篇——见 [24-卸载模块-KSU与ZN排除列表](24-卸载模块-KSU与ZN排除列表.md)。

---

## 📚 资料来源

- [KernelSU app-profile.md](https://github.com/tiann/KernelSU/blob/main/website/docs/zh_CN/guide/app-profile.md)
