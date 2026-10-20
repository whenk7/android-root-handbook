# 31 · KSU 模块是什么

> 这篇讲 KSU 模块的格式、能干什么、不能干什么。

---

## 🎯 一句话定义

> **KSU 模块就是一个 ZIP 包**，解压到 `/data/adb/modules/<MODID>/`，里面放一些文件和脚本，KSU 在开机时按规则处理。

---

## 📦 模块的物理形式

### 安装位置

```
/data/adb/modules/<MODID>/
```

`MODID` 是模块的唯一 ID（在 `module.prop` 里定义）。

### 完整目录结构

```
/data/adb/modules/<MODID>/
│
│   ═══ 必须 ═══
├── module.prop          ← 模块元数据（没它 KSU 不识别）
│
│   ═══ 挂载相关 ═══
├── system/              ← 这个目录会被 OverlayFS 挂到 /system
│   ├── app/Foo/Foo.apk
│   ├── etc/hosts
│   └── ...
│
│   ═══ 启动脚本（按时机执行）═══
├── post-fs-data.sh      ← Zygote 启动前（阻塞，10s 超时）
├── post-mount.sh        ← OverlayFS 挂载后
├── service.sh           ← late_start 服务（推荐）
├── boot-completed.sh    ← 开机完成后
│
│   ═══ 其他脚本 ═══
├── uninstall.sh         ← 卸载时跑
├── action.sh            ← Manager 里点 Action 按钮跑
├── customize.sh         ← 安装时跑（在 ZIP 里）
│
│   ═══ 配置 ═══
├── system.prop          ← 改 system properties
├── sepolicy.rule        ← 注入 SELinux 规则
├── config.json          ← 模块配置（新版支持）
│
│   ═══ 高级 ═══
├── initrc/              ← 注入 init.rc
│   └── myservice.rc
├── preinit/             ← SELinux 加载前跑
│   ├── preinit.conf
│   └── kmod/foo.ko
├── webroot/             ← WebUI 静态文件
│   └── index.html
│
│   ═══ 状态标志（KSU 自己用）═══
├── skip_mount           ← 存在则不挂载 system/
├── disable              ← 存在则模块被禁用
├── remove               ← 存在则下次重启删除
├── update               ← 存在则是更新版本
│
│   ═══ 自动生成的符号链接 ═══
├── vendor -> system/vendor
├── product -> system/product
└── system_ext -> system/system_ext
```

---

## 📝 module.prop（必须）

```ini
id=my_awesome_module
name=My Awesome Module
version=v1.0.0
versionCode=100
author=your_name
description=这个模块干 XXX
updateJson=https://your-site.com/update.json
```

**字段约束**：

| 字段 | 约束 |
|---|---|
| `id` | 正则 `^[a-zA-Z][a-zA-Z0-9._-]+$`，发布后**不要改** |
| `versionCode` | **整数**，用于版本比较 |
| 其他 | 单行字符串，**必须 Unix (LF) 换行** |

---

## 🎯 模块能干什么

### 1. 改 /system 文件（OverlayFS 挂载）

```
模块里放 system/ 目录 → 开机时被 OverlayFS 挂到 /system
```

**典型用途**：
- 改 hosts
- 加字体
- 改 framework 资源
- 替换系统 App

**前提**：装了 metamodule（如 meta-overlayfs）。

### 2. 跑启动脚本

按时机：
- `post-fs-data.sh`：Zygote 之前
- `service.sh`：late_start 服务（推荐）
- `boot-completed.sh`：开机完成后

**典型用途**：
- 启动 daemon
- 改 props
- 跑定时任务

### 3. 改 SELinux 规则

```
sepolicy.rule → 注入到 SELinux 策略
```

**典型用途**：
- 让某个 App 能读模块文件
- 让脚本能做某些操作

### 4. 改 system properties

```
system.prop → 等价于改 build.prop
```

### 5. 加载内核模块

```
preinit/preinit.conf → insmod xxx.ko
```

**典型用途**：
- 加载自定义驱动
- 加载特殊内核功能

### 6. 注入 init.rc 服务

```
initrc/myservice.rc → 注册一个 init service
```

**典型用途**：
- 注册系统服务
- 在特定触发器启动 daemon

### 7. 提供 WebUI

```
webroot/index.html → 在 Manager 里有配置界面
```

---

## ❌ 模块不能干什么

| 不能干 | 原因 |
|---|---|
| 直接 hook Java 方法 | KSU 不动 ART，那是 LSP 的事 |
| 改某个 App 的内存 | KSU 不在 App 进程里 |
| 在运行时改配置 | 模块挂载在 post-fs-data 时就定型了 |
| 改已运行的进程 | 同上 |

**要干这些**，用 LSP 模块。

---

## 🔄 模块的生命周期

### 安装

```
1. 你在 Manager 里选 ZIP
2. Manager 解压到 /data/adb/modules_update/<MODID>/
3. 跑 customize.sh（如果有）
4. 提示重启
5. 重启时，modules_update 移动到 modules/
6. 挂载 system/、跑脚本
```

### 启用 / 禁用

```
启用：删除 /data/adb/modules/<MODID>/disable 文件
禁用：创建 /data/adb/modules/<MODID>/disable 文件

都需要重启生效
```

### 卸载

```
方式 1：Manager 里点卸载 → 标记 remove → 重启时删除

方式 2：手动
  touch /data/adb/modules/<MODID>/remove   # 标记
  reboot                                    # 重启删除
```

### 更新

```
装同名 ID 的新版本 ZIP → 走"安装"流程
老版本被替换
```

---

## 🆚 KSU 模块 vs Magisk 模块

**格式几乎兼容**——大部分 Magisk 模块可以直接刷到 KSU。

**关键差异**：

| 维度 | Magisk | KSU |
|---|---|---|
| 挂载方式 | 内置 Magic Mount | metamodule 可插拔 |
| 删除文件 | `.replace` 文件 | `mknod c 0 0` |
| 环境变量 | `MAGISK_VER_CODE` 真实 | 固定 `25200` |
| 安装方式 | Manager / Recovery | 仅 Manager |
| Zygisk | 内置 | 要装 ZygiskNext |

---

## 🧪 亲手看看

```bash
adb shell
su

# 看所有模块
ls /data/adb/modules/

# 看一个模块的结构
ls -la /data/adb/modules/<某模块>/

# 看模块的元数据
cat /data/adb/modules/<某模块>/module.prop

# 看模块的脚本
cat /data/adb/modules/<某模块>/service.sh
```

---

## 🎯 经典疑问

### Q1: 我能直接改 `/data/adb/modules/<mod>/` 里的文件吗？

**可以，但要重启生效**——因为挂载和脚本执行都发生在开机时。

### Q2: 我装的模块在 Manager 里看不到？

可能：
- 没有 `module.prop` 文件
- `module.prop` 里 `id` 不合法
- 文件权限不对（应 0644）

### Q3: 我能用 Magisk 模块直接刷到 KSU 吗？

**大部分能**——格式兼容。但用到以下特性的会出问题：
- `.replace` 文件
- Recovery 模式刷入
- 内置 Zygisk 相关

---

## 📚 资料来源

- [KernelSU module.md](https://github.com/tiann/KernelSU/blob/main/website/docs/guide/module.md)
- [KSU vs Magisk 差异](https://github.com/tiann/KernelSU/blob/main/website/docs/guide/difference-with-magisk.md)
