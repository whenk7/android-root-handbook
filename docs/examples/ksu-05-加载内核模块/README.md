# ksu-05-加载内核模块

> 对应文档：[docs/62-KSU启动脚本](../../62-KSU启动脚本.md)（preinit 部分）
>
> 难度：⭐⭐⭐

在 SELinux 加载**之前**加载一个 `.ko` 内核模块——这是 KSU 最早的可执行点。

---

## ⚠️ 前置要求

- 你有一个**匹配你内核**的 `.ko` 文件
- `.ko` 必须是**静态链接**
- `/metadata` 分区有足够空间（4MB 限制）

---

## 📂 结构

```
ksu-05-加载内核模块/
├── README.md
├── module.prop
└── preinit/
    ├── preinit.conf          ← 配置
    └── kmod/
        └── mydrv.ko          ← 你的内核模块（这里没真的放）
```

---

## 🧬 preinit.conf 语法

每行一个 action：

```
# 注释以 # 开头

insmod kmod/foo.ko debug=1           ← 加载内核模块（带参数）
insmod? kmod/optional.ko             ← 加 ? 表示失败不报错
exec -t 10000 bin/fixup --flag      ← 跑二进制（10s 超时）
write /proc/sys/kernel/printk_devkmsg on    ← 写 procfs/sysfs
```

---

## ⚠️ 限制

- 只能用**静态链接**的二进制（没 shell、没动态库）
- `/apex` 还没挂载
- 不能读写 Android properties
- 30 秒超时
- 整个 stage 占用 `/metadata` **最多 4MB**

---

## 🧪 测试用的 .ko

本示例**没真的提供 .ko**——你需要：

1. **自己编译**（要写内核模块代码）
2. **从网上找现成的**（注意 KMI 必须匹配你的内核）

### 一个最小 .ko 源码

`mydrv.c`:
```c
#include <linux/module.h>
#include <linux/kernel.h>
#include <linux/init.h>

static int __init mydrv_init(void) {
    printk(KERN_INFO "mydrv: loaded\n");
    return 0;
}

static void __exit mydrv_exit(void) {
    printk(KERN_INFO "mydrv: unloaded\n");
}

module_init(mydrv_init);
module_exit(mydrv_exit);
MODULE_LICENSE("GPL");
```

### 编译

需要内核源码和交叉编译工具链，过程复杂，超出本手册范围。

---

## 📦 打包

```bash
cd ksu-05-加载内核模块
zip -r ../ksu-05.zip . -x ".*" -x "README.md"
```

---

## 📥 安装

```
1. adb push ../ksu-05.zip /sdcard/
2. KSU Manager → 模块 → ➕ → 选 /sdcard/ksu-05.zip
3. 重启
```

---

## ✅ 验证

```bash
# 看 .ko 是否被加载
adb shell su -c "lsmod | grep mydrv"

# 看内核日志
adb shell su -c "dmesg | grep mydrv"
```

应该看到 `mydrv: loaded`。

---

## 🧠 学到的东西

- `preinit` 是 KSU 最早的执行点
- 只能跑静态二进制
- 30 秒超时
- 4MB 空间限制
