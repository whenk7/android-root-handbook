# lsp-04-构造函数Hook

> 对应文档：[docs/68-LSP进阶技巧](../../68-LSP进阶技巧.md)
>
> 难度：⭐⭐

hook 类的构造函数，在对象创建时改它的字段。

---

## 🎯 干啥

每次 `User` 对象被创建时，把它的 `name` 字段改成 "Hacker"。

---

## 🛠️ 编译

```bash
./gradlew assembleDebug
```

---

## 🧠 学到的东西

- `XposedHelpers.findAndHookConstructor` 的用法
- `param.thisObject` 是刚构造的对象
