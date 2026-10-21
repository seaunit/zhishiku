# 邮箱验证码注册验收

**日期**：2026-10-22

**治理版本**：3.0.0

## 变更结果

- 注册页不再显示或校验图形验证码。
- 注册发码请求只提交邮箱地址。
- 登录仍要求服务端缓存的一次性图形验证码。
- 同一邮箱两次成功发码之间至少间隔 60 秒，环境配置不能低于该下限。
- 邮箱验证码仍为 6 位随机数字、哈希保存、一次性使用、默认 10 分钟过期、最多尝试 5 次。
- 邮箱全局唯一、来源 IP 频控和临时邮箱域名拒绝保持不变。

## 接口

```http
POST /kb/api/v1/auth/register/email-code
Content-Type: application/json

{"email":"owner@example.com"}
```

响应仍返回 `expiresInSeconds` 和 `resendAfterSeconds`；生产环境不返回验证码明文。

## 验收证据

| 检查 | 结果 |
|------|------|
| 注册页图形验证码控件 | 0 个 |
| 登录页图形验证码 | 保留并可用 |
| 59 秒内再次发码 | `RATE_LIMITED` |
| 60 秒后再次发码 | 成功 |
| 注册页 Taste 1440/768/390 | 0 FAIL |
| 后端测试 | 111 项通过 |
| 前端 Vitest | 41 项通过 |
| 前端 E2E | 20 项通过 |
| 前端 lint/typecheck/build | 通过 |

视觉证据位于 `.taste-skill/email-only-registration/`。
