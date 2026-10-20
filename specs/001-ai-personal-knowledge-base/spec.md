# Feature Specification: AI 个人知识库

**Feature Branch**: `001-ai-personal-knowledge-base`

**Created**: 2026-10-21

**Status**: Draft

**Input**: 用户需要一个支持手动录入、AI 辅助录入、自动归类、文档与截图分析、本地优先搜索及联网搜索的个人知识库网站；网页链接通过 AI 分析页面截图中的可见文字，不得使用爬虫技术。联网搜索拟采用 DeepSeek Search 与 Fetch 能力，具体接入方式在计划阶段确认。补充要求：分类和标签由系统在导入知识时自动生成、自动分配，用户无需手动建立标签；知识检索只使用一个输入框，统一覆盖标题、内容、标签等信息，并需满足大规模个人知识库的检索性能目标。

## Clarifications

### Session 2026-10-21

- Q: 个人知识库应以哪里作为最终数据源，并为删除设置什么处理方式？ → A: 采用单账户私有服务端作为权威数据源；删除先进入有明确期限的回收站，到期永久删除，备份按声明周期淘汰。
- Q: 当系统检测到高置信密钥，或附件无法完成完整扫描时，应如何处理？ → A: 高置信密钥自动脱敏后保存其余内容；无法完成完整扫描的附件整次拒绝，不保存原始附件。
- Q: 自动 AI 处理授权应覆盖到什么范围？ → A: 按外部服务和内容类型分别持久授权；同范围自动处理，切换范围或在内容编辑后重新分析时再次确认。
- Q: 知识点在 AI 处理尚未完成，或互联网结果尚未确认保存时，应以什么状态存在？ → A: 手动和 AI 导入立即创建可检索条目，处理状态作为条目字段；互联网候选确认后才创建知识条目。
- Q: 当系统以后重新分析或归并标签时，用户手动修改过的分类和标签应如何处理？ → A: 用户修改后立即锁定；AI 只补充未锁定或暂定内容，标签归并只对锁定项提出建议。

## User Scenarios & Testing *(mandatory)*

### User Story 1 - 手动收藏并找回知识 (Priority: P1)

用户可以把文字、标题、链接和个人备注手动保存到知识库，并通过统一检索框输入关键词找回知识。

**Why this priority**: 手动录入与本地检索构成最小可用闭环，即使 AI 服务暂时不可用，知识库仍然可以保存和查找内容。

**Independent Test**: 在没有 AI 服务的条件下，创建一条手动知识条目，再通过关键词检索到它，即可独立验证本故事。

**Acceptance Scenarios**:

1. **Given** 知识库为空，**When** 用户填写标题和正文并保存，**Then** 系统立即创建可检索知识条目、自动安排分类处理并显示创建时间。
2. **Given** 已有多条知识，**When** 用户在一个输入框中输入与标题、正文、备注、分类或标签匹配的关键词，**Then** 本地结果按相关度展示并标明匹配字段。
3. **Given** 本地没有匹配结果，**When** 搜索完成，**Then** 页面保留清晰的本地空结果状态，并提供用户主动发起互联网搜索的入口。

---

### User Story 2 - AI 自动归类并生成标签 (Priority: P2)

用户保存知识后，系统自动生成摘要、分类和标签，不需要用户预先建立标签或分类。高置信度结果直接应用，低置信度结果先自动应用为暂定分类并标记待复核，不阻塞入库和检索。

**Why this priority**: 全自动分类和标签是知识库从“收藏夹”升级为可整理知识系统的核心价值，也决定统一检索是否能够覆盖可靠的结构化信息。

**Independent Test**: 在没有任何手工标签和分类的条件下保存一段原始文字，验证系统自动生成摘要、分类和至少一个标签，并允许用户事后纠正。

**Acceptance Scenarios**:

1. **Given** 用户保存一段未分类文字，**When** 系统完成自动整理，**Then** 条目自动获得分类、标签和摘要，用户无需手动建立标签。
2. **Given** AI 分类置信度低于阈值或存在多个相近分类，**When** 自动整理完成，**Then** 系统仍应用暂定分类和标签、标记待复核，并允许用户稍后纠正。
3. **Given** 用户修改、替换或撤销自动生成的结果，**When** 用户保存修改，**Then** 系统锁定用户的最终选择、更新检索信息，并禁止后续自动分析覆盖该选择。
4. **Given** 用户锁定的标签与系统标签可能是近义词，**When** 系统执行标签规范化，**Then** 系统只提出合并建议，未获用户确认前不得更改锁定标签。

