# 富文本 quickstart 执行报告

**功能**：`004-rich-text-editor`

**报告日期**：2026-10-22

**执行环境**：本机全新独立 PostgreSQL 数据库、local Spring Boot profile 和 Vite 前端。

**当前状态**：基础编辑、详情、搜索、迁移和恢复链路已执行；图片原生扫描、真实 SMTP/AI、
002 点数账本、真实设备和 10 人研究仍未具备外部条件。

## 环境信息

| 项目 | 实际值 |
| --- | --- |
| Git commit/tag | `4ee4115`；尚未创建部署 tag |
| 操作系统 | Windows，PowerShell |
| Java 17 版本 | 17.0.18 |
| Node/pnpm 版本 | Node 24.14.0；pnpm 10.33.0 |
| PostgreSQL 版本/独立数据库 | PostgreSQL 18.1；`zhishiku_004_test_20261022` |
| S3 兼容存储 | 本地文件适配器；未连接真实 S3 |
| 浏览器与版本 | Playwright + Chrome，桌面 1280×900；Taste 多视口 |
| AI/SMTP 测试凭据来源 | 环境变量或受管密钥，不记录值 |
| 测试图片和恶意 HTML 来源 | `tests/fixtures/rich-text/` |

## 启动记录

```powershell
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local,worker

cd ../frontend/kb-web
pnpm install --frozen-lockfile
pnpm dev
```

| 检查 | 结果 | 证据或错误 |
| --- | --- | --- |
| 迁移执行成功 | 通过 | Flyway `0001 → 0004 → 0005`，全部 `success=true` |
| `/kb/` 可打开 | 通过 | Vite `http://127.0.0.1:4173/kb/` |
| Worker 可租用任务 | 未执行 | 当前 Spring Boot 本地进程未启动独立 Worker |
| 对象存储可写且私有 | 部分通过 | 本地文件适配器可用；真实 S3 未连接 |

## 场景记录

| quickstart 场景 | 状态 | 结果与证据 |
| --- | --- | --- |
| 1. 基础富文本 | 通过 | 两段正文经 Vue → Spring 写入独立 PostgreSQL；刷新详情后文字一致；格式矩阵 6 项 Vitest 通过 |
| 2. 正文图片 | 阻塞 | 本机无 libvips/Tesseract；上传链路按失败关闭，真实扫描未执行 |
| 3. 截图与文档入口边界 | 部分通过 | 剪贴板粘贴、文档文件入口和图片选择 E2E 通过；真实相册/相机未执行 |
| 4. 密钥与全次拒绝 | 阻塞 | 缺少真实脱敏引擎和密钥扫描服务 |
| 5. 外部粘贴与来源追踪 | 部分通过 | 浏览器安全转换、来源请求、服务端来源策略与转换报告测试通过；真实 Word/Google Docs 剪贴板未执行 |
| 6. 详情页 | 通过 | 实际保存条目刷新后保持两段正文；表格、图片灯箱和 Escape 关闭 E2E 通过 |
| 7. 搜索与 AI | 部分通过 | 实际搜索 `PostgreSQL` 命中 1 条；AI 文本投影和图片去重单测通过；真实 AI/SMTP/点数账本未执行 |
| 8. 双标签页冲突 | 部分通过 | 冲突策略 4 项单测和浏览器三出口 E2E 通过；未使用两个真实浏览器会话 |
| 9. 移动端与无障碍 | 部分通过 | 320/360/390/430、横屏、200% 字体和键盘自动化通过；真实软键盘/屏幕阅读器未执行 |
| 10. 旧数据迁移与来源 | 部分通过 | 旧文本幂等转换、链接和来源单测通过；未启动独立迁移 Worker |
| 11. 清理与恢复 | 部分通过 | 生命周期策略、引用清理和上传恢复测试通过；未连接真实对象存储清理 Worker |

## 本机自动化基线

| 命令 | 最近结果 |
| --- | --- |
| `node tests/fixtures/rich-text/validate-fixtures.mjs` | 通过 |
| `node tests/fixtures/rich-text/generate-security-corpus.mjs --check` | 通过 |
| `backend/mvnw.cmd -pl kb-app -am test` | kb-core 18 项、kb-app 88 项，共 106 项通过 |
| `pnpm test` | 41 项通过 |
| `pnpm exec playwright test e2e/app.spec.ts` | 20 项通过 |
| `pnpm exec playwright test --config ../../tests/playwright.config.ts --workers=1` | 15 通过，2 个生产门禁用例按环境变量跳过 |
| 生产零影响检查 | 公开面 1 项通过；生产主机内部 sidecar `/health` 通过 |

## 缺陷与阻塞

| ID | 级别 | 现象 | 影响场景 | 处理状态 |
| --- | --- | --- | --- | --- |
| IMG-01 | P0 | 本机缺少 libvips/Tesseract | 2、4、11 | 部署环境安装并执行真实扫描/脱敏 |
| BILL-01 | P0 | 002 计费和点数账本尚未实现 | 7 | 完成 002 后执行真实报价、预留、结算和释放 |
| UX-01 | P1 | 没有 10 名真实参与者 | 可用性 | 按 `tests/usability/richtext.md` 执行并填写观察数据 |

## 签字

- 产品验收：部分通过，外部条件项待验收
- 开发确认：已确认
- 测试确认：自动化范围已确认
- 回滚负责人：项目所有者

## 结论

基础新环境链路已经执行，但本报告不是完整生产验收。只有补齐图片原生工具、002 计费、
真实设备和 10 人研究后，才能把整体结论改为“通过”。
