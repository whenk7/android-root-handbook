---
title: "KSU 启动脚本"
---

> KSU 模块的 4 种启动脚本：`post-fs-data.sh`、`service.sh`、`boot-completed.sh`、`preinit/preinit.conf`。这篇讲清楚每个该干啥。

---

## 🕐 时机对比

```
开机
 ↓
[preinit/preinit.conf]     ← SELinux 加载前
 ↓
SELinux 加载
 ↓
[post-fs-data.sh]          ← Zygote 启动前，阻塞式，10s 超时
 ↓
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

---

## 1️⃣ preinit/preinit.conf（最早）

### 时机

SELinux 加载**之前**。整个系统最早的可执行点。

### 能干什么

- **加载内核模块（`.ko`）**
- 写 `/proc`、`/sys`
- 跑**静态链接**的二进制

### 不能干什么

- 不能用 shell（没有 shell 可跑）
- 不能读写 Android properties
- 不能用 `/apex`

### 文件格式

`preinit/preinit.conf`，**每行一个 action**：

```
# 注释以 # 开头
insmod kmod/foo.ko debug=1            ← 加载内核模块
insmod? kmod/optional.ko              ← 加 ? 表示失败不报错
exec -t 10000 bin/fixup --flag       ← 跑二进制（10s 超时）
write /proc/sys/kernel/printk_devkmsg on    ← 写 procfs/sysfs
```

### 限制

- 整个 stage **30 秒超时**
- `/metadata` 分区最多用 **4MB**
- 只能跑**静态链接**二进制
- 不能用 shell 语法

### 适用场景

- 加载驱动
- 改内核参数
- 早期 debug

---

## 2️⃣ post-fs-data.sh（早，阻塞）

### 时机

Zygote 启动**之前**，分区刚挂完。

### 特点

- **阻塞式**：KSU 等你脚本跑完才继续
- **10 秒超时**
- **此时模块还没挂载**——能在这里"动态改模块文件"

### 陷阱

**绝对不能用 `setprop`**，会死锁整个开机流程。

**正确做法**：用 `resetprop -n <key> <value>`

### 适用场景

- 在挂载前修改模块的 `system/` 目录内容
- 根据设备状态决定要不要启用某些文件

### 例子

```sh
#!/system/bin/sh
MODDIR=${0%/*}

# 检查某个条件
if [ -f "/data/local/tmp/disable_feature" ]; then
    # 移除模块的某个文件（阻止它被挂载）
    rm -f $MODDIR/system/etc/some_feature.xml
fi

# 改 prop（注意要用 resetprop）
resetprop -n ro.debuggable 0
```

---

## 3️⃣ post-mount.sh（中）

### 时机

OverlayFS 挂载**完成后**。

### 特点

- 非阻塞
- 此时模块的 `system/` 已经挂到 `/system`

### 适用场景

- 验证挂载是否成功
- 做挂载后的清理

### 例子

```sh
#!/system/bin/sh
MODDIR=${0%/*}

# 检查挂载是否成功
if grep -q "my_module" /proc/mounts; then
    log "Mount successful"
else
    log "Mount failed"
fi
```

---

## 4️⃣ service.sh（推荐）

### 时机

Zygote 已启动，系统服务正在启动。

### 特点

- **非阻塞**
- 系统基本就绪

### 适用场景

**90% 的脚本应该放这里**——启动 daemon、定时任务、写日志、改 props。

### 例子

```sh
#!/system/bin/sh
MODDIR=${0%/*}

# 等系统启动完
until [ "$(getprop sys.boot_completed)" = "1" ]; do
    sleep 1
done

# 启动 daemon（后台）
nohup $MODDIR/bin/mydaemon > /data/local/tmp/mydaemon.log 2>&1 &

# 改 prop
resetprop ro.my.custom.prop 1
```

---

## 5️⃣ boot-completed.sh（最晚）

### 时机

系统完全启动完毕（`sys.boot_completed=1`）。

### 适用场景

- 弹通知
- 拉起用户态服务
- 做需要系统完全就绪才能做的事

### 例子

```sh
#!/system/bin/sh
MODDIR=${0%/*}

# 系统已经完全启动，直接干活
am broadcast -a my.module.BOOT_COMPLETED
```

---

## 🌐 全局脚本目录

KSU 还支持**全局脚本**（不属于任何模块）：

```
/data/adb/
├── post-fs-data.d/     ← 开机时跑（post-fs-data 阶段）
│   └── myscript.sh
├── service.d/          ← 开机时跑（service 阶段）
│   └── myservice.sh
├── post-mount.d/       ← post-mount 阶段
│   └── ...
└── boot-completed.d/   ← boot-completed 阶段
    └── ...
```

**要求**：脚本必须**可执行**（`chmod +x`）。

**模块不应该写到这些目录**——这些是**用户全局脚本**，不属于任何模块。

---

## ⚠️ 常见坑

### 坑 1：post-fs-data.sh 用了 setprop

**后果**：开机死锁。

**解法**：改用 `resetprop -n`。

### 坑 2：service.sh 里跑前台命令

**后果**：拖慢开机。

**解法**：用 `nohup ... &` 后台跑。

### 坑 3：脚本里访问未挂载的路径

**后果**：失败。

**例子**：post-fs-data.sh 里访问 `/system`——此时模块还没挂载。

### 坑 4：用相对路径

**后果**：失败——脚本运行时 cwd 不一定是模块目录。

**正确**：用 `$MODDIR` 或绝对路径。

```sh
# 错
./my_binary

# 对
$MODDIR/bin/my_binary
```

### 坑 5：脚本里产生大量输出

**后果**：日志爆炸。

**解法**：重定向到文件：

```sh
echo "log" >> /data/local/tmp/my_module.log
```

---

## 🎯 推荐写法

### 模板：service.sh（90% 场景够用）

```sh
#!/system/bin/sh
MODDIR=${0%/*}

# 等系统启动完
until [ "$(getprop sys.boot_completed)" = "1" ]; do
    sleep 1
done

# 你的逻辑
# ...

# 写日志（可选）
echo "$(date): service.sh ran" >> /data/local/tmp/my_module.log
```

### 模板：post-fs-data.sh（需要早期干预时）

```sh
#!/system/bin/sh
MODDIR=${0%/*}

# 你的逻辑（注意：不能用 setprop）
# ...

# 改 prop 用 resetprop
resetprop -n ro.some.prop value
```

---

## 📚 资料来源

- [KernelSU module.md - Boot scripts](https://github.com/tiann/KernelSU/blob/main/website/docs/guide/module.md#boot-scripts)