---

### User Story 3 - 通过网页、文档和截图采集知识 (Priority: P3)

用户可以提交网页链接、文档或截图，由 AI 提取文字、理解内容并生成可编辑且立即检索的知识条目。

**Why this priority**: 多种采集方式覆盖用户日常收藏的主要来源，但依赖 AI 与外部读取能力，因此排在手动录入和基础归类之后。

**Independent Test**: 分别提交一个公开网页、一个支持的文档和一张包含清晰文字的截图，验证三者均可生成带来源信息的可检索知识条目。

**Acceptance Scenarios**:

1. **Given** 用户提交一个可访问网页链接，**When** 系统接受该导入，**Then** 系统立即创建可检索条目并显示处理状态；获得单页渲染截图后分析可见文字，自动补充摘要、分类和标签，同时保留原始链接和采集时间。
2. **Given** 用户上传受支持的文档，**When** 系统接受该导入，**Then** 条目立即可检索，并在分析完成后自动补充提取文字、摘要、分类和标签。
3. **Given** 用户上传包含文字的截图，**When** 系统接受该导入，**Then** 条目立即可检索，并在分析完成后自动补充识别文字、分类和标签；未检测到密钥时保留原图，检测到密钥时只保留脱敏后的派生图片，无法可靠脱敏时整次拒绝。
4. **Given** 网页无法访问、需要登录或内容无法可靠识别，**When** 处理失败，**Then** 系统说明失败原因，不生成虚构内容，并允许用户重试或改为手动录入。
5. **Given** 网页渲染截图能力不可用，**When** 用户仍希望收藏该网页，**Then** 系统提示用户上传页面截图，而不是改用爬虫抓取。

---

### User Story 4 - 单框本地优先混合检索 (Priority: P4)

用户通过首页唯一的搜索框输入关键词，即可同时匹配标题、正文、备注、分类和标签；系统先检索个人知识库，再按需检索互联网，并可将互联网结果保存到个人知识库。

**Why this priority**: 本地优先检索是产品区别于通用搜索引擎的核心体验，同时需要已有知识条目作为基础。

**Independent Test**: 在同时存在本地与互联网候选结果的场景下，确认本地结果先显示、互联网搜索需用户主动继续，且互联网结果可以保存。

**Acceptance Scenarios**:

1. **Given** 用户从首页唯一搜索框输入搜索词，**When** 搜索开始，**Then** 系统同时匹配标题、正文、备注、分类和标签，优先展示个人知识库结果，并明确区分本地与互联网来源。
2. **Given** 本地结果已显示，**When** 用户选择继续互联网搜索，**Then** 系统展示带来源链接的互联网结果，且不自动发送个人知识库正文。
3. **Given** 用户选择一条互联网结果，**When** 用户确认收入知识库，**Then** 系统立即创建可检索条目，包含来源链接、可见摘要、自动分类和标签，并继续在后台完成整理。
4. **Given** 互联网搜索超时或无结果，**When** 搜索结束，**Then** 本地结果保持不变，并显示可重试的明确状态。

---

### User Story 5 - 双语且安全的个人使用 (Priority: P5)

用户默认以中文使用系统，也可以切换英文界面；个人知识、外部搜索和密钥信息均受到明确保护。

**Why this priority**: 双语体验可以随核心流程逐步完善，但用户知识的隐私与密钥安全是跨阶段发布门槛，必须从 P1 起满足，不能因本故事的优先级推迟。

**Independent Test**: 切换中英文界面，并检查密钥、个人知识和外部搜索行为，确认语言与安全约束均得到满足。

**Acceptance Scenarios**:

1. **Given** 系统首次打开，**When** 用户未设置语言，**Then** 系统以中文展示。
2. **Given** 用户切换为英文，**When** 浏览核心录入、整理和搜索流程，**Then** 关键界面、错误提示和操作标签均提供完整英文表达。
3. **Given** 用户输入的内容检测到高置信密钥，**When** 系统准备保存或发送给 AI，**Then** 系统自动移除该密钥、显示安全提示并保存其余内容；无法完整扫描的附件则整次拒绝。
4. **Given** 用户尚未授权自动 AI 处理，**When** 用户保存手动输入的知识，**Then** 系统说明将发送的数据范围、接收服务和用途，并在用户拒绝后照常保存条目、保持可检索且标记为待分类。
5. **Given** 用户已授权某一外部服务处理文字，**When** 用户随后上传文档或切换外部服务，**Then** 系统在新范围内再次请求授权，未获授权前不发送对应内容。

