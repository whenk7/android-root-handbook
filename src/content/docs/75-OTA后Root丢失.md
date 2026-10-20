---
title: "OTA 后 Root 丢失"
---

> OTA 升级后 root 没了？这篇讲怎么找回。

---

## 🎯 原因

**A/B 槽机制**：OTA 写到另一槽，你 patch 的 init_boot 在当前槽——重启切到新槽，root 没了。

```
T1: 你在 A 槽，A 槽的 init_boot 已被 patch（root 在）
   ↓
T2: OTA 下载新版本，写到 B 槽
   ↓
T3: 重启 → 切到 B 槽
   ↓
T4: B 槽的 init_boot 是原厂的 → root 没了
```

---

## 🛠️ 找回流程

### 第 1 步：确认状态

```bash
adb shell

# 看当前槽
getprop ro.boot.slot_suffix
# 输出 _a 或 _b（OTA 后的那个）

# 看版本
getprop ro.build.version.release

# 试试 root
su
# 应该提示 not found 或 Permission denied
```

### 第 2 步：提取当前槽的 init_boot

```bash
adb shell

# 当前槽（OTA 后的新槽）
SLOT=$(getprop ro.boot.slot_suffix)

# dump init_boot（这是新版本的，但是原厂的）
# 注意：此时没 root，要用 dd 直接读
# 但 dd 默认要 root...
# 解决：用 fastboot
```

**如果没 root，走 fastboot 路线**：

```
1. 关机
2. 进 fastboot
3. fastboot flash init_boot <patch后的新版本init_boot.img>
4. fastboot reboot
```

### 第 3 步：拿到新版本 init_boot

**方法 A：从官方全量包提取（推荐）**

```
1. 下载你刚升级到的新版本的全量包
2. 解压，拿 init_boot.img
```

**方法 B：从当前槽 dump（要 root，已经没了）**

不可行。

**方法 C：从另一槽 dump（旧版本，不能用）**

```
# 这是旧版本，patch 了跟新版本不匹配
dd if=/dev/block/by-name/init_boot_<另一槽>
```

**不行**——必须用当前版本的。

### 第 4 步：patch

```bash
# 用 KSU Manager 的"选择并 patch"
# 或命令行 ksud
ksud boot-patch -i init_boot_new.img -o init_boot_new_patched.img
```

### 第 5 步：刷入

```bash
# 进 fastboot
adb reboot bootloader

# 看当前槽
fastboot getvar current-slot
# 输出：current-slot: a 或 b

# 刷到当前槽
fastboot flash init_boot init_boot_new_patched.img

# 或者明确指定槽
fastboot flash init_boot_a init_boot_new_patched.img
# 或
fastboot flash init_boot_b init_boot_new_patched.img

# 重启
fastboot reboot
```

---

## 🛡️ 预防

**下次 OTA**：

```
OTA 完成后，**先别重启**
   ↓
打开 KSU Manager
   ↓
点"安装到另一槽位"
   ↓
等完成
   ↓
再重启
```

详见 [52-保Root升级](/android-root-handbook/52-保root升级)。

---

## 🎯 经典疑问

### Q1: 我忘了点"安装到另一槽位"，重启了，还能救吗？

**能**——按上面流程找回。

### Q2: 我能不能切回旧槽？

**能**：
```bash
fastboot --set-active=a   # 或 b
fastboot reboot
```

**但**：你回到旧版本了，**OTA 更新丢了**。

### Q3: 我 OTA 之后 KSU Manager 还在吗？

**在**——Manager 是普通 App，跟 /data 一起，OTA 不动 /data。

但 Manager 没法用（没 root）。

### Q4: 我能不 root 直接用 fastboot 刷吗？

**能**——你已经解锁了 BL，bootloader 会放行。

---

## 📚 资料来源

- [Android A/B OTA](https://source.android.com/docs/core/ota/ab)
- [KernelSU 安装文档](https://github.com/tiann/KernelSU/blob/main/website/docs/guide/installation.md)
