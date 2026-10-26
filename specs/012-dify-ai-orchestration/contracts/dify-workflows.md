# Dify 工作流接口契约

**日期**：2026-10-27

## 通用调用

- **方法**：`POST`
- **路径**：`{SHIHAI_DIFY_BASE_URL}/workflows/run`
- **认证**：`Authorization: Bearer <对应工作流 API Key>`
- **内容类型**：`application/json`
- **外部用户**：`user` 字段使用当前账号 UUID，不使用邮箱。
- **阻塞模式**：`response_mode=blocking`
- **流式模式**：`response_mode=streaming`

## 知识整理工作流

**名称**：`shihai-knowledge-organizer`

请求输入：

```json
{
  "operation": "knowledge_organize",
  "sourceKind": "webpage",
  "content": "<已授权并脱敏的文本>"
}
```

结束节点必须输出：

```json
{
  "result": "{\"items\":[{\"title\":\"...\",\"body\":\"...\",\"summary\":\"...\",\"category\":\"...\",\"tags\":[\"...\"]}]}"
}
```

后端只接受 `workflow_finished.data.status=succeeded` 且 `data.outputs.result` 非空的响应。

## 小识对话工作流

**名称**：`shihai-xiaoshi-chat`

请求输入：

```json
{
  "operation": "knowledge_answer",
  "question": "<已脱敏问题>",
  "history": "<已脱敏短历史>",
  "evidence": "<已脱敏候选证据>"
}
```

各操作输出：

| `operation` | 输出格式 |
|---|---|
| `knowledge_answer` | JSON：`answer`、`usedSourceIds`、`needsInternet` |
| `general_answer` | JSON：`answer`、`needsInternet` |
| `query_plan` | JSON：`queries`，1–8 条 |
| `internet_answer` | JSON：`answer`、`usedSourceIds`、`needsInternet` |
| `stream_knowledge` | 纯文本，引用使用 `[1]`、`[2]`；证据不足输出 `INSUFFICIENT_EVIDENCE` |
| `stream_general` | 纯文本；需要实时信息输出 `NEEDS_INTERNET` |
| `stream_internet` | 纯文本，引用使用 `[1]`、`[2]`；证据不足输出 `INSUFFICIENT_EVIDENCE` |

流式事件：

- 只消费 `event=text_chunk` 的 `data.text`。
- 完成时校验 `event=workflow_finished` 且状态为成功。
- 忽略无关节点事件，不把原始事件透传前端。
- `event=error`、连接中断、状态失败或空文本均转换为识海统一 AI 错误。

## 安全规则

- 不向 Dify 发送原始 URL、Cookie、文件名、文件二进制、账号邮箱、密码、支付信息或未脱敏文本。
- Dify 返回内容视为不可信模型输出，后端必须执行长度、结构、URL、引用和账号边界校验。
- 工作流密钥按应用独立，不与其他项目共用。
