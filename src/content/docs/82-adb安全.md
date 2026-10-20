---
title: "adb 安全"
---

> root 之后，**adb shell 默认就是 root 的**——这是个潜在的安全风险。

---

## ⚠️ 风险

```
你的手机 root 了
   ↓
adb shell 默认就是 root
   ↓
任何能连上 adb 的设备/网络都能拿你手机的 root
```

### 攻击场景

1. **公共 USB 充电桩**：可能通过 adb 攻击
2. **被入侵的电脑**：攻击者通过 adb 拿你手机
3. **同一 Wi-Fi**：开了"无线 adb"的话

---

## 🛡️ 防御

### 1. 不用时关掉开发者选项

```
设置 → 开发者选项 → 关闭
```

### 2. 不用时拔掉 USB 调试授权

```
设置 → 开发者选项 → 撤销 USB 调试授权
```

### 3. KSU 设置里关闭 adb shell 默认 root

```
KSU Manager → 设置 → 关闭"允许 adb shell 默认 root"
```

效果：
```
adb shell
$ su
# 会弹窗让你确认
```

### 4. 不用无线 adb

```
设置 → 开发者选项 → 关闭"无线调试"
```

### 5. 用 USB 仅充电模式

```
插 USB 时选"仅充电"
```

### 6. 用完擦除调试痕迹

"USB 调试已开启"本身就是检测项，调试还会在系统里留下痕迹（授权记录、临时文件）。用完 adb 可以跑一遍痕迹清理脚本（如 [ADB-Trace-Cleaner](https://github.com/YiJieqwq/ADB-Trace-Cleaner/releases)）。

---

## 🔍 检查 adb 授权

```bash
# 看授权过的电脑
adb shell
cat /data/misc/adb/adb_keys
```

---

## 🎯 经典疑问

### Q1: 我用 adb 就要 root，怎么办？

**KSU Manager 里给 shell 单独配 App Profile**：
- 允许 shell 拿 root
- 但限制 Capabilities

### Q2: 无线 adb 完全不能用吗？

**能，但要小心**：
- 只在自己信的 Wi-Fi 下开
- 用完立刻关
- 不要在公共 Wi-Fi 下开

### Q3: adb 授权能撤销吗？

**能**：
```
设置 → 开发者选项 → 撤销 USB 调试授权
```

或者在手机上：
```
adb shell
rm /data/misc/adb/adb_keys
```

---

## 📚 资料来源

- [Android ADB 官方文档](https://developer.android.com/studio/command-line/adb)
