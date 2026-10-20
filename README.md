# Android Root 与 LSPosed 实战手册

> 给半新手的玩机指南。在线阅读（部署后生效）：`https://whenk7.github.io/android-root-handbook/`

---

## 📖 这是什么

一本**看得懂、用得上**的 Android Root 玩机手册，覆盖：

- KernelSU / SukiSU / Magisk / APatch
- Zygisk / ZygiskNext / LSPosed / Xposed
- susfs / ZygiskNext / HMA / Play Integrity Fix / prop 隐藏
- App Profile / 隐藏 Root / OTA 保 Root / 救砖

## 🚀 部署到 GitHub Pages

```bash
# 1. 在 GitHub 创建仓库 android-root-handbook（Public）
# 2. push 代码
git init
git add .
git commit -m "Initial commit"
git branch -M main
git remote add origin git@github.com:whenk7/android-root-handbook.git
git push -u origin main

# 3. GitHub Actions 会自动构建并部署到 gh-pages 分支
# 4. 仓库 Settings → Pages → Source 选 gh-pages 分支
# 5. 几分钟后访问 https://whenk7.github.io/android-root-handbook/
```

## 💻 本地预览

```bash
pip install mkdocs-material
mkdocs serve
# 打开 http://127.0.0.1:8000
```

## 📂 仓库结构

```
android-root-handbook/
├── docs/                  ← MkDocs 源目录（所有章节）
│   ├── index.md          ← 首页（章节导航）
│   ├── 01-名词宇宙.md
│   ├── ...
│   ├── 90-FAQ.md
│   ├── cheatsheets/      ← 速查表
│   └── examples/         ← 实战代码示例
├── mkdocs.yml            ← MkDocs 配置
└── .github/workflows/
    └── deploy.yml        ← GitHub Actions 自动部署
```

## 📝 更新记录

- 2026-10-19：初版
- 2026-10-20：第三阶段重排（ZygiskNext v1.3.0+ 整合 Shamiko、KSU v3.3.0+ 内置反检测、prop 隐藏独立成篇）

## ⚠️ 免责声明

- 玩机有风险，操作需谨慎，刷机变砖请自担
- 部分内容涉及绕过应用安全机制，仅供学习研究，请勿用于非法用途
