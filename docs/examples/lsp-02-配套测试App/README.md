# lsp-02-配套测试App

> 对应文档：[docs/66-LSP模块开发入门](../../66-LSP模块开发入门.md)
>
> 难度：⭐

一个最简单的测试 App——给 [lsp-01-最小Hook模块](../lsp-01-最小Hook模块/README.md) 当靶子。

---

## 🎯 干啥

显示一行字："原始消息"。被 hook 后变成 "被 hook 了！"。

---

## 📂 结构

```
lsp-02-配套测试App/
├── README.md
├── app/
│   ├── build.gradle
│   └── src/main/
│       ├── AndroidManifest.xml
│       └── java/com/example/targetapp/
│           └── MainActivity.java
└── build.gradle
```

---

## 🛠️ 编译

```bash
cd lsp-02-配套测试App
./gradlew assembleDebug
```

---

## 📥 安装

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## ✅ 验证

打开 App：
- 没被 hook：显示 "原始消息"
- 被 lsp-01 hook：显示 "被 hook 了！"

---

## 🧠 学到的东西

- 自己当靶子是最简单的测试方法
- 不用 hook 别人的 App，先 hook 自己写的
