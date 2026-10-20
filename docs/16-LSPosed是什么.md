# 16 · LSPosed 是什么

> LSPosed 是当前最活跃的 Xposed 实现。这篇讲它的原理和核心特性。

---

## 🎯 一句话定义

> **LSPosed 是一个 Riru/Zygisk 模块，提供与原版 Xposed 一致的 API，基于 LSPlant 做 ART hook，能在不修改 APK 的前提下改变系统和 App 行为。**

—— LSPosed 仓库 README

---

## 🏗️ LSPosed 的组件

```
┌──────────────────────────────────┐
│ Zygote 进程                       │
│  ┌────────────────────────────┐ │
│  │ Zygisk                      │ │
│  │  ┌──────────────────────┐ │ │
│  │  │ liblspd.so           │ │ │  ← LSPosed 的 Zygisk 模块
│  │  │ - 初始化 ART hook     │ │ │
│  │  │ - 加载启用的模块       │ │ │
│  │  └──────────────────────┘ │ │
│  └────────────────────────────┘ │
└──────────────────────────────────┘
              ↓ fork
        ┌─────────────┐
        │  微信进程    │
        │  ┌────────┐ │
        │  │liblspd │ │  ← fork 时带过来
        │  │加载模块  │ │
        │  │hook 方法│ │
        │  └────────┘ │
        └─────────────┘
```

---

## 🔧 核心技术：LSPlant

**LSPlant** 是 LSPosed 团队自己写的 ART hook 引擎。

### 原理

ART 里每个方法对应一个 `ArtMethod` 结构体，里面有：
- `entry_point_from_jni_`：JNI 入口
- `entry_point_from_quick_compiled_code_`：编译后的机器码入口

**LSPlant 干的事**：把 `ArtMethod` 的入口指针改成你自己的函数地址。

下次这个方法被调用，CPU 跳到的是你的代码——这就是 hook。

### 为什么自己写 LSPlant

社区曾有 YAHFA、SandHook、Dobby 等引擎。LSPosed 团队发现它们各有问题：
- YAHFA：维护停滞
- SandHook：稳定性差
- Dobby：主要用于 inline hook，对 ART 方法 hook 不够专

所以自己写了 LSPlant，专门为 ART 优化。

---

## 🎯 作用域（Scope）机制

### 是啥

每个 LSP 模块**声明它作用于哪些 App**。LSPosed 只在那些 App 进程里加载它。

### 声明方式（模块开发者写）

**方式 1：AndroidManifest.xml**
```xml
<meta-data
    android:name="xposedscope"
    android:resource="@array/xposed_scope"/>
```

`res/values/arrays.xml`:
```xml
<string-array name="xposed_scope">
    <item>com.tencent.mm</item>
    <item>tv.danmaku.bili</item>
</string-array>
```

**方式 2：代码里判断**
```java
public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
    if (!lpparam.packageName.equals("com.tencent.mm")) return;
    // ...
}
```

### 为什么重要

| 没作用域 | 有作用域 |
|---|---|
| 模块对所有 App 生效 | 只对勾选的 App 生效 |
| 一个 hook 写错，整机所有 App 都崩 | 崩了只崩那一个 App |
| 银行 App 检测到 hook 框架就闪退 | 银行 App 不在作用域，看不见你 |
| 性能损耗全局 | 性能损耗只在目标 App |

---

## 🎨 LSPosed 管理器

LSPosed 自带一个 Manager App：

**功能**：
- 启用/禁用模块
- 设置模块作用域
- 查看日志
- 下载模块（从 Xposed-Modules-Repo）

**打开方式**：
- 通知栏（LSPosed 通知）
- 拨号盘输入 `*#*#5776733#*#*`（"lsposed"对应数字）

---

## 🌐 模块仓库

**官方仓库**：`Xposed-Modules-Repo`

地址：https://github.com/Xposed-Modules-Repo

模块开发者可以提交模块到这个仓库，用户在 LSPosed Manager 里直接搜到。

**第三方仓库**：有些模块不在官方仓库（比如涉及破解的），要去 GitHub/Telegram 找。

---

## 🧪 亲手验证 LSPosed

```bash
adb shell

# 1. 看 LSPosed 是否在 Zygote 里
su -c "cat /proc/$(pidof zygote64)/maps | grep -i lsp"

# 2. 看 LSPosed 是否在某个 App 里
su -c "cat /proc/$(pidof com.tencent.mm)/maps | grep -i lsp"

# 3. 看 LSPosed 日志
# 打开 LSPosed Manager → 日志页面

# 或者命令行
su -c "cat /data/adb/lspd/log/verbose.log"
```

---

## 🎯 经典疑问

### Q1: LSPosed 和 Xposed 兼容吗？

**理论上完全兼容**——LSPosed 实现了 XposedBridge 的全部 API。

**实际上**：
- 老模块（Xposed 时代）可能用了已废弃的 API
- 部分模块对 LSPosed 的作用域机制不适配

### Q2: LSPosed 慢吗？

**几乎不慢**——LSPlant 是 ART hook，不是 interpreter。被 hook 的方法第一次调用会慢一点（JIT 编译），之后跟原速度几乎一样。

### Q3: LSPosed 会不会让 App 崩溃？

**会**——如果模块写错了。但 LSPosed 的作用域机制意味着崩溃只影响目标 App，不会拖累系统。

### Q4: 我能不能不写模块，直接用别人的？

**完全可以**。Xposed-Modules-Repo 有上千个现成模块。写模块是进阶需求。

---

## 📚 资料来源

- [LSPosed GitHub](https://github.com/LSPosed/LSPosed)
- [LSPlant GitHub](https://github.com/LSPosed/LSPlant)
- [Xposed-Modules-Repo](https://github.com/Xposed-Modules-Repo)
