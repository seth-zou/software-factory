# Agent 测试与 Review 规范

> 通用的 E2E 执行效率、用例编写与提交前自审规范。仓库事实见 [../AGENTS.md](../AGENTS.md)。

## 1. 环境与依赖前置

- 缺 `node_modules` 时先装依赖（`npm ci` / `npm install`），再编译或类型检查。
- 直接使用 `playwright.config.ts` 的默认配置，不在命令行传 `--reporter`、`--viewport` 等覆盖参数，一次执行同时拿到终端输出和 HTML 报告。既定默认值：
  - reporter：`html`（输出到默认 `playwright-report/`）+ `list`。
  - 视口 Desktop Chrome 1280x720；`timeout` 30s、`expect.timeout` 10s。
  - 浏览器：`PLAYWRIGHT_CHROMIUM_PATH` 指向的可执行文件，否则回退 `/opt/chromium.org/chromium/chrome`。
  - `webServer` 自动执行 `npm run dev` 并探测 `http://localhost:5173`，本地已在跑时复用。
- **禁止执行 `npx playwright install chromium`（含 `playwright install` / `--with-deps`）**：浏览器由环境预装，配置已通过 `executablePath` 指定。若报“浏览器不存在”，先定位系统 Chrome（如 `/usr/bin/google-chrome`、`/opt/chromium.org/chromium/chrome`），用 `PLAYWRIGHT_CHROMIUM_PATH=<路径> npx playwright test` 指定；这是环境路径问题，不靠下载解决。

## 2. 断言编写预防清单

- **CSS 过渡/动画**：全局有 250ms `transition`（`src/index.css`）。断言 `computed style` 前等待至少 `transition-duration + 100ms`；断言 class/可见性优先用自动等待，不依赖过渡时长。
- **响应式断点**：`lg:hidden` / `md:flex` 元素要确认在默认 1280x720 下可见（移动端底部导航在该视口隐藏、桌面导航显示）。需验证移动端布局时显式 `setViewportSize` 到断点以下（如 390x844）。
- **异步渲染**：用 `toBeVisible` / `toBeAttached` 等自动等待，不用固定 `sleep` / `waitForTimeout`。

## 3. 执行与修复策略

- 首轮跑全部受影响用例，收集所有失败后一次性批量修复，不逐个修复逐个重跑。
- 多个受影响 spec 必须在**单次命令**里一起跑（如 `npx playwright test e2e/a.spec.ts e2e/b.spec.ts`），避免重复启动 webServer/浏览器；只改单个 spec 才指定单文件。
- 修复后用 `--grep`/`-g` 只跑失败用例验证，通过后再全量确认一次；全量通过即上传报告，不再重复执行。
- 轮询外部状态（CI/部署）设固定间隔和上限（如每 30s、最多 5 分钟），命中终态（success/failure/cancelled）立即停止。

## 4. 产物与日志读取

- 不 `cat`/`head` 大体量或内嵌资源的文件：`playwright-report/index.html` 内嵌 base64 截图/视频，`node_modules/`、`dist/assets/`、图片、视频、压缩包同样不读入上下文。
- PASS/FAIL 以 `list` 终端输出为准；HTML 报告仅用于上传共享和人工复核，报告缺失不影响结论，也**不构成重跑理由**（先查 `playwright-report/` 等实际路径，确需重跑需在交付说明中写明原因）。
- 从 HTML/JSON 产物取结构化信息时用 `rg -n` 定位或脚本（`node -e`/`python3`）只输出摘要；截图/录屏用报告或 trace viewer 查看。
- 构建/部署日志只过滤关键段：`rg -n -C 'error|failed|exit code'`。

## 5. E2E 用例编写

- 用例按需求编号命名（`REQ-001` / `NFR-001`），`test.describe` 标注功能域，单用例只验证一个验收点；正反路径都要覆盖。
- 跨视图的重复点击步骤收敛到导航辅助函数（参考 `goToBrothSpec`），用例体只留与断言相关的操作。
- 定位优先级：`getByRole`（带可访问名）> `getByText`/`getByLabel` > 语义 locator（`article`、`heading`）> 兜底 CSS；避免依赖易变 DOM 层级，确需 nth 时注释原因。
- 断言业务结果而非实现：可见文案、Tailwind 业务 class（如 `border-chili-500`）、元素可见性，不断言内部状态字段。
- 用例独立、可乱序执行（应用为内存态，刷新即重置）；点击被设计上的遮罩拦截时可 `force: true` 并注释。

## 6. 提交前自审与验证选择

- 是否覆盖需求正反两类路径（如超级辣弹提示、其它辣度不弹）。
- 改样式/文案时是否破坏既有基于 class/文案的 E2E 断言；zh/en 文案是否同步；新增 localStorage key 是否遵循 try/catch 降级。
- 是否触及 [../AGENTS.md](../AGENTS.md) 的“不要做”；目录结构、色板或约定变化时同步更新文档。
- 按报错来源选命令：类型/构建用 `npm run build` 或 `npx tsc -b --noEmit`；静态规范用 `npm run lint`；交互回归用 `npx playwright test`；不用构建命令排查 E2E 行为问题。
- 无依赖的只读命令（`git status`、`git log`、`rg -n`、读配置）合并到同一轮并行调用，减少串行往返。
