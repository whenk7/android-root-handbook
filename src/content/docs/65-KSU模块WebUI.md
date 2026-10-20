---
title: "KSU 模块 WebUI"
---

> 模块可以有自己的 Web 界面，让用户在 KSU Manager 里点齿轮图标就能配置。这篇讲怎么写。

---

## 🎯 是啥

KSU 模块可以包含一个 `webroot/` 目录，里面是 HTML/CSS/JS，KSU Manager 会渲染成一个内嵌网页。

**用户体验**：
```
KSU Manager → 模块页 → 找到模块 → 点齿轮图标
→ 弹出 WebView → 显示模块的 webroot/index.html
```

---

## 📂 模块结构

```
my_module/
├── module.prop
├── webroot/              ← WebUI 目录
│   ├── index.html
│   ├── style.css
│   └── script.js
└── ...
```

---

## 🌐 最小示例

### webroot/index.html

```html
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>My Module</title>
    <style>
        body {
            font-family: sans-serif;
            padding: 20px;
            background: #1a1d29;
            color: #fff;
        }
        button {
            background: #4a9eff;
            color: white;
            border: none;
            padding: 10px 20px;
            border-radius: 4px;
            cursor: pointer;
        }
        #output {
            margin-top: 20px;
            padding: 10px;
            background: #2a2d39;
            border-radius: 4px;
            font-family: monospace;
            white-space: pre-wrap;
        }
    </style>
</head>
<body>
    <h1>My Module Config</h1>
    <button onclick="runCommand()">执行命令</button>
    <div id="output"></div>
    
    <script>
        function runCommand() {
            ksu.exec("echo Hello from KSU", {}, (result) => {
                document.getElementById("output").textContent = result.stdout;
            });
        }
    </script>
</body>
</html>
```

---

## 🔧 KSU 提供的 JS API

KSU 在 WebView 里注入了 `ksu` 对象：

### ksu.exec(cmd, options, callback)

**执行 shell 命令**：

```javascript
ksu.exec("ls /data", {}, (result) => {
    console.log(result.stdout);  // 标准输出
    console.log(result.stderr);  // 标准错误
    console.log(result.exitCode); // 退出码
});
```

### ksu.spawn(cmd, args, options)

**启动一个进程**（流式）：

```javascript
const proc = ksu.spawn("top", ["-b", "-n", "1"], {});

proc.stdout.on("data", (data) => {
    console.log(data);
});

proc.on("exit", (code) => {
    console.log("退出:", code);
});
```

### ksu.listModules()

**列出所有模块**：

```javascript
ksu.listModules((modules) => {
    modules.forEach(m => {
        console.log(m.id, m.name, m.version);
    });
});
```

---

## 🎨 完整实战：广告过滤模块的 WebUI

### 功能

- 显示当前 hosts 状态
- 切换"启用 / 禁用"
- 编辑自定义规则

### webroot/index.html

```html
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>AdBlock Config</title>
    <style>
        body {
            font-family: -apple-system, sans-serif;
            padding: 16px;
            background: #f5f5f5;
        }
        .card {
            background: white;
            padding: 16px;
            margin-bottom: 12px;
            border-radius: 8px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        }
        .switch {
            display: flex;
            justify-content: space-between;
            align-items: center;
        }
        button {
            background: #4a9eff;
            color: white;
            border: none;
            padding: 8px 16px;
            border-radius: 4px;
        }
        textarea {
            width: 100%;
            min-height: 100px;
            font-family: monospace;
            padding: 8px;
        }
    </style>
</head>
<body>
    <div class="card">
        <h2>广告过滤</h2>
        <div class="switch">
            <span>启用</span>
            <button id="toggle">切换</button>
        </div>
        <div id="status"></div>
    </div>
    
    <div class="card">
        <h3>自定义规则</h3>
        <textarea id="rules" placeholder="127.0.0.1 ads.example.com"></textarea>
        <button onclick="saveRules()">保存</button>
    </div>
    
    <div class="card">
        <h3>日志</h3>
        <pre id="log"></pre>
    </div>
    
    <script>
        const MODDIR = "/data/adb/modules/adblock";
        
        // 加载状态
        function loadStatus() {
            ksu.exec(`test -f ${MODDIR}/disable && echo "禁用" || echo "启用"`, {}, (r) => {
                document.getElementById("status").textContent = "当前状态: " + r.stdout.trim();
            });
        }
        
        // 切换启用
        document.getElementById("toggle").onclick = () => {
            ksu.exec(`test -f ${MODDIR}/disable && rm ${MODDIR}/disable || touch ${MODDIR}/disable`, {}, () => {
                loadStatus();
                alert("重启后生效");
            });
        };
        
        // 加载自定义规则
        function loadRules() {
            ksu.exec(`cat ${MODDIR}/custom_rules.txt 2>/dev/null || echo ""`, {}, (r) => {
                document.getElementById("rules").value = r.stdout;
            });
        }
        
        // 保存规则
        function saveRules() {
            const rules = document.getElementById("rules").value;
            ksu.exec(`echo '${rules}' > ${MODDIR}/custom_rules.txt`, {}, () => {
                alert("已保存");
            });
        }
        
        // 初始化
        loadStatus();
        loadRules();
    </script>
</body>
</html>
```

---

## ⚠️ 注意事项

### 1. WebView 默认配置

KSU Manager 用的是普通 Android WebView：
- 支持现代 HTML5 / CSS / JS
- 不支持 ES6 module（要打包成单文件）
- localStorage 可用

### 2. 权限

WebUI 里的 JS 跑在 **root 权限**——`ksu.exec` 跑命令就是 root。**别暴露危险接口**。

### 3. 安全

- 不要把密码、token 写在 WebUI 里
- 不要从外部 URL 加载 JS（可能被劫持）

### 4. 大小限制

webroot 整个目录建议 < 5MB。

---

## 🎯 经典疑问

### Q1: 我能用 Vue / React 写 WebUI 吗？

**能**——但要打包成**单文件 HTML**（用 Vite 的 singlefile 插件等）。

### Q2: WebUI 能联网吗？

**能**——但没必要。模块配置一般是本地的。

### Q3: WebUI 能跟模块的 service.sh 通信吗？

**间接**——通过写文件：
- WebUI 写配置文件到模块目录
- service.sh 读配置文件

---

## 📚 资料来源

- [KernelSU module-webui.md](https://github.com/tiann/KernelSU/blob/main/website/docs/zh_CN/guide/module-webui.md)
