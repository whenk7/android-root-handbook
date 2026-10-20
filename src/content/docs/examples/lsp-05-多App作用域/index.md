---
title: "lsp-05-多App作用域"
---

> 对应文档：[docs/68-LSP进阶技巧](/android-root-handbook/68-lsp进阶技巧)
>
> 难度：⭐⭐

一个模块 hook 多个 App——hook 微信和 QQ，让它们都打印一行日志。

---

## 🎯 干啥

同一个模块，分别 hook：
- `com.tencent.mm`（微信）
- `com.tencent.mobileqq`（QQ）

---

## 🛠️ 编译

```bash
./gradlew assembleDebug
```

---

## 📥 使用

```
1. LSPosed Manager → 启用模块
2. 点模块 → 勾选作用域（微信 + QQ）
3. 强停这两个 App
4. 重启它们
```

---

## 🧠 学到的东西

- `handleLoadPackage` 的 switch 分流
- 多 App 作用域的配置
