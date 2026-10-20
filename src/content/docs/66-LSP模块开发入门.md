---
title: "LSP 模块开发入门"
---

> 想自己写 LSP 模块？这篇讲最小可用模块怎么搭。

---

## 🎯 LSP 模块是啥

**一个普通 APK**，里面打包了 Xposed API 的实现。LSPosed 加载它，在目标 App 进程里 hook Java 方法。

---

## 🏗️ 最小可用模块

### 第 1 步：创建 Android 项目

用 Android Studio 新建 **Empty Activity** 项目。

### 第 2 步：加依赖

`app/build.gradle`:

```groovy
dependencies {
    compileOnly 'de.robv.android.xposed:api:82'
    compileOnly 'de.robv.android.xposed:api:82:sources'  // 可选，看源码方便
}
```

### 第 3 步：AndroidManifest.xml

```xml
<application ...>
    <!-- 告诉 LSPosed 这是模块 -->
    <meta-data
        android:name="xposedmodule"
        android:value="true"/>
    
    <!-- 模块描述 -->
    <meta-data
        android:name="xposeddescription"
        android:value="我的第一个 Xposed 模块"/>
    
    <!-- 兼容的最小 Xposed 版本 -->
    <meta-data
        android:name="xposedminversion"
        android:value="93"/>
    
    <!-- 作用域（可选，但推荐） -->
    <meta-data
        android:name="xposedscope"
        android:resource="@array/xposed_scope"/>
</application>
```

### 第 4 步：作用域（可选）

`res/values/arrays.xml`:

```xml
<resources>
    <string-array name="xposed_scope">
        <item>com.tencent.mm</item>     <!-- 微信 -->
        <item>tv.danmaku.bili</item>    <!-- B 站 -->
    </string-array>
</resources>
```

### 第 5 步：xposed_init（入口声明）

`app/src/main/assets/xposed_init`（**纯文本，每行一个入口类**）：

```
com.example.myxposed.HookEntry
```

### 第 6 步：入口类

`HookEntry.java`:

```java
package com.example.myxposed;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookEntry implements IXposedHookLoadPackage {
    
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        // 只处理目标 App
        if (!lpparam.packageName.equals("com.tencent.mm")) return;
        
        XposedBridge.log("MyModule: 微信进程加载了，pid=" + android.os.Process.myPid());
    }
}
```

---

## 🚀 编译 + 安装

### 编译

```bash
./gradlew assembleDebug
```

输出：`app/build/outputs/apk/debug/app-debug.apk`

### 安装

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 启用

1. 打开 LSPosed Manager
2. 模块页 → 找到你的模块 → 启用
3. 点模块 → **勾选作用域**（微信）
4. **强行停止微信**（设置 → 应用 → 微信 → 强行停止）
5. 重新打开微信

### 看日志

```
LSPosed Manager → 日志
```

应该看到：`MyModule: 微信进程加载了，pid=xxx`

---

## 🧬 完整目录结构

```
my_lsp_module/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── assets/
│   │   │   └── xposed_init
│   │   ├── java/com/example/myxposed/
│   │   │   ├── HookEntry.java
│   │   │   └── MainActivity.java     ← 模块自己的 UI（可选）
│   │   └── res/
│   │       └── values/
│   │           └── arrays.xml
│   └── build.gradle
├── build.gradle
└── settings.gradle
```

---

## 🎯 三种入口接口

LSP 模块可以实现多个入口接口：

### IXposedHookLoadPackage（最常用，95%）

**时机**：每个 App 加载时调用一次。

```java
public class HookEntry implements IXposedHookLoadPackage {
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals("target.app")) return;
        // hook 这个 App
    }
}
```

### IXposedHookZygoteInit（Zygote 启动时）

**时机**：Zygote 启动时（每个 App fork 之前）。

```java
public class HookEntry implements IXposedHookZygoteInit {
    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        // 在 Zygote 阶段做点事
    }
}
```

**用得少**——大部分场景用 `IXposedHookLoadPackage` 就够。

### IXposedHookInitPackageResources（资源初始化）

**时机**：App 的资源加载时。

```java
public class HookEntry implements IXposedHookInitPackageResources {
    @Override
    public void handleInitPackageResources(XC_InitPackageResources.InitPackageResourcesParam resparam) throws Throwable {
        // hook 资源加载
    }
}
```

---

## 🛠️ 常用 API 速查

### 找类

```java
Class<?> clazz = XposedHelpers.findClass("com.example.Foo", lpparam.classLoader);
```

### hook 方法

```java
XposedHelpers.findAndHookMethod(
    "com.example.Foo",          // 类名
    lpparam.classLoader,         // ClassLoader
    "doSomething",               // 方法名
    String.class,                // 参数类型 1
    int.class,                   // 参数类型 2
    new XC_MethodHook() {        // 最后一个是回调
        @Override
        protected void beforeHookedMethod(MethodHookParam param) {
            // 在原方法跑之前
        }
        @Override
        protected void afterHookedMethod(MethodHookParam param) {
            // 在原方法跑之后
        }
    }
);
```

### hook 构造函数

```java
XposedHelpers.findAndHookConstructor(
    "com.example.Foo",
    lpparam.classLoader,
    String.class,                // 构造参数类型
    new XC_MethodHook() { ... }
);
```

### 读写字段

```java
// 读
Object value = XposedHelpers.getObjectField(obj, "mCount");
int count = XposedHelpers.getIntField(obj, "mCount");

// 写
XposedHelpers.setObjectField(obj, "mCount", 100);
```

### 调方法

```java
// 实例方法
Object result = XposedHelpers.callMethod(obj, "doSomething", arg1, arg2);

// 静态方法
Object result = XposedHelpers.callStaticMethod(clazz, "getInstance");
```

### 打日志

```java
XposedBridge.log("MyModule: 消息");
XposedBridge.log(throwable);  // 打堆栈
```

---

## ⚠️ 常见错误

### 错误 1：NoSuchMethodError

**原因**：方法名写错、参数类型不对。

**排查**：

```java
// 列出类的所有方法
Class<?> clazz = XposedHelpers.findClass("com.example.Foo", lpparam.classLoader);
for (Method m : clazz.getDeclaredMethods()) {
    XposedBridge.log("Method: " + m);
}
```

### 错误 2：ClassNotFoundError

**原因**：类名写错，或者用了 `null` ClassLoader。

**正确**：用 `lpparam.classLoader`：

```java
// 错
Class<?> clazz = XposedHelpers.findClass("com.example.Foo", null);

// 对
Class<?> clazz = XposedHelpers.findClass("com.example.Foo", lpparam.classLoader);
```

### 错误 3：模块不生效

**排查**：
1. LSPosed Manager 里启用了吗？
2. 目标 App 加进作用域了吗？
3. 强停并重启目标 App 了吗？
4. 看 LSPosed 日志有没有报错？

### 错误 4：hook 到了但没生效

**原因**：方法被混淆了，类名/方法名变了。

**解法**：用 JADX 反编译目标 App，看真实类名。

---

## 🧪 调试

### 打印日志

```java
XposedBridge.log("MyModule: 在这里");
```

### 看日志

```
LSPosed Manager → 日志页面
```

或命令行：

```bash
adb logcat | grep -E "LSPosed|XposedBridge"
```

### 打印调用栈

```java
XposedBridge.log(new Exception("trace"));
```

---

## 📚 资料来源

- [XposedBridge API](https://api.xposed.info/)
- [LSPosed 仓库](https://github.com/LSPosed/LSPosed)
- [XposedBridge 源码](https://github.com/rovo89/XposedBridge)
