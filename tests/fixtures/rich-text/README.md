# 富文本跨语言夹具

本目录是 `004-rich-text-editor` 的跨语言契约夹具。Vue、Java 和 Playwright 测试都读取同一组
JSON 文件；夹具不在 HTML 中保存可执行标记，避免测试本身成为注入源。

## 目录约定

- `manifest.json`：夹具索引、预期结果、FR/SC 映射和适用验证层。
- `valid/`：应被契约和领域验证器接受的文档。
- `invalid/`：节点、属性或语义非法的文档。
- `limits/`：超过领域上限的输入；JSON Schema 不负责全部上限。
- `images/`：图片引用、合法图片字节和必须拒绝的脚本化图片样例。
- `migration/`：旧纯文本、换行、链接、代码和危险旧数据的迁移输入。
- `security/`：安全模糊语料和粘贴向量。`xss-corpus.json` 由
  `generate-security-corpus.mjs` 生成，执行生成脚本后应保持内容稳定。

## 消费方式

Vue/Vitest 可直接导入 `manifest.json` 和具体 JSON；不得把夹具中的 HTML 字符串插入
`innerHTML`。Java/JUnit 使用 Jackson `ObjectMapper.readTree(Path)` 读取并用网络版 JSON Schema
校验结构，再调用领域验证器检查深度、图片数量、总字符数等跨节点规则。

`pixel.png.base64` 在运行时解码后作为 `image/png` 上传；仓库不保存带元数据的真实用户图片。
`scripted.svg` 是负面样例，只能作为拒绝输入，不得渲染。

## 验证

```powershell
node tests/fixtures/rich-text/validate-fixtures.mjs
node tests/fixtures/rich-text/generate-security-corpus.mjs --check
```

修改夹具时必须同步 `manifest.json`、生成语料和相关测试预期。不要用真实凭据、个人图片或
可访问的内网地址做夹具。
