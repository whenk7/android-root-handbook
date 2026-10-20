# Android Root 与 LSPosed 实战手册

> ⚠️ **免责声明**
>
> - 玩机有风险，操作需谨慎，刷机变砖请自担
> - 部分内容涉及绕过应用安全机制，仅供学习研究，请勿用于非法用途

📖 **在线阅读**：[https://whenk7.github.io/android-root-handbook/](https://whenk7.github.io/android-root-handbook/)

---

## 📖 这是什么

一本**看得懂、用得上**的 Android Root 玩机手册，覆盖：

- KernelSU / SukiSU / Magisk / APatch
- Zygisk / ZygiskNext / LSPosed / Xposed
- susfs / ZygiskNext / HMA / Play Integrity Fix / prop 隐藏
- App Profile / 隐藏 Root / OTA 保 Root / 救砖

## 📂 仓库结构

```
android-root-handbook/
├── src/content/docs/      ← 文档源文件（Starlight，所有章节）
│   ├── index.mdx         ← 首页（卡片导航 + 决策树）
│   ├── 01-名词宇宙.md
│   ├── ...
│   ├── 90-FAQ.md
│   ├── cheatsheets/      ← 速查表
│   └── examples/         ← 实战代码示例
├── src/styles/custom.css ← 自定义样式（中文字体栈等）
├── astro.config.mjs      ← Starlight 站点配置
├── sidebar.mjs           ← 侧边栏（由脚本生成）
└── .github/workflows/
    └── deploy.yml        ← GitHub Actions 自动部署
```

站点基于 [Starlight](https://starlight.astro.build/zh-cn/) + [Galaxy 主题](https://frostybee.github.io/starlight-theme-galaxy/) 构建。推送 `main` 分支后 Actions 自动构建并部署到 GitHub Pages。

## 📝 更新记录

- 2026-10-19：初版
- 2026-10-20：第三阶段重排（ZygiskNext v1.3.0+ 整合 Shamiko、KSU v3.3.0+ 内置反检测、prop 隐藏独立成篇）
- 2026-10-20：站点从 MkDocs Material 迁移到 Astro Starlight + Galaxy 主题（旧版保留在 `mkdocs-legacy` 分支）
