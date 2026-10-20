---
title: "App 闪退排查（银行/支付类）"
---

> 银行 App 闪退是最常见的问题。这篇给完整排查流程。

---

## 🔍 排查清单

```
□ 1. SELinux 是 Enforcing 吗？
□ 2. susfs 装了吗？
□ 3. Shamiko 装了吗？
□ 4. Hide My Applist 装了吗？配了吗？
□ 5. DenyList 加了吗？Enforce 关了吗？
□ 6. 这个 App 在 LSPosed 作用域里吗？（应该不在）
□ 7. Play Integrity 过了吗？
□ 8. App 是最新版吗？
```

---

## 1️⃣ SELinux 必须 Enforcing

```bash
adb shell getenforce
# 必须是 Enforcing
```

**如果是 Permissive**：
```bash
adb shell su -c "setenforce 1"
```

**长期**：找谁改了它（可能是某个模块）。

---

## 2️⃣ susfs 装了吗

```bash
adb shell
su

# 看 susfs 是否工作
cat /proc/mounts | grep -i ksu
# 应该返回空

cat /proc/kallsyms | grep -i ksu | head -3
# 应该返回空
```

**没装**：
- SukiSU 用户：内核已集成
- 官方 KSU 用户：装 susfs4ksu 模块

---

## 3️⃣ Shamiko 装了吗

```bash
adb shell
su

# 看 Shamiko 模块
ls /data/adb/modules/ | grep -i shamiko

# 看 Shamiko 是否在银行 App 进程里
cat /proc/$(pidof com.icbc)/maps | grep -i shamiko
```

**没装**：
1. 下载 Shamiko ZIP（LSPosed 官方渠道）
2. KSU Manager 装
3. 重启

---

## 4️⃣ DenyList + Enforce 配置

### 加进 DenyList

```
ZygiskNext 设置 → DenyList → 添加 com.icbc
```

或：
```bash
adb shell su -c "echo 'com.icbc' >> /data/adb/zygisksu/denylist.txt"
```

### 关闭 Enforce DenyList（关键）

```
ZygiskNext 设置 → 关闭 "Enforce DenyList"
```

**为啥**：开启 Enforce 会阻止 ZygiskNext 注入。详见 [24-卸载模块-KSU与ZN排除列表](/android-root-handbook/24-卸载模块-ksu与zn排除列表)。

---

## 5️⃣ Hide My Applist 配置

### 建模板

HMA → 模板 → 新建"藏 root 工具"：

```
要藏的 App：
- me.weishu.kernelsu
- com.topjohnwu.magisk
- org.lsposed.manager
- io.github.vvb2060.magiskhide
- moe.shizuku.privileged.api
- com.omarea.vtools
- ...
```

### 应用到目标 App

HMA → 应用管理 → com.icbc → 应用模板"藏 root 工具"。

---

## 6️⃣ LSPosed 作用域

**银行 App 不应该在任何 LSP 模块的作用域里**（除非这个模块是专门给银行 App 设计的）。

```
LSPosed Manager → 模块 → 每个模块都看一下
→ 确保 com.icbc 没被勾选
```

**例外**：HMA 本身要 hook 银行 App——HMA 的作用域要包含 com.icbc。

---

## 7️⃣ Play Integrity

```bash
# 用 Play Integrity API Checker App 测
```

**不通过**：
- 装 Play Integrity Fix
- 更新指纹

---

## 8️⃣ 完整排查脚本

```bash
#!/bin/bash
# diagnose_bank.sh - 排查银行 App 闪退

TARGET_PACKAGE=$1

if [ -z "$TARGET_PACKAGE" ]; then
    echo "用法: $0 <package_name>"
    echo "例: $0 com.icbc"
    exit 1
fi

echo "=== 排查 $TARGET_PACKAGE 闪退 ==="
echo ""

echo "1. SELinux 状态："
adb shell getenforce

echo ""
echo "2. susfs 是否工作（看 ksu 是否被藏）："
adb shell "cat /proc/mounts | grep -i ksu || echo ✓ 已隐藏"
adb shell "cat /proc/kallsyms | grep -i ksu | head -3 || echo ✓ 已隐藏"

echo ""
echo "3. 已装模块："
adb shell "su -c 'ls /data/adb/modules/'"

echo ""
echo "4. DenyList 内容："
adb shell "su -c 'cat /data/adb/zygisksu/denylist.txt 2>/dev/null'"

echo ""
echo "5. 目标 App 是否在运行："
adb shell "pidof $TARGET_PACKAGE"

echo ""
echo "6. 目标 App 进程的 maps（看有没有 lspd）："
PID=$(adb shell pidof $TARGET_PACKAGE | tr -d '\r')
if [ -n "$PID" ]; then
    adb shell "su -c 'cat /proc/$PID/maps | grep -i lsp'"
    adb shell "su -c 'cat /proc/$PID/maps | grep -i zygisk'"
fi

echo ""
echo "7. HMA 是否生效："
adb shell "pm list packages | grep hidemyapplist"
```

---

## 🎯 经典疑问

### Q1: 我都装了，为啥还闪退？

**可能**：
- App 用了**新的检测手段**
- 你的某个模块**留下了痕迹**
- 指纹过期了

**对策**：
1. 用 RootBeer Fresh 测哪一项不过
2. 看 logcat 里 App 闪退的原因
3. 更新所有工具到最新版

### Q2: 我能用"应用分身"绕过吗？

**不一定**——分身的环境跟主应用一样，检测一样能跑。

### Q3: 我用冰箱/小黑屋冻了银行 App，再解冻，它能跑吗？

**不能**——检测在 App 启动时跑，跟"冻结"无关。

### Q4: 银行 App 更新了，更严格了，咋办？

**只能等**：
- susfs 更新
- Shamiko 更新
- HMA 更新

或者**找替代**：
- 用网页版
- 用旧版 App（可能检测弱）

---

## 📚 资料来源

- [LSPosed 检测对抗讨论](https://github.com/LSPosed/LSPosed/issues)
- [Play Integrity API Checker](https://play.google.com/store/apps/details?id=gr.nikolasspyr.integritycheck)
