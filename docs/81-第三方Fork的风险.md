# 81 · 第三方 Fork 的风险

> 你用了一个第三方 fork 的 KSU/SukiSU，怎么知道它没带毒？

---

## 🎯 风险在哪

第三方 fork 可能在：
- **内核模块 (.ko)** 里塞恶意代码——内核态代码能干任何事
- **Manager APK** 里塞恶意代码——能读你的授权列表、模块配置
- **构建脚本** 里加料——CI 出来的产物跟源码不一致

---

## 🛡️ 验证方法（按强度排序）

### A. 弱验证：查文件哈希 + 社区声誉

```bash
# 下载后看 SHA256
sha256sum android16-6.12-aarch64_kernelsu.ko

# 对比作者公布的哈希（如果作者提供）
```

**作者维度**：
- GitHub followers、仓库历史
- 是否开源构建脚本（让你能自己 repro）
- 有没有社区 review

### B. 中验证：自己从源码构建

```
1. clone fork 仓库
2. 看 CI 配置（.github/workflows/）
3. 自己跑构建
4. 对比产物哈希
```

### C. 强验证：二进制分析

```bash
# 看 .ko 的符号表
readelf -s android16-6.12-aarch64_kernelsu.ko | head -50

# 看字符串（找可疑的 URL、命令）
strings android16-6.12-aarch64_kernelsu.ko | grep -iE "http|curl|wget|exec|reverse|shell"

# 跟官方 SukiSU 同 KMI 的 .ko 对比大小
ls -l *.ko
```

**字符串里出现这些就该警惕**：
- 奇怪的 IP / 域名
- `curl` `wget` `http`
- 不在官方 KSU 里的函数名

### D. 实战验证：装完后监控

```bash
# 看网络连接
adb shell su -c "netstat -tupn"
adb shell su -c "ss -tupn"

# 看进程
adb shell su -c "ps -ef | grep -v '\[.*\]' | sort -k2"

# 看内核日志有没有奇怪的 syscall
adb shell su -c "dmesg | grep -iE 'ksu|suspicious'"
```

---

## 📋 验证清单

```
□ 1. 源码是否完整公开
□ 2. CI 构建记录可查（GitHub Actions）
□ 3. 跟上游 diff 干净（只加了声明的改动）
□ 4. 作者在社区有声誉
□ 5. 有二进制哈希可对比
□ 6. 装完后看进程/网络是否干净
```

---

## 🚨 高危信号

看到以下任一信号，**立刻别用**：

- 只发二进制不发源码
- 源码跟产物对不上
- 作者匿名
- 仓库刚创建几天
- 没有 issue 区
- 没有 license
- 强制要求"关闭所有安全检测"

---

## 🎯 推荐做法

### 1. 优先用官方渠道

- **官方 SukiSU**: `SukiSU-Ultra/SukiSU-Ultra`
- **官方 KernelSU**: `tiann/KernelSU`
- **官方 LSPosed**: `LSPosed/LSPosed`

### 2. 第三方 fork 只在没有官方替代品时考虑

比如：
- 官方不支持你的设备
- 官方缺少你急需的功能

### 3. 已经装了第三方

- 别慌
- 看网络连接（有没有可疑外联）
- 看进程（有没有可疑进程）
- 必要时换回官方

---

## 🧬 一个真实案例

### 案例：某 fork 的 .ko 被加料

```
症状：
- 用户装了某 fork 的 KSU
- 手机经常发热
- 流量异常

排查：
- netstat 看到有到外网的连接
- ps 看到可疑进程

根因：
- fork 的 .ko 里加了挖矿代码

教训：
- 只装官方或知名 fork
- 装完看网络连接
```

---

## 📚 资料来源

- [KernelSU 官方仓库](https://github.com/tiann/KernelSU)
- [SukiSU-Ultra 官方仓库](https://github.com/SukiSU-Ultra/SukiSU-Ultra)
