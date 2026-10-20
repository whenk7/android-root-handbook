---
title: "LSP 核心 API（XposedHelpers / XC_MethodHook）"
---

> 这篇把 LSP 模块最常用的 API 讲透——基于 XposedBridge 源码。

---

## 📚 核心类

| 类 | 干啥 |
|---|---|
| `IXposedHookLoadPackage` | 接口：App 加载时回调 |
| `XC_MethodHook` | 类：方法 hook 回调 |
| `XC_MethodReplacement` | 类：完全替换方法 |
| `XposedHelpers` | 工具类：findClass、findAndHookMethod 等 |
| `XposedBridge` | 核心：hookMethod、log 等 |
| `XSharedPreferences` | 跨进程读 SharedPreferences |

---

## 🔌 IXposedHookLoadPackage

### 源码

```java
public interface IXposedHookLoadPackage extends IXposedMod {
    /**
     * This method is called when an app is loaded. It's called very early, even before
     * {@link Application#onCreate} is called.
     * Modules can set up their app-specific hooks here.
     */
    void handleLoadPackage(LoadPackageParam lpparam) throws Throwable;
}
```

**关键**：`handleLoadPackage` 在 `Application.onCreate` **之前**调用。

### LoadPackageParam 的字段

```java
public class LoadPackageParam {
    public String packageName;          // 包名
    public String processName;          // 进程名
    public ClassLoader classLoader;     // 这个 App 的 ClassLoader
    public ApplicationInfo appInfo;     // ApplicationInfo
    public boolean isFirstApplication;  // 是不是这个包的第一个 App
}
```

### 经典用法

```java
@Override
public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
    // 过滤包名
    if (!lpparam.packageName.equals("com.tencent.mm")) return;
    
    // 过滤进程（一个 App 可能有多个进程）
    if (!lpparam.processName.equals("com.tencent.mm")) return;
    
    // 用 classLoader 找类
    Class<?> clazz = XposedHelpers.findClass("com.tencent.mm.ui.LauncherUI", lpparam.classLoader);
    
    // hook ...
}
```

---

## 🪝 XC_MethodHook

### 源码（精简）

```java
public abstract class XC_MethodHook extends XCallback {
    /**
     * 在原方法调用前执行
     */
    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {}

    /**
     * 在原方法调用后执行
     */
    protected void afterHookedMethod(MethodHookParam param) throws Throwable {}
    
    public static final class MethodHookParam {
        public Member method;          // 被 hook 的方法
        public Object thisObject;      // this 引用（静态方法是 null）
        public Object[] args;          // 参数数组，可改
        public Object getResult();     // 拿返回值
        public void setResult(Object); // 设置返回值（会跳过原方法）
        public void setThrowable(Throwable); // 让方法抛异常
    }
}
```

### 用法

```java
XposedHelpers.findAndHookMethod(
    "com.example.Foo",
    lpparam.classLoader,
    "sendMessage",
    String.class,
    new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) {
            // 在原方法跑之前
            // 改参数
            param.args[0] = "替换后的消息";
            
            // 或者：完全跳过原方法
            // param.setResult(null);
        }
        
        @Override
        protected void afterHookedMethod(MethodHookParam param) {
            // 在原方法跑之后
            // 改返回值
            Object original = param.getResult();
            param.setResult(original + " (hooked)");
        }
    }
);
```

---

## 🔄 XC_MethodReplacement

**完全替换**原方法：

```java
XposedHelpers.findAndHookMethod(
    "com.example.Foo",
    lpparam.classLoader,
    "isVip",
    new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            return true;  // 直接返回，原方法不执行
        }
    }
);
```

---

## 🛠️ XposedHelpers（最常用）

### 找类

```java
// 找类
Class<?> clazz = XposedHelpers.findClass("com.example.Foo", lpparam.classLoader);

// 找类（不抛异常版本）
Class<?> clazz = XposedHelpers.findClassIfExists("com.example.Foo", lpparam.classLoader);
```

### 找字段

```java
// 找字段
Field f = XposedHelpers.findField(clazz, "mCounter");

// 不抛异常版本
Field f = XposedHelpers.findFieldIfExists(clazz, "mCounter");
```

### 读写字段

```java
// 读
Object value = XposedHelpers.getObjectField(obj, "mCounter");
int count = XposedHelpers.getIntField(obj, "mCount");
boolean flag = XposedHelpers.getBooleanField(obj, "mFlag");

// 写
XposedHelpers.setObjectField(obj, "mCounter", 100);
XposedHelpers.setIntField(obj, "mCount", 42);
XposedHelpers.setBooleanField(obj, "mFlag", true);

// 静态字段
Object val = XposedHelpers.getStaticObjectField(clazz, "INSTANCE");
XposedHelpers.setStaticObjectField(clazz, "INSTANCE", newValue);
```

