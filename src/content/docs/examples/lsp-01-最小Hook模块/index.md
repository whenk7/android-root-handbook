---
title: "lsp-01-最小Hook模块"
---

> 对应文档：[docs/66-LSP模块开发入门](/android-root-handbook/66-lsp模块开发入门)
>
> 难度：⭐⭐

最小的 LSP 模块——hook 一个测试 App 的方法。

---

## 🎯 干啥

hook 一个简单测试 App（见 [lsp-02-配套测试App](/android-root-handbook/examples/lsp-02-配套测试app)）的 `getMessage()` 方法，让它返回 "被 hook 了！"

---

## 📂 结构

```
lsp-01-最小Hook模块/
├── README.md
├── app/
│   ├── build.gradle
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── assets/
│       │   └── xposed_init
│       ├── java/com/example/lsp01/
│       │   └── HookEntry.java
│       └── res/
│           └── values/
│               ├── arrays.xml
│               └── strings.xml
└── build.gradle
```

---

## 🛠️ 编译

```bash
cd lsp-01-最小Hook模块
./gradlew assembleDebug
```

输出：`app/build/outputs/apk/debug/app-debug.apk`

---

## 📥 安装

```bash
# 装模块
adb install app/build/outputs/apk/debug/app-debug.apk

# 装配套测试 App（先编译 lsp-02）
adb install ../lsp-02-配套测试App/app/build/outputs/apk/debug/app-debug.apk
```

**LSPosed 启用**：
1. 打开 LSPosed Manager
2. 模块 → 启用 "lsp-01"
3. 点模块 → 勾选作用域 `com.example.targetapp`
4. 强行停止目标 App
5. 重新打开

---

## ✅ 验证

打开测试 App：
- 没 hook：显示 "原始消息"
- hook 成功：显示 "被 hook 了！"

看日志：
```bash
adb logcat | grep MyLSP01
```

---

## 🧠 学到的东西

- LSP 模块的最小结构
- `IXposedHookLoadPackage` 的用法
- `XposedHelpers.findAndHookMethod` 的用法
