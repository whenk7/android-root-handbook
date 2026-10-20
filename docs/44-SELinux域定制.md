# 44 · SELinux 域定制

> App Profile 的第三层防护：给每个 root App 一个**自定义的 SELinux 域**，精细控制它能碰什么。

---

## 🎯 是啥

默认情况下，App 拿到 `su` 后，SELinux context 是 `u:r:ksu:s0`——这是 KSU 给的"几乎不受限"的域。

**Root Profile 改它**：把 su 后的 SELinux context 改成你指定的域，然后给这个域写规则。

---

## 🏷️ SELinux Context 复习

格式：`用户:角色:类型:级别`

```
u:r:ksu:s0              ← KSU 给的默认 su 域
u:r:system_app:s0       ← 系统 App
u:r:shell:s0            ← shell
u:r:untrusted_app:s0    ← 普通 App
```

**类型（type）最重要**——策略规则就是基于类型的。

---

## 🛠️ 自定义域

### 步骤

1. **起个名字**：比如 `myapp`
2. **定义类型**：
   ```
   type myapp, domain;
   ```
3. **打标签**：
   ```
   typeattribute myapp mlstrustedsubject
   ```
4. **写规则**：
   ```
   allow myapp app_data_file file { read write };
   allow myapp system_file file read;
   ```
5. **在 App Profile 里指定**：
   ```
   SELinux Context: u:r:myapp:s0
   ```

---

## 🎬 实战例子

### 例子 1：限制文件管理器只能读，不能写

**自定义域**：`fm_limited`

```
type fm_limited, domain;
typeattribute fm_limited mlstrustedsubject

# 允许读所有文件
allow fm_limited file_type file { read open getattr };
allow fm_limited file_type dir { read open getattr search };

# 不允许写
# （不写规则就是默认拒绝）

# 不允许网络
# （不写规则就是默认拒绝）
```

**App Profile 配置**：

```
SELinux Context: u:r:fm_limited:s0
```

**效果**：
- App 拿到 root 后能读所有文件
- 写操作被 SELinux 拒绝
- 网络操作被 SELinux 拒绝

### 例子 2：让备份 App 只能访问 /data

**自定义域**：`backup_only_data`

```
type backup_only_data, domain;

# 允许访问 /data
allow backup_only_data data_file file { read write };
allow backup_only_data data_file dir { read write search };

# 允许访问 /sdcard
allow backup_only_data sdcard_file file { read write };

# 拒绝访问 /system
# （不写规则就是拒绝）

# 拒绝网络
# （不写规则就是拒绝）
```

---

## 🚫 "allow app1 * * *" 陷阱

### 官方文档的警告

> 注意：此处的 `allow app1 * * *` 仅仅作为演示方便而使用，实际过程中不应使用这个规则，因为它跟 permissive 区别不大。

### 意思

```
allow app1 * * *
     ↑     ↑ ↑ ↑
     │     │ │ └─ 所有操作
     │     │ └─── 所有类型
     │     └───── 所有目标
     └─────────── 你的域
```

**意思是"全允许"**——等于没限制，跟 permissive 一样。

**别这么写**。

---

## 🔧 规则语法

```
allow <源类型> <目标类型> : <类别> <操作>;
```

### 例子

```
# 允许 myapp 读普通文件
allow myapp file_type file read;

# 允许 myapp 读 /system 下的文件
allow myapp system_file file { read open getattr };

# 允许 myapp 创建 socket
allow myapp self tcp_socket create;

# 允许 myapp 连网
allow myapp self tcp_socket { create connect read write };

# 允许 myapp 执行某文件
allow myapp my_exec_file file { execute execute_no_trans };
```

### 常用类别（class）

- `file`：文件
- `dir`：目录
- `tcp_socket`：TCP socket
- `udp_socket`：UDP socket
- `process`：进程
- `service_manager`：服务管理器

### 常用操作

- 文件：`read write create open getattr setattr unlink rename execute`
- 目录：`read write create open search add_name remove_name`
- socket：`create connect bind listen accept read write`

---

## 🛠️ 怎么调试 SELinux 规则

### 步骤

```
1. 切宽容模式
   adb shell su -c "setenforce 0"

2. 跑你的 App，让所有该发生的拦截都发生一遍

3. 收集 avc 日志
   adb shell su -c "dmesg | grep avc" > /tmp/avc.log

4. 切回严格模式
   adb shell su -c "setenforce 1"

5. 用 audit2allow 把日志翻译成规则
   adb shell su -c "audit2allow < /tmp/avc.log"

6. 把规则加到你的自定义域

7. 重启验证
```

### audit2allow 输出示例

输入日志：
```
avc: denied { read } for pid=1234 comm="myapp" 
     path="/data/local/tmp/config" 
     scontext=u:r:myapp:s0 
     tcontext=u:object_r:shell_data_file:s0 
     tclass=file
```

`audit2allow` 输出：
```
allow myapp shell_data_file:file read;
```

---

## 🎯 经典疑问

### Q1: 我能不能不自定义域，就用默认的 `u:r:ksu:s0`？

**可以**——大部分用户用默认的就够了。自定义域是**进阶需求**。

### Q2: 自定义域能完全替代 Capabilities 吗？

**不能完全替代**：
- Capabilities 控制"能不能干"（粗粒度）
- SELinux 控制"让不让你碰这个具体资源"（细粒度）

**两者配合**才是完整的防护。

### Q3: 我不会写 SELinux 规则怎么办？

**先用工具**：
- `audit2allow` 自动从日志生成
- `sesearch` 查现有规则
- 参考其他模块的 `sepolicy.rule`

### Q4: 我写错规则会怎样？

- 写少了 → App 功能不正常
- 写多了 → 安全漏洞

**建议**：从最严格的开始，根据 `avc: denied` 日志**逐步放开**。

---

## 📚 资料来源

- [KernelSU app-profile.md](https://github.com/tiann/KernelSU/blob/main/website/docs/zh_CN/guide/app-profile.md)
- [SELinux 官方文档（红帽）](https://www.redhat.com/en/topics/linux/what-is-selinux)
- [ArchWiki SELinux](https://wiki.archlinux.org/title/SELinux)
