---
title: "prop 隐藏"
---

> ZygiskNext v1.3.0+ 整合了 Shamiko 的大部分功能，但**prop 隐藏**没接。这篇讲怎么自己搞。

---

## 🎯 是什么

**prop 隐藏** = 修改 Android 系统属性（system properties），让 App 读到的值是"干净"的。

### 常见要改的 prop

| prop | 正常值 | root 后可能被改成 | 检测含义 |
|---|---|---|---|
| `ro.debuggable` | `0` | `1` | 是否可调试 |
| `ro.secure` | `1` | `0` | 是否安全模式 |
| `ro.build.type` | `user` | `userdebug` / `eng` | 构建类型 |
| `ro.boot.verifiedbootstate` | `green` | `orange` / `yellow` | 启动验证状态 |
| `ro.boot.vbmeta.device_state` | `locked` | `unlocked` | BL 锁状态 |
| `init.svc.adbd` | `stopped` | `running` | adb 是否在跑 |

---

## 🔍 为啥 ZN 没接

ZygiskNext v1.3.0 release note 原文：

> ZN 并未整合 Shamiko 的 prop 隐藏功能。若需要 prop 隐藏，请自行提取 Shamiko 的 service.sh，并通过 service.d 等方式开机启动。

**原因**：
- prop 隐藏是**系统级**的，不是**进程级**的
- ZN 专注于 Zygisk 进程内的事
- prop 修改需要在**开机时**执行，不是 App fork 时

---

## 🛠️ 怎么做

### 方法 1：提取 Shamiko 的 service.sh（推荐）

Shamiko 的 ZIP 包里有个 `service.sh`，里面就是 prop 隐藏的命令。

#### 步骤

1. **下载 Shamiko ZIP**（不用装，只要文件）

2. **解压 ZIP**，找到 `service.sh`

3. **提取关键命令**：

```bash
#!/system/bin/sh
# Shamiko 的 service.sh 内容示例

# 修改 ro.debuggable
resetprop ro.debuggable 0

# 修改 ro.secure
resetprop ro.secure 1

# 修改 ro.build.type
resetprop ro.build.type user

# 修改 ro.boot.verifiedbootstate
resetprop ro.boot.verifiedbootstate green

# 修改 ro.boot.vbmeta.device_state
resetprop ro.boot.vbmeta.device_state locked
```

4. **放到自己的模块里**：

创建一个 KSU 模块，把上面的命令放到 `service.sh` 或 `post-fs-data.sh` 里。

**目录结构**：

```
my-prop-hide/
├── module.prop
├── service.sh          ← 放这里
└── system.prop         ← 可选
```

**module.prop**：

```ini
id=my-prop-hide
name=My Prop Hide
version=v1.0
versionCode=1
author=you
description=Hide root-related system properties
```

**service.sh**：

```bash
#!/system/bin/sh

# 等待系统启动完成
sleep 10

# 修改 prop
resetprop ro.debuggable 0
resetprop ro.secure 1
resetprop ro.build.type user
resetprop ro.boot.verifiedbootstate green
resetprop ro.boot.vbmeta.device_state locked
```

5. **打包安装**：

```bash
cd my-prop-hide
zip -r ../my-prop-hide.zip *
```

然后在 KSU Manager 里装这个 ZIP。

---

### 方法 2：自己写 post-fs-data.sh

如果你不想提取 Shamiko 的，可以自己写。

**post-fs-data.sh**（开机早期执行）：

```bash
#!/system/bin/sh

# 注意：post-fs-data.sh 不能用 setprop，会死锁
# 必须用 resetprop -n

resetprop -n ro.debuggable 0
resetprop -n ro.secure 1
resetprop -n ro.build.type user
resetprop -n ro.boot.verifiedbootstate green
resetprop -n ro.boot.vbmeta.device_state locked
```

**⚠️ 关键区别**：
- `post-fs-data.sh`：**必须用 `resetprop -n`**（`-n` 表示不触发 property service）
- `service.sh`：可以用 `resetprop`（系统已经启动，不会死锁）

---

### 方法 3：用现成的模块

社区有现成的 prop 隐藏模块，比如：

- **MagiskHide Props Config**（Magisk 模块，KSU 也能用）
- **Play Integrity Fix**（自带 prop 修改）

---

## 🧪 验证

```bash
adb shell

# 1. 看当前 prop 值
getprop ro.debuggable
# 应该返回 0

getprop ro.secure
# 应该返回 1

getprop ro.build.type
# 应该返回 user

getprop ro.boot.verifiedbootstate
# 应该返回 green

getprop ro.boot.vbmeta.device_state
# 应该返回 locked

# 2. 用 App 验证
# 装个 "RootBeer Fresh" 或 "Momo"，跑一下看哪项过不过
```

---

## ⚠️ 注意事项

### 1. 不要改太多

**只改必要的**——改太多反而容易被检测（因为正常手机的 prop 组合是有规律的）。

### 2. 有些 prop 改不了

某些 prop 是**只读**的（比如 `ro.bootloader`），改了会报错。

### 3. 重启后失效

prop 修改是**运行时**的，重启后要重新跑一遍。所以要放在 `service.sh` 或 `post-fs-data.sh` 里。

### 4. 跟模块的冲突

如果你装了多个模块都改 prop，**后执行的会覆盖先执行的**。注意顺序。

---

## 🎯 经典疑问

### Q1: 我装了 ZygiskNext，还需要 prop 隐藏吗？

**看情况**：
- 如果你的银行 App 只检测 Zygisk 痕迹 → 不需要
- 如果还检测 `ro.debuggable` 等 prop → 需要

### Q2: prop 隐藏会被检测吗？

**会**——如果 App 知道你改了 prop。但大部分 App 只是读值，不会验证"这个值是不是被改的"。

### Q3: 我能不能直接改 build.prop 文件？

**不推荐**：
- 改 `/system/build.prop` 会被 dm-verity 检测
- 用 `resetprop` 是运行时修改，不改文件

### Q4: resetprop 和 setprop 啥区别？

- `setprop`：标准命令，会触发 property service
- `resetprop`：Magisk/KSU 提供的，**绕过 property service** 直接改内存

**post-fs-data.sh 必须用 `resetprop -n`**，否则会死锁（因为这时候 property service 还没启动）。

---

## 📚 资料来源

- [ZygiskNext v1.3.0 release](https://github.com/Dr-TSNG/ZygiskNext/releases/tag/v1.3.0)
- [Magisk resetprop 文档](https://github.com/topjohnwu/Magisk/blob/master/docs/index.md)
- [MagiskHide Props Config](https://github.com/Magisk-Modules-Repo/MagiskHidePropsConf)
