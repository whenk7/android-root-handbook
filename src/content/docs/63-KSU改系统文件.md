---
title: "KSU 改系统文件（system/ 目录）"
---

> 这篇讲 KSU 模块怎么改 /system——挂载、覆盖、删除、新增。

---

## 🎯 核心机制

模块里的 `system/` 目录会被 **OverlayFS** 挂载到 `/system`：

```
模块里的：/data/adb/modules/<MODID>/system/etc/hosts
挂载后变成：/system/etc/hosts
```

**前提**：装了 metamodule（如 meta-overlayfs）。

---

## 📂 system/ 目录的映射

```
模块里                             挂载后
─────────────────────────────────────────────
system/etc/hosts                →  /system/etc/hosts
system/app/Foo/Foo.apk          →  /system/app/Foo/Foo.apk
system/framework/services.jar   →  /system/framework/services.jar
system/bin/mytool               →  /system/bin/mytool
```

### 自动符号链接

KSU 会自动创建：

```
vendor -> system/vendor
product -> system/product
system_ext -> system/system_ext
```

意思是：
- 你写 `system/vendor/xxx` 等价于写 `vendor/xxx`
- 模块里**只要写 `system/`**，vendor/product/system_ext 自动有了

---

## 🛠️ 三种操作

### 1. 加文件

**做法**：在 `system/` 下放一个**下层没有**的文件。

**例子**：加一个自定义命令 `mytool` 到 `/system/bin/`：

```
my_module/
└── system/
    └── bin/
        └── mytool       ← 你的二进制
```

挂载后：`/system/bin/mytool` 就有了。

### 2. 改文件

**做法**：在 `system/` 下放一个**下层有**的同名文件。

**例子**：改 hosts：

```
my_module/
└── system/
    └── etc/
        └── hosts       ← 你的版本，覆盖原版
```

挂载后：`/system/etc/hosts` 用的是你的版本。

### 3. 删文件（whiteout）

**做法**：在 `system/` 下放一个**字符设备文件**（`c 0:0`）。

**例子**：删 `/system/app/YouTube`：

**方法 A：手动 mknod**

```bash
mkdir -p my_module/system/app
cd my_module/system/app
mknod YouTube c 0 0
```

**方法 B：customize.sh 用 REMOVE 变量（推荐）**

`customize.sh`:
```sh
REMOVE="
/system/app/YouTube
/system/app/Bloatware
"
```

KSU 安装时自动执行：
```bash
mknod $MODPATH/system/app/YouTube c 0 0
mknod $MODPATH/system/app/Bloatware c 0 0
```

**详见 [05-挂载与覆盖](/android-root-handbook/05-挂载与覆盖)**。

### 4. 替换整个目录

**做法**：用 `REPLACE` 变量。

`customize.sh`:
```sh
REPLACE="
/system/app/YouTube
"
```

KSU 自动执行：
```bash
mkdir -p $MODPATH/system/app/YouTube
setfattr -n trusted.overlay.opaque -v y $MODPATH/system/app/YouTube
```

效果：`/system/app/YouTube` 被替换成**空目录**（不是删除，是替换成空）。

---

## 🛠️ customize.sh（安装时跑）

`customize.sh` 是**模块安装时**跑的脚本（在 ZIP 里，不在模块目录里）。

### 用途

- 检测设备兼容性
- 设置文件权限
- 打印安装信息
- 用 `REMOVE` / `REPLACE` 变量

### 例子

```sh
#!/system/bin/sh

# 检查 Android 版本
if [ "$API" -lt 30 ]; then
    abort "需要 Android 11+"
fi

# 打印信息
ui_print "安装我的模块..."
ui_print "Magisk/KSU 版本: $KSU_VER"

# 设权限
set_perm $MODPATH/bin/mytool 0 0 0755
set_perm_recursive $MODPATH/bin 0 0 0755 0755

# 删文件
REMOVE="
/system/app/YouTube
"

# 替换目录
REPLACE="
/system/app/Bloatware
"

ui_print "安装完成"
```

### 跳过默认安装

如果你想**完全自己控制**安装过程：

```sh
SKIPUNZIP=1   # 告诉 KSU 跳过默认的解压

# 然后你自己处理 ZIP 内容
unzip -o "$ZIPFILE" -d $MODPATH
# ...
```

---

## 🎯 完整例子：去广告模块

### 目录结构

```
adblock/
├── module.prop
├── customize.sh
└── system/
    └── etc/
        └── hosts
```

### module.prop

```ini
id=adblock
name=AdBlock Hosts
version=v1
versionCode=1
author=***
description=Replace hosts to block ads
```

### customize.sh

```sh
#!/system/bin/sh

ui_print "安装广告过滤模块..."

# 检查 metamodule
if [ ! -d "/data/adb/modules/meta-overlayfs" ]; then
    ui_print "警告：未检测到 meta-overlayfs，模块可能不生效"
fi

ui_print "完成"
```

### system/etc/hosts

```
127.0.0.1 localhost
::1 localhost

# 广告域名
127.0.0.1 ads.google.com
127.0.0.1 ad.xiaomi.com
127.0.0.1 union.mi.com
# ...
```

### 打包

```bash
cd adblock
zip -r ../adblock.zip . -x ".*"
```

### 装

```
1. adb push adblock.zip /sdcard/
2. KSU Manager → 装
3. 重启
4. 验证：ping ads.google.com → 应该是 127.0.0.1
```

---

## ⚠️ 注意事项

### 1. 必须先装 metamodule

**没装 meta-overlayfs 或类似**，模块的 `system/` 不会被挂载。

### 2. 文件权限

模块里 `system/` 下的文件**权限要保持**：
- 二进制要 `0755`
- 配置文件 `0644`

**用 customize.sh 设权限**：
```sh
set_perm $MODPATH/system/bin/mytool 0 0 0755
```

### 3. SELinux context

模块文件的 SELinux context 默认是 `u:object_r:system_file:s0`。

**特殊文件**可能要单独的 context：

```sh
set_perm $MODPATH/system/bin/mytool 0 0 0755 u:object_r:system_exec:s0
```

### 4. 不要破坏关键文件

**别乱改**：
- `/system/framework/services.jar`（系统核心）
- `/system/bin/app_process`（Zygote 启动器）
- `/system/build.prop`（系统属性）

改错了**开不了机**。

---

## 🧪 调试

### 看模块挂载情况

```bash
adb shell
su
mount | grep overlay
```

### 看模块文件是否生效

```bash
adb shell
cat /system/etc/hosts    # 看是不是你写的版本
```

### 看 whiteout 是否生效

```bash
adb shell
ls /system/app/YouTube   # 应该返回 "No such file or directory"
```

---

## 📚 资料来源

- [KernelSU module.md](https://github.com/tiann/KernelSU/blob/main/website/docs/guide/module.md)
- [KernelSU metamodule.md](https://github.com/tiann/KernelSU/blob/main/website/docs/guide/metamodule.md)