---

### Edge Cases

- 网页需要登录、受访问限制、内容为空或页面结构无法稳定读取时，系统必须明确说明无法读取，不得通过爬虫或其他自动遍历方式绕过限制。
- 同一网页在采集期间内容发生变化时，系统必须以采集快照为准，并记录采集时间。
- 网页包含多屏内容时，系统可以分析该单页的一个或多个用户请求渲染视图，但不得自动跟随链接、无限滚动或扩展到其他页面。
- 用户提交带凭据、指向本机或私网地址、或试图访问非公开网络资源的链接时，系统必须拒绝处理。
- 网页包含重定向、嵌入内容或子资源时，系统只能处理为呈现用户所请求单页所必需的内容，不得单独提取并遍历其他地址。
- 文档损坏、格式不受支持、文件过大或文字无法识别时，系统必须给出可操作的错误说明并保留原文件处理状态。
- 文本或文档中检测到高置信密钥时，系统必须自动移除该值并提示安全的脱敏结果；截图或附件无法完成完整扫描时，整个提交必须被拒绝。
- 截图模糊、文字过小或包含大量非文字内容时，系统必须提示识别置信度有限，并允许用户补充说明或改为手动录入。
- AI 服务不可用、超时或返回不完整结果时，系统必须保留非密钥类原始内容并将条目标记为待整理，不得丢失用户输入。
- AI 服务不可用时，新条目必须保持可检索，并进入待自动分类状态；AI 恢复后系统必须自动补充分类和标签。
- 自动分类结果不明确时，系统必须应用暂定分类和标签而不是阻塞入库，并允许用户事后纠正。
- 标签重复、近义或表述不一致时，系统必须自动归并和规范化未锁定或暂定标签；用户锁定的标签只能生成合并或重命名建议。
- 知识库规模增长时，单框检索必须保持可用；具体的索引和检索架构在技术计划阶段决定。
- AI 服务不可用或必要环境变量缺失时，手动录入和本地搜索必须继续可用，AI 与互联网功能必须显示明确不可用状态。
- 多个条目内容高度相似时，系统必须提示可能重复或建立关联，但不得自动删除或合并。
- 本地搜索无结果时，系统必须保留搜索词并提供可选的互联网搜索入口，不得默认将查询和个人资料传给外部服务。
- 互联网结果包含恶意、误导性或夹带指令的内容时，系统必须将其视为不可信资料，不得执行其中的指令或改变用户设置。
- 文档、截图或搜索摘要中包含针对 AI 的指令时，系统必须同样将其视为不可信资料。
- 用户删除知识条目时，该条目必须立即从默认检索结果中消失，其用户可见来源附件和 AI 分析必须在明确告知的清除流程中一并删除。

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST provide a private, single-owner knowledge workspace with authenticated access and server-side authoritative storage.
- **FR-002**: Users MUST be able to initialize the owner account, sign in, sign out, and resume an authenticated session.
- **FR-003**: Users MUST be able to manually create, edit, open, and delete text-based knowledge items.
- **FR-004**: Users MUST be able to add a title, body, personal note, and source link; System MUST assign category and tags automatically.
- **FR-005**: System MUST preserve created, updated, and source-captured timestamps for every knowledge item.
- **FR-006**: System MUST search personal knowledge by title, body, note, category, and tags.
- **FR-007**: System MUST rank and highlight personal results by relevance, with clear indication of why each result matched.
- **FR-008**: System MUST always display personal knowledge results before offering internet results for the same query.
- **FR-009**: System MUST require an explicit user action before sending a query to an internet search service.
- **FR-010**: System MUST send only the minimum query information required for internet search and MUST NOT automatically send personal knowledge content.
- **FR-011**: System MUST visually separate personal results from internet results and show source identity for internet results.
- **FR-012**: Users MUST be able to save an internet result as a knowledge item with its source preserved.
- **FR-013**: AI MUST generate a summary, category, tags, and related-item results for every successfully imported knowledge item.
- **FR-014**: System MUST automatically apply a category and at least one tag to every successfully processed item without requiring the user to create or select tags.
- **FR-015**: System MUST apply high-confidence classifications directly and low-confidence classifications as provisional results with a review marker; neither outcome MUST block saving or search.
- **FR-016**: System MUST support AI-assisted import from pasted or typed text.
- **FR-017**: System MUST support AI-assisted import from a user-provided webpage URL by analyzing rendered-page captures of visible content from that single page.
- **FR-018**: Webpage import MUST analyze only the user-requested page and its captured views; it MUST NOT crawl, follow links, enumerate pages, or bulk-collect content.
- **FR-019**: System MUST retain the original URL and capture timestamp for every webpage-derived knowledge item.
- **FR-020**: When rendered-page capture is unavailable, System MUST offer screenshot upload as the fallback and MUST NOT substitute crawling or automated site traversal.
- **FR-021**: System MUST support AI-assisted import from user-uploaded documents.
- **FR-022**: System MUST support AI-assisted import from user-uploaded screenshots; when no secret is detected, it MUST preserve the original image, and when a secret is detected, it MUST preserve only a reliably redacted derivative or reject the submission if redaction cannot be completed.
- **FR-023**: System MUST show processing status and allow failed imports to be retried or cancelled.
- **FR-024**: System MUST preserve non-secret raw user input when AI processing is unavailable or unsuccessful.
- **FR-025**: System MUST detect likely secrets such as API keys, SMTP credentials, passwords, and access tokens before persisting or transmitting content to an AI service.
- **FR-026**: System MUST redact high-confidence detected secrets before persistence or external processing, save the remaining non-secret content, and MUST NOT expose the removed secret in user-visible content or diagnostic messages.
- **FR-027**: System MUST obtain AI, SMTP, and other credentials only from environment variables and MUST NOT store credential values in application data.
- **FR-028**: System MUST treat webpage content, documents, screenshots, extracted text, and search summaries as untrusted data and MUST NOT execute instructions embedded in them.
- **FR-029**: System MUST default to Chinese and provide complete English coverage for primary workflows, errors, security notices, and destructive actions.
- **FR-030**: System MUST keep personal knowledge private by default and MUST NOT provide public sharing or multi-user collaboration in this feature.
- **FR-031**: System MUST allow users to identify whether a knowledge item came from manual entry, URL, document, screenshot, text, or internet search.
- **FR-032**: System MUST provide a clear empty, loading, partial-failure, and retry state for every AI-assisted workflow.
- **FR-033**: The home page MUST provide one prominent search box that starts a personal-first search without requiring navigation to another screen.
- **FR-034**: Before sending manual title, body, note, pasted text, documents, screenshots, or webpage analysis inputs to an AI service, System MUST disclose the data scope, receiving service, content type, and purpose and require authorization for that service and content type.
- **FR-035**: Users MUST be able to open a search result, read the complete saved item, and continue editing it.
- **FR-036**: When an item is deleted, System MUST remove the item, its source attachments, and its AI analysis from active search immediately, place them in a visible recycle bin for a disclosed retention period, and then permanently delete them.
- **FR-037**: System MUST reject webpage URLs containing embedded credentials and URLs targeting local, private, or otherwise non-public network resources.
- **FR-038**: System MUST persist the selected interface language while preserving user content exactly as entered and MUST NOT automatically translate user knowledge when the interface changes.
- **FR-039**: AI-generated summaries and classifications MUST use the current interface language by default unless the user chooses the original content language.
- **FR-040**: Internet search timeout, no-result, partial-failure, and retry outcomes MUST be shown separately from personal search results.
- **FR-041**: Internet results MUST remain transient candidates until the user confirms saving; confirmation MUST create an active knowledge item that preserves the source link and generates category and tags automatically.
- **FR-042**: System MUST check required environment variables at startup, list only missing variable names without values, and keep non-dependent local features available.
- **FR-043**: Secret detection MUST occur before persistence and external transmission; high-confidence secrets MUST be redacted automatically, while content or attachments that cannot be scanned completely MUST be rejected before any persistence.
- **FR-044**: Raw credentials MUST NOT appear in source-controlled files, configuration files, documents, tests, logs, screenshots, build artifacts, or version control.
- **FR-045**: Users MUST be able to correct, replace, or remove automatically generated categories and tags without being required to create or maintain a tag vocabulary; user-edited values MUST become locked against automatic overwrite.
- **FR-046**: System MUST automatically normalize duplicate, synonymous, and inconsistent-generated tags into a reusable vocabulary for unlocked or provisional values, but MUST only propose merges or renames for user-locked item fields.
- **FR-047**: The knowledge search interface MUST provide exactly one text input and MUST query title, body, note, category, and tags together.
- **FR-048**: Users MUST NOT need to choose a separate title, content, category, or tag search mode to find matching knowledge.
- **FR-049**: Items awaiting automatic classification MUST remain searchable and MUST be retried automatically when classification becomes available.
- **FR-050**: A user MUST be able to grant persistent automatic-processing authorization for a specific external service and content type, revoke it at any time, and later authorize classification for individual pending items.
- **FR-051**: Declining AI processing MUST NOT prevent manual saving, opening, editing, deleting, or searching a knowledge item.
- **FR-052**: The authoritative knowledge, attachment, analysis, and search-index data MUST reside in the private server-side store under the single owner account; deletion and backup retention MUST follow the disclosed recycle-bin and expiry policy.
- **FR-053**: Automatically redacted content MUST remain usable as a knowledge item, and the system MUST show a safe notice that redaction occurred without revealing the removed value.
- **FR-054**: If the system cannot complete secret scanning for any submitted content or attachment, it MUST reject the entire submission and MUST NOT persist the unscanned original.
- **FR-055**: System MUST require a new authorization when the external service changes or when a different content type is submitted.
- **FR-056**: System MUST require a new authorization when previously authorized content is edited and the user requests re-analysis.
- **FR-057**: Revoking an authorization MUST stop future transmissions for that scope while preserving already saved knowledge and its current classifications.
- **FR-058**: Manual entries and accepted AI imports MUST create an active, searchable knowledge item immediately, even while classification or extraction is still pending.
- **FR-059**: Processing, extraction, classification, and failure states MUST be stored as knowledge-item attributes and MUST NOT prevent access to already available title, body, note, or source content.
- **FR-060**: Internet search results MUST remain transient candidates until the user confirms saving; confirmation MUST immediately create an active, searchable knowledge item with automatic classification pending or applied.
- **FR-061**: Automatic analysis, retry, and tag normalization MUST update only unlocked or provisional classification fields and MUST NOT overwrite user-locked categories or tags.
- **FR-062**: Category and tag locks MUST apply to the specific fields of a knowledge item, not to shared vocabulary entries; changing one item MUST NOT silently lock or alter the same category or tag on other items.

