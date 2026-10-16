# 沸点火锅点单演示

一个面向火锅门店场景的移动端点单与履约流程演示项目，用于展示从绑定餐桌、多人点餐到订单履约和结账支付的完整体验。

> 本项目为概念演示，不代表正式产品。

## 功能介绍

- 绑定餐桌并进入点餐流程
- 按分类浏览、搜索和选择菜品
- 配置菜品规格、口味及下单人
- 多人协同点餐与购物车管理
- 查看订单制作及上菜进度
- 呼叫加汤、饮料、餐具和结账服务
- 模拟菜品售罄、服务响应及履约状态
- 模拟结账和支付成功流程
- 支持中英文切换和老人模式

## 技术栈

- React 18
- TypeScript
- Vite
- Tailwind CSS
- Radix UI
- i18next
- Playwright

## 环境要求

- Node.js 18 或更高版本
- npm 9 或更高版本

## 本地开发

安装依赖：

```bash
npm install
```

启动开发服务：

```bash
npm run dev
```

服务默认运行在 `http://localhost:5173`。如果该端口已被占用，Vite 会自动选择其他可用端口。

## 常用命令

```bash
# 启动开发服务
npm run dev

# 执行代码检查
npm run lint

# 构建生产版本
npm run build
```

生产构建产物会生成在 `dist/` 目录。

## 项目结构

```text
.
├── e2e/                    # Playwright 端到端测试
├── server/                 # 演示服务端
├── src/
│   ├── assets/             # 图片资源
│   ├── components/         # 页面及通用组件
│   ├── data/               # 菜单等演示数据
│   ├── hooks/              # React Hooks
│   ├── state/              # 订单状态管理
│   ├── App.tsx             # 应用入口组件
│   ├── i18n.ts             # 中英文文案
│   └── index.css           # 全局样式
├── index.html
├── tailwind.config.js
└── vite.config.ts
```

## 预览模式

访问以下地址可以直接进入已绑定餐桌并带有购物车数据的菜单预览：

```text
http://localhost:5173/?preview=menu
```

实际端口以 Vite 启动日志为准。
