---
title: "LSPatch 无 Root 方案"
---

> 不想 root 也想用 Xposed 模块？LSPatch 可以——它把 hook 代码直接打进 APK。

---

## 🎯 是什么

**LSPatch** 是 LSPosed 团队做的"无 root 版 LSPosed"。

**原理**：把 LSPosed 框架代码 + 你选的模块**直接打进目标 APK**，重新打包 + 签名，装上就能用。

```
原 APK
  ↓ LSPatch 处理
塞入 LSPosed 代码 + 你选的模块
  ↓ 重新打包 + 签名
新 APK（装到手机直接用，不需要 root）
```

---

## 🆚 跟 LSPosed 的对比

| 维度 | LSPosed | LSPatch |
|---|---|---|
| 需要 root | 是 | **否** |
| 需要 Magisk/KSU | 是 | 否 |
| 改 /system | 否 | 否 |
| 修改 APK | 否 | **是** |
| 影响范围 | 进程级 | 单 APK 级 |
| 模块更新 | 装上即用 | 要重新打包 |
| 签名 | 不变 | **变了** |
| 过签名检测 | 容易 | 难 |

---

## 🛠️ 使用流程

### 方式 1：命令行（推荐）

```bash
# 下载 lspatch.jar
# 从 https://github.com/LSPosed/LSPatch/releases

# 基本用法
java -jar lspatch.jar <目标APK> -m <模块APK>

# 例：把模块 patch 进微信
java -jar lspatch.jar wechat.apk -m mymodule.apk

# 输出：wechat-lspatched.apk
```

### 方式 2：Manager App

LSPatch 有 Manager APK（不是 LSPosed Manager）：

1. 装 LSPatch Manager
2. 选目标 APK（可以是已装的 App 或本地文件）
3. 选模块
4. 点 Patch
5. 装 patch 后的 APK

---

## ⚠️ 注意事项

### 签名变了

LSPatch 重新打包后**签名变了**。这带来几个问题：

1. **覆盖安装失败**：要先卸载原版
2. **数据丢失**：卸载会清数据（除非用 `adb install -r -d`）
3. **签名校验**：部分 App 会检测自己签名，发现变了就闪退

### 部分 App 会检测

现代 App 越来越多检测：
- 检测自己是否被重打包
- 检测 dex 是否被改
- 检测签名校验

LSPatch 不一定能过这些检测。

### 模块更新麻烦

每次模块更新都要：
1. 重新 patch APK
2. 卸载旧版
3. 装新版
4. （可能丢数据）

---

## 🎯 适合场景

### 适合

- **不想 root 手机**
- 只需要 hook **一两个** App
- 临时测试一个模块
- 给别人发"定制版" App（谨慎，涉及版权）

### 不适合

- 长期用（更新太麻烦）
- 需要 hook 很多 App
- 目标 App 有强签名校验

---

## 🧬 原理细节

LSPatch 做的事：

1. **反编译 APK**（用 Apkzlib）
2. **修改 DEX**：把 LSPosed 初始化代码塞进 Application 类
3. **塞入模块 DEX**
4. **塞入 LSPosed 框架 .so**
5. **重新打包 + 签名**

启动时：

```
 patched APK 启动
   ↓
 Application.onCreate 被调用
   ↓
 LSPosed 初始化代码先跑（LSPatch 塞的）
   ↓
 加载塞入的模块
   ↓
 跑模块的 hook
   ↓
 继续原来的 Application.onCreate
```

---

## 🧪 亲手试一次

```bash
# 1. 下载 LSPatch
wget https://github.com/LSPosed/LSPatch/releases/latest/download/lspatch.jar

# 2. 准备一个目标 APK（比如自己写的测试 App）
# 3. 准备一个 LSP 模块 APK

# 4. Patch
java -jar lspatch.jar target.apk -m module.apk

# 5. 装上
adb install target-lspatched.apk
```

---

## 🎯 经典疑问

### Q1: LSPatch 后 App 会变大多少？

**会大不少**——要塞入 LSPosed 框架（几 MB）+ 你的模块。

### Q2: 能 patch 系统 App 吗？

**理论可以**，但系统 App 有签名绑定，patch 后**装不上**（签名跟系统登记的不一致）。

要 patch 系统 App 还是得 root + LSPosed。

### Q3: 能不能检测出 APK 被 LSPatch 了？

**能**——而且很容易：
- 看是否有 LSPosed 的特征字符串
- 看 dex 数量（patch 后多了模块 dex）
- 看 .so 列表

### Q4: LSPatch 比 root + LSPosed 好在哪？

**唯一好处**：不需要 root。

其他维度（功能、稳定性、隐蔽性）都是 root + LSPosed 更好。

---

## 📚 资料来源

- [LSPatch 仓库](https://github.com/LSPosed/LSPatch)
- [LSPatch README](https://github.com/LSPosed/LSPatch/blob/master/index.md)