### Key Entities

- **Knowledge Item**: A saved unit of knowledge with title, body, note, category and tag assignments, source, timestamps, processing status, and active-or-recycled lifecycle state.
- **Source**: The origin of an item, including manual entry, webpage, uploaded document, screenshot, pasted text, or internet search result.
- **Capture Task**: A user-initiated import operation with source input, progress state, result, and retry information.
- **AI Analysis**: Automatically generated summary, category, tags, confidence, related items, review state, and user corrections.
- **Category**: A shared user-visible organizational group generated automatically and reusable across knowledge items; item-specific corrections and lock state belong to Classification Assignment.
- **Tag**: A shared normalized descriptive label generated automatically and reusable across knowledge items; item-specific corrections and lock state belong to Classification Assignment.
- **Classification Assignment**: An item-specific association to a category or tag with confidence, locked-or-provisional state, source, and correction history.
- **AI Processing Authorization**: A per-service and per-content-type permission record with grant, revocation, and scope history.
- **Internet Search Result**: A transient external search candidate that is not part of the knowledge library until the owner confirms saving.
- **Search Session**: One user query with personal results, optional internet results, source separation, and save actions.
- **Owner Account**: The single private user who owns all knowledge, sources, and settings in this feature.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: In a facilitated test with 10 first-time users, at least 9 save a manual knowledge item in under 60 seconds.
- **SC-002**: In a benchmark library of 100,000 items and a documented reference environment, at least 95% of test queries show a first page of at least 20 personal results within 2 seconds, measured from search submission to visible result rendering.
- **SC-003**: At least 85% of a 100-item benchmark receives an accepted, lightly edited, or correctly auto-applied classification.
- **SC-004**: At least 90% of supported text, document, and screenshot benchmark imports produce a searchable knowledge item containing a non-empty title, summary, category, and at least one tag within 2 minutes.
- **SC-005**: 100% of tested webpage imports preserve the requested source URL and do not follow additional links.
- **SC-006**: 100% of tested searches display personal results before internet results.
- **SC-007**: 100% of tested secret-handling scenarios prevent raw credentials from appearing in source-controlled files, configuration, documents, tests, logs, screenshots, build artifacts, saved knowledge, user-visible output, or diagnostic messages.
- **SC-008**: 100% of primary workflows and required error states render complete, understandable Chinese and English interface text.
- **SC-009**: In a facilitated test with 10 users, at least 9 distinguish personal results from internet results without additional explanation.
- **SC-010**: 100% of successfully imported benchmark items receive a category and at least one tag automatically, or enter a provisional classification state, within 2 minutes and without manual tag creation.
- **SC-011**: 100% of benchmark queries targeting title, body, note, category, or tag return the expected item through the single search input without changing search mode.
- **SC-012**: 100% of tested cases where automatic AI processing is declined still save the item, keep it searchable, and mark it as awaiting classification.
- **SC-013**: 100% of deleted test items disappear from active search immediately, remain recoverable during the disclosed recycle-bin period, and are permanently removed after that period and applicable backup expiry.
- **SC-014**: 100% of tested high-confidence secret cases remove the raw secret while preserving non-secret content, and 100% of unscannable attachment cases are rejected without persistence.
- **SC-015**: 100% of tested AI transmissions occur only within an active authorization matching both the external service and content type, and revoking a scope produces no further transmission for that scope.
- **SC-016**: 100% of accepted manual and AI imports are searchable immediately after acceptance, while their processing status may remain pending or failed.
- **SC-017**: 100% of tested re-analysis and normalization runs preserve user-locked categories and tags while updating only unlocked or provisional values.

