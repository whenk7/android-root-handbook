---
title: "ksu-01-最小模块"
---

> 对应文档：[docs/61-KSU模块开发入门](/android-root-handbook/61-ksu模块开发入门)
>
> 难度：⭐

最小的 KSU 模块——开机后往日志写一行字。

---

## 📂 结构

```
ksu-01-最小模块/
├── README.md           ← 你在这
├── module.prop         ← 必须，模块元数据
└── service.sh          ← 开机后跑
```

---

## 📦 打包

```bash
cd ksu-01-最小模块
zip -r ../ksu-01.zip . -x ".*" -x "README.md"
```

---

## 📥 安装

```
1. adb push ../ksu-01.zip /sdcard/
2. KSU Manager → 模块 → ➕ → 选 /sdcard/ksu-01.zip
3. 装上后重启
```

---

## ✅ 验证

```bash
adb shell cat /data/local/tmp/my_first_module.log
```

应该看到：

```
2026-10-19 15:30:22: Hello from my first module
```

---

## 🧠 学到的东西

- 模块最少要 `module.prop` 和一个脚本
- `MODDIR=${0%/*}` 是模块目录的标准写法
- 用 `until getprop sys.boot_completed` 等系统启动完
