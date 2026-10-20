---
title: "KSU 还是 LSP 决策树"
---

> 你想做个功能，该用 KSU 模块还是 LSP 模块？这篇给你一张决策图。

---

## 🎯 核心区别（一句话）

- **KSU 改"环境"**：文件、内核、init、SELinux、属性 —— 改的是**手机本身**
- **LSP 改"行为"**：hook Java 方法 —— 改的是**App 的运行时**

---

## 🌳 决策树

```
要改的东西在哪一层？
│
├─ 1. 改文件系统 / 系统分区里的文件
│     （APK、so、xml、字体、hosts、build.prop）
│     → 用 KSU
│     例：删预装 App、换字体、改 hosts、刷 ViPER4Android 驱动
│
├─ 2. 改内核行为 / 加载内核模块 / 改 SELinux 策略 / 写 procfs/sysfs
│     → 用 KSU (SukiSU 的 KPM 也行)
│     例：susfs 隐藏 root、调度器调优、温控修改
│
├─ 3. 改 init 启动流程 / 注册系统服务 / 改系统属性
│     → 用 KSU
│     例：开机启动一个 daemon、改 ro.xxx 属性
│
├─ 4. 改某个 App 内部的 Java 方法行为（不改文件，只改运行时）
│     → 用 LSP
│     例：去广告、防撤回、解锁会员、绕过 SSL Pinning
│
├─ 5. 需要同时 hook 多个 App 的同一种方法
│     → 用 LSP（一个模块可以同时作用域多个 App）
│
├─ 6. 想在 App 启动前就拦截、改 Zygote 行为
│     → 用 LSP（Zygisk 本身就是注入 Zygote）
│
└─ 7. 既要改文件又要 hook 方法
      → 组合用（KSU + LSP）
      例：Hide My Applist 需要 LSP 模块 + KSU 提供 root 环境
```

---

## 🔍 反向排除法

### 什么时候**不能**用 KSU

| 场景 | 原因 |
|---|---|
| 改微信红包逻辑 | KSU 改文件，改不到 App 内存里的方法调用 |
| 去 B 站视频广告 | 广告是 App 内 SDK 行为，得 hook 方法 |
| 改 Application.onCreate | KSU 不在 App 进程里 |

### 什么时候**不能**用 LSP

| 场景 | 原因 |
|---|---|
| 改 hosts / 屏蔽域名 | LSP 不改文件系统 |
| 删系统预装 App | LSP 不挂 OverlayFS |
| 加载 `.ko` 内核模块 | LSP 是用户态 ART hook |
| 修改 SELinux 规则 | LSP 不管 sepolicy |

### 两者都能干

| 场景 | 谁更好 |
|---|---|
| 隐藏 root | KSU 层（susfs）做**内核级隐藏**；LSP 层（HMA）做**应用列表隐藏**——**两个都要** |
| 改系统属性 | KSU（system.prop） |
| 改 App 内的设置 | LSP（hook 设置读取方法） |

---

## 🧩 真实场景对比

### 场景 1：去除 B 站启动广告

**用 LSP**——hook B 站的广告 SDK 初始化方法。

```java
XposedHelpers.findAndHookMethod(
    "com.bilibili.ad.common.AdsManager",
    lpparam.classLoader,
    "init",
    Context.class,
    new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) {
            param.setResult(null);  // 不初始化
        }
    }
);
```

**KSU 干不了**——广告是 App 内的 SDK 调用，不是文件系统问题。

---

### 场景 2：屏蔽某些域名

**用 KSU**——改 `/system/etc/hosts`：

```
my_module/
├── module.prop
└── system/
    └── etc/
        └── hosts
```

`hosts` 内容：
```
127.0.0.1 ads.google.com
127.0.0.1 ad.xiaomi.com
...
```

**LSP 干不了**——LSP 不改文件系统。你只能 hook 每个 App 的网络库，但效率低。

---

### 场景 3：防撤回

**用 LSP**——hook 微信的撤回处理方法：

```java
XposedHelpers.findAndHookMethod(
    "com.tencent.mm.modelmsg.MsgRevokeHandler",
    lpparam.classLoader,
    "onRevoke",
    new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) {
            param.setResult(null);  // 不执行撤回
        }
    }
);
```

**KSU 干不了**——撤回是 App 内的逻辑。

---

### 场景 4：删预装毒瘤 App

**用 KSU**——模块里写：

```sh
REMOVE="
/system/app/YouTube
/system/app/Bloatware
"
```

**LSP 干不了**——LSP 不动文件系统。

---

### 场景 5：绕过 SSL Pinning

**用 LSP**——hook App 的网络库：

```java
XposedHelpers.findAndHookMethod(
    "okhttp3.CertificatePinner",
    lpparam.classLoader,
    "check",
    String.class, List.class,
    new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) {
            param.setResult(null);  // 不校验证书
        }
    }
);
```

**KSU 干不了**——这是 App 内的代码逻辑。

---

### 场景 6：让 App 以为没 root

**两个都要**：
- **KSU 层**：susfs 藏文件、ZygiskNext 藏进程
- **LSP 层**：Hide My Applist 藏应用列表

---

### 场景 7：开机自动跑一个 daemon

**用 KSU**——`service.sh`：

```sh
#!/system/bin/sh
MODDIR=${0%/*}
until [ "$(getprop sys.boot_completed)" = "1" ]; do
    sleep 1
done
nohup $MODDIR/bin/mydaemon &
```

**LSP 干不了**——LSP 只在 App 启动时激活。

---

### 场景 8：拦截所有 App 的某个方法

**用 LSP**——hook `android` 包名（系统 framework）：

```java
// 在 LSPosed 里把作用域选 "Android"
if (!lpparam.packageName.equals("android")) return;

XposedHelpers.findAndHookMethod(
    "android.app.ApplicationPackageManager",
    lpparam.classLoader,
    "getInstalledApplications",
    int.class,
    new XC_MethodHook() { ... }
);
```

**KSU 干不了**。

---

## 🎯 决策口诀

| 你要干啥 | 用啥 |
|---|---|
| 改"手机本身" | KSU |
| 改"App 行为" | LSP |
| 改文件 / 文件夹 | KSU |
| 改方法 / 函数 | LSP |
| 改启动 / 内核 | KSU |
| 改 App 内部状态 | LSP |
| 全局生效 | KSU |
| 只影响某 App | LSP |

---

## 🧪 组合实战：Hide My Applist

HMA 是个**完美组合例子**：

```
KSU 层（提供 root 环境）
   ↓
ZygiskNext（提供 Zygisk 环境）
   ↓
LSPosed（提供 hook 框架）
   ↓
HMA（LSP 模块，hook PackageManager）
```

**KSU 不直接干活**，但它**让 LSP 能跑起来**。LSP 真正干活。

---

## 📚 资料来源

- [KernelSU 官方文档](https://kernelsu.org/)
- [LSPosed 仓库](https://github.com/LSPosed/LSPosed)