## Assumptions

- The first release serves one private owner through an authenticated website. The private server-side store is authoritative for knowledge, attachments, AI analysis, and search indexes; the same owner account may access the service across devices.
- Deleted knowledge enters a visible recycle bin with a planned retention period, then becomes permanently deleted. Backups follow a documented expiry schedule and are not used to restore data after the disclosed deletion window.
- “Local-first search” means personal knowledge results are ordered before internet results; it does not by itself require that all data be stored only on the user's device.
- The owner has stable internet access when using AI-assisted import or internet search.
- Initial document support includes common text, PDF, and word-processing formats; initial image support includes common screenshot formats.
- The exact supported file size and retention limits are planning decisions and must be visible to users before upload.
- Internet search and webpage reading use a user-approved external service. The user has named DeepSeek Search and Fetch as the intended capability, while provider selection and integration details are deferred to planning.
- A webpage import is limited to the single page explicitly provided by the user; rendered capture may include multiple user-requested views of that page, but no crawling or automatic link discovery is allowed.
- A high-confidence threshold controls automatic classification; its numeric value is a planning decision and must be evaluated against a benchmark before release.
- Low-confidence results are still applied automatically as provisional classifications; user review is optional and never blocks saving or search.
- Categories and tags are system-managed and user-correctable; manual tag creation and taxonomy administration are not required.
- AI processing authorization is scoped by external service and content type; same-scope saves may proceed automatically, while scope changes or re-analysis after edits require confirmation.
- Manual and AI imports are active knowledge items from acceptance, with processing state represented as an attribute; internet search candidates become knowledge items only after owner confirmation.
- User-edited categories and tags are authoritative and locked; future AI analysis fills only unlocked fields, and normalization may propose but not apply changes to locked values.
- Category and tag locks are item-field scoped; shared vocabulary normalization never changes a locked field without an accepted suggestion.
- Large-library search is validated at 100,000 knowledge items before release, while indexing and retrieval architecture remain planning decisions.
- The planning phase must define the reproducible search benchmark, including query set and distribution, cold/warm cache conditions, reference environment, and concurrency.
- The planning phase must define a benchmark sample and pass threshold for duplicate and synonymous tag normalization.
- Credentials are supplied only through environment variables and are never stored inside knowledge content.
- Knowledge export, mobile applications, browser extensions, public APIs, and automatic scheduled internet ingestion are outside the first feature scope.
- Search history is not persisted by default; only saved knowledge items and their source records are retained.
- User knowledge is not automatically translated when the interface language changes.

## Out of Scope (v1)

- Web crawling, site mirroring, bulk page capture, and automatic link traversal.
- Multi-user workspaces, public sharing, comments, and collaborative editing.
- Native mobile applications and browser extensions.
- Automatic continuous monitoring of external sites or sources.
- Payment, subscription, and billing workflows.
- SMTP-based notifications, unless a future notification feature is separately specified.
- Conversational question answering over the personal knowledge base.
- Manual tag-taxonomy creation and administration workflows.
- Separate title, content, tag, or category search modes and advanced multi-field filter forms.
