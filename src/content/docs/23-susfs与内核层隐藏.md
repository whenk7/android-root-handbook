---
title: "susfs 与内核层隐藏"
---

> susfs 是"内核层"防御——直接在内核里改数据，让 App 看到的都是假的。是最深的一层隐藏。这篇讲 susfs 是什么、SukiSU 怎么集成、以及 KSU v3.3.0+ 的内置替代方案。

---

## 🎯 是什么

**susfs = SU + FS**（hide superuser in filesystem）

是 **simonpunk** 写的一个 **kernel patch + 内核模块**，专门给 KernelSU 用。

**仓库**：`gitlab.com/simonpunk/susfs4ksu`

---

## 🛡️ 能藏什么

### 1. 文件隐藏

```
正常：ls /data/adb/ksu → 显示文件
susfs：ls /data/adb/ksu → 返回 "No such file or directory"
```

App 调 `stat()` / `open()` 这些路径，**内核直接返回"不存在"**。

### 2. 挂载点隐藏

```
正常：cat /proc/mounts → 看到 overlay on /system
susfs：cat /proc/mounts → 看不到 overlay 行
```

App 读 `/proc/mounts`，**内核返回过滤后的内容**。

### 3. 内核符号隐藏

```
正常：cat /proc/kallsyms | grep ksu → 看到一堆
susfs：cat /proc/kallsyms | grep ksu → 看不到
```

App 读 `/proc/kallsyms`，**内核返回抹掉可疑符号的版本**。

### 4. 进程隐藏

```
正常：ps -ef → 看到 ksud
susfs：ps -ef → 看不到 ksud
```

App 遍历 `/proc/<pid>/`，**内核跳过可疑进程**。

### 5. bootloader 状态伪装

让 App 以为 BL 还锁着（实际上解锁了）。

### 6. 其他

- 隐藏 KSU 的 prctl 接口
- 隐藏 KSU 的 ioctl 接口
- 拦截 sus_path 配置的路径

---

## 🏗️ 怎么工作

susfs 由两部分组成：

```
┌──────────────────────────────────┐
│ 内核态                            │
│ ┌────────────────────────────┐ │
│ │ susfs kernel patch         │ │
│ │ - hook 文件系统调用          │ │
│ │ - 修改 stat/open 返回       │ │
│ │ - 修改 /proc 读取           │ │
│ └────────────────────────────┘ │
└──────────────────────────────────┘
              ↕
┌──────────────────────────────────┐
│ 用户态                            │
│ ┌────────────────────────────┐ │
│ │ ksu_susfs 命令行工具        │ │
│ │ - 配置要藏的路径            │ │
│ │ - 配置要藏的挂载            │ │
│ │ - 配置要藏的端口            │ │
│ └────────────────────────────┘ │
└──────────────────────────────────┘
```

**关键**：所有拦截都在**内核态**做——App 用什么方法都读不到真相。

---

## 📦 安装

### 方式 1：用 SukiSU（推荐）

SukiSU 已经集成 susfs：

1. 刷 SukiSU 内核（LKM 或 GKI）
2. 装 SukiSU Manager
3. Manager 里有 susfs 管理界面

**技术细节**：SukiSU 通过内核编译选项 `CONFIG_KSU_SUSFS` 内置 susfs。要启用，编译 SukiSU 内核时需要在 defconfig 里加：

```
CONFIG_KSU_SUSFS=y
```

### 方式 2：给官方 KSU 装

1. 内核要支持 susfs（要么自己编，要么找现成的 patch 过的内核）
2. 装 susfs4ksu 用户态模块（KSU 模块）

---

## 🆕 KSU v3.3.0+ 的内置替代：selinux_hide

如果你**不想装 susfs**，KSU v3.3.0+ 提供了一个内置 feature 可以做**部分**事情：

### 是什么

`selinux_hide`（feature ID=4）：**伪造 `/sys/fs/selinux` 的访问结果**，让 App 以为 SELinux 是 Enforcing（即使实际是 Permissive）。

### 开启

```bash
adb shell su -c "ksud feature set selinux_hide 1"
```

### 能做什么 / 不能做什么

