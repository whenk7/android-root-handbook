---
title: "权能 Capabilities"
---

> Capabilities 是 Linux 的"分权"机制——把 root 的超能力拆成 ~40 个独立的权能，每个权能管一件事。

---

## 🎯 是啥

传统 Unix：要么 root（UID=0，啥都能干），要么不是 root。

**Linux 2.2+ 引入 Capabilities**：把 root 的"超能力"拆成 ~40 个独立的权能，每个权能管一件事。

**好处**：
- 让 root 进程"只拥有必要的权限"
- 即使被攻破，损失也可控

---

## 📋 常见 Capabilities

### 文件相关

| Capability | 干啥 |
|---|---|
| `CAP_DAC_OVERRIDE` | 绕过文件读/写/执行权限检查 |
| `CAP_DAC_READ_SEARCH` | 绕过文件读取权限检查（只读） |
| `CAP_CHOWN` | 改文件 owner |
| `CAP_FOWNER` | 绕过文件 owner 检查 |
| `CAP_FSETID` | 改文件 setuid/setgid 位 |

### 进程相关

| Capability | 干啥 |
|---|---|
| `CAP_KILL` | 杀任何进程（绕过信号发送权限） |
| `CAP_SETUID` | 切换 UID |
| `CAP_SETGID` | 切换 GID |
| `CAP_SETPCAP` | 改自己的 capabilities |
| `CAP_SYS_PTRACE` | ptrace 任何进程（Frida 需要） |

### 网络相关

| Capability | 干啥 |
|---|---|
| `CAP_NET_RAW` | 用 raw socket（抓包） |
| `CAP_NET_ADMIN` | 配置网络（改路由、防火墙） |
| `CAP_NET_BIND_SERVICE` | 绑定 1024 以下端口 |
| `CAP_NET_BROADCAST` | 发送广播包 |

### 系统相关

| Capability | 干啥 |
|---|---|
| `CAP_SYS_ADMIN` | 挂载/卸载文件系统（**root 的"大杀器"**） |
| `CAP_SYS_BOOT` | 重启系统 |
| `CAP_SYS_MODULE` | 加载/卸载内核模块 |
| `CAP_SYS_TIME` | 改系统时间 |
| `CAP_SYS_RESOURCE` | 绕过资源限制 |
| `CAP_SYS_NICE` | 改进程优先级 |
| `CAP_SYSLOG` | 读内核日志 |

### 设备相关

| Capability | 干啥 |
|---|---|
| `CAP_MKNOD` | 创建设备文件 |
| `CAP_NET_BIND_SERVICE` | 绑定特权端口 |
| `CAP_LINUX_IMMUTABLE` | 改文件的 immutable 标志 |

**完整列表**：`man 7 capabilities`

---

## 🔍 查看进程的 Capabilities

```bash
adb shell

# 看自己的 capabilities
cat /proc/self/status | grep Cap

# 输出示例：
# CapInh: 0000000000000000
# CapPrm: 0000000000000000
# CapEff: 0000000000000000
# CapBnd: 0000003fffffffff
# CapAmb: 0000000000000000
```

**CapEff（Effective）**才是进程实际生效的 capabilities。

### 解码 capabilities

```bash
# 用 capsh 工具
adb shell su -c "capsh --decode=0000003fffffffff"

# 输出：
# 0x0000003fffffffff=cap_chown,cap_dac_override,...
```

---

## 🛠️ Root Profile 改 Capabilities

### 默认情况

App 拿到 root 后，**Capabilities 全开**——啥都能干。

### Root Profile 改它

让 App 拿到 `su` 后，**只有部分 Capabilities**。

---

## 🎬 三个经典例子

### 例子 1：抓包 App，只给网络权限

**需求**：你给抓包 App root，让它能抓包，但**不能干别的**。

**配置**：

```
Root Profile:
  Capabilities:
    ✓ CAP_NET_RAW       ← 抓包
    ✓ CAP_NET_ADMIN     ← 改网络配置
    ✗ 其他全部移除
```

**效果**：

```
App 拿到 root 后：
- 能创建 raw socket 抓包 ✓
- 能改 iptables ✓
- 不能改文件 owner ✗
- 不能加载内核模块 ✗
- 不能重启手机 ✗
```

