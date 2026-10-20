# ksu-04-开机跑Daemon

> 对应文档：[docs/62-KSU启动脚本](../../62-KSU启动脚本.md)
>
> 难度：⭐⭐

开机后跑一个 root daemon——一个简单的"心跳"程序，每秒往日志写一行。

---

## 📂 结构

```
ksu-04-开机跑Daemon/
├── README.md
├── module.prop
├── service.sh           ← 启动 daemon
└── bin/
    └── heartbeat.sh     ← 一个简单的 daemon 脚本
```

---

## 🧬 原理

```
service.sh → 等系统启动完 → 后台启动 bin/heartbeat.sh → 心跳
```

---

## 📦 打包

```bash
cd ksu-04-开机跑Daemon
zip -r ../ksu-04.zip . -x ".*" -x "README.md"
```

---

## 📥 安装

```
1. adb push ../ksu-04.zip /sdcard/
2. KSU Manager → 模块 → ➕ → 选 /sdcard/ksu-04.zip
3. 重启
```

---

## ✅ 验证

```bash
# 看 daemon 是否在跑
adb shell "ps -ef | grep heartbeat"

# 看心跳日志
adb shell "tail -10 /data/local/tmp/heartbeat.log"
```

应该看到：
```
2026-10-19 15:30:22 alive
2026-10-19 15:30:23 alive
2026-10-19 15:30:24 alive
...
```

---

## 🛠️ 自定义

把 `bin/heartbeat.sh` 换成你自己的二进制/脚本：

```
mydaemon/
├── bin/
│   └── mydaemon          ← 静态编译的二进制
└── service.sh            ← 启动它
```

**关键**：用 `nohup ... &` 后台跑，别阻塞 service.sh。

---

## 🧠 学到的东西

- `service.sh` 里启动 daemon 的正确姿势
- `nohup ... &` 后台跑
- 日志输出到 `/data/local/tmp/`（shell 可读）
