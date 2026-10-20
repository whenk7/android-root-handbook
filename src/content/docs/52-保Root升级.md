---
title: "保 Root 升级（OTA）"
---

> 系统弹"发现新版本"，点了升级会不会丢 root？这篇讲怎么"既升级又保 root"。

---

## 🎯 核心问题

**A/B 槽机制下，OTA 写到另一槽，重启后切到新槽——你 patch 的 init_boot 就丢了**。

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

## 🛠️ 三种保 root 的方法

### 方法 1：KSU Manager 的"安装到另一槽位"（推荐）

**时机**：OTA 完成后**重启前**

**操作**：

```
1. OTA 下载完成，系统提示重启
2. 不要立刻重启
3. 打开 KSU Manager
4. 找到"安装"或"维护"
5. 选"安装到未使用槽位"（或"安装到另一槽位"）
6. KSU 自动 patch 新槽的 init_boot
7. 完成后再重启
```

**原理**：

```
OTA 完成（B 槽已是新版本，init_boot 是原厂的）
   ↓
KSU Manager 读 B 槽的 init_boot
   ↓
现场做 patch
   ↓
把 patch 后的写回 B 槽
   ↓
重启 → B 槽启动 → root 还在
```

### 方法 2：手动 patch

**如果 KSU Manager 没找到"安装到另一槽位"**：

```bash
# 1. OTA 完成，但不重启
# 2. 看 OTA 写到哪个槽了
adb shell getprop ro.boot.slot_suffix
# 假设当前是 _a，OTA 写到 _b

# 3. 提取 _b 槽的 init_boot（已是新版本）
adb shell
su
dd if=/dev/block/by-name/init_boot_b of=/sdcard/init_boot_new.img

# 4. 拉到电脑
adb pull /sdcard/init_boot_new.img ./

# 5. 用 KSU Manager 的"选择并 patch"功能
#    或者命令行 ksud boot-patch
ksud boot-patch -i init_boot_new.img -o init_boot_new_patched.img

# 6. 推回手机
adb push init_boot_new_patched.img /sdcard/

# 7. 刷到 _b 槽
adb shell
su
dd if=/sdcard/init_boot_new_patched.img of=/dev/block/by-name/init_boot_b

# 8. 重启
reboot
```

### 方法 3：先卸 root 再升级（最稳但最麻烦）

```
1. 卸 KSU（恢复原厂 init_boot）
2. 重启（无 root 状态）
3. OTA 升级
4. 重启进新版本
5. 重新走 KSU 安装流程
```

**适用**：KSU Manager 出问题，或者大版本升级（Android 15 → 16）。

---

## ⚠️ 常见翻车场景

### 翻车 1：忘了点"安装到另一槽位"就重启了

**症状**：重启后 root 没了。

**救法**：

```
1. 不慌，数据都在
2. 提取当前槽的 init_boot（已是新版本，但是原厂的）
3. 用 KSU Manager 重新 patch
4. fastboot flash init_boot_X patched.img
5. 重启 → root 回来
```

### 翻车 2：OTA 中断了

**症状**：升级失败，可能进不了系统。

**救法**：

```
1. fastboot --set-active=<另一槽>    # 切回旧槽
2. fastboot reboot
3. 旧系统应该能起来（A/B 的回滚机制）
```

### 翻车 3：升级后模块不工作

**症状**：root 在，但某些模块失效。

**原因**：新版本可能改了 SELinux 策略、内核行为。

**救法**：

```
1. 更新模块到最新版本
2. 看模块的兼容性声明
3. 不行就先卸载，等模块作者适配
```

---

## 🛡️ 升级前的检查清单

```
升级前：
□ 备份当前 init_boot（已 patch 的）
□ 备份当前 boot.img（万一）
□ 记下当前 KSU/SukiSU 版本
□ 记下所有装了的模块
□ 准备好新版本的官方全量包下载链接（万一救砖）

OTA 完成后重启前：
□ 点 KSU Manager 的"安装到另一槽位"
□ 等完成
□ 再重启

升级后：
□ 确认 root 还在（跑 id）
□ 确认所有模块还在（ls /data/adb/modules/）
□ 确认银行 App 能开
```

---

## 🎯 完整备份脚本

```bash
#!/bin/bash
# backup_before_ota.sh

SLOT=$(adb shell getprop ro.boot.slot_suffix | tr -d '\r')
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="backup_${DATE}"

mkdir -p "$BACKUP_DIR"

echo "备份当前槽: $SLOT"

for part in boot init_boot vbmeta dtbo vendor_boot; do
    echo "备份 ${part}${SLOT}..."
    adb shell "su -c 'dd if=/dev/block/by-name/${part}${SLOT} of=/sdcard/${part}.img' 2>/dev/null"
    adb pull "/sdcard/${part}.img" "${BACKUP_DIR}/${part}${SLOT}.img"
    adb shell "rm /sdcard/${part}.img"
done

echo "备份 KSU 数据..."
adb shell "su -c 'tar czf /sdcard/ksu_backup.tar.gz -C /data/adb .'"
adb pull /sdcard/ksu_backup.tar.gz "${BACKUP_DIR}/"
adb shell "rm /sdcard/ksu_backup.tar.gz"

echo "完成。备份在 ${BACKUP_DIR}/"
ls -la "${BACKUP_DIR}/"
```

---

## 🎯 经典疑问

### Q1: OTA 包从哪下载？

**厂商官网**：
- 小米：MIUI 官网
- 一加：一加官网
- 三星：Frija 工具
- Pixel：developers.google.com

**第三方**：
- XiaomiROM
- 各种玩机论坛

### Q2: 我能跳过某些版本升级吗？

**增量 OTA 不行**——必须按顺序。

**全量包可以**——直接刷最新版全量包。

### Q3: 我用第三方 recovery（TWRP）怎么升级？

**A/B 机型通常没 TWRP**——因为 A/B 不需要 recovery 分区。

**只能用 fastboot** 或者官方 recovery。

---

## 📚 资料来源

- [Android A/B OTA 官方文档](https://source.android.com/docs/core/ota/ab)
- [KernelSU 安装文档](https://github.com/tiann/KernelSU/blob/main/website/docs/guide/installation.md)
