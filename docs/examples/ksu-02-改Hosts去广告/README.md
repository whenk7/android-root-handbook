# ksu-02-改Hosts去广告

> 对应文档：[docs/63-KSU改系统文件](../../63-KSU改系统文件.md)
>
> 难度：⭐

通过 OverlayFS 把 `/system/etc/hosts` 替换成自定义版本，屏蔽广告域名。

---

## 📂 结构

```
ksu-02-改Hosts去广告/
├── README.md
├── module.prop
└── system/
    └── etc/
        └── hosts          ← 替换 /system/etc/hosts
```

---

## ⚠️ 前提

**必须先装 metamodule**（如 `meta-overlayfs`）——否则 `system/` 不会被挂载。

```bash
adb shell su -c "ls /data/adb/metamodule"
# 应该指向某个目录
```

---

## 📦 打包

```bash
cd ksu-02-改Hosts去广告
zip -r ../ksu-02.zip . -x ".*" -x "README.md"
```

---

## 📥 安装

```
1. adb push ../ksu-02.zip /sdcard/
2. KSU Manager → 模块 → ➕ → 选 /sdcard/ksu-02.zip
3. 重启
```

---

## ✅ 验证

```bash
# 看 /system 是不是 overlay 挂载
adb shell mount | grep " /system "

# 看 hosts 是不是你的版本
adb shell cat /system/etc/hosts | head

# 测试广告域名
adb shell ping -c 1 ads.google.com
# 应该解析到 127.0.0.1
```

---

## 🛠️ 自定义

编辑 `system/etc/hosts`，加你要屏蔽的域名。

**广告域名清单参考**：
- [AdAway hosts](https://adaway.org/hosts.txt)
- [StevenBlack/hosts](https://github.com/StevenBlack/hosts)

---

## 🧠 学到的东西

- `system/` 目录会被 OverlayFS 挂载
- 模块改 /system 不动真分区
- 卸载模块就恢复原状
