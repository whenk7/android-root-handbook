---
title: "KSU 元模块（Metamodule）"
---

> Metamodule 是 KSU 的"插件化挂载"机制——把"怎么挂模块"这件事外包出去。这篇讲它的设计思想和使用。

---

## 🎯 是啥

**Metamodule（元模块）**是一种特殊的 KSU 模块，**给其他模块提供挂载实现**。

**核心思想**：KernelSU 自己不实现"怎么把模块挂到 /system"，把这件事外包给 metamodule。

---

## 🏗️ 架构对比

### 传统 Magisk

```
┌─────────────┐
│ Magisk 核心  │
│  内置 mount  │
│  逻辑写死    │
└──────┬──────┘
       │
       ↓
   挂载 /system
```

### KernelSU

```
┌─────────────┐
│ KernelSU 核心│
│  不含 mount  │
│  逻辑       │
└──────┬──────┘
       │ 委托
       ↓
   ┌──────────────┐
   │ Metamodule    │
   │ (三选一)      │
   │ ① meta-overlayfs (官方)
   │ ② meta-magicmount (Magisk 风格)
   │ ③ 你自己写一个
   └──────┬───────┘
          ↓
       挂载 /system
```

---

## 🌟 好处

1. **反检测**：KernelSU 自己不 mount，就少一个被检测的特征
2. **可插拔**：你想换挂载方案不用重编 KernelSU，换个 metamodule 就行
3. **创新**：社区可以做 FUSE 挂载、EROFS 挂载，不用动主仓库

---

## 📦 主流 Metamodule

### meta-overlayfs（官方）

- **作者**：tiann（KSU 作者）
- **挂载方式**：Linux OverlayFS
- **特点**：默认、稳定、推荐

### meta-magicmount（第三方）

- **挂载方式**：Magisk 风格 bind mount
- **特点**：兼容 Magisk 老模块

### 其他

社区可能有 FUSE-based、EROFS-based 的实验性 metamodule。

---

## 🛠️ 使用

### 安装

跟普通模块一样：
1. 下载 metamodule ZIP
2. KSU Manager 装
3. 重启

### 卸载

```
Manager → 卸载 → 重启
```

**警告**：卸载 metamodule 后**所有模块都不会被挂载**——直到你装另一个。

### 单一性约束

**只能装一个 metamodule**——装第二个会顶掉第一个。

---

## 🔍 查看当前 metamodule

```bash
adb shell
su

# 看 /data/adb/metamodule 指向哪
ls -la /data/adb/metamodule

# 应该看到：
# /data/adb/metamodule -> /data/adb/modules/meta-overlayfs
```

---

## 🧬 Metamodule 是怎么工作的

### 关键文件

```
meta-overlayfs/
├── module.prop          ← 元数据，含 metamodule=true
├── metainstall.sh       ← 其他模块安装时的钩子
├── metamount.sh         ← 挂载时的钩子
├── metainstaller.sh     ← 模块安装器
└── ...
```

### module.prop

```ini
id=meta-overlayfs
name=Meta OverlayFS
version=v1
versionCode=1
author=tiann
description=Official OverlayFS metamodule
metamodule=true    ← 关键标记
```

**`metamodule=true` 告诉 KSU**："我是 metamodule，不是普通模块"。

### 挂载流程

```
1. 开机 post-fs-data 阶段
2. KSU 看 /data/adb/metamodule 指向哪
3. 加载 metamodule 的 metamount.sh
4. 调用它来挂载普通模块的 system/
```

---

## 🛠️ 自己写一个 Metamodule（进阶）

### 最小骨架

```
my_metamodule/
├── module.prop
├── metainstall.sh
└── metamount.sh
```

### module.prop

```ini
id=my_metamodule
name=My Meta Module
version=v1
versionCode=1
author=me
description=My custom mount implementation
metamodule=true
```

### metamount.sh

```sh
#!/system/bin/sh
# $1 = 要挂载的模块目录
# $2 = 挂载点（一般是 /system）

MODULE_DIR=$1
TARGET=$2

# 用 OverlayFS 挂载
mount -t overlay overlay \
    -o lowerdir=$TARGET,upperdir=$MODULE_DIR/system \
    $TARGET
```

---

## 🎯 经典疑问

### Q1: 我没装 metamodule，模块还能用吗？

**部分能用**：
- 模块的 `service.sh` 能跑（不需要挂载）
- 模块的 `sepolicy.rule` 能生效
- 模块的 `system.prop` 能生效

**不能用**：
- 模块的 `system/` 目录挂载（没挂载实现）

### Q2: 我装了 meta-overlayfs，但模块还是不生效？

可能原因：
- 模块没启用
- 模块的 `system/` 目录是空的
- metamodule 自己有问题

### Q3: 我能写一个"不挂载，只跑脚本"的 metamodule 吗？

**能**——metamodule 的 `metamount.sh` 可以**啥都不干**：

```sh
#!/system/bin/sh
# 不挂载任何东西
exit 0
```

效果：所有模块的 `system/` 都不被挂载（但脚本还是会跑）。

---

## 📚 资料来源

- [KernelSU metamodule.md](https://github.com/tiann/KernelSU/blob/main/website/docs/guide/metamodule.md)
- [meta-overlayfs 仓库](https://github.com/tiann/KernelSU/tree/main/userspace/meta-overlayfs)