| 能力 | selinux_hide | susfs |
|---|---|---|
| 伪造 SELinux 状态 | ✅ | ✅ |
| 隐藏文件路径 | ❌ | ✅ |
| 隐藏挂载点 | ❌ | ✅ |
| 隐藏内核符号 | ❌ | ✅ |
| 隐藏进程 | ❌ | ✅ |

**结论**：selinux_hide 只是"藏 SELinux 状态"这一个点，**不能替代 susfs**。但如果你的场景只是"银行 App 检测 SELinux"，开这个就够了。

---

## ⚙️ 配置

### 命令行

```bash
adb shell
su

# 添加要藏的路径
ksu_susfs add_sus_path /data/adb/ksu

# 添加要藏的挂载
ksu_susfs add_sus_mount /system

# 查看当前配置
ksu_susfs show

# 启用/禁用 susfs
ksu_susfs enable
ksu_susfs disable
```

### 通过 SukiSU Manager

SukiSU Manager → susfs 管理页面 → 图形化配置。

---

## 🧪 验证 susfs 是否工作

```bash
adb shell

# 1. 看 /proc/mounts（不含 root）
cat /proc/mounts | grep -E "ksu|magisk|overlay"
# 应该返回空

# 2. 看 /proc/kallsyms
cat /proc/kallsyms | grep -i ksu
# 应该返回空

# 3. 看 KSU 目录
ls /data/adb/ksu
# 应该返回 "No such file or directory"（在没 root 的视角）

# 但有 root 后能看到
su
ls /data/adb/ksu
# 这次能看到了
```

**注意**：susfs 是"对非 root 进程隐藏"。root 进程还能看到真相。

---

## ⚠️ 注意事项

### 1. 不是万能

susfs 只能藏**它能 hook 的 syscall**。如果 App 用了：
- 直接读裸设备
- 通过其他间接方式

可能绕开 susfs。

### 2. 内核版本兼容

susfs 是 kernel patch，**跟内核版本强绑定**。内核升级可能要重打 patch。

### 3. 跟模块的兼容

某些模块也会改文件系统行为，可能跟 susfs 冲突。

### 4. 性能开销

每次文件系统调用都要过 susfs 的过滤逻辑，**有轻微性能损耗**。

---

## 🎯 经典疑问

### Q1: 我已经装了 ZygiskNext，还需要 susfs 吗？

**需要**——它们藏的是不同东西：
- ZygiskNext 藏**进程内存**里的痕迹（.so 加载、堆栈）
- susfs 藏**文件系统**里的痕迹（文件、挂载、符号）

### Q2: susfs 能被绕过吗？

**理论上能**——如果 App 知道 susfs 的存在并主动对抗。但实际上：
- 大部分 App 不会专门对抗 susfs
- susfs 也在持续更新

### Q3: susfs 跟 Zygisk 啥关系？

**无关**。susfs 是**内核 patch**，不依赖 Zygisk。它在更底层工作。

### Q4: 我开了 selinux_hide，还需要 susfs 吗？

**看场景**：
- 只担心 SELinux 状态被检测 → 开 selinux_hide 就够了
- 还要藏文件/挂载/符号 → 必须装 susfs

---

## 🧰 同类 / 互补工具

- **[PathMask](https://github.com/Andrea-lyz/LKM-PathMask)**：LKM 形态的路径隐藏，带"隔离防护（procguard）"——KSU LKM 模式下没编译 susfs 时的替代
- **[NoMount](https://github.com/maxsteeel/nomount)**：内核 VFS 路径重定向框架，按元模块方式工作
- **[FuseFixer](https://github.com/5ec1cff/FuseFixer)**：修 FUSE 的零宽读取探测（App 用 0 长度 read 探挂载痕迹）
- **[Inode-Hijacker](https://github.com/YiJieqwq/Inode-Hijacker/releases)**：把 `/data/local/tmp` 的 inode 编号伪装回正常范围。副作用：用后有线投屏（Scrcpy 等）可能挂，执行 `su -c restorecon -RF /data/local/tmp` 恢复

---

## 📚 资料来源

- [susfs4ksu 仓库](https://gitlab.com/simonpunk/susfs4ksu)
- [SukiSU 集成 susfs 提交](https://github.com/SukiSU-Ultra/SukiSU-Ultra)
- [KernelSU v3.3.0 release](https://github.com/tiann/KernelSU/releases)
