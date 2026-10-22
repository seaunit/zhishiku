# 截图 OCR 与 AI 拆分验证记录

**日期**：2026-10-22

**功能**：`008-screenshot-ai-import`

**状态**：本地 P0/P1 自动化验证通过；生产真实 OCR 和部署验收待完成

**Git commit**：未提交

## 已通过

| 检查 | 结果 | 证据 |
|------|------|------|
| 截图入口唯一性 | 通过 | 截图粘贴区 `input[type=file]` 数量为 0 |
| PNG 安全检查与 OCR 调用 | 通过 | `ScreenshotTextExtractorTest` |
| 坏图/空图/超限/超长/无文字 | 通过 | `SCREENSHOT_IMAGE_INVALID` 等失败码测试 |
| 多知识拆分 | 通过 | `ScreenshotImportServiceTest` 创建两条截图知识 |
| 无图片附件 | 通过 | 服务只创建正文、来源和备注；测试断言正文不含 `image` 节点或 `assetId` |
| 密钥脱敏 | 通过 | AI 输入含 `[REDACTED]`，响应 `redactionApplied=true` |
| 同意和计费门禁 | 通过 | 未同意和未计费均在 OCR 前拒绝 |
| Controller | 通过 | 认证、CSRF、Multipart、成功和同意错误 |
| 截图文字提取 | 8 项通过 | `ScreenshotTextExtractorTest` |
| 截图导入服务 | 6 项通过 | `ScreenshotImportServiceTest` |
| 后端全量测试 | `kb-app` 175 项通过；worker 1 项通过 | Maven reactor |
| Spotless | 通过 | Maven Spotless check |
| 应用与 worker 构建 | 通过 | `kb-app.jar` 128,715,804 字节，worker jar 同时构建 |
| 前端 lint/typecheck | 通过 | `pnpm lint`、`pnpm typecheck` |
| Vitest | 47 项通过 | `pnpm test` |
| 前端生产构建 | 通过 | `pnpm build` |
| E2E | 34 项通过 | `pnpm exec playwright test e2e/app.spec.ts --workers=1` |
| 截图移动端 | 1 项通过 | `screenshot-import-taste.spec.ts` 覆盖 1440/768/430/390/320 |
| YAML 和原型语法 | 通过 | 主/008 OpenAPI、application.yml、`node --check` |

## 关键证据

- 前端截图 E2E 断言只调用 `/api/v1/screenshot-imports`，不调用 rich-text 图片上传或普通知识创建。
- 截图临时文件在 OCR 后删除，不进入对象存储。
- 知识来源为 `screenshot`，正文由 AI 拆分结果生成，状态为 `succeeded`。
- 共享 `TesseractOcrService` 同时服务网页截图与剪贴板截图，并排空大输出。

## 未完成或外部条件

- 本机未安装 Tesseract，自动化测试使用 Mock OCR；真实中文截图 OCR 必须在部署环境验收。
- 未执行生产部署、真实 DeepSeek 调用和原应用零影响检查。
- 生产未计费开关保持 false，002 点数门禁接入前返回 `402 AI_BILLING_REQUIRED`。
- WebP/HEIC、多截图和公式/表格增强属于 P2。
- 本机无 Docker，Testcontainers PostgreSQL 用例未启动。
