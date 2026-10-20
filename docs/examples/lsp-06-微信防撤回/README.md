# lsp-06-微信防撤回

> 综合实战
>
> 难度：⭐⭐⭐

hook 微信的撤回处理方法，让"撤回"失效。

---

## ⚠️ 重要说明

**这是一个"概念验证"示例**。微信的真实类名/方法名**每个版本都不一样**——这个示例**不能直接在你的微信上跑**。

实际使用时你需要：
1. 反编译你当前版本的微信（用 JADX）
2. 找到真实的撤回处理类名和方法名
3. 替换代码里的占位符

---

## 🧬 思路

```
微信里有人撤回消息
   ↓
微信调用撤回处理函数
   ↓
【我们 hook 这个函数】
   ↓
不执行原方法 → 消息不会被撤回
```

---

## 📂 结构

```
lsp-06-微信防撤回/
├── README.md
└── app/
    ├── build.gradle
    └── src/main/
        ├── AndroidManifest.xml
        ├── assets/xposed_init
        ├── java/com/example/lsp06/
        │   └── HookEntry.java
        └── res/values/
            ├── arrays.xml
            └── strings.xml
```

---

## 🛠️ 怎么找真实类名

### 步骤

1. **反编译微信 APK**：
   ```
   用 JADX 打开微信 APK
   ```

2. **搜关键词**：
   ```
   搜：revoke
   搜：撤回
   搜：Recall
   搜：MsgRevoke
   ```

3. **找类似这样的类**：
   ```java
   // 假设的类名（每个版本不一样）
   com.tencent.mm.modelmsg.MsgRevokeHandler
   com.tencent.mm.storage.MsgRevokeStorage
   ```

4. **看它的方法**：
   ```java
   public void onRevoke(long msgId) { ... }
   public void handleRevoke(...) { ... }
   ```

5. **改 HookEntry.java 里的类名/方法名**

---

## 🛠️ 编译

```bash
./gradlew assembleDebug
```

---

## 📥 安装

```
1. 装 APK
2. LSPosed Manager → 启用
3. 勾选作用域：微信
4. 强停微信
5. 重新打开
```

---

## ⚠️ 法律/道德提醒

- 防撤回**只影响你自己的手机**
- 对方撤回的消息在**对方手机、服务器**上还是撤回了
- 你只是"自己还能看到"
- 别用这个干违规的事

---

## 🧠 学到的东西

- 反编译找 hook 点的流程
- 类名/方法名混淆的应对
- hook 真实 App 的方法
