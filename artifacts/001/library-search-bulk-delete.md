# 知识库列表检索与批量删除验证

日期：2026-10-22

执行角色：产品、开发、测试子智能体。

实现提交：`cb59600`

## 范围

- 知识库列表内单框检索，由服务端覆盖标题、正文、备注、分类和标签，并返回命中字段。
- 来源筛选与检索组合、清除检索、快速连续搜索的旧响应丢弃。
- 每行删除、表头三态全选、移动端全选/取消全选、批量删除和选择清理。
- 批量回收的账号隔离、稳定 UUID 排序、全有或全无事务与持久化幂等重试。
- 桌面、平板、320/360/390/430px 手机、44px 触控区、软键盘和安全区布局。

## 接口与数据

- `POST /api/v1/knowledge-items/bulk-recycle`
- 请求：`{"itemIds":["uuid"]}`，去重后 1–100 条。
- 响应：`{"recycled":2}`。
- 必填请求头：`Idempotency-Key`。
- 删除只进入 30 天回收站，不执行永久清除；成功条目立即退出活动列表和搜索。

## 验证证据

| 检查 | 结果 |
|------|------|
| 后端完整 Maven 测试 | `kb-core 22`、`kb-app 218`、browser worker `1`，共 241 项通过 |
| 列表专项 Playwright | 6 项通过 |
| 前端完整 Playwright | 48 项通过，2 项真实外部环境用例跳过 |
| Vitest | 48 项通过 |
| 前端 lint、typecheck、生产构建 | 通过 |
| 原型 JavaScript 语法与 Taste app lint | 通过，0 FAIL / 0 WARN |
| 前端 Taste app lint | 0 FAIL；4 个既有浮层阴影 WARN |
| 原型浏览器交互 | 检索从 12 条筛至 2 条；取消单删保持 12 条；批量确认后变 10 条 |
| 原型手机 390px | 页面级横向溢出 0；复选框和删除按钮均为 44×44px |
| `git diff --check` | 通过 |

截图位于 `frontend/kb-web/test-results/`：

- `library-desktop.png`
- `library-tablet.png`
- `library-mobile.png`
- `prototype-library-desktop.png`
- `prototype-library-mobile.png`

## 覆盖

- FR-003、FR-006、FR-007、FR-035、FR-036、FR-047、FR-075 至 FR-078。
- SC-013、SC-019、SC-020。
- 003 FR-049 至 FR-051、SC-017。

## 剩余风险

- 本机 Docker 引擎不可用，Testcontainers PostgreSQL 用例执行 0 项；真实并发事务和数据库回滚仍需在
  CI 或具备 Docker 的环境运行。
- 当前列表和搜索一次最多返回 100 条，尚无游标分页；来源计数在超过 100 条时可能不完整。
- 真实 iOS Safari、Android Chrome、软键盘、物理安全区和屏幕阅读器尚未执行。
- 本轮未连接生产服务，未执行线上发布和拾题零影响复核。

## 回滚

回滚前端列表交互、批量接口和对应文案即可；不存在数据库迁移，已移入回收站的数据可按既有恢复流程
处理。回滚不会永久删除知识或修改支付、点数、AI 授权和生产环境变量。
