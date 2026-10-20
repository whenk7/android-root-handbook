# Examples 索引

每个例子对应一篇文档，能直接跑。

## KSU 模块

### ksu-01-最小模块

对应 [docs/61-KSU模块开发入门](../61-KSU模块开发入门.md)

最简单的 KSU 模块，service.sh 打日志。

### ksu-02-改Hosts去广告

对应 [docs/63-KSU改系统文件](../63-KSU改系统文件.md)

改 `/system/etc/hosts` 屏蔽广告域名。

### ksu-03-删预装App

对应 [docs/63-KSU改系统文件](../63-KSU改系统文件.md)

用 `REMOVE` 变量删除预装 App。

### ksu-04-开机跑Daemon

对应 [docs/62-KSU启动脚本](../62-KSU启动脚本.md)

`service.sh` 启动一个 root daemon。

### ksu-05-加载内核模块

对应 [docs/62-KSU启动脚本](../62-KSU启动脚本.md)（preinit 部分）

用 `preinit/preinit.conf` 加载 `.ko`。

### ksu-06-InitRc服务注入

对应 [docs/62-KSU启动脚本](../62-KSU启动脚本.md)

用 `initrc/myservice.rc` 注册系统服务。

---

## LSP 模块

### lsp-01-最小Hook模块

对应 [docs/66-LSP模块开发入门](../66-LSP模块开发入门.md)

最小的 LSP 模块，打印一行日志。

### lsp-02-配套测试App

对应 [docs/66-LSP模块开发入门](../66-LSP模块开发入门.md)

一个简单的测试 App，给 lsp-01 hook 用。

### lsp-03-方法替换示例

对应 [docs/67-LSP核心API](../67-LSP核心API.md)

用 `XC_MethodReplacement` 完全替换方法。

### lsp-04-构造函数Hook

对应 [docs/68-LSP进阶技巧](../68-LSP进阶技巧.md)

hook 构造函数。

### lsp-05-多App作用域

对应 [docs/68-LSP进阶技巧](../68-LSP进阶技巧.md)

一个模块 hook 多个 App。

### lsp-06-微信防撤回

综合实战。

---

## 怎么用

1. 选你感兴趣的示例
2. 进入对应目录
3. 看里面的 README.md
4. 按文档操作
