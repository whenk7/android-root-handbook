---
title: "LSP 进阶技巧"
---

> 进阶：hook 构造函数、hook 系统服务、跨进程共享、多 App 作用域、内嵌 dex 加载。

---

## 🎬 hook 构造函数

### 标准用法

```java
XposedHelpers.findAndHookConstructor(
    "com.example.MyClass",
    lpparam.classLoader,
    String.class,                          // 构造参数类型
    int.class,
    new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) {
            // param.thisObject 是刚构造出来的对象
            XposedBridge.log("构造了一个 MyClass: " + param.thisObject);
        }
    }
);
```

### 应用场景

- 在对象创建时改它的字段
- 记录对象的创建
- 替换构造参数

---

## 🌐 hook 系统服务

把作用域选 "Android 系统"（包名 `android`），就能 hook 系统 framework。

### 例子：让某个 App 不被杀

```java
@Override
public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
    if (!lpparam.packageName.equals("android")) return;
    
    XposedHelpers.findAndHookMethod(
        "com.android.server.am.ActivityManagerService",
        lpparam.classLoader,
        "killBackgroundProcesses",
        String.class, int.class,
        new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                String packageName = (String) param.args[0];
                if ("com.my.protected.app".equals(packageName)) {
                    param.setResult(null);  // 不杀
                }
            }
        }
    );
}
```

### ⚠️ 危险

写错可能让系统不停 FC。新手慎入。

---

## 🌍 多 App 作用域

一个模块可以 hook 多个 App：

```java
@Override
public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
    switch (lpparam.packageName) {
        case "com.tencent.mm":
            hookWechat(lpparam);
            break;
        case "tv.danmaku.bili":
            hookBili(lpparam);
            break;
        case "com.tencent.mobileqq":
            hookQQ(lpparam);
            break;
    }
}

private void hookWechat(XC_LoadPackage.LoadPackageParam lpparam) {
    // ...
}

private void hookBili(XC_LoadPackage.LoadPackageParam lpparam) {
    // ...
}
```

**在 LSPosed Manager 里勾选所有目标 App**。

---

## 🔗 跨进程共享配置

### 问题

模块自己是个 App，有设置界面。但 hook 跑在**目标 App 的进程**里，怎么读模块的设置？

### 解法：XSharedPreferences

#### 模块端（写）

```java
// 在模块的 Activity 里
SharedPreferences prefs = context.getSharedPreferences("my_prefs", Context.MODE_PRIVATE);
prefs.edit()
    .putBoolean("feature_x_enabled", true)
    .putString("custom_text", "hello")
    .apply();
```

#### hook 端（读）

```java
XSharedPreferences pref = new XSharedPreferences("com.example.myxposed", "my_prefs");
pref.makeWorldReadable();
pref.reload();

boolean enabled = pref.getBoolean("feature_x_enabled", false);
String text = pref.getString("custom_text", "");
```

### 注意

**`pref.reload()` 每次都要调**——不会自动刷新。

---

## 🧬 加载外部 dex

### 场景

你想在 hook 里用一些"重型"逻辑，不想全塞进模块 APK。

### 解法：动态加载 dex

```java
// 在 hook 里
String dexPath = "/data/local/tmp/my_plugin.dex";

DexClassLoader loader = new DexClassLoader(
    dexPath,
    null,
    null,
    lpparam.classLoader
);

Class<?> pluginClass = loader.loadClass("com.example.MyPlugin");
Object plugin = pluginClass.newInstance();
// 调用 plugin 的方法
```

---

## 🎭 hook 内部类 / 匿名类

### 内部类

```java
// 类名：com.example.Outer$Inner
XposedHelpers.findAndHookMethod(
    "com.example.Outer$Inner",
    lpparam.classLoader,
    "method",
    new XC_MethodHook() { ... }
);
```

### 匿名类

匿名类名是 `Outer$1`、`Outer$2`...：

```java
// 列出所有以 com.example.Outer$ 开头的类
// 或者用 XposedBridge.hookAllConstructors 看哪个被调用了
```

---

## 🔬 hook 泛型方法

泛型擦除后，方法签名是 raw type：

```java
// 目标：List<T> get()
// 实际：List get()

XposedHelpers.findAndHookMethod(
    "com.example.Foo",
    lpparam.classLoader,
    "get",
    new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) {
            List<?> result = (List<?>) param.getResult();
            // ...
        }
    }
);
```

---

## 🛡️ 处理 hook 失败

### 完整 try-catch

```java
try {
    XposedHelpers.findAndHookMethod(
        "com.example.Foo",
        lpparam.classLoader,
        "doSomething",
        new XC_MethodHook() { ... }
    );
} catch (NoSuchMethodError e) {
    XposedBridge.log("MyModule: 方法不存在，可能 App 更新了");
} catch (ClassNotFoundError e) {
    XposedBridge.log("MyModule: 类不存在");
} catch (Throwable t) {
    XposedBridge.log("MyModule: hook 失败 " + t);
}
```

### 优雅降级

```java
Method method = null;
try {
    method = XposedHelpers.findMethodExact(
        "com.example.Foo",
        lpparam.classLoader,
        "doSomething_v1"
    );
} catch (NoSuchMethodError e) {
    // 试另一个版本
    try {
        method = XposedHelpers.findMethodExact(
            "com.example.Foo",
            lpparam.classLoader,
            "doSomething_v2"
        );
    } catch (NoSuchMethodError e2) {
        // 两个版本都没有
    }
}

if (method != null) {
    XposedBridge.hookMethod(method, new XC_MethodHook() { ... });
}
```

---

## 🎯 hook 系统属性

```java
// 读
String value = (String) XposedHelpers.callStaticMethod(
    XposedHelpers.findClass("android.os.SystemProperties", null),
    "get",
    "ro.build.fingerprint"
);

// 写（要 root）
XposedHelpers.callStaticMethod(
    XposedHelpers.findClass("android.os.SystemProperties", null),
    "set",
    "ro.my.prop",
    "value"
);
```

---

## 🎯 hook native 方法（不支持）

LSPosed 是 **ART hook**，**hook 不了 native 方法**（C/C++ 实现的 JNI 方法）。

要 hook native：
- 用 Frida
- 用 Dobby / InlineHook（在 Zygisk 模块里做）

---

## 🧪 调试技巧

### 1. 列出类的所有方法

```java
Class<?> clazz = XposedHelpers.findClass("com.example.Foo", lpparam.classLoader);
for (Method m : clazz.getDeclaredMethods()) {
    XposedBridge.log("Method: " + m);
}
for (Field f : clazz.getDeclaredFields()) {
    XposedBridge.log("Field: " + f);
}
```

### 2. 打印调用栈

```java
new XC_MethodHook() {
    @Override
    protected void beforeHookedMethod(MethodHookParam param) {
        XposedBridge.log(new Exception("trace"));  // 打堆栈
    }
};
```

### 3. 打印参数

```java
new XC_MethodHook() {
    @Override
    protected void beforeHookedMethod(MethodHookParam param) {
        StringBuilder sb = new StringBuilder("Args: ");
        for (Object arg : param.args) {
            sb.append(arg).append(", ");
        }
        XposedBridge.log(sb.toString());
    }
};
```

---

## 📚 资料来源

- [XposedBridge 源码](https://github.com/rovo89/XposedBridge)
- [LSPosed 仓库](https://github.com/LSPosed/LSPosed)
