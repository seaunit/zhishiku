# 富文本性能基线报告

**日期**：2026-10-22

**测试**：`tests/performance/richtext/richtext.spec.ts`

**环境**：Windows、Node 24、Playwright Chromium、固定 API 响应、本地 Vite。

## 命令

```powershell
pnpm --dir frontend/kb-web exec playwright test --config ../../tests/playwright.config.ts performance --workers=1
```

## 最近结果

| 场景 | 预算 | 实测 | 结果 |
| --- | ---: | ---: | --- |
| 200,000 字符详情首次可读 | 15,000 ms | 495 ms | 通过 |
| 20 张图片详情完成 | 15,000 ms | 346 ms | 通过 |
| 390px 视口输入 200,000 字符 | 15,000 ms | 219 ms | 通过 |
| 手机输入后页面横向溢出 | 0 | 0 | 通过 |

原始结果：

```text
.runtime/rich-text-tests/results/*/performance/metrics.json
```

## 预算覆盖

可通过环境变量收紧或放宽阈值：

```powershell
$env:SHIHAI_PERF_DETAIL_200K_MS='15000'
$env:SHIHAI_PERF_20_IMAGES_MS='15000'
$env:SHIHAI_PERF_MOBILE_INPUT_200K_MS='15000'
```

## 限制

- 本测试测量浏览器渲染和编辑器输入，不包含 PostgreSQL 查询、S3 下载、Tika/libvips、AI 或网络抖动。
- 20 张图片使用本地固定 PNG 响应，不是 50 MiB 最坏负载。
- 这些数值适合作为开发回归门禁，不应当作为生产容量承诺；生产容量应另做 API p95、数据库
  `EXPLAIN ANALYZE`、对象存储吞吐和移动网络基准。
