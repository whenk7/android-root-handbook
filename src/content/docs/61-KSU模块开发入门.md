---
title: "KSU 模块开发入门"
---

> 想自己写 KSU 模块？这篇讲最小可用模块怎么搭。

---

## 🎯 KSU 模块是啥

**一个 ZIP 包**，解压到 `/data/adb/modules/<MODID>/`，里面放：
- 元数据（`module.prop`）
- 启动脚本（`service.sh` 等）
- 可选的 system 文件、配置、二进制等

---

## 🏗️ 最小可用模块

### 只有 2 个文件

```
my_first_module/
├── module.prop       ← 必须
└── service.sh        ← 干活用的脚本
```

### module.prop

```ini
id=my_first
name=My First Module
version=v1
versionCode=1
author=your_name
description=Hello world
```

**字段约束**：

| 字段 | 约束 |
|---|---|
| `id` | 正则 `^[a-zA-Z][a-zA-Z0-9._-]+$`，发布后**不要改** |
| `versionCode` | **整数**，用于版本比较 |
| 其他 | 单行字符串，**必须 Unix (LF) 换行** |

### service.sh

```sh
#!/system/bin/sh
# MODDIR = 模块所在目录（自动）
MODDIR=${0%/*}

# 等系统启动完
until [ "$(getprop sys.boot_completed)" = "1" ]; do
    sleep 1
done

# 干点事：写日志
echo "$(date): Hello from my module" >> /data/local/tmp/my_module.log
```

### 打包

```bash
cd my_first_module
zip -r ../my_first.zip . -x ".*"
```

### 安装

```
1. adb push my_first.zip /sdcard/
2. 打开 KSU Manager
3. 模块页 → ➕ → 选这个 ZIP
4. 装完 → 重启
```

### 验证

```bash
adb shell cat /data/local/tmp/my_module.log
# 应该看到一行带时间的日志
```

---

## 🌲 完整目录结构

```
my_module/
├── module.prop          ← 必须，元数据
│
│   ═══ 启动脚本 ═══
├── post-fs-data.sh      ← 可选，Zygote 启动前（阻塞）
├── post-mount.sh        ← 可选，OverlayFS 挂载后
├── service.sh           ← 可选，late_start 服务（推荐）
├── boot-completed.sh    ← 可选，开机完成后
│
│   ═══ 其他脚本 ═══
├── uninstall.sh         ← 可选，卸载时跑
├── action.sh            ← 可选，Manager 里点 Action 按钮跑
├── customize.sh         ← 可选，安装时跑（在 ZIP 里）
│
│   ═══ 挂载 ═══
├── system/              ← 可选，要挂载覆盖 /system 的文件
│   ├── app/Foo/Foo.apk
│   ├── etc/hosts
│   └── ...
│
│   ═══ 配置 ═══
├── system.prop          ← 可选，改 system properties
├── sepolicy.rule        ← 可选，SELinux 规则
├── config.json          ← 可选，模块配置
│
│   ═══ 高级 ═══
├── initrc/              ← 可选，注入 init.rc
│   └── myservice.rc
├── preinit/             ← 可选，SELinux 加载前跑
│   ├── preinit.conf
│   └── kmod/foo.ko
├── webroot/             ← 可选，WebUI 静态文件
│   └── index.html
│
│   ═══ 状态标志（KSU 自己用）═══
├── skip_mount           ← 存在则不挂载 system/
├── disable              ← 存在则模块被禁用
├── remove               ← 存在则下次重启删除
└── update               ← 存在则是更新版本
```

---

## 🎬 启动脚本执行时机

按时间顺序：

```
开机
 ↓
[preinit/preinit.conf]     ← SELinux 加载前，可加载 .ko
 ↓
SELinux 加载
 ↓
[post-fs-data.sh]          ← Zygote 启动前，阻塞式，10s 超时
 ↓                            ⚠️ 千万别用 setprop，会死锁！要用 resetprop -n
Zygote 启动
 ↓
[post-mount.sh]            ← OverlayFS 挂载完后
 ↓
[service.sh]               ← late_start 服务，非阻塞，推荐
 ↓
系统启动完成
 ↓
[boot-completed.sh]        ← 开机完成
```

**官方建议**：
- **能放 service.sh 就别放 post-fs-data.sh**
- **post-fs-data.sh 里绝对不能用 setprop，必须用 `resetprop -n`**

---

## 🔧 环境变量（脚本里可用）

| 变量 | 值 | 用途 |
|---|---|---|
| `KSU` | `true` | 判断是否在 KSU 环境（区分 Magisk） |
| `KSU_VER` | `v0.4.0` 之类 | KSU 版本字符串 |
| `KSU_VER_CODE` | `10672` 之类 | KSU 版本号 |
| `KSU_KERNEL_VER_CODE` | 内核态版本号 | |
| `BOOTMODE` | 永远 `true` | 兼容 Magisk |
| `MODPATH` | 模块安装路径 | **必须用这个，不要硬编码** |
| `TMPDIR` | 临时目录 | |
| `ZIPFILE` | 安装 ZIP 路径 | |
| `ARCH` | `arm`/`arm64`/`x86`/`x64` | |
| `API` | Android API level（如 33） | |
| `KSU_RUNTIME_MODE` | `built-in`/`lkm`/`late-load` | |
| `KSU_HAS_HOT_INSTALL` | 是否支持热安装 | |

**反向陷阱**：`MAGISK_VER` 在 KSU 里固定为 `v25.2`、`MAGISK_VER_CODE` 固定 `25200`——**不要用这两个判断是不是 Magisk**。

---

## 🛠️ 实用函数（在 customize.sh 里可用）

```sh
ui_print "hello"           # 打印到安装界面
abort "error msg"          # 终止安装

# 设置单个文件权限 + SELinux context
set_perm <file> <owner> <group> <mode> [context]
set_perm $MODPATH/bin/mytool 0 0 0755

# 递归设置目录
set_perm_recursive <dir> <owner> <group> <dirmode> <filemode> [context]
set_perm_recursive $MODPATH/bin 0 0 0755 0755
```

---

## 🧪 调试技巧

```bash
# 1. 看模块日志
adb shell
su
cat /data/adb/ksu/log/ksud.log

# 2. 手动跑脚本看错误
cd /data/adb/modules/<MODID>
sh -x service.sh

# 3. 看 SELinux 拦截
dmesg | grep avc

# 4. 临时禁用模块
touch /data/adb/modules/<MODID>/disable

# 5. 删除模块
touch /data/adb/modules/<MODID>/remove   # 下次重启生效
```

---

## 🎯 经典疑问

### Q1: 我能不能在模块里跑 Python / Node？

**理论上可以**——但要把 Python/Node 解释器静态编译后塞进模块里。

**不推荐**：
- 包大
- 性能差
- 大部分场景 shell 够用

### Q2: 我能在脚本里 sleep 很久吗？

**不建议**——会拖慢开机。

如果非要等：
- `service.sh`：可以
- `post-fs-data.sh`：10 秒超时
- `boot-completed.sh`：可以

### Q3: 模块能联网吗？

**能**——只要你有 inet 组。

```sh
# 模块脚本默认有 root + inet
curl https://example.com
```

---

## 📚 资料来源

- [KernelSU module.md](https://github.com/tiann/KernelSU/blob/main/website/docs/guide/module.md)
- [KernelSU difference-with-magisk.md](https://github.com/tiann/KernelSU/blob/main/website/docs/guide/difference-with-magisk.md)
