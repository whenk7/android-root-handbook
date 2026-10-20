# ksu-06-InitRc服务注入

> 对应文档：[docs/62-KSU启动脚本](../../62-KSU启动脚本.md)
>
> 难度：⭐⭐

通过 initrc 注入，在 init.rc 里注册一个系统服务。

---

## 🎯 是啥

KSU 在 init 读 `init.rc` 时**拦截 syscall**，把你模块里的 `.rc` 内容**追加到 init.rc 末尾**。init 就会注册你定义的服务。

---

## 📂 结构

```
ksu-06-InitRc服务注入/
├── README.md
├── module.prop
├── service.sh                       ← 也写一个，跟 init.rc 双保险
├── initrc/
│   └── myservice.rc                 ← init.rc 片段
└── bin/
    └── myservice.sh                 ← 服务脚本
```

---

## 🧬 myservice.rc 详解

```
service myservice /data/adb/modules/my_initrc/bin/myservice.sh
    user root
    group root
    disabled
    seclabel u:r:ksu:s0

on property:sys.boot_completed=1
    start myservice
```

**翻译**：
- 注册一个叫 `myservice` 的服务
- 二进制路径：`/data/adb/modules/my_initrc/bin/myservice.sh`
- 用 root 跑
- 默认 disabled（不自动启动）
- SELinux context：`u:r:ksu:s0`
- **触发器**：`sys.boot_completed=1` 时启动

---

## 🧬 init 触发器（常用）

```
on early-init           ← 最早的初始化
on init                 ← 标准初始化
on late-init            ← 晚初始化
on boot                 ← boot 阶段
on property:sys.boot_completed=1   ← 系统启动完成
on property:sys.boot_from_charger_mode=1  ← 充电模式
```

---

## 📦 打包

```bash
cd ksu-06-InitRc服务注入
zip -r ../ksu-06.zip . -x ".*" -x "README.md"
```

---

## 📥 安装

```
1. adb push ../ksu-06.zip /sdcard/
2. KSU Manager → 模块 → ➕ → 选 /sdcard/ksu-06.zip
3. 重启
```

---

## ✅ 验证

```bash
# 看服务是否被注册
adb shell su -c "getprop | grep myservice"

# 看服务是否在跑
adb shell su -c "ps -ef | grep myservice"

# 看日志
adb shell "cat /data/local/tmp/myservice.log"
```

---

## 🎯 经典用法

### 场景 1：开机启动一个监控服务

```rc
service mymonitor /data/adb/modules/my_initrc/bin/monitor
    user root
    group root
    oneshot
    seclabel u:r:ksu:s0

on property:sys.boot_completed=1
    start mymonitor
```

### 场景 2：定时任务

```rc
service mycron /data/adb/modules/my_initrc/bin/cron
    user root
    group root
    disabled
    seclabel u:r:ksu:s0

on property:vendor.sys.boot_completed=1
    start mycron
```

---

## ⚠️ 注意

### 1. late-load 模式不可用

官方文档：
> initrc injection is **not available** in **late-load mode**, as the system call hooks are not installed in that mode.

### 2. 服务脚本的执行权限

`myservice.sh` 必须可执行：

```bash
chmod 0755 bin/myservice.sh
```

### 3. SELinux context

`seclabel u:r:ksu:s0` 是 KSU 给 root 服务的默认 context。如果不够，自己写 sepolicy.rule。

---

## 🧠 学到的东西

- initrc 注入机制
- init.rc 服务语法
- 触发器（on property:...）
