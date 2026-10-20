---
title: "SELinux 拒绝怎么看（avc 日志）"
---

> 模块不工作、App 闪退，先看 avc 日志。这篇教你看。

---

## 🔍 在哪看

按可靠性排序：

```bash
# 1. 内核日志（最准，实时）
adb shell su -c "dmesg -w | grep avc"

# 2. 内核 ring buffer
adb shell su -c "dmesg | grep avc"

# 3. logcat events buffer
adb logcat -b events | grep avc

# 4. logcat main
adb logcat | grep avc
```

---

## 🧬 avc 日志格式

### 典型日志

```
avc: denied { read } for pid=1234 comm="com.bank" 
     path="/data/adb/modules/xxx/config" 
     dev="sda30" ino=12345 
     scontext=u:r:untrusted_app:s0 
     tcontext=u:object_r:adb_data_file:s0 
     tclass=file
```

### 字段拆解

| 字段 | 含义 |
|---|---|
| `avc: denied` | SELinux 拒绝了 |
| `{ read }` | 拒绝的操作（read / write / execute / ...） |
| `pid=1234` | 进程 PID |
| `comm="com.bank"` | 进程名 |
| `path="..."` | 目标路径（如果是文件操作） |
| `scontext=u:r:untrusted_app:s0` | **源** context（谁发起的） |
| `tcontext=u:object_r:adb_data_file:s0` | **目标** context（被访问的） |
| `tclass=file` | 目标类别（file / dir / socket / ...） |

**翻译**：进程 `com.bank`（pid 1234）想 read 一个文件，源标签是 untrusted_app，目标标签是 adb_data_file，类型是 file——被拒绝了。

---

## 🛠️ 从日志生成 sepolicy 规则

### 工具：audit2allow

```bash
# 收集日志
adb shell su -c "dmesg | grep avc" > /tmp/avc.log

# 翻译成规则
adb shell su -c "audit2allow < /tmp/avc.log"
```

### 输出示例

```
#============= untrusted_app ==============
allow untrusted_app adb_data_file:file read;
allow untrusted_app adb_data_file:file open;
allow untrusted_app adb_data_file:file getattr;
```

### 加到模块

把这些行加到模块的 `sepolicy.rule`：

```
/data/adb/modules/<MODID>/sepolicy.rule
```

重启生效。

---

## 🧪 调试流程

```
1. 切宽容模式
   adb shell su -c "setenforce 0"

2. 跑你的 App / 模块，让所有该发生的拦截都发生一遍

3. 收集 avc 日志
   adb shell su -c "dmesg | grep avc" > /tmp/avc.log

4. 切回严格模式
   adb shell su -c "setenforce 1"

5. 用 audit2allow 翻译
   adb shell su -c "audit2allow < /tmp/avc.log"

6. 把规则加到模块的 sepolicy.rule

7. 重启验证
```

---

## 📊 常见 avc 类型

### 文件访问

```
avc: denied { read } ... tclass=file
avc: denied { write } ... tclass=file
avc: denied { execute } ... tclass=file
```

**翻译规则**：
```
allow <source> <target>:file <operation>;
```

### 目录访问

```
avc: denied { search } ... tclass=dir
avc: denied { add_name } ... tclass=dir
```

**翻译规则**：
```
allow <source> <target>:dir <operation>;
```

### Socket

```
avc: denied { create } ... tclass=tcp_socket
avc: denied { connect } ... tclass=tcp_socket
```

**翻译规则**：
```
allow <source> self:tcp_socket <operation>;
```

### 进程

```
avc: denied { ptrace } ... tclass=process
```

**翻译规则**：
```
allow <source> <target>:process <operation>;
```

### Service Manager

```
avc: denied { find } ... tclass=service_manager
```

**翻译规则**：
```
allow <source> <service>:service_manager find;
```

---

## ⚠️ 别一刀切

### 反例：直接给所有权限

```
allow untrusted_app * * *;
```

**后果**：等于没限制，跟 permissive 一样。

### 正确：精细控制

```
allow myapp specific_file_type:file read;
```

---

## 🛠️ 实用工具

### sesearch（查现有规则）

```bash
adb shell su -c "sesearch -A -s untrusted_app | head -20"
```

### seinfo（看类型）

```bash
adb shell su -c "seinfo -a file -x | head"
```

---

## 🎯 经典疑问

### Q1: 我看到 avc: denied，但 App 工作正常？

**可能**：
- App 有 fallback 逻辑
- 拒绝的是非关键路径
- 这个拦截是"误报"（实际上是 App 试探）

**不用处理**——只要 App 功能正常。

### Q2: 我加了 sepolicy.rule，但还是不行？

**排查**：
1. 重启了吗？
2. 规则语法对吗？
3. 模块是启用的吗？

```bash
# 看模块的 sepolicy 是否被加载
adb shell su -c "sesearch -A -s <你的type>"
```

### Q3: 我能不能全局 permissive？

**能**：
```bash
adb shell su -c "setenforce 0"
```

**但**：
- 银行 App 会检测
- 安全性差
- 重启失效

---

## 📚 资料来源

- [SELinux 官方文档（红帽）](https://www.redhat.com/en/topics/linux/what-is-selinux)
- [audit2allow man page](https://man7.org/linux/man-pages/man1/audit2allow.1.html)