### 调方法

```java
// 实例方法
Object result = XposedHelpers.callMethod(obj, "doSomething", arg1, arg2);

// 静态方法
Object result = XposedHelpers.callStaticMethod(clazz, "getInstance");

// 带参数类型（解决重载歧义）
Object result = XposedHelpers.callMethod(obj, "foo", 
    new Class<?>[]{String.class, int.class}, 
    "hello", 42);
```

### hook 方法

```java
// 标准
XposedHelpers.findAndHookMethod(
    Class<?> clazz,
    String methodName,
    Object... parameterTypesAndCallback  // 参数类型... + 最后一个是回调
);

// 带类名
XposedHelpers.findAndHookMethod(
    String className,
    ClassLoader classLoader,
    String methodName,
    Object... parameterTypesAndCallback
);

// hook 构造函数
XposedHelpers.findAndHookConstructor(
    Class<?> clazz,
    Object... parameterTypesAndCallback
);
```

---

## 🌉 XposedBridge

### hook 方法

```java
// 直接 hook（XposedHelpers.findAndHookMethod 内部就是调它）
XposedBridge.hookMethod(Method method, XC_MethodHook callback);

// hook 所有重载
XposedBridge.hookAllMethods(Class<?> clazz, String methodName, XC_MethodHook callback);

// hook 所有构造函数
XposedBridge.hookAllConstructors(Class<?> clazz, XC_MethodHook callback);
```

### 打日志

```java
XposedBridge.log("message");
XposedBridge.log(throwable);
```

---

## 🍩 XSharedPreferences（跨进程读配置）

模块自己是个 App，可以存 SharedPreferences。hook 别的 App 时读这些配置：

### 模块自己写配置

```java
// 在模块的 Activity 里
SharedPreferences prefs = context.getSharedPreferences("my_prefs", Context.MODE_PRIVATE);
prefs.edit().putBoolean("enable_feature_x", true).apply();
```

### hook 里读

```java
XSharedPreferences pref = new XSharedPreferences("com.example.myxposed", "my_prefs");
pref.makeWorldReadable();  // 让其他 App 能读
pref.reload();
boolean enabled = pref.getBoolean("enable_feature_x", false);
```

**注意**：`Context.MODE_WORLD_READABLE` 已被废弃，用 `XSharedPreferences` 是正路。

---

## 🎬 完整实战：去广告 hook

```java
public class AdBlockHook implements IXposedHookLoadPackage {
    
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals("tv.danmaku.bili")) return;
        
        // 1. hook 广告 SDK 的初始化方法
        XposedHelpers.findAndHookMethod(
            "com.bilibili.ad.common.AdsManager",
            lpparam.classLoader,
            "init",
            android.content.Context.class,
            new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    param.setResult(null);
                }
            }
        );
        
        // 2. hook 某个返回广告的方法
        XposedHelpers.findAndHookMethod(
            "com.bilibili.ad.splash.SplashAdRepo",
            lpparam.classLoader,
            "getSplashAd",
            new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    param.setResult(null);
                }
            }
        );
        
        // 3. hook 某个"是否显示广告"的 boolean 方法
        XposedHelpers.findAndHookMethod(
            "com.bilibili.pegasus.channel.GuideManager",
            lpparam.classLoader,
            "shouldShowAd",
            new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) {
                    return false;
                }
            }
        );
    }
}
```

---

## 🎯 经典疑问

### Q1: beforeHookedMethod 和 afterHookedMethod 区别？

- **before**：在原方法跑之前——能改参数、能 setResult 跳过原方法
- **after**：在原方法跑之后——能改返回值

### Q2: 我能不能在 hook 里调原方法？

**能**：

```java
new XC_MethodHook() {
    @Override
    protected void afterHookedMethod(MethodHookParam param) {
        // 原方法已经跑过了，param.getResult() 是它的返回值
    }
}
```

要"手动调原方法"用 `XposedBridge.invokeOriginalMethod`：

```java
Object original = XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args);
```

### Q3: 一个方法能被多个模块 hook 吗？

**能**——LSPosed 按优先级串起来执行。

```java
new XC_MethodHook(PRIORITY_HIGHEST) { ... }   // 优先级最高
```

---

## 📚 资料来源

- [XposedBridge 源码](https://github.com/rovo89/XposedBridge)
- [Xposed API 文档](https://api.xposed.info/)
