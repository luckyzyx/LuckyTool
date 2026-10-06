import { defineConfig } from 'vitepress'

// https://vitepress.dev/reference/site-config
export default defineConfig({
  lang: 'zh-CN',
  title: 'LuckyTool',
  description: 'ColorOS 系统增强模块使用文档',
  cleanUrls: true,
  lastUpdated: true,
  themeConfig: {
    nav: [
      { text: '指南', link: '/guide/', activeMatch: '/guide/' },
      { text: '更新日志', link: '/changelog' }
    ],
    sidebar: [
      {
        text: '指南',
        items: [
          { text: '简介', link: '/guide/' },
          { text: '安装与激活', link: '/guide/install' },
          { text: '功能列表', link: '/guide/features' },
          { text: '常见问题', link: '/guide/faq' }
        ]
      },
      {
        text: '其它',
        items: [
          { text: '更新日志', link: '/changelog' }
        ]
      }
    ],
    outline: { level: [2, 3], label: '本页目录' },
    docFooter: { prev: '上一页', next: '下一页' },
    lastUpdated: { text: '最后更新' },
    darkModeSwitchLabel: '外观',
    sidebarMenuLabel: '菜单',
    returnToTopLabel: '返回顶部',
    search: {
      provider: 'local'
    },
    footer: {
      message: '基于 <a href="https://vitepress.dev/">VitePress</a> 构建',
      copyright: 'Copyright © 2026 luckyzyx'
    }
  }
})