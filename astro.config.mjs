// @ts-check
import { defineConfig } from 'astro/config';
import starlight from '@astrojs/starlight';
import starlightThemeGalaxy from 'starlight-theme-galaxy';
import sidebar from './sidebar.mjs';

export default defineConfig({
	site: 'https://whenk7.github.io',
	base: '/android-root-handbook',
	integrations: [
		starlight({
			plugins: [starlightThemeGalaxy()],
			title: 'Android Root 与 LSPosed 实战手册',
			description:
				'给半新手的玩机指南 — KernelSU / SukiSU / LSPosed / Zygisk / susfs / HMA / PIF',
			locales: {
				root: { label: '简体中文', lang: 'zh-CN' },
			},
			logo: {
				src: './src/assets/logo.svg',
				alt: 'Android Root 手册 Logo',
			},
			social: [
				{
					icon: 'github',
					label: 'GitHub 仓库',
					href: 'https://github.com/whenk7/android-root-handbook',
				},
			],
			sidebar,
			customCss: ['./src/styles/custom.css'],
			pagination: true,
		}),
	],
});
