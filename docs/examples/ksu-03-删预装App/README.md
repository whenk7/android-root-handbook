# ksu-03-删预装App

> 对应文档：[docs/63-KSU改系统文件](../../63-KSU改系统文件.md)
>
> 难度：⭐⭐

通过 OverlayFS 的 whiteout 机制"删除"系统预装 App（实际不删分区上的文件，只是看不见）。

---

## 📂 结构

```
ksu-03-删预装App/
├── README.md
├── module.prop
└── customize.sh         ← 用 REMOVE 变量指定要删的路径
```

---

## 🧬 原理

```
模块里的 customize.sh:
   REMOVE="/system/app/YouTube"

KSU 安装时：
   mknod $MODPATH/system/app/YouTube c 0 0
   （创建 whiteout 字符设备文件）

下次开机：
   OverlayFS 看到 c 0:0 → 把 /system/app/YouTube 隐藏
```

---

## 📦 打包

```bash
cd ksu-03-删预装App
zip -r ../ksu-03.zip . -x ".*" -x "README.md"
```

---

## 📥 安装

```
1. adb push ../ksu-03.zip /sdcard/
2. KSU Manager → 模块 → ➕ → 选 /sdcard/ksu-03.zip
3. 重启
```

---

## ✅ 验证

```bash
# 看 YouTube 是否被隐藏
adb shell ls /system/app/YouTube
# 应该返回：No such file or directory

# 看模块里的 whiteout 文件
adb shell su -c "ls -l /data/adb/modules/remove_bloatware/system/app/"
# 应该看到：crw------- 1 root root 0, 0 ... YouTube
```

---

## 🛠️ 自定义

编辑 `customize.sh`，把你想要删的预装 App 路径加进 `REMOVE` 列表。

**怎么找要删的路径**：

```bash
# 列出系统预装 App
adb shell ls /system/app/
adb shell ls /system/priv-app/

# 看 product 分区
adb shell ls /product/app/
adb shell ls /product/priv-app/

# 看 vendor 分区
adb shell ls /vendor/app/
```

---

## ⚠️ 警告

**别乱删**：
- `/system/framework/` 下的东西
- `/system/bin/` 下的关键二进制
- 系统服务（如 `SystemUI`、`Settings`）

**删错了开不了机**。

---

## 🧠 学到的东西

- `REMOVE` 变量自动创建 whiteout
- OverlayFS 的 c 0:0 = "删除标记"
- 卸载模块就恢复原状