---

### 例子 2：文件管理器，不给内核权限

**需求**：你给文件管理器 root，但**不让它碰内核**。

**配置**：

```
Root Profile:
  Capabilities:
    ✗ CAP_SYS_MODULE    ← 不能加载内核模块
    ✗ CAP_SYS_BOOT      ← 不能重启
    ✗ CAP_SYS_ADMIN     ← 不能挂载文件系统
    ✓ 其他保留
```

**效果**：

```
App 拿到 root 后：
- 能读写所有文件 ✓
- 不能加载 .ko ✗
- 不能重启手机 ✗
- 不能挂载 ✗
```

---

### 例子 3：监控 App，只读

**需求**：你给系统监控 App root，让它看系统状态，但**不让它改任何东西**。

**配置**：

```
Root Profile:
  Capabilities:
    ✓ CAP_DAC_READ_SEARCH   ← 只读权限
    ✗ CAP_DAC_OVERRIDE      ← 不能写
    ✗ 其他写相关全部移除
```

**效果**：

```
App 拿到 root 后：
- 能读所有文件 ✓
- 不能改任何文件 ✗
- 不能改任何设置 ✗
```

---

## 🧬 实现原理

### 内核态怎么做

KSU 内核模块在 `exec` 系统调用上 hook：

```
App 跑 su
   ↓
exec("su")
   ↓
KSU 内核拦截
   ↓
读 App 的 App Profile
   ↓
设置进程的 capabilities 为 Profile 指定的值
   ↓
执行 su，进程已经有"受限的 capabilities"
```

### Capabilities 在 task_struct

```c
struct task_struct {
    ...
    kernel_cap_t cap_effective;   // 生效的
    kernel_cap_t cap_permitted;   // 允许的
    kernel_cap_t cap_inheritable; // 可继承的
    ...
};
```

KSU 改的是 `cap_effective`——进程实际生效的权能。

---

## 🆚 UID/GID/Groups vs Capabilities

| 维度 | UID/GID/Groups | Capabilities |
|---|---|---|
| 控制什么 | 你是谁 | 你能干啥 |
| 例子 | UID 0 = root | CAP_NET_ADMIN = 改网络 |
| 粒度 | 粗 | 细 |
| 历史 | Unix 传统 | Linux 2.2+ |

**它们配合用**：

```
场景：抓包 App
   ↓
UID/GID 设置：UID=0（要是 root）
   ↓
Capabilities 设置：只保留 CAP_NET_RAW + CAP_NET_ADMIN
   ↓
效果：是 root，但只能干网络相关的事
```

---

## ⚠️ 注意事项

### 1. 不是所有 Capabilities 都常用

实际上**常用的就 10 几个**。其他大部分是给特定场景（容器、虚拟化）的。

### 2. CAP_SYS_ADMIN 是"大杀器"

`CAP_SYS_ADMIN` 包含了太多功能（挂载、reboot、关键操作）。**最小化原则下，能不给就不给**。

### 3. 移除 Capabilities 不等于"完全阻止"

有些操作可以**绕过** capabilities：
- 利用 setuid 二进制
- 利用内核漏洞

Capabilities 是"减小攻击面"，不是"绝对防御"。

---

## 🎯 经典疑问

### Q1: 我给 App 移除了所有 Capabilities，它还能干啥？

**几乎啥都干不了**——它能跑命令，但所有特权操作都会失败。

### Q2: Capabilities 跟 SELinux 啥关系？

**两层防护**：
- Capabilities：管"能不能干"
- SELinux：管"让不让你碰这个具体资源"

即使 Capabilities 允许，SELinux 也可能拒绝。

### Q3: 我能不能给一个 App"满 root 但去掉网络"？

**能**：
```
UID: 0
Capabilities: 全开（保留所有）
Groups: 移除 inet
```

效果：Capabilities 都在，但 inet 组没了 → 不能联网。

---

## 📚 资料来源

- [Linux capabilities man page](https://man7.org/linux/man-pages/man7/capabilities.7.html)
- [KernelSU app-profile.md](https://github.com/tiann/KernelSU/blob/main/website/docs/zh_CN/guide/app-profile.md)
