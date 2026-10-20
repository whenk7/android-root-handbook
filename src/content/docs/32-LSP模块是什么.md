---
title: "LSP 模块是什么"
---

> LSP 模块是 APK，被 LSPosed 加载，在目标 App 进程里 hook Java 方法。这篇讲清楚它的形态和工作方式。

---

## 🎯 一句话定义

> **LSP 模块是一个普通 APK**，里面打包了 Xposed API 的实现，被 LSPosed 加载后，在目标 App 进程里 hook Java/Kotlin 方法。

---

## 📦 模块的物理形式

### 安装位置

```
/data/app/<包名>-xxxx/base.apk     ← 跟普通 App 一样
```

LSP 模块就是普通 APK，**装在 /data/app/**，走 Android 包管理器。

### 关键区别（跟普通 App）

| 维度 | 普通 App | LSP 模块 |
|---|---|---|
| 入口 | `Activity` | `IXposedHookLoadPackage` |
| 启动方式 | 用户点图标 | LSPosed 加载 |
| 是否运行在自己进程 | 是 | **否**——跑在目标 App 进程里 |
| 能否独立使用 | 能 | 不能 |

---

## 🏗️ 模块结构（开发者视角）

```
my_lsp_module/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml    ← 声明这是 Xposed 模块
│   │   ├── assets/
│   │   │   └── xposed_init        ← 声明入口类
│   │   ├── java/com/example/
│   │   │   └── HookEntry.java     ← 入口类
│   │   └── res/
│   └── build.gradle               ← 依赖 XposedBridge API
└── ...
```

### AndroidManifest.xml 关键 meta-data

```xml
<application ...>
    <!-- 告诉 LSPosed 这是模块 -->
    <meta-data
        android:name="xposedmodule"
        android:value="true"/>
    
    <!-- 模块描述 -->
    <meta-data
        android:name="xposeddescription"
        android:value="我的模块"/>
    
    <!-- 兼容的最小 Xposed 版本 -->
    <meta-data
        android:name="xposedminversion"
        android:value="93"/>
    
    <!-- 作用域（新版推荐） -->
    <meta-data
        android:name="xposedscope"
        android:resource="@array/xposed_scope"/>
</application>
```

### assets/xposed_init（入口声明）

**纯文本，每行一个入口类**：

```
com.example.myxposed.HookEntry
```

### 入口类

```java
public class HookEntry implements IXposedHookLoadPackage {
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        // hook 逻辑
    }
}
```

---

## 🎬 模块的生命周期

### 安装

```
1. 编译出 APK
2. 装上手机（跟普通 App 一样）
3. 打开 LSPosed Manager
4. 模块页 → 找到你的模块 → 启用
5. 点模块 → 勾选作用域（要选目标 App）
6. 强行停止目标 App（设置 → 应用 → 强停）
7. 重新打开目标 App → 模块生效
```

### 工作

```
目标 App 启动（被 AMS fork 出来）
   ↓
Zygisk 注入完成
   ↓
LSPosed 在新进程里初始化
   ↓
LSPosed 扫描所有启用的模块
   ↓
对每个模块，检查它的作用域是否包含当前 App
   ↓
包含 → 调模块的 handleLoadPackage
   ↓
模块在 handleLoadPackage 里 hook 方法
   ↓
目标 App 继续启动，被 hook 的方法已经被替换
```

### 卸载

```
1. LSPosed Manager 里禁用模块
2. 在系统设置里卸载 APK（跟普通 App 一样）
3. 强停目标 App → 模块不再生效
```

---

## 🎯 模块能干什么

### 1. hook Java 方法

```java
XposedHelpers.findAndHookMethod(
    "com.example.Foo",
    lpparam.classLoader,
    "doSomething",
    new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) {
            // 改参数
            param.args[0] = "modified";
        }
        @Override
        protected void afterHookedMethod(MethodHookParam param) {
            // 改返回值
            param.setResult(null);
        }
    }
);
```

### 2. 替换方法

```java
XposedHelpers.findAndHookMethod(
    "com.example.Foo",
    lpparam.classLoader,
    "isVip",
    new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) {
            return true;  // 完全替换
        }
    }
);
```

### 3. hook 构造函数

```java
XposedHelpers.findAndHookConstructor(
    "com.example.Foo",
    lpparam.classLoader,
    String.class,
    new XC_MethodHook() { ... }
);
```

### 4. 读写字段

```java
XposedHelpers.setObjectField(obj, "mCount", 100);
Object value = XposedHelpers.getObjectField(obj, "mCount");
```

### 5. 调方法

```java
Object result = XposedHelpers.callMethod(obj, "doSomething", arg1, arg2);
```

### 6. hook 系统服务

```java
// hook ActivityManagerService（系统 framework）
XposedHelpers.findAndHookMethod(
    "com.android.server.am.ActivityManagerService",
    lpparam.classLoader,
    "killBackgroundProcesses",
    ...
);
```

---

## ❌ 模块不能干什么

| 不能干 | 原因 |
|---|---|
| 改 /system 文件 | LSP 不动文件系统，那是 KSU 的事 |
| 改 SELinux 规则 | 同上 |
| 加载内核模块 | 同上 |
| hook 非 Java 方法 | LSPosed 是 ART hook，hook 不了纯 native |
| 跨进程 hook | 模块只在目标 App 进程里 |

**要干这些**，用 KSU 模块。

---

## 🆚 LSP 模块 vs KSU 模块

| 维度 | LSP 模块 | KSU 模块 |
|---|---|---|
| 格式 | APK | ZIP |
| 开发语言 | Java / Kotlin | Shell + 可选二进制 |
| 工作位置 | 目标 App 进程内 | 系统全局 |
| 改文件系统 | ❌ | ✅ |
| Hook Java 方法 | ✅ | ❌ |
| 改 SELinux | ❌ | ✅ |
| 加载 .ko | ❌ | ✅ |
| 作用域 | 按 App | 全局 |

---

## 🎯 经典疑问

### Q1: LSP 模块也是个 App，那它能有自己的界面吗？

**可以**——LSP 模块可以有 Activity、Fragment 等 UI，让用户配置。

**例子**：BiliRoaming 有设置界面，让用户选"去哪些广告"。

### Q2: LSP 模块和 hook 代码在同一个 APK 里吗？

**是的**——LSP 模块 APK 同时包含：
- 模块自己的 UI/逻辑
- 给目标 App 注入的 hook 代码

### Q3: 我能用 LSP 模块改系统服务吗？

**可以**——把作用域选 "Android 系统"（即 `android` 包名），就能 hook 系统 framework。

**危险**：写错可能让系统不停 FC。新手慎入。

### Q4: LSP 模块有 GUI 还能被 LSPosed 加载吗？

**可以**——LSPosed 只关心你的 `xposed_init` 里声明的入口类，其他部分（UI、Activity）跟普通 App 一样。

---

## 🧪 亲手验证

```bash
adb shell

# 1. 看 LSP 模块装在哪
pm list packages | grep -i xposed
pm list packages | grep -i lsp

# 2. 看 LSPosed 管理的模块
# 打开 LSPosed Manager → 模块

# 3. 看 LSPosed 日志
# LSPosed Manager → 日志

# 4. 看 LSP 模块在目标 App 进程里
su -c "cat /proc/$(pidof com.tencent.mm)/maps | grep -i lsp"
```

---

## 📚 资料来源

- [LSPosed 仓库](https://github.com/LSPosed/LSPosed)
- [XposedBridge API](https://api.xposed.info/)
- [Xposed-Modules-Repo](https://github.com/Xposed-Modules-Repo)
