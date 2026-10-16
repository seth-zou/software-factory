# GitHub Pages 发布核验

> 低频 runbook。职责只到“构建部署成功、站点可访问”，不重复 lint/build/E2E 全量验收（那些结论在上游环节取得，直接采信）。仓库事实见 [../AGENTS.md](../AGENTS.md)。

## 背景

- 工作流名 `Deploy GitHub Pages`（`.github/workflows/deploy-pages.yml`），push 到 `main` 触发；只执行 `npm ci` + `npm run build` 后上传 `dist`，CI 不跑 E2E。
- 项目级 Pages 站点：`https://<owner>.github.io/<repo>/`（Vite `base: './'`，资源为相对路径）。

下文中 `<owner>`/`<repo>` 替换为实际仓库归属和名称；API 为公开仓库只读查询，未鉴权即可。

## 步骤

1. 确认触发提交已在 `origin/main`（与第 2、4 步可并行）：
   ```bash
   git fetch origin main --quiet && git log --oneline origin/main -3
   ```
2. 查询 `main` 最新部署 run：
   ```bash
   curl -fsSL "https://api.github.com/repos/<owner>/<repo>/actions/workflows/deploy-pages.yml/runs?branch=main&per_page=1" \
     | jq '.workflow_runs[0] | {id, status, conclusion, head_sha, html_url}'
   ```
   判定：`status=completed` 且 `conclusion=success` 为成功；`queued`/`in_progress` 则每 30s 轮询、上限 5 分钟；`failure`/`cancelled` 为失败。
3. 失败时查失败 job 与步骤，只拉关键日志：
   ```bash
   curl -fsSL "https://api.github.com/repos/<owner>/<repo>/actions/runs/<run_id>/jobs" \
     | jq -r '.jobs[] | select(.conclusion!="success") | .name, (.steps[]|select(.conclusion=="failure")|.name)'
   ```
4. 成功后验证站点可访问：`curl -fsSI "https://<owner>.github.io/<repo>/" | head -5`，期望 `HTTP/2 200`；CDN 刚部署可能短暂 404，间隔重试 2-3 次。
5. 可选功能上线确认：从首页 HTML 取 JS 资源名后 `curl -fsSL <js 地址> | rg -o "沸点|超级辣" | head`，确认线上含关键功能标记，不把整包读入上下文。

## 完成条件

workflow run `completed/success` 且 Pages URL 返回 200，输出可访问链接；失败或超时则记录 run 链接、失败 job/步骤和处理建议。
