---
title: "Hide My Applist 藏应用"
---

> Hide My Applist (HMA) 是 LSP 模块，专门对付"列出已安装应用"这种检测。

---

## 🎯 是什么

**HMA** 是一个 LSP 模块，hook `PackageManager` 的所有查询 API，让目标 App 看到的"应用列表"是你过滤后的版本。

**仓库**：`Dr-TSNG/Hide-My-Applist`

---

## 🔍 为啥需要它

### App 的"应用列表探测"

```java
// App 想知道你装了啥
PackageManager pm = context.getPackageManager();

// 方式 1：列出所有应用
List<ApplicationInfo> apps = pm.getInstalledApplications(0);

// 方式 2：查询特定包名
try {
    pm.getPackageInfo("me.weishu.kernelsu", 0);
    // 没抛异常 → 装了 KSU
} catch (NameNotFoundException e) {
    // 没装
}

// 方式 3：Intent 查询
Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://..."));
List<ResolveInfo> handlers = pm.queryIntentActivities(intent, 0);
```

**HMA 拦截这些 API，返回过滤后的结果**。

---

## 🛡️ Android 11 的"应用可见性"限制

### 系统自带的限制

Android 11 引入了 `QUERY_ALL_PACKAGES` 权限：

```xml
<uses-permission android:name="android.permission.QUERY_ALL_PACKAGES"/>
```

App 不声明这个权限，**默认只能看到部分应用**。

### 但有空子

1. **声明权限很容易**——这是"普通权限"，装上就授予，不弹窗
2. **文件存在性探测**：
   ```java
   new File("/data/data/com.tencent.mm").exists()
   ```
   不需要权限就能探测特定包名

3. **`/proc/<pid>/cmdline`**：遍历进程看哪些包名在跑

**所以 HMA 是必需的**——它在更底层拦截 API。

---

## 🛠️ 使用

### 安装

1. 下载 HMA APK（GitHub Releases）
2. 装上
3. 在 LSPosed Manager 里启用 HMA 模块
4. 重启

### 配置

打开 HMA App → 配置：

#### 第 1 步：建模板

**模板 = 一组"要藏的 App 列表"**

新建模板 → 命名（比如"藏 root 工具"）→ 选要藏的 App：

```
要藏的 App（黑名单模板）：
- me.weishu.kernelsu       ← KSU Manager
- com.topjohnwu.magisk     ← Magisk
- org.lsposed.manager      ← LSPosed Manager
- io.github.vvb2060.magiskhide
- moe.shizuku.privileged.api
- ...
```

#### 第 2 步：应用模板

**模板建好后，选择"作用于哪些 App"**：

```
把模板应用到：
- com.icbc         ← 工行
- com.cmbchina     ← 招行
- ...
```

#### 第 3 步：选拦截方法

HMA 可以拦截这些 API：
- `getInstalledApplications`（列出所有应用）
- `getInstalledPackages`
- `getPackageInfo`
- `getApplicationInfo`
- `queryIntentActivities`
- ...

**默认全选**就行。

---

## 🎯 白名单 vs 黑名单

### 黑名单模式（推荐新手）

```
默认行为：让目标 App 看到所有应用
模板行为：让目标 App 看不到模板里列的应用
```

**优点**：除了 root 工具，其他应用正常显示  
**缺点**：你要把所有可疑 App 都加进模板

### 白名单模式

```
默认行为：让目标 App 看不到任何应用
模板行为：只让目标 App 看到模板里列的应用
```

**优点**：极严格，银行 App 看到的应用列表几乎为空  
**缺点**：可能误伤——银行 App 需要"看到"某些系统 App 才能正常工作

**新手推荐黑名单**。

---

## ⚠️ 注意事项

### 1. 不是万能

HMA 只能拦 `PackageManager` API。**不能拦**：
- 文件存在性探测（`new File(...).exists()`）
- `/proc/<pid>/cmdline` 遍历

要补这些，需要 ZygiskNext v1.3.0+ 在 Zygisk 层拦文件系统调用。

### 2. 模板要更新

你装了新的 root 工具，**要把它加进模板**——否则银行 App 还是能看到。

### 3. 部分 App 会"扫描包名"

有些 App 不用 PackageManager，直接试 `pm.getPackageInfo("已知包名", 0)` 看会不会抛异常。HMA 也能拦这个，但**要确保你拦截了 getPackageInfo**。

### 4. HMA 自己也得藏

银行 App 可能扫"HMA 装没装"。所以：
- 把 HMA 自己加进黑名单模板
- 或者用 ZygiskNext + HMA 配合

---

## 🧪 验证 HMA 是否工作

### 方法 1：用 HMA 自带的"测试"

HMA App → 测试功能 → 选目标 App → 看它能不能看到被藏的 App。

### 方法 2：手动测试

```bash
# 看 HMA 是否在目标 App 进程里
adb shell
su -c "cat /proc/$(pidof com.icbc)/maps | grep -i hma"
```

### 方法 3：实测

直接打开银行 App，看会不会闪退。

---

## 🎯 经典疑问

### Q1: 我把所有 App 都加白名单，银行 App 看到空列表会怎样？

**可能闪退**——银行 App 期望看到至少系统 App（com.android.settings 等）。

**正确做法**：黑名单模式 + 只藏 root 工具。

### Q2: HMA 能藏 App 的"运行状态"吗？

**不能**——HMA 只拦 PackageManager 查询。如果银行 App 看 /proc/<pid>/ 找运行中的进程，HMA 拦不住（那是 ZygiskNext 的事）。

### Q3: HMA 会影响正常 App 的功能吗？

**对作用域 App 会**——比如把应用商店加进作用域，它可能看不到其他 App。

**对策**：谨慎选作用域，只加银行/支付等必要 App。

### Q4: HMA 配置复杂吗？

**第一次麻烦**（建模板 + 应用到 App），之后基本不用动。

---

## 🎁 推荐模板配置

### 模板 1：藏 root 工具

```
me.weishu.kernelsu
com.topjohnwu.magisk
org.lsposed.manager
io.github.vvb2060.magiskhide
moe.shizuku.privileged.api
com.omarea.vtools          ← Scene
com.yuk.fkmm               ← FKMM
com.rifsxd.ksunext         ← KernelSU-Next Manager
com.sukisu.ultra           ← SukiSU Manager
```

### 模板 2：藏 hook 工具

```
de.robv.android.xposed.installer  ← 老 Xposed
org.lsposed.manager
com.solohsu.android.edxp.manager  ← EdXposed
```

### 模板 3：藏调试工具

```
com.frida                       ← Frida
com.npx.traceback               ← 调试
bin.mt.plus                     ← MT 管理器
```

---

## 📚 资料来源

- [Hide My Applist 仓库](https://github.com/Dr-TSNG/Hide-My-Applist)
- [Android 11 应用可见性](https://developer.android.com/training/package-visibility)
