---
title: "权限与 SELinux"
---

> 你装好模块它不工作，90% 是权限/SELinux 问题。这篇讲清楚 Android 的两层权限模型。

---

## 🛡️ 两层权限模型

Android 的权限模型是**双层**的：

```
第 1 层：传统 Linux 权限（DAC，自主访问控制）
   - 文件有 owner/group/other 三个权限位
   - rwx（读/写/执行）
   - root 用户万能
   
第 2 层：SELinux（MAC，强制访问控制）
   - 每个进程、每个文件都有"标签"
   - 策略规定：什么标签的进程能访问什么标签的文件
   - 即使 root，没策略允许也不行
```

---

## 🆔 第 1 层：UID / GID / Groups

### UID（用户 ID）

```
UID 0        = root（最高权限）
UID 1000     = system
UID 2000     = shell（adb shell）
UID 10000+   = 普通 App（每个 App 一个 UID）
```

**查看自己**：
```bash
adb shell id
# uid=2000(shell) gid=2000(shell) groups=2000(shell),...
```

### GID 和补充组

每个进程除了主 UID/GID，还有**补充组**。某些权限是通过组控制的：

```
inet            → 可以创建网络 socket（联网）
sdcard_rw       → 可以读写 /sdcard
sdcard_r        → 只能读 /sdcard
net_bt          → 蓝牙
net_bt_admin    → 蓝牙管理
input           → 输入设备
log             → 读日志
```

**示例**（`id` 命令输出）：
```
uid=2000(shell) gid=2000(shell)
groups=2000(shell),1004(input),1007(log),1011(adb),
       1015(sdcard_rw),1028(sdcard_r),
       3001(net_bt_admin),3002(net_bt),3003(inet),...
context=u:r:shell:s0
```

意思：你是 shell 用户，在 inet 组里（能联网）、在 sdcard_rw 组里（能读写 SD 卡）...

### Capabilities（权能）

Linux 2.2+ 引入：把 root 的"超能力"拆成 ~40 个独立的权能。

**常见的**：
```
CAP_DAC_OVERRIDE      绕过文件读/写/执行权限检查
CAP_DAC_READ_SEARCH   绕过文件读取权限检查
CAP_CHOWN             改文件 owner
CAP_KILL              杀任何进程
CAP_NET_RAW           用 raw socket（抓包）
CAP_NET_ADMIN         配置网络（改路由、防火墙）
CAP_NET_BIND_SERVICE  绑定 1024 以下端口
CAP_SYS_ADMIN         挂载、卸载文件系统
CAP_SYS_BOOT          重启系统
CAP_SYS_MODULE        加载/卸载内核模块
CAP_SYS_PTRACE        ptrace 任何进程
CAP_SYS_TIME          改系统时间
CAP_SETUID            切换 UID
CAP_SETGID            切换 GID
CAP_MKNOD             创建设备文件
```

完整列表见 `man 7 capabilities`。

**这些在 App Profile 里会用到**——KSU 可以给每个 root App 精细分配 Capabilities。详见 [41-47 章节]。

---

## 🔐 第 2 层：SELinux

### 是啥

**SELinux = Security-Enhanced Linux**，美国国家安全局（NSA）搞的安全框架，Android 4.3 开始内置。

**核心思想**：默认拒绝。任何未经明确允许的行为都会被拒绝。

### 三种模式

| 模式 | 英文 | 行为 |
|---|---|---|
| **严格模式** | Enforcing | 违反策略就拒绝 + 记日志 |
| **宽容模式** | Permissive | 违反策略只记日志不拒绝（用来调试） |
| **关闭** | Disabled | 完全不工作 |

**你的手机默认是 Enforcing**。

### 查看自己手机

```bash
adb shell getenforce
# Enforcing   ← 严格模式
# Permissive  ← 宽容模式
```

### SELinux 标签（context）

每个进程、文件都有标签，格式：`用户:角色:类型:级别`

```
u:r:system_app:s0          ← 系统 App 进程的标签
u:r:shell:s0               ← shell 进程的标签
u:r:kernel:s0              ← 内核的标签
u:r:untrusted_app:s0       ← 普通 App 的标签
u:r:ksu:s0                 ← KSU 给 root 进程的标签
u:object_r:system_file:s0  ← /system 下文件的标签
u:object_r:apk_data_file:s0 ← App 数据文件的标签
```

**类型（type）最重要**——策略规则就是基于类型的。

### 查看标签

```bash
# 文件标签
adb shell ls -Z /system/build.prop
# u:object_r:system_file:s0 /system/build.prop

# 进程标签
adb shell ps -Z | head
# LABEL                          USER     PID   NAME
# u:r:kernel:s0                  root      1     init
# u:r:system_server:s0           system    1234  system_server
```

---

## 🚫 SELinux 拦截案例

### 例 1：KSU 模块想让 App 读 `/data/adb/xxx`

