# lsp-03-方法替换示例

> 对应文档：[docs/67-LSP核心API](../../67-LSP核心API.md)
>
> 难度：⭐⭐

用 `XC_MethodReplacement` **完全替换**原方法。

---

## 🎯 干啥

hook 测试 App 的 `isVip()` 方法，让它**永远返回 true**。

---

## 🆚 XC_MethodHook vs XC_MethodReplacement

### XC_MethodHook

```java
new XC_MethodHook() {
    @Override
    protected void beforeHookedMethod(MethodHookParam param) {
        // 在原方法跑之前
        param.setResult(true);   // 这样也能跳过原方法
    }
    
    @Override
    protected void afterHookedMethod(MethodHookParam param) {
        // 在原方法跑之后
    }
}
```

### XC_MethodReplacement

```java
new XC_MethodReplacement() {
    @Override
    protected Object replaceHookedMethod(MethodHookParam param) {
        // 完全替换原方法
        return true;
    }
}
```

**区别**：
- `XC_MethodHook`：原方法**会跑**（除非 before 里 setResult）
- `XC_MethodReplacement`：原方法**根本不跑**

---

## 📂 结构

跟 lsp-01 一样，只是 hook 逻辑不同。

---

## 🛠️ 编译

```bash
./gradlew assembleDebug
```

---

## 🧠 学到的东西

- `XC_MethodReplacement` 的用法
- 什么时候用 hook，什么时候用 replacement
