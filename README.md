# Zhishiku

AI 个人知识库项目，采用规格驱动开发（Spec-Driven Development，SDD）流程。

## 项目状态

当前处于需求、宪法与实施计划阶段，尚未创建应用代码。

## 核心文档

- [项目协作约定](AGENTS.md)
- [项目宪法](.specify/memory/constitution.md)
- [交互原型](prototype/index.html)
- [功能规格](specs/001-ai-personal-knowledge-base/spec.md)
- [实施计划](specs/001-ai-personal-knowledge-base/plan.md)
- [数据模型](specs/001-ai-personal-knowledge-base/data-model.md)
- [部署共存方案](specs/001-ai-personal-knowledge-base/deployment.md)
- [验证指南](specs/001-ai-personal-knowledge-base/quickstart.md)
- [OpenAPI 契约](specs/001-ai-personal-knowledge-base/contracts/openapi.yaml)

## 交互原型

“识海”前端原型位于 [prototype/index.html](prototype/index.html)，可直接打开，也可在仓库根目录运行：

```powershell
python -m http.server 8088 --bind 127.0.0.1 --directory prototype
```

然后访问 `http://127.0.0.1:8088/`。

## 开发流程

1. `$speckit-specify`：建立或更新功能规格。
2. `$speckit-clarify`：澄清高影响需求歧义。
3. `$speckit-plan`：生成技术计划、数据模型与接口契约。
4. `$speckit-tasks`：生成可执行任务。
5. `$speckit-implement`：开发实现。
6. 测试子智能体独立执行测试与验收。

## 部署约束

- GitHub 是源码、迁移和部署脚本的唯一权威来源。
- 部署必须支持指定 Git tag 或 commit、重复执行、健康检查和失败回滚。
- 知识库部署在现有 `seaunit.site` 的 `/kb` 路径。
- 不得影响现有应用的 `/api`、`/app`、`/bank`、`/pay`、`/apk`、证书或认证配置。
- 所有密钥仅通过服务器环境变量或受管密钥服务提供，不得进入仓库。