**默认 SELinux 策略**：
- `/data/adb/` 的标签是 `u:object_r:adb_data_file:s0`
- App 进程的标签是 `u:r:untrusted_app:s0`
- 策略里**没有** "允许 untrusted_app 读 adb_data_file"

**结果**：App `open()` 这个文件 → 内核 SELinux 检查 → 没策略允许 → **拒绝** → App 报 Permission denied

**解决**：模块加 `sepolicy.rule`：
```
allow untrusted_app adb_data_file file read
```

### 例 2：root 工具想 ptrace 别的进程

- `ptrace` 系统调用默认在 SELinux 里**只允许同标签进程**
- 你的 root 工具标签是 `u:r:su:s0`
- 目标 App 标签是 `u:r:untrusted_app:s0`
- 不同标签 → 拒绝

**Frida 在严格模式下经常跑不动**，就是因为这个。

### 例 3：模块想 hook 系统服务

- 系统服务标签是 `u:r:system_server:s0`
- 你的 hook 代码想往它的内存里写
- SELinux 检查 → 拒绝

---

## 🔍 怎么看 SELinux 拒绝（avc 日志）

按可靠性排序：

```bash
# 1. 内核日志（最准，实时）
adb shell su -c "dmesg -w | grep avc"

# 2. 内核 ring buffer
adb shell su -c "cat /proc/kmsg | grep avc"

# 3. logcat events buffer
adb logcat -b events | grep avc

# 4. logcat main
adb logcat | grep avc
```

**典型 avc 日志格式**：
```
avc: denied { read } for pid=1234 comm="com.bank" 
     path="/data/adb/modules/xxx/config" 
     dev="sda30" ino=12345 
     scontext=u:r:untrusted_app:s0 
     tcontext=u:object_r:adb_data_file:s0 
     tclass=file
```

**翻译**：进程 `com.bank`（pid 1234）想 read 一个文件，源标签是 untrusted_app，目标标签是 adb_data_file，类型是 file——被拒绝了。

---

## 🛠️ 从 avc 日志生成 sepolicy.rule

**完整流程**：

```bash
# 1. 切宽容，跑一次功能（让所有该发生的拦截都发生一遍）
adb shell su -c "setenforce 0"
# 然后正常用模块/App，让它跑

# 2. 收集 avc 日志
adb shell su -c "dmesg | grep avc" > /tmp/avc.log

# 3. 切回严格
adb shell su -c "setenforce 1"

# 4. 用 audit2allow 把日志翻译成规则
adb shell su -c "audit2allow < /tmp/avc.log"
```

`audit2allow` 会输出类似：
```
allow untrusted_app adb_data_file file read;
allow zygote zygote capability sys_resource;
```

把这些行**加到模块的 `sepolicy.rule` 里**，重启，功能正常 + 严格模式保持。

---

## ⚠️ 严格 vs 宽容：怎么选

### 银行 App 检测 SELinux 吗？

**绝对检测**。常见手段：

```java
// Java 层
String enforce = runCommand("getenforce");
if (!"Enforcing".equals(enforce)) {
    // 拒绝服务或上报
}
```

```c
// Native 层
int enforcing = security_getenforce();  // libselinux
if (enforcing != 1) {
    abort();
}
```

**实测会检测的 App**：
- 各家银行（工行、招行、建行、农行）
- 支付宝、微信支付
- 部分游戏（原神、王者反外挂）
- Google Pay / 三星 Pay

### 加 sepolicy.rule vs 切宽容

| | sepolicy.rule | 切宽容 |
|---|---|---|
| 安全性 | 精细控制，只放必要的 | 全开，全部允许 |
| 银行检测 | 通过 | **不通过** |
| 持久化 | 重启还在 | 重启失效 |
| 排错 | 精准定位权限问题 | 不知道是哪个规则在拦 |

**唯一推荐切宽容的场景**：调试阶段，先确认"是不是 SELinux 在拦"。确认后**立刻切回来**，把缺失的 rule 补上。

### 切换命令

```bash
adb shell su -c "setenforce 0"     # 0 = Permissive
adb shell su -c "setenforce 1"     # 1 = Enforcing
```

**注意**：切换不持久，重启后**自动回到 Enforcing**（持久化要改 kernel cmdline，不推荐）。

---

## 🎯 实用建议

1. **永远保持 Enforcing**
2. 模块不工作 → 先看是不是 SELinux 拦的（看 `avc: denied`）
3. 自己写模块 → 先 `setenforce 0` 调通功能 → 抓 `avc` 日志 → 翻译成 rule → `setenforce 1` 验证
4. **别乱给模块加 sepolicy.rule**——每条规则都是一个攻击面

---

## 📚 资料来源

- [KernelSU app-profile.md](https://github.com/tiann/KernelSU/blob/main/website/docs/zh_CN/guide/app-profile.md)
- [Linux capabilities man page](https://man7.org/linux/man-pages/man7/capabilities.7.html)
- [SELinux 官方文档（红帽）](https://www.redhat.com/en/topics/linux/what-is-selinux)
- [ArchWiki SELinux](https://wiki.archlinux.org/title/SELinux)
