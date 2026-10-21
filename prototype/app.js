const sourceMeta = {
  manual: { labelZh: "手动", labelEn: "Manual", icon: "icon-file" },
  webpage: { labelZh: "网页", labelEn: "Web", icon: "icon-link" },
  document: { labelZh: "文档", labelEn: "Document", icon: "icon-file" },
  screenshot: { labelZh: "截图", labelEn: "Screenshot", icon: "icon-image" },
};

const items = [
  {
    id: "k01",
    title: "深海工作法：用三层归档减少认知噪音",
    summary: "将即时灵感、近期项目与长期资料分层保存，避免所有内容挤在同一个收藏池里。",
    body: [
      "表层只保留未来七天需要继续处理的内容，中层放正在推进的项目，深海保存低频但重要的资料。",
      "分类不是为了建立一套永久正确的目录，而是让下一次检索更容易发生。",
    ],
    category: "产品设计",
    tags: ["知识管理", "分类", "工作流"],
    source: "webpage",
    sourceLabel: "producttalk.cn",
    status: "done",
    updated: "今天 08:42",
    depth: "surface",
    confidence: 0.94,
  },
  {
    id: "k02",
    title: "Agentic RAG 的检索策略笔记",
    summary: "从一次查询扩展、召回与重排，观察智能检索如何降低漏检率。",
    body: [
      "单一向量召回在长文档和多义词上容易出现漏检。",
      "更稳妥的组合是先做查询扩展，再用关键词和向量共同召回，最后让模型按任务目标重排。",
    ],
    category: "人工智能",
    tags: ["RAG", "检索", "Agent"],
    source: "document",
    sourceLabel: "agentic-rag-notes.pdf",
    status: "review",
    updated: "昨天 21:16",
    depth: "middle",
    confidence: 0.62,
  },
  {
    id: "k03",
    title: "把网页保存为可检索知识的最小路径",
    summary: "用户只提交一个链接，系统截图、识别可见文字并自动分类，不遍历其他页面。",
    body: [
      "网页导入只处理用户指定的单页。",
      "系统先获得渲染截图，再进行 OCR、密钥扫描与自动归类；截图能力失败时回退到用户粘贴剪贴板截图。",
    ],
    category: "工程实践",
    tags: ["网页采集", "OCR", "安全"],
    source: "webpage",
    sourceLabel: "docs.seaunit.site",
    status: "processing",
    updated: "昨天 17:08",
    depth: "surface",
    confidence: 0.5,
  },
  {
    id: "k04",
    title: "产品访谈：知识工作者如何整理收藏",
    summary: "六位受访者都把收藏当作临时缓冲区，真正困难的是再次找到并继续使用。",
    body: [
      "受访者很少主动维护标签，更愿意在搜索结果里纠正分类。",
      "自动打标签只有和检索、重访产生关系时才有价值。",
    ],
    category: "研究",
    tags: ["访谈", "知识工作", "用户研究"],
    source: "document",
    sourceLabel: "interviews-2026-10.md",
    status: "done",
    updated: "10 月 19 日",
    depth: "middle",
    confidence: 0.89,
  },
  {
    id: "k05",
    title: "截图中的文字如何被安全识别",
    summary: "OCR 之后先做完整密钥扫描，高风险区域必须像素级脱敏后才能保存派生图片。",
    body: [
      "原图不应在扫描完成前进入持久化存储。",
      "无法可靠定位和脱敏敏感区域时必须拒绝整次提交。",
    ],
    category: "工程实践",
    tags: ["截图", "OCR", "脱敏"],
    source: "screenshot",
    sourceLabel: "security-flow.png",
    status: "done",
    updated: "10 月 18 日",
    depth: "deep",
    confidence: 0.91,
  },
  {
    id: "k06",
    title: "Meilisearch 中文检索测试记录",
    summary: "在短词、混合语言与标签命中场景中记录排序差异，并保留 PostgreSQL 兜底路径。",
    body: [
      "中文短查询需要验证分词、高亮和字段权重。",
      "索引不可用时，刚保存的内容仍需通过受限兜底检索找到。",
    ],
    category: "工程实践",
    tags: ["搜索", "Meilisearch", "中文"],
    source: "document",
    sourceLabel: "search-benchmark.xlsx",
    status: "done",
    updated: "10 月 17 日",
    depth: "deep",
    confidence: 0.95,
  },
  {
    id: "k07",
    title: "一次关于自动标签的评审",
    summary: "标签由系统自动生成和归并，但用户修改后的条目字段必须锁定。",
    body: [
      "自动分类的价值是减少操作，而不是隐藏决策。",
      "低置信度结果先暂定应用，并保留清晰的复核入口。",
    ],
    category: "产品设计",
    tags: ["标签", "评审", "AI"],
    source: "manual",
    sourceLabel: "手动录入",
    status: "done",
    updated: "10 月 16 日",
    depth: "middle",
    confidence: 0.88,
  },
  {
    id: "k08",
    title: "DeepSeek Search / Fetch 交互草稿",
    summary: "互联网搜索必须由用户主动触发，结果保持临时状态，确认后才进入知识库。",
    body: [
      "本地结果始终先出现，失败状态不能遮挡已有结果。",
      "外部结果只携带查询词和来源链接，不发送个人知识正文。",
    ],
    category: "人工智能",
    tags: ["DeepSeek", "Search", "隐私"],
    source: "webpage",
    sourceLabel: "api-docs.deepseek.com",
    status: "review",
    updated: "10 月 15 日",
    depth: "middle",
    confidence: 0.58,
  },
  {
    id: "k09",
    title: "设计系统里的蓝色层级",
    summary: "深海蓝不依赖渐变，而用连续色阶、低饱和边框和单一青色强调建立深度。",
    body: [
      "最低层使用近黑蓝，工作表面提高一档亮度，浮层再提高一档。",
      "强调色只出现在数据、焦点与一条选择线上。",
    ],
    category: "设计",
    tags: ["色彩", "界面", "设计系统"],
    source: "screenshot",
    sourceLabel: "deep-blue-tokens.png",
    status: "done",
    updated: "10 月 14 日",
    depth: "deep",
    confidence: 0.93,
  },
  {
    id: "k10",
    title: "如何判断一条知识值得收藏",
    summary: "能被再次使用、能改变现有判断、或能连接已有知识的内容，才值得进入长期层。",
    body: [
      "收藏不是囤积。每条知识至少应该在未来某次检索中拥有明确用途。",
    ],
    category: "阅读摘录",
    tags: ["判断", "收藏", "阅读"],
    source: "manual",
    sourceLabel: "阅读笔记",
    status: "done",
    updated: "10 月 13 日",
    depth: "deep",
    confidence: 0.86,
  },
  {
    id: "k11",
    title: "海图：知识分类的深度模型",
    summary: "用表层、中层、深海表示知识的活跃程度，让分类图本身成为检索入口。",
    body: [
      "海图不是装饰图，每个节点都对应一个自动分类及其真实条目数。",
      "点击节点即可把分类词带回唯一搜索框。",
    ],
    category: "产品设计",
    tags: ["海图", "分类", "检索"],
    source: "manual",
    sourceLabel: "原型笔记",
    status: "done",
    updated: "10 月 12 日",
    depth: "surface",
    confidence: 0.96,
  },
  {
    id: "k12",
    title: "大模型结构化输出的校验清单",
    summary: "分类、标签与摘要必须经过 schema 校验，旧 revision 的结果不得覆盖新内容。",
    body: [
      "输出只接受严格 JSON schema，相关条目 ID 必须再次校验。",
    ],
    category: "人工智能",
    tags: ["结构化输出", "schema", "校验"],
    source: "document",
    sourceLabel: "structured-output.md",
    status: "done",
    updated: "10 月 11 日",
    depth: "deep",
    confidence: 0.92,
  },
  {
    id: "k13",
    title: "知识库导入失败的重试策略",
    summary: "网络错误可以重试，私网地址、登录页和无法完整扫描的内容必须永久拒绝。",
    body: [
      "可重试错误使用指数退避和抖动。",
      "策略错误不重试，并把可操作的失败原因留在对应条目中。",
    ],
    category: "工程实践",
    tags: ["重试", "失败处理", "安全"],
    source: "webpage",
    sourceLabel: "capture-policy",
    status: "failed",
    updated: "10 月 10 日",
    depth: "deep",
    confidence: 0.42,
  },
  {
    id: "k14",
    title: "自动分类低置信度的产品处理",
    summary: "低置信度不阻塞入库，先应用暂定分类并标记待复核。",
    body: [
      "用户不需要在导入前建立标签体系。",
      "系统先完成归类，再提供可选的纠正入口。",
    ],
    category: "产品设计",
    tags: ["置信度", "暂定分类", "复核"],
    source: "manual",
    sourceLabel: "产品草案",
    status: "done",
    updated: "10 月 9 日",
    depth: "middle",
    confidence: 0.72,
  },
];

let trashItems = [
  {
    id: "trash-1",
    item: items[5],
    deletedAt: "2026-10-19 18:04",
    purgeAt: "2026-11-18",
  },
  {
    id: "trash-2",
    item: items[9],
    deletedAt: "2026-10-18 09:22",
    purgeAt: "2026-11-17",
  },
];

for (const entry of trashItems) {
  const index = items.findIndex((item) => item.id === entry.item.id);
  if (index >= 0) items.splice(index, 1);
}

const externalResults = [
  {
    id: "ext-1",
    title: "Knowledge organization: a practical guide",
    domain: "www.nngroup.com",
    summary: "从任务、检索与重访角度讨论个人信息组织方式。",
    url: "https://www.nngroup.com/articles/knowledge-organization/",
  },
  {
    id: "ext-2",
    title: "RAG from scratch: retrieval and ranking",
    domain: "developer.mozilla.org",
    summary: "检索增强生成中的查询扩展、召回和结果排序基础。",
    url: "https://developer.mozilla.org/",
  },
  {
    id: "ext-3",
    title: "Designing dense product workspaces",
    domain: "www.nngroup.com",
    summary: "面向长期使用的信息密度、层级和导航设计。",
    url: "https://www.nngroup.com/",
  },
];

const categoryPositions = {
  产品设计: { x: 74, y: 54, depth: "surface" },
  人工智能: { x: 188, y: 72, depth: "middle" },
  工程实践: { x: 246, y: 132, depth: "deep" },
  研究: { x: 126, y: 148, depth: "middle" },
  设计: { x: 52, y: 124, depth: "deep" },
  阅读摘录: { x: 182, y: 168, depth: "deep" },
};

const translations = {
  zh: {
    scopeLabel: "个人知识域",
    synced: "已同步",
    homeKicker: "私人知识域",
    homeSubtitle: "把收藏、网页与文档沉入同一片可检索的深度。",
    capture: "采集知识",
    searchAction: "潜入检索",
    localFirst: "先检索我的知识",
    searchHint: "只有一个搜索框，标题、正文、标签与分类同时命中。",
    allKnowledge: "全部知识",
    autoCategories: "自动分类",
    pendingReview: "待整理",
    uncategorized: "未分类",
    lastIndexed: "最近入海",
    localResults: "我的知识",
    viewAll: "查看全部",
    internetSearch: "继续搜索互联网",
    internetSearchHint: "只发送搜索词，不会携带知识正文",
    depthHint: "自动分类在深度中的分布",
    processingQueue: "待处理",
    processingHint: "AI 正在识别与归类",
    openQueue: "打开队列",
    libraryKicker: "所有收藏",
    libraryTitle: "知识库",
    librarySubtitle: "分类与标签由系统自动生成，你只需要纠正结果。",
    captureKicker: "新的知识流",
    captureTitle: "采集",
    captureSubtitle: "文字、网页、文档与截图都会先经过安全检查，再进入自动归类。",
    processingKicker: "自动整理",
    processingTitle: "待处理",
    processingSubtitle: "低置信度会以暂定结果保存，不会阻塞检索。",
    trashKicker: "可恢复区",
    trashTitle: "回收站",
    trashSubtitle: "删除后立即退出检索，30 天后永久清理。",
    settingsKicker: "控制与边界",
    settingsTitle: "设置",
    settingsSubtitle: "AI 只在授权的服务与内容类型内处理你的知识。",
    settingsAiNav: "AI 与外发",
    settingsLanguageNav: "语言与显示",
    settingsPrivacyNav: "隐私与保留",
    settingsAuthTitle: "AI 处理授权",
    settingsAuthDesc: "同一服务、同一内容类型可自动处理；切换范围需要重新确认。",
    authTextTitle: "DeepSeek 分类 · 文本",
    authTextDesc: "标题、正文、备注与粘贴文字",
    authVisionTitle: "DeepSeek Vision · 截图",
    authVisionDesc: "网页渲染图与用户粘贴截图",
    authSearchTitle: "DeepSeek Search · 查询",
    authSearchDesc: "仅在你主动继续互联网搜索时发送查询词",
    settingsLanguageTitle: "默认语言",
    settingsLanguageDesc: "界面默认中文，用户内容不会被自动翻译。",
    settingsRetentionTitle: "回收站保留",
    settingsRetentionDesc: "删除后 30 天永久清理，备份会按声明周期淘汰。",
    retentionValue: "30 天",
    accountMenuLabel: "账户菜单",
    accountOpenMenu: "打开{name}的账户菜单",
    accountCurrentAccount: "当前账户",
    accountProfile: "个人资料",
    accountLogout: "退出登录",
    accountLogoutTitle: "退出登录？",
    accountLogoutBody: "退出后会结束当前会话，需要重新登录才能继续使用识海。",
    accountConfirmLogout: "确认退出",
    accountSignedOut: "已退出当前会话（原型）",
    profileTitle: "个人资料",
    profileSubtitle: "管理你在识海中显示的名称。登录邮箱不会在这里修改。",
    profileAccountTitle: "账户信息",
    profileAccountDesc: "显示名称只用于识海界面，不会改变你的登录方式。",
    profilePrivate: "私人账户",
    profileDisplayName: "显示名称",
    profileDisplayNameHint: "1–40 个字符；保存后会用于头像和账户菜单。",
    profileEmail: "登录邮箱",
    profileEmailHint: "邮箱用于登录和接收验证码，当前为只读。",
    profileSave: "保存修改",
    profileSaved: "个人资料已保存",
    profileInvalid: "显示名称需为 1–40 个字符，且不能包含换行或 HTML 标签。",
    profileBoundariesTitle: "账户边界",
    profileBoundaryIdentity: "登录身份",
    profileBoundaryIdentityValue: "邮箱只读，用于登录和验证码。",
    profileBoundaryIsolation: "知识隔离",
    profileBoundaryIsolationValue: "知识、附件与搜索仅当前账号可访问。",
    profileBoundaryDisplayName: "显示名称",
    profileBoundaryDisplayNameValue: "用于界面识别，不参与搜索或 AI。",
    languageChinese: "中文",
    languageEnglish: "English",
    backToLibrary: "返回知识库",
    reanalyze: "重新分析",
    searchPlaceholder: "搜索标题、正文、标签、分类……",
    recentSummary: "最近更新的 6 条知识",
    resultSummary: "找到 {count} 条本地知识",
    noResults: "没有找到本地知识。保留搜索词，或继续搜索互联网。",
    internetHeading: "互联网海流",
    internetHint: "临时结果 · 仅在你确认后入库",
    internetLoading: "正在搜索互联网，已有本地结果保持不变…",
    saveToSea: "收入识海",
    review: "复核",
    retry: "重试",
    open: "查看",
    processingTitleCell: "正在处理",
    source: "来源",
    category: "分类",
    updated: "更新",
    status: "状态",
    searchBoxOnly: "唯一检索输入框",
    queueEmpty: "没有待处理内容。",
    trashEmpty: "回收站为空。",
    restore: "恢复",
    purge: "永久删除",
    deletedAt: "删除于",
    purgeAt: "永久清理",
    all: "全部",
    manual: "手动",
    webpage: "网页",
    document: "文档",
    screenshot: "截图",
    saved: "已保存并开始自动整理。",
    internetSaved: "已收入识海，正在自动生成分类与标签。",
    restored: "已恢复到知识库并重新进入检索。",
    reanalysis: "已提交重新分析；锁定字段不会改变。",
    retryStarted: "任务已重新排队。",
    lockHint: "用户修改后锁定",
    depthSurface: "表层",
    depthMiddle: "中层",
    depthDeep: "深海",
    captureDialogTitle: "采集一条知识",
    captureDialogKicker: "快速入海",
    formTitle: "标题",
    formBody: "内容",
    formUrl: "网页链接",
    formNote: "备注",
    formFile: "选择文件",
    formFileHint: "支持 PDF、Word、TXT、Markdown、JPG、PNG、WebP、HEIC，单文件最大 25 MiB",
    documentDropTitle: "拖入或选择文档或图片",
    documentDropHint: "文档与图片共用同一套安全扫描流程",
    screenshotPasteTitle: "点击这里，然后粘贴截图",
    screenshotPasteHint: "电脑：复制后按 Ctrl+V / ⌘V；手机：复制截图后在粘贴区使用系统粘贴菜单",
    screenshotPasteReady: "剪贴板截图已就绪",
    screenshotPasteEmpty: "请先粘贴一张剪贴板截图。",
    screenshotClipboardOnly: "仅支持剪贴板粘贴",
    screenshotClear: "清除截图",
    pasteInputAria: "剪贴板截图粘贴区",
    fileMissing: "请先选择一个文档或图片文件。",
    screenshotTooLarge: "截图超过 25 MiB，请压缩后重新复制。",
    fileTooLarge: "文件超过 25 MiB，请压缩或拆分后重试。",
    fileUnsupported: "不支持此文件类型，请选择受支持的文档或 JPG、PNG、WebP、HEIC 图片。",
    removeFile: "移除",
    editorAria: "富文本格式工具栏",
    editorPlaceholder: "开始整理这段知识……",
    editorHint: "支持富文本、链接和图片；粘贴内容会先经过安全清洗。",
    editorBold: "粗体",
    editorItalic: "斜体",
    editorUnderline: "下划线",
    editorStrike: "删除线",
    editorHeading2: "二级标题",
    editorHeading3: "三级标题",
    editorBulletList: "无序列表",
    editorOrderedList: "有序列表",
    editorQuote: "引用",
    editorCode: "代码块",
    editorLink: "链接",
    editorImage: "插入图片",
    editorUndo: "撤销",
    editorRedo: "重做",
    editorClear: "清除格式",
    editorImageCaption: "图片说明（可选）",
    editorImageCaptionAction: "编辑说明",
    editorLinkPrompt: "输入链接地址（http 或 https）",
    editorLinkInvalid: "只支持 http 或 https 链接。",
    editorImageFailed: "图片无法插入，请检查格式和大小。",
    edit: "编辑",
    editKnowledgeTitle: "编辑知识",
    saveChanges: "保存修改",
    goToDocument: "无法粘贴图片？前往文档入口选择 JPG、PNG 等图片文件。",
    saveAuto: "保存并自动整理",
    cancel: "取消",
    safetyNote: "提交前会扫描密钥；无法完整扫描的文件会被拒绝。",
  },
  en: {
    scopeLabel: "Personal knowledge field",
    synced: "Synced",
    homeKicker: "Private knowledge field",
    homeSubtitle: "Let saved pages, documents, and notes settle into one searchable depth.",
    capture: "Capture knowledge",
    searchAction: "Dive in",
    localFirst: "Search my knowledge first",
    searchHint: "One search box for titles, body, notes, tags, and categories.",
    allKnowledge: "Knowledge",
    autoCategories: "Auto categories",
    pendingReview: "To review",
    uncategorized: "Uncategorized",
    lastIndexed: "Last added",
    localResults: "My knowledge",
    viewAll: "View all",
    internetSearch: "Continue to internet search",
    internetSearchHint: "Only the query is sent. Your notes are never included.",
    depthHint: "Automatic categories by depth",
    processingQueue: "In progress",
    processingHint: "AI is reading and organizing",
    openQueue: "Open queue",
    libraryKicker: "All saved knowledge",
    libraryTitle: "Library",
    librarySubtitle: "Categories and tags are automatic. You only correct what matters.",
    captureKicker: "New knowledge stream",
    captureTitle: "Capture",
    captureSubtitle: "Text, pages, documents, and screenshots are scanned before classification.",
    processingKicker: "Automatic organization",
    processingTitle: "In progress",
    processingSubtitle: "Low-confidence results are provisional and never block retrieval.",
    trashKicker: "Recoverable",
    trashTitle: "Trash",
    trashSubtitle: "Deleted items leave search immediately and purge after 30 days.",
    settingsKicker: "Control and boundaries",
    settingsTitle: "Settings",
    settingsSubtitle: "AI only processes the services and content types you authorize.",
    settingsAiNav: "AI and sharing",
    settingsLanguageNav: "Language and display",
    settingsPrivacyNav: "Privacy and retention",
    settingsAuthTitle: "AI processing authorization",
    settingsAuthDesc: "The same service and content type may run automatically. Scope changes require confirmation.",
    authTextTitle: "DeepSeek Classification · Text",
    authTextDesc: "Titles, body, notes, and pasted text",
    authVisionTitle: "DeepSeek Vision · Screenshot",
    authVisionDesc: "Rendered pages and clipboard screenshots",
    authSearchTitle: "DeepSeek Search · Query",
    authSearchDesc: "Only the query is sent when you explicitly continue to internet search",
    settingsLanguageTitle: "Default language",
    settingsLanguageDesc: "The interface defaults to Chinese. User content is never translated automatically.",
    settingsRetentionTitle: "Trash retention",
    settingsRetentionDesc: "Deleted items purge after 30 days. Backups expire on the declared schedule.",
    retentionValue: "30 days",
    accountMenuLabel: "Account menu",
    accountOpenMenu: "Open account menu for {name}",
    accountCurrentAccount: "Current account",
    accountProfile: "Profile",
    accountLogout: "Sign out",
    accountLogoutTitle: "Sign out?",
    accountLogoutBody: "Signing out ends this session. You will need to sign in again to use Shihai.",
    accountConfirmLogout: "Sign out now",
    accountSignedOut: "Signed out of this session (prototype)",
    profileTitle: "Profile",
    profileSubtitle: "Manage the name shown inside Shihai. Your sign-in email is not changed here.",
    profileAccountTitle: "Account details",
    profileAccountDesc: "Your display name is only used in Shihai and does not change how you sign in.",
    profilePrivate: "Private account",
    profileDisplayName: "Display name",
    profileDisplayNameHint: "1–40 characters. Used by your avatar and account menu after saving.",
    profileEmail: "Sign-in email",
    profileEmailHint: "This email is used to sign in and receive verification codes. It is read-only.",
    profileSave: "Save changes",
    profileSaved: "Profile saved",
    profileInvalid: "Use 1–40 characters without line breaks or HTML tags.",
    profileBoundariesTitle: "Account boundaries",
    profileBoundaryIdentity: "Sign-in identity",
    profileBoundaryIdentityValue: "The read-only email is used for sign-in and verification codes.",
    profileBoundaryIsolation: "Knowledge isolation",
    profileBoundaryIsolationValue: "Knowledge, attachments, and search are limited to this account.",
    profileBoundaryDisplayName: "Display name",
    profileBoundaryDisplayNameValue: "Used for interface identity, not search or AI.",
    languageChinese: "中文",
    languageEnglish: "English",
    backToLibrary: "Back to library",
    reanalyze: "Re-analyze",
    searchPlaceholder: "Search titles, body, tags, categories…",
    recentSummary: "6 recently updated items",
    resultSummary: "{count} local results",
    noResults: "No local results. Keep the query or continue to the internet.",
    internetHeading: "Internet current",
    internetHint: "Temporary results · saved only after confirmation",
    internetLoading: "Searching the internet. Existing local results stay in place…",
    saveToSea: "Save to Sea",
    review: "Review",
    retry: "Retry",
    open: "Open",
    processingTitleCell: "Processing",
    source: "Source",
    category: "Category",
    updated: "Updated",
    status: "Status",
    searchBoxOnly: "Single search field",
    queueEmpty: "Nothing is waiting.",
    trashEmpty: "Trash is empty.",
    restore: "Restore",
    purge: "Delete forever",
    deletedAt: "Deleted",
    purgeAt: "Purge",
    all: "All",
    manual: "Manual",
    webpage: "Web",
    document: "Document",
    screenshot: "Screenshot",
    saved: "Saved and queued for automatic organization.",
    internetSaved: "Saved to Sea. Classification and tags are being generated.",
    restored: "Restored to the library and search.",
    reanalysis: "Re-analysis queued. Locked fields will stay unchanged.",
    retryStarted: "The job is queued again.",
    lockHint: "Locked after user correction",
    depthSurface: "Surface",
    depthMiddle: "Midwater",
    depthDeep: "Abyss",
    captureDialogTitle: "Capture knowledge",
    captureDialogKicker: "Drop into the sea",
    formTitle: "Title",
    formBody: "Content",
    formUrl: "Page URL",
    formNote: "Note",
    formFile: "Choose file",
    formFileHint: "PDF, Word, TXT, Markdown, JPG, PNG, WebP, HEIC; max 25 MiB",
    documentDropTitle: "Drop or choose a document or image",
    documentDropHint: "Documents and images share the same safety scan",
    screenshotPasteTitle: "Click here, then paste a screenshot",
    screenshotPasteHint: "Desktop: press Ctrl+V / ⌘V. Phone: copy the screenshot, then use the system paste menu here.",
    screenshotPasteReady: "Clipboard screenshot ready",
    screenshotPasteEmpty: "Paste a clipboard screenshot first.",
    screenshotClipboardOnly: "Clipboard paste only",
    screenshotClear: "Clear screenshot",
    pasteInputAria: "Clipboard screenshot paste area",
    fileMissing: "Choose a document or image file first.",
    screenshotTooLarge: "The screenshot exceeds 25 MiB. Compress it and copy again.",
    fileTooLarge: "The file exceeds 25 MiB. Compress or split it, then try again.",
    fileUnsupported: "Unsupported file type. Choose a supported document or a JPG, PNG, WebP, or HEIC image.",
    removeFile: "Remove",
    editorAria: "Rich text formatting toolbar",
    editorPlaceholder: "Start shaping this knowledge…",
    editorHint: "Rich text, links, and images are supported. Pasted content is cleaned first.",
    editorBold: "Bold",
    editorItalic: "Italic",
    editorUnderline: "Underline",
    editorStrike: "Strikethrough",
    editorHeading2: "Heading 2",
    editorHeading3: "Heading 3",
    editorBulletList: "Bulleted list",
    editorOrderedList: "Numbered list",
    editorQuote: "Quote",
    editorCode: "Code block",
    editorLink: "Link",
    editorImage: "Insert image",
    editorUndo: "Undo",
    editorRedo: "Redo",
    editorClear: "Clear formatting",
    editorImageCaption: "Image caption (optional)",
    editorImageCaptionAction: "Edit caption",
    editorLinkPrompt: "Enter an http or https link",
    editorLinkInvalid: "Only http or https links are supported.",
    editorImageFailed: "The image could not be inserted. Check its format and size.",
    edit: "Edit",
    editKnowledgeTitle: "Edit knowledge",
    saveChanges: "Save changes",
    goToDocument: "Can't paste an image? Choose a JPG, PNG, or other image in the Document tab.",
    saveAuto: "Save and organize",
    cancel: "Cancel",
    safetyNote: "Secrets are scanned before saving. Unscannable files are rejected.",
  },
};

const state = {
  lang: "zh",
  view: "home",
  query: "",
  libraryFilter: "all",
  captureMode: "manual",
  dialogMode: "manual",
  pastedScreenshots: { dialog: null, page: null },
  selectedFiles: { dialog: null, page: null },
  notes: { dialog: "", page: "" },
  richDrafts: { dialog: "", page: "" },
  editingItemId: null,
  currentDetailId: null,
  externalVisible: false,
  accountName: "林舟",
  accountEmail: "linzhou@seaunit.site",
};

const $ = (selector, root = document) => root.querySelector(selector);
const $$ = (selector, root = document) => [...root.querySelectorAll(selector)];

function t(key, vars = {}) {
  let value = translations[state.lang][key] ?? translations.zh[key] ?? key;
  Object.entries(vars).forEach(([name, replacement]) => {
    value = value.replace(`{${name}}`, replacement);
  });
  return value;
}

function escapeHtml(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function highlight(text, query) {
  const safe = escapeHtml(text);
  if (!query) return safe;
  const escapedQuery = query.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
  return safe.replace(new RegExp(`(${escapedQuery})`, "gi"), "<mark>$1</mark>");
}

function sourceName(source) {
  return state.lang === "en" ? sourceMeta[source].labelEn : sourceMeta[source].labelZh;
}

function statusLabel(status) {
  const labels = {
    done: state.lang === "en" ? "Ready" : "已整理",
    review: state.lang === "en" ? "Review" : "待复核",
    processing: state.lang === "en" ? "Processing" : "处理中",
    failed: state.lang === "en" ? "Failed" : "失败",
  };
  return labels[status];
}

function statusClass(status) {
  return status === "done" ? "done" : status === "failed" ? "failed" : status;
}

function renderSourceIcon(source) {
  return `<span class="source-icon" aria-hidden="true"><svg><use href="#${sourceMeta[source].icon}"></use></svg></span>`;
}

function categoryCounts() {
  return items.reduce((acc, item) => {
    acc[item.category] = (acc[item.category] || 0) + 1;
    return acc;
  }, {});
}

function pendingItems() {
  return items.filter((item) => item.status === "review" || item.status === "processing" || item.status === "failed");
}

function updateAllCounts() {
  const counts = categoryCounts();
  const pending = pendingItems().length;
  ["allItemsCount", "filterAllCount"].forEach((id) => {
    const node = document.getElementById(id);
    if (node) node.textContent = items.length;
  });
  ["categoryCount", "filterCategoryCount"].forEach((id) => {
    const node = document.getElementById(id);
    if (node) node.textContent = Object.keys(counts).length;
  });
  ["homePendingCount", "navPendingCount"].forEach((id) => {
    const node = document.getElementById(id);
    if (node) node.textContent = pending;
  });
  const webCount = items.filter((item) => item.source === "webpage").length;
  const docCount = items.filter((item) => item.source === "document").length;
  const shotCount = items.filter((item) => item.source === "screenshot").length;
  const manualCount = items.filter((item) => item.source === "manual").length;
  const map = {
    filterWebCount: webCount,
    filterDocCount: docCount,
    filterShotCount: shotCount,
    filterManualCount: manualCount,
  };
  Object.entries(map).forEach(([id, value]) => {
    const node = document.getElementById(id);
    if (node) node.textContent = value;
  });
}

function renderResultRow(item, query = "") {
  return `
    <button class="result-row" data-item-id="${item.id}" aria-label="${escapeHtml(item.title)}">
      ${renderSourceIcon(item.source)}
      <span class="result-body">
        <span class="result-title">${highlight(item.title, query)}</span>
        <span class="result-summary">${highlight(item.summary, query)}</span>
        <span class="result-meta">
          <span>${escapeHtml(item.category)}</span>
          <span>${item.tags.slice(0, 2).map((tag) => highlight(tag, query)).join(" / ")}</span>
          <span>${escapeHtml(item.sourceLabel)}</span>
        </span>
      </span>
      <span class="status-pill ${statusClass(item.status)}">${statusLabel(item.status)}</span>
    </button>
  `;
}

function searchMatches(query) {
  const normalized = query.trim().toLowerCase();
  if (!normalized) return [];
  return items
    .filter((item) => {
      const haystack = [item.title, item.summary, item.body.join(" "), item.category, item.tags.join(" "), item.sourceLabel]
        .join(" ")
        .toLowerCase();
      return haystack.includes(normalized);
    })
    .sort((a, b) => {
      const aTitle = a.title.toLowerCase().includes(normalized);
      const bTitle = b.title.toLowerCase().includes(normalized);
      return Number(bTitle) - Number(aTitle);
    });
}

function renderHomeResults() {
  updateAllCounts();
  const query = state.query.trim();
  const localResults = query ? searchMatches(query) : items.slice(0, 6);
  const container = $("#localResults");
  container.innerHTML = localResults.length
    ? localResults.map((item) => renderResultRow(item, query)).join("")
    : `<div class="empty-state">${t("noResults")}</div>`;
  $("#resultSummary").textContent = query ? t("resultSummary", { count: localResults.length }) : t("recentSummary");
  $("#internetTrigger").hidden = !query;
  $("#externalResults").hidden = !state.externalVisible;
  if (state.externalVisible) renderExternalResults();
}

function renderExternalResults() {
  const container = $("#externalResults");
  if (!state.externalVisible) {
    container.hidden = true;
    container.innerHTML = "";
    return;
  }
  container.hidden = false;
  container.innerHTML = `
    <div class="external-heading">
      <strong>${t("internetHeading")}</strong>
      <span>${t("internetHint")}</span>
    </div>
    ${externalResults.map((result, index) => `
      <div class="external-row">
        <span class="external-index">${index + 1}</span>
        <div>
          <p class="external-title">${escapeHtml(result.title)}</p>
          <p class="external-domain">${escapeHtml(result.domain)} · ${escapeHtml(result.summary)}</p>
        </div>
        <button class="save-link" data-save-external="${result.id}">
          <svg><use href="#icon-plus"></use></svg>
          ${t("saveToSea")}
        </button>
      </div>
    `).join("")}
  `;
}

function renderQueuePreview() {
  const queue = pendingItems().slice(0, 4);
  const container = $("#queuePreview");
  if (!queue.length) {
    container.innerHTML = `<div class="empty-state">${t("queueEmpty")}</div>`;
    return;
  }
  container.innerHTML = queue.map((item) => `
    <div class="queue-row">
      <div>
        <strong>${escapeHtml(item.title)}</strong>
        <small>${escapeHtml(item.sourceLabel)} · ${statusLabel(item.status)}</small>
      </div>
      <button class="mini-action" data-review-id="${item.id}">
        ${item.status === "failed" ? t("retry") : t("review")}
      </button>
    </div>
  `).join("");
}

function renderDepthMap() {
  const counts = categoryCounts();
  const categories = Object.keys(counts);
  const nodeMarkup = categories.map((category) => {
    const pos = categoryPositions[category] || { x: 100, y: 100, depth: "middle" };
    const count = counts[category];
    return `
      <g class="map-node" data-category="${escapeHtml(category)}" tabindex="0" role="button" aria-label="${escapeHtml(category)} ${count}">
        <circle cx="${pos.x}" cy="${pos.y}" r="7"></circle>
        <text x="${pos.x + 12}" y="${pos.y - 1}">${escapeHtml(category)}</text>
        <text class="node-count" x="${pos.x + 12}" y="${pos.y + 11}">${count} 条</text>
      </g>
    `;
  }).join("");
  const surfaceY = 84;
  const middleY = 128;
  const deepY = 170;
  $("#depthMap").innerHTML = `
    <svg viewBox="0 0 300 206" role="img" aria-label="${state.lang === "en" ? "Knowledge depth map" : "知识深度海图"}">
      <path class="map-contour" d="M8 42 C72 29, 130 50, 292 35"></path>
      <path class="map-contour" d="M8 88 C82 70, 166 100, 292 82"></path>
      <path class="map-contour" d="M8 132 C82 118, 188 144, 292 124"></path>
      <path class="map-contour" d="M8 176 C100 160, 190 188, 292 168"></path>
      <text class="map-depth-label" x="8" y="${surfaceY}">${t("depthSurface")}</text>
      <text class="map-depth-label" x="8" y="${middleY}">${t("depthMiddle")}</text>
      <text class="map-depth-label" x="8" y="${deepY}">${t("depthDeep")}</text>
      ${nodeMarkup}
    </svg>
  `;
  $$(".map-node").forEach((node) => {
    const activate = () => {
      const category = node.dataset.category;
      $("#searchInput").value = category;
      state.query = category;
      state.externalVisible = false;
      state.view = "home";
      showView("home");
      renderHomeResults();
      window.scrollTo({ top: 0, behavior: "smooth" });
    };
    node.addEventListener("click", activate);
    node.addEventListener("keydown", (event) => {
      if (event.key === "Enter" || event.key === " ") {
        event.preventDefault();
        activate();
      }
    });
  });
}

function filteredItems() {
  if (state.libraryFilter === "all") return items;
  return items.filter((item) => item.source === state.libraryFilter);
}

function renderKnowledgeTable() {
  const list = filteredItems();
  const container = $("#knowledgeTable");
  if (!list.length) {
    container.innerHTML = `<div class="empty-state">${t("noResults")}</div>`;
    return;
  }
  container.innerHTML = list.map((item) => `
    <div class="knowledge-row" data-item-id="${item.id}" tabindex="0" role="button">
      <div class="knowledge-main">
        ${renderSourceIcon(item.source)}
        <div>
          <strong>${escapeHtml(item.title)}</strong>
          <small>${escapeHtml(item.tags.join(" · "))}</small>
        </div>
      </div>
      <span class="table-label table-category">${escapeHtml(item.category)}</span>
      <span class="table-label table-source">${escapeHtml(sourceName(item.source))}</span>
      <span class="table-date">${escapeHtml(item.updated)}</span>
      <span class="status-pill ${statusClass(item.status)}">${statusLabel(item.status)}</span>
    </div>
  `).join("");
  $$(".knowledge-row", container).forEach((row) => {
    const open = () => openDetail(row.dataset.itemId);
    row.addEventListener("click", open);
    row.addEventListener("keydown", (event) => {
      if (event.key === "Enter" || event.key === " ") {
        event.preventDefault();
        open();
      }
    });
  });
}

function renderDetail(itemId) {
  const item = items.find((candidate) => candidate.id === itemId);
  if (!item) return;
  state.currentDetailId = item.id;
  const tags = item.tags.map((tag) => `
    <span class="tag">
      <svg><use href="#icon-lock"></use></svg>
      ${escapeHtml(tag)}
    </span>
  `).join("");
  const richBody = sanitizeRichHtml(
    item.bodyHtml || item.body.map((paragraph) => `<p>${escapeHtml(paragraph)}</p>`).join(""),
  );
  $("#detailContent").innerHTML = `
    <section class="detail-main">
      <h1>${escapeHtml(item.title)}</h1>
      <p class="detail-lead">${escapeHtml(item.summary)}</p>
      <div class="detail-meta">
        <span>${escapeHtml(item.category)}</span>
        <span><svg><use href="#${sourceMeta[item.source].icon}"></use></svg>${escapeHtml(item.sourceLabel)}</span>
        <span><svg><use href="#icon-clock"></use></svg>${escapeHtml(item.updated)}</span>
        <span class="status-pill ${statusClass(item.status)}">${statusLabel(item.status)}</span>
      </div>
      <div class="detail-body rich-prose">
        ${richBody}
      </div>
      ${item.attachments?.length ? `
        <section class="detail-attachments" aria-label="${state.lang === "en" ? "Source images" : "来源图片"}">
          <h2>${state.lang === "en" ? "Source images" : "来源图片"}</h2>
          ${item.attachments.map((attachment) => `
            <figure>
              <img src="${attachment.url}" alt="${escapeHtml(attachment.name)}" />
              <figcaption>
                <strong>${escapeHtml(attachment.name)}</strong>
                <span>${state.lang === "en" ? "Safe source attachment · not inserted into the body" : "安全来源附件 · 未自动插入正文"}</span>
              </figcaption>
            </figure>
          `).join("")}
        </section>
      ` : ""}
    </section>
    <aside class="detail-aside">
      <section class="aside-section">
        <h2>${state.lang === "en" ? "Classification" : "自动整理"}</h2>
        <div class="tag-list">
          <span class="tag">${escapeHtml(item.category)}</span>
          ${tags}
        </div>
        <div class="confidence">
          <span>${state.lang === "en" ? "Confidence" : "分类置信度"} ${Math.round(item.confidence * 100)}%</span>
          <div class="meter"><span style="--confidence:${item.confidence * 100}%"></span></div>
        </div>
      </section>
      <section class="aside-section">
        <h2>${state.lang === "en" ? "Source trail" : "来源与处理"}</h2>
        <p class="detail-lead" style="font-size:12px;margin:0">
          ${escapeHtml(sourceName(item.source))} · ${escapeHtml(item.sourceLabel)}<br />
          ${state.lang === "en" ? "Classified automatically. Edited fields become locked." : "系统自动归类；用户修改后的字段将被锁定。"}
        </p>
      </section>
      <section class="aside-section">
        <h2>${state.lang === "en" ? "Related knowledge" : "可能相关"}</h2>
        <div class="tag-list">
          ${items.filter((candidate) => candidate.id !== item.id && candidate.category === item.category).slice(0, 2)
            .map((candidate) => `<button class="mini-action" data-item-id="${candidate.id}">${escapeHtml(candidate.title)}</button>`)
            .join("") || `<span class="table-label">${state.lang === "en" ? "No related items yet." : "暂无相关条目。"}</span>`}
        </div>
      </section>
    </aside>
  `;
}

function openDetail(itemId) {
  renderDetail(itemId);
  showView("detail");
}

function renderProcessing() {
  const list = pendingItems();
  const container = $("#processingList");
  if (!list.length) {
    container.innerHTML = `<div class="empty-state">${t("queueEmpty")}</div>`;
    return;
  }
  container.innerHTML = list.map((item) => `
    <div class="processing-row">
      <div class="processing-info">
        <strong>${escapeHtml(item.title)}</strong>
        <small>${escapeHtml(item.sourceLabel)} · ${escapeHtml(item.category)}</small>
      </div>
      <span class="processing-type table-label">${escapeHtml(sourceName(item.source))}</span>
      <span class="status-pill ${statusClass(item.status)}">${statusLabel(item.status)}</span>
      <span class="processing-time">${escapeHtml(item.updated)}</span>
      <div class="row-actions">
        <button class="mini-action" data-review-id="${item.id}">${item.status === "failed" ? t("retry") : t("review")}</button>
        <button class="mini-action" data-open-id="${item.id}">${t("open")}</button>
      </div>
    </div>
  `).join("");
}

function renderTrash() {
  const container = $("#trashList");
  if (!trashItems.length) {
    container.innerHTML = `<div class="empty-state">${t("trashEmpty")}</div>`;
    return;
  }
  container.innerHTML = trashItems.map((entry) => `
    <div class="trash-row">
      <div class="trash-info">
        <strong>${escapeHtml(entry.item.title)}</strong>
        <small>${t("deletedAt")} ${escapeHtml(entry.deletedAt)} · ${t("purgeAt")} ${escapeHtml(entry.purgeAt)}</small>
      </div>
      <span class="table-label">${escapeHtml(entry.item.category)}</span>
      <span class="status-pill failed">${state.lang === "en" ? "Recycled" : "已删除"}</span>
      <span class="trash-time">${escapeHtml(entry.item.updated)}</span>
      <div class="row-actions">
        <button class="mini-action" data-restore-id="${entry.id}">${t("restore")}</button>
      </div>
    </div>
  `).join("");
}

const MAX_SCREENSHOT_BYTES = 25 * 1024 * 1024;
const IMAGE_FILE_PATTERN = /\.(jpe?g|png|webp|heic|heif)$/i;
const DOCUMENT_FILE_PATTERN = /\.(pdf|docx?|txt|md|rtf)$/i;
const SUPPORTED_IMAGE_MIME = new Set([
  "image/jpeg",
  "image/png",
  "image/webp",
  "image/heic",
  "image/heif",
]);

function isImageFile(file) {
  if (!file) return false;
  const mediaType = String(file.type || "").toLowerCase();
  const name = String(file.name || "");
  const supportedType = mediaType ? SUPPORTED_IMAGE_MIME.has(mediaType) : false;
  const supportedExtension = IMAGE_FILE_PATTERN.test(name);
  const hasDifferentExtension = /\.[^.]+$/.test(name) && !supportedExtension;
  return !hasDifferentExtension && (supportedType || supportedExtension);
}

function isDocumentFile(file) {
  return Boolean(file && (DOCUMENT_FILE_PATTERN.test(file.name) || file.type === "application/pdf"));
}

function formatFileSize(bytes) {
  if (!Number.isFinite(bytes) || bytes <= 0) return "0 KB";
  if (bytes >= 1024 * 1024) return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  return `${Math.max(1, Math.round(bytes / 1024))} KB`;
}

function clearPastedScreenshot(target) {
  const current = state.pastedScreenshots[target];
  if (current?.previewUrl) URL.revokeObjectURL(current.previewUrl);
  state.pastedScreenshots[target] = null;
  paintPastedScreenshot(target);
}

function setPastedScreenshot(target, file) {
  if (!isImageFile(file)) return false;
  if (file.size > MAX_SCREENSHOT_BYTES) {
    showToast(t("screenshotTooLarge"));
    return false;
  }
  const current = state.pastedScreenshots[target];
  if (current?.previewUrl) URL.revokeObjectURL(current.previewUrl);
  state.pastedScreenshots[target] = {
    file,
    previewUrl: URL.createObjectURL(file),
  };
  paintPastedScreenshot(target);
  return true;
}

function seedDemoPastedScreenshot() {
  const canvas = document.createElement("canvas");
  canvas.width = 960;
  canvas.height = 540;
  const context = canvas.getContext("2d");
  context.fillStyle = "#071a28";
  context.fillRect(0, 0, canvas.width, canvas.height);
  context.strokeStyle = "#66d7e6";
  context.lineWidth = 3;
  context.strokeRect(34, 34, 892, 472);
  context.fillStyle = "#effaff";
  context.font = "600 34px sans-serif";
  context.fillText("Clipboard screenshot", 78, 112);
  context.fillStyle = "#9bb8c8";
  context.font = "22px sans-serif";
  context.fillText("Search every saved note, page and document.", 78, 164);
  context.fillStyle = "#0d2a3d";
  context.fillRect(78, 232, 804, 88);
  context.fillStyle = "#66d7e6";
  context.fillRect(78, 232, 8, 88);
  state.pastedScreenshots.page = {
    file: { name: "clipboard-example.png", size: 184320, type: "image/png" },
    previewUrl: canvas.toDataURL("image/png"),
  };
}

function paintPastedScreenshot(target) {
  const zone = $(`[data-paste-zone="${target}"]`);
  if (!zone) return;
  const visual = $("[data-paste-visual]", zone);
  if (!visual) return;
  const pasted = state.pastedScreenshots[target];
  const submit = $(`[data-submit-screenshot="${target}"]`);
  if (!pasted) {
    zone.classList.remove("has-image");
    visual.innerHTML = `
      <span class="paste-zone-icon"><svg><use href="#icon-image"></use></svg></span>
      <strong>${t("screenshotPasteTitle")}</strong>
      <p>${t("screenshotPasteHint")}</p>
      <span class="paste-shortcuts" aria-hidden="true">
        <kbd>Ctrl</kbd><span>+</span><kbd>V</kbd>
        <i>${state.lang === "en" ? "or" : "或"}</i>
        <kbd>⌘</kbd><span>+</span><kbd>V</kbd>
      </span>
    `;
    if (submit) submit.disabled = true;
    return;
  }
  zone.classList.add("has-image");
  visual.innerHTML = `
    <span class="paste-preview-frame">
      <img src="${pasted.previewUrl}" alt="${state.lang === "en" ? "Pasted screenshot preview" : "剪贴板截图预览"}" />
    </span>
    <span class="paste-preview-meta">
      <span>
        <strong>${t("screenshotPasteReady")}</strong>
        <small>${escapeHtml(pasted.file.name || (state.lang === "en" ? "Clipboard image" : "剪贴板图片"))} · ${formatFileSize(pasted.file.size)}</small>
      </span>
      <button class="mini-action" type="button" data-clear-screenshot="${target}">${t("screenshotClear")}</button>
    </span>
  `;
  if (submit) submit.disabled = false;
}

function paintSelectedFile(target, file) {
  const container = target === "dialog" ? $("#dialogFields") : $("#captureContent");
  if (!container) return;
  const status = $("[data-file-selection]", container);
  if (!status) return;
  if (!file) {
    status.hidden = true;
    status.innerHTML = "";
    return;
  }
  const image = isImageFile(file);
  status.hidden = false;
  status.innerHTML = `
    <span class="selected-file-icon"><svg><use href="#${image ? "icon-image" : "icon-file"}"></use></svg></span>
    <span class="selected-file-copy">
      <strong>${escapeHtml(file.name)}</strong>
      <small>${image ? (state.lang === "en" ? "Image file" : "图片文件") : (state.lang === "en" ? "Document" : "文档")} · ${formatFileSize(file.size)}</small>
    </span>
    <button class="mini-action" type="button" data-clear-file="${target}">${t("removeFile")}</button>
  `;
}

function attachDocumentFileControls(container, target) {
  const drop = $("[data-file-drop]", container);
  const input = $("[data-file-input]", container);
  if (!drop || !input) return;

  const acceptFile = (file) => {
    if (!file) return;
    if (!isDocumentFile(file) && !isImageFile(file)) {
      showToast(t("fileUnsupported"));
      return;
    }
    if (file.size > MAX_SCREENSHOT_BYTES) {
      showToast(t("fileTooLarge"));
      return;
    }
    state.selectedFiles[target] = file;
    paintSelectedFile(target, file);
  };

  const picker = $("[data-file-picker]", container);
  picker?.addEventListener("click", () => input.click());
  input.addEventListener("change", () => {
    acceptFile(input.files?.[0]);
    input.value = "";
  });
  ["dragenter", "dragover"].forEach((type) => {
    drop.addEventListener(type, (event) => {
      event.preventDefault();
      drop.classList.add("is-dragging");
    });
  });
  ["dragleave", "drop"].forEach((type) => {
    drop.addEventListener(type, (event) => {
      event.preventDefault();
      drop.classList.remove("is-dragging");
    });
  });
  drop.addEventListener("drop", (event) => acceptFile(event.dataTransfer?.files?.[0]));
}

const RICH_TEXT_TAGS = new Set([
  "P", "BR", "H2", "H3", "H4", "STRONG", "B", "EM", "I", "U", "S", "DEL",
  "UL", "OL", "LI", "BLOCKQUOTE", "A", "CODE", "PRE", "FIGURE", "IMG",
  "FIGCAPTION", "HR", "DIV", "SPAN",
]);
const RICH_TEXT_DROP_TAGS = new Set([
  "SCRIPT", "STYLE", "IFRAME", "OBJECT", "EMBED", "FORM", "INPUT", "BUTTON",
  "TEXTAREA", "SELECT", "META", "LINK",
]);

function sanitizeRichHtml(html) {
  const parsed = new DOMParser().parseFromString(`<div data-rich-root>${String(html || "")}</div>`, "text/html");
  const root = parsed.querySelector("[data-rich-root]");
  if (!root) return "";

  [...root.querySelectorAll([...RICH_TEXT_DROP_TAGS].join(","))].forEach((node) => node.remove());
  [...root.querySelectorAll("*")].reverse().forEach((node) => {
    if (!RICH_TEXT_TAGS.has(node.tagName)) {
      node.replaceWith(...node.childNodes);
      return;
    }

    [...node.attributes].forEach((attribute) => {
      const keepLink = node.tagName === "A" && attribute.name === "href";
      const keepImage = node.tagName === "IMG" && (
        attribute.name === "src" ||
        attribute.name === "alt" ||
        attribute.name === "data-local-rich-image"
      );
      if (!keepLink && !keepImage) node.removeAttribute(attribute.name);
    });

    if (node.tagName === "A") {
      const href = node.getAttribute("href") || "";
      try {
        const url = new URL(href, location.href);
        if (!["http:", "https:"].includes(url.protocol)) throw new Error("unsafe");
      } catch {
        node.replaceWith(...node.childNodes);
      }
    }

    if (node.tagName === "IMG") {
      const src = node.getAttribute("src") || "";
      const local = node.hasAttribute("data-local-rich-image") && src.startsWith("blob:");
      if (!local) {
        const placeholder = parsed.createElement("span");
        placeholder.className = "remote-image-placeholder";
        placeholder.textContent = state.lang === "en"
          ? "Remote image not loaded"
          : "远程图片未加载";
        node.replaceWith(placeholder);
      }
    }
  });

  return root.innerHTML;
}

function getRichEditor(target) {
  const container = target === "dialog" ? $("#dialogFields") : $("#captureContent");
  return $("[data-rich-editor]", container);
}

function getRichToolbarButton(target, command) {
  const container = target === "dialog" ? $("#dialogFields") : $("#captureContent");
  return $(`[data-editor-command="${command}"]`, container);
}

function saveRichSelection(editor) {
  const selection = window.getSelection();
  if (!selection?.rangeCount) return null;
  const range = selection.getRangeAt(0);
  return editor.contains(range.commonAncestorContainer) ? range.cloneRange() : null;
}

function restoreRichSelection(editor, range) {
  editor.focus();
  if (!range) return;
  const selection = window.getSelection();
  selection.removeAllRanges();
  selection.addRange(range);
}

function insertRichImage(editor, savedRange, file) {
  if (!isImageFile(file) || file.size > MAX_SCREENSHOT_BYTES) {
    showToast(t("editorImageFailed"));
    return false;
  }
  const previewUrl = URL.createObjectURL(file);
  restoreRichSelection(editor, savedRange);
  const safeName = escapeHtml(file.name || "image");
  const mediaType = String(file.type || "").toLowerCase();
  const isHeic = mediaType === "image/heic" || mediaType === "image/heif" || /\.(heic|heif)$/i.test(file.name || "");
  const preview = isHeic
    ? `<span class="image-conversion-placeholder">${state.lang === "en" ? "HEIC preview is generated after the safety scan." : "HEIC 将在安全扫描后生成预览。"}</span>`
    : `<img src="${previewUrl}" alt="${safeName}" data-local-rich-image="true" />`;
  document.execCommand(
    "insertHTML",
    false,
    `<figure class="editor-inline-image" contenteditable="false">
      ${preview}
      <figcaption>${safeName}</figcaption>
      <button class="image-caption-action" type="button" data-edit-caption aria-label="${t("editorImageCaptionAction")}">${t("editorImageCaptionAction")}</button>
    </figure><p><br></p>`,
  );
  return true;
}

function updateRichToolbarState(target) {
  const editor = getRichEditor(target);
  if (!editor) return;
  const selection = window.getSelection();
  if (!selection?.rangeCount || !editor.contains(selection.anchorNode)) return;
  [["bold", "bold"], ["italic", "italic"], ["underline", "underline"], ["strikeThrough", "strikeThrough"]]
    .forEach(([command, key]) => {
      const button = getRichToolbarButton(target, command);
      if (!button) return;
      button.setAttribute("aria-pressed", String(Boolean(document.queryCommandState(command))));
    });
}

function attachRichEditor(container, target) {
  const editor = $("[data-rich-editor]", container);
  if (!editor) return;
  const imageInput = $("[data-editor-image-input]", container);
  let savedRange = null;
  document.execCommand("defaultParagraphSeparator", false, "p");

  editor.addEventListener("keyup", () => updateRichToolbarState(target));
  editor.addEventListener("mouseup", () => updateRichToolbarState(target));
  editor.addEventListener("input", () => {
    state.richDrafts[target] = sanitizeRichHtml(editor.innerHTML);
  });
  editor.addEventListener("blur", () => {
    state.richDrafts[target] = sanitizeRichHtml(editor.innerHTML);
  });

  editor.addEventListener("paste", (event) => {
    const imageItem = [...(event.clipboardData?.items || [])]
      .find((item) => item.kind === "file" && item.type.startsWith("image/"));
    if (imageItem) {
      event.preventDefault();
      const file = imageItem.getAsFile();
      if (file) insertRichImage(editor, saveRichSelection(editor), file);
      return;
    }
    const pastedHtml = event.clipboardData?.getData("text/html");
    if (pastedHtml) {
      event.preventDefault();
      document.execCommand("insertHTML", false, sanitizeRichHtml(pastedHtml));
    }
  });

  const editImageCaption = (image) => {
    const figure = image?.closest("figure") || image;
    if (!figure) return;
    const captionNode = $("figcaption", figure);
    const next = window.prompt(t("editorImageCaption"), captionNode?.textContent || image.alt || "");
    if (next === null) return;
    const caption = next.trim();
    const previousAlt = typeof image.getAttribute === "function" ? image.getAttribute("alt") : "";
    const nextAlt = caption || previousAlt || "image";
    if (typeof image === "object" && "alt" in image) image.alt = nextAlt;
    if (captionNode) captionNode.textContent = nextAlt;
  };

  editor.addEventListener("click", (event) => {
    const action = event.target.closest("[data-edit-caption]");
    if (!action) return;
    event.preventDefault();
    const figure = action.closest("figure");
    editImageCaption($("img", figure) || figure);
  });

  editor.addEventListener("dblclick", (event) => {
    const image = event.target.closest("img[data-local-rich-image]");
    if (!image) return;
    editImageCaption(image);
  });

  $$("[data-editor-command]", container).forEach((button) => {
    button.addEventListener("mousedown", (event) => event.preventDefault());
    button.addEventListener("click", () => {
      const command = button.dataset.editorCommand;
      savedRange = saveRichSelection(editor);
      if (command === "createLink") {
        restoreRichSelection(editor, savedRange);
        const href = window.prompt(t("editorLinkPrompt"), "https://");
        if (href === null) return;
        try {
          const url = new URL(href, location.href);
          if (!["http:", "https:"].includes(url.protocol)) throw new Error("unsafe");
          document.execCommand("createLink", false, url.href);
        } catch {
          showToast(t("editorLinkInvalid"));
        }
        updateRichToolbarState(target);
        return;
      }
      if (command === "insertImage") {
        imageInput?.click();
        return;
      }
      restoreRichSelection(editor, savedRange);
      const value = button.dataset.editorValue || null;
      document.execCommand(command, false, value);
      updateRichToolbarState(target);
    });
  });

  imageInput?.addEventListener("change", () => {
    const file = imageInput.files?.[0];
    if (file && insertRichImage(editor, savedRange || saveRichSelection(editor), file)) {
      imageInput.value = "";
      updateRichToolbarState(target);
    }
  });
  window.requestAnimationFrame(() => updateRichToolbarState(target));
}

function richEditorMarkup(target) {
  const draft = state.richDrafts[target] || "";
  const button = (command, label, content, value = "", toggle = false) => `
    <button
      type="button"
      class="editor-tool"
      data-editor-command="${command}"
      ${value ? `data-editor-value="${value}"` : ""}
      aria-label="${label}"
      title="${label}"
      ${toggle ? 'aria-pressed="false"' : ""}
    >${content}</button>
  `;
  return `
    <div class="editor-shell">
      <div class="editor-toolbar" role="toolbar" aria-label="${t("editorAria")}">
        ${button("formatBlock", t("editorHeading2"), "H2", "h2")}
        ${button("formatBlock", t("editorHeading3"), "H3", "h3")}
        <span class="editor-tool-divider" aria-hidden="true"></span>
        ${button("bold", t("editorBold"), "<strong>B</strong>", "", true)}
        ${button("italic", t("editorItalic"), "<em>I</em>", "", true)}
        ${button("underline", t("editorUnderline"), "<u>U</u>", "", true)}
        ${button("strikeThrough", t("editorStrike"), "<s>S</s>", "", true)}
        <span class="editor-tool-divider" aria-hidden="true"></span>
        ${button("insertUnorderedList", t("editorBulletList"), "•")}
        ${button("insertOrderedList", t("editorOrderedList"), "1.")}
        ${button("formatBlock", t("editorQuote"), "“", "blockquote")}
        ${button("formatBlock", t("editorCode"), "&lt;/&gt;", "pre")}
        ${button("createLink", t("editorLink"), '<svg><use href="#icon-link"></use></svg>')}
        ${button("insertImage", t("editorImage"), '<svg><use href="#icon-image"></use></svg>')}
        <span class="editor-tool-divider" aria-hidden="true"></span>
        ${button("undo", t("editorUndo"), "↶")}
        ${button("redo", t("editorRedo"), "↷")}
        ${button("removeFormat", t("editorClear"), "Tx")}
      </div>
      <div
        class="rich-editor"
        contenteditable="true"
        role="textbox"
        aria-multiline="true"
        aria-label="${t("formBody")}"
        data-rich-editor
        data-placeholder="${t("editorPlaceholder")}"
      >${draft}</div>
      <input
        type="file"
        hidden
        data-editor-image-input
        accept=".jpg,.jpeg,.png,.webp,.heic,.heif,image/jpeg,image/png,image/webp,image/heic,image/heif"
      />
      <div class="editor-hint">${t("editorHint")}</div>
    </div>
  `;
}

function renderCaptureForm(mode, target = "page") {
  const container = target === "dialog" ? $("#dialogFields") : $("#captureContent");
  const inputId = `${target}-title`;
  const bodyId = `${target}-body`;
  const urlId = `${target}-url`;
  const fileId = `${target}-file`;
  const openTag = target === "page"
    ? `<form class="capture-form" data-capture-form="page">`
    : `<div class="capture-form">`;
  const closeTag = target === "page" ? "</form>" : "</div>";
  const actions = target === "page"
    ? `
        <div class="form-actions">
          <button class="outline-button" type="button" data-view="home">${t("cancel")}</button>
          <button class="primary-button" type="submit">${t("saveAuto")}</button>
        </div>
      `
    : "";

  if (mode === "manual") {
    container.innerHTML = `
      ${openTag}
        <div class="field-group">
          <label for="${inputId}">${t("formTitle")}</label>
          <input class="text-input" id="${inputId}" name="title" placeholder="给这条知识一个名字" required />
        </div>
        <div class="field-group rich-field">
          <span class="field-label" id="${bodyId}-label">${t("formBody")}</span>
          ${richEditorMarkup(target)}
        </div>
        <div class="field-group">
          <label for="${urlId}">${t("formNote")}</label>
          <input class="text-input" id="${urlId}" name="note" placeholder="可选：你为什么要保留它" />
        </div>
        ${actions}
      ${closeTag}
      <aside class="capture-side">
        <h3>${state.lang === "en" ? "Automatic organization" : "自动整理"}</h3>
        <p>${state.lang === "en" ? "The system will generate a summary, category, and tags. You can correct them later." : "系统会生成摘要、分类和标签；之后你可以纠正并锁定字段。"}</p>
        <h3>${state.lang === "en" ? "Search immediately" : "立即进入检索"}</h3>
        <p>${state.lang === "en" ? "Saving happens before AI classification. The item is searchable right away." : "保存不等待 AI 完成，条目会立即进入检索。"}</p>
      </aside>
    `;
    attachRichEditor(container, target);
  } else if (mode === "webpage") {
    container.innerHTML = `
      ${openTag}
        <div class="field-group">
          <label for="${urlId}">${t("formUrl")}</label>
          <input class="text-input" id="${urlId}" name="url" type="url" placeholder="https://developer.mozilla.org/zh-CN/docs/Web" required />
        </div>
        <div class="field-group">
          <label for="${inputId}">${t("formNote")}</label>
          <input class="text-input" id="${inputId}" name="note" placeholder="可选：这条链接与什么有关" />
        </div>
        ${target === "page" ? `
          <div class="form-actions">
            <button class="outline-button" type="button" data-view="home">${t("cancel")}</button>
            <button class="primary-button" type="submit">${state.lang === "en" ? "Capture page" : "截图并分析"}</button>
          </div>
        ` : ""}
      ${closeTag}
      <aside class="capture-side">
        <h3>${state.lang === "en" ? "One page, no crawling" : "只截图，不爬取"}</h3>
        <p>${state.lang === "en" ? "Only the page you submit is rendered. The system does not follow links or traverse the site." : "只渲染你明确提交的这一页；系统不会跟随链接或遍历站点。"}</p>
        <h3>${state.lang === "en" ? "Fallback" : "失败回退"}</h3>
        <p>${state.lang === "en" ? "If rendering fails, paste a screenshot in the Screenshot tab." : "如果渲染失败，可以到截图入口粘贴页面截图。"}</p>
      </aside>
    `;
  } else if (mode === "document") {
    container.innerHTML = `
      ${openTag}
        <div class="file-drop" data-file-drop>
          <svg><use href="#icon-file"></use></svg>
          <p>${t("documentDropTitle")}</p>
          <small class="table-label">${t("formFileHint")}</small>
          <small class="table-label">${t("documentDropHint")}</small>
          <button class="outline-button file-picker-button" type="button" data-file-picker>${t("formFile")}</button>
        </div>
        <input
          id="${fileId}"
          name="file"
          type="file"
          hidden
          data-file-input
          accept=".pdf,.doc,.docx,.txt,.md,.rtf,.jpg,.jpeg,.png,.webp,.heic,.heif,application/pdf,image/jpeg,image/png,image/webp,image/heic,image/heif"
        />
        <div class="file-selection" data-file-selection hidden></div>
        <div class="field-group">
          <label for="${inputId}">${t("formNote")}</label>
          <input class="text-input" id="${inputId}" name="note" placeholder="可选：补充上下文" />
        </div>
        ${target === "page" ? `
          <div class="form-actions">
            <button class="outline-button" type="button" data-view="home">${t("cancel")}</button>
            <button class="primary-button" type="submit">${state.lang === "en" ? "Scan and organize" : "安全扫描并整理"}</button>
          </div>
        ` : ""}
      ${closeTag}
      <aside class="capture-side">
        <h3>${state.lang === "en" ? "Documents and images" : "文档与图片"}</h3>
        <p>${state.lang === "en" ? "Choose documents or JPG, PNG, WebP, and HEIC/HEIF images. Both use the same safety scan." : "可选择文档或 JPG、PNG、WebP、HEIC/HEIF 图片，二者使用同一套安全扫描。"}</p>
        <h3>${state.lang === "en" ? "Fail closed" : "安全优先"}</h3>
        <p>${t("safetyNote")}</p>
      </aside>
    `;
    attachDocumentFileControls(container, target);
    paintSelectedFile(target, state.selectedFiles[target]);
  } else {
    const pasted = state.pastedScreenshots[target];
    container.innerHTML = `
      ${openTag}
        <div
          class="paste-zone${pasted ? " has-image" : ""}"
          data-paste-zone="${target}"
        >
          <textarea
            class="paste-capture"
            data-paste-input
            rows="1"
            spellcheck="false"
            aria-label="${t("pasteInputAria")}"
          ></textarea>
          <div class="paste-visual" data-paste-visual aria-hidden="true"></div>
        </div>
        <div class="field-group paste-note-field">
          <label for="${inputId}">${t("formNote")}</label>
          <input class="text-input" id="${inputId}" name="note" placeholder="可选：补充上下文" />
        </div>
        ${target === "page" ? `
          <div class="form-actions">
            <button class="outline-button" type="button" data-view="home">${t("cancel")}</button>
            <button class="primary-button" type="submit" data-submit-screenshot="${target}" disabled>${state.lang === "en" ? "Scan and organize" : "安全扫描并整理"}</button>
          </div>
        ` : ""}
      ${closeTag}
      <aside class="capture-side">
        <h3>${t("screenshotClipboardOnly")}</h3>
        <p>${state.lang === "en" ? "Click the paste area, copy a screenshot, then paste. This tab has no file chooser." : "点击粘贴区、复制截图后执行粘贴；这个入口不提供文件选择。"}</p>
        <button class="text-link capture-fallback" type="button" data-switch-${target}="document">${t("goToDocument")}</button>
        <h3>${state.lang === "en" ? "Original image" : "原始图片"}</h3>
        <p>${state.lang === "en" ? "The original remains only when no secret is detected. Otherwise only a verified redacted image remains." : "未检测到密钥时保留原图；检测到密钥时只保留经过验证的脱敏派生图。"}</p>
      </aside>
    `;
    paintPastedScreenshot(target);
    const pasteInput = $(`[data-paste-zone="${target}"] [data-paste-input]`, container);
    window.requestAnimationFrame(() => pasteInput?.focus());
  }
}

function renderCapturePage() {
  renderCaptureForm(state.captureMode, "page");
}

function switchCaptureMode(mode, target = "page") {
  const container = target === "dialog" ? $("#dialogFields") : $("#captureContent");
  const editor = $("[data-rich-editor]", container);
  if (editor) state.richDrafts[target] = sanitizeRichHtml(editor.innerHTML);
  const currentNote = $('input[name="note"]', container)?.value;
  if (typeof currentNote === "string") state.notes[target] = currentNote;
  if (target === "dialog") {
    state.dialogMode = mode;
    renderDialogFields();
  } else {
    state.captureMode = mode;
    $$(".capture-tab").forEach((tab) => {
      const active = tab.dataset.captureMode === mode;
      tab.classList.toggle("is-active", active);
      tab.setAttribute("aria-selected", String(active));
      tab.tabIndex = active ? 0 : -1;
    });
    renderCapturePage();
  }
  const nextContainer = target === "dialog" ? $("#dialogFields") : $("#captureContent");
  const nextNote = $('input[name="note"]', nextContainer);
  if (nextNote) nextNote.value = state.notes[target];
}

function setLanguage(lang) {
  state.lang = lang;
  document.documentElement.lang = lang === "en" ? "en" : "zh-CN";
  $("#languageLabel").textContent = lang === "en" ? "中" : "EN";
  $$("[data-i18n]").forEach((node) => {
    node.textContent = t(node.dataset.i18n);
  });
  const navLabels = {
    home: lang === "en" ? "Home" : "首页",
    library: lang === "en" ? "Library" : "知识库",
    capture: lang === "en" ? "Capture" : "采集",
    processing: lang === "en" ? "In progress" : "待处理",
    trash: lang === "en" ? "Trash" : "回收站",
    settings: lang === "en" ? "Settings" : "设置",
  };
  $$(".rail-item").forEach((button) => {
    const label = $("span", button);
    if (label) label.textContent = navLabels[button.dataset.view];
  });
  $("#searchInput").placeholder = t("searchPlaceholder");
  $("#defaultLanguage").value = lang;
  renderHomeResults();
  renderDepthMap();
  renderQueuePreview();
  renderKnowledgeTable();
  renderProcessing();
  renderTrash();
  renderCapturePage();
  renderAccountProfile();
  if (state.view === "detail" && state.currentDetailId) renderDetail(state.currentDetailId);
  updateStaticDynamicLabels();
}

function updateStaticDynamicLabels() {
  $$(".filter-chip").forEach((chip) => {
    const count = $("span", chip)?.textContent || "";
    const label = chip.dataset.filter === "all" ? t("all") : t(chip.dataset.filter);
    chip.childNodes[0].nodeValue = `${label} `;
    if (!chip.querySelector("span")) {
      const countNode = document.createElement("span");
      countNode.textContent = count;
      chip.append(countNode);
    }
  });
  $$(".capture-tab").forEach((tab) => {
    const mode = tab.dataset.captureMode;
    tab.textContent = mode === "manual"
      ? (state.lang === "en" ? "Manual" : "手动录入")
      : mode === "webpage"
        ? (state.lang === "en" ? "Web page" : "网页链接")
        : mode === "document"
          ? (state.lang === "en" ? "Document" : "文档")
          : (state.lang === "en" ? "Screenshot" : "截图");
  });
  $$(".dialog-tabs button").forEach((tab) => {
    const mode = tab.dataset.dialogMode;
    tab.textContent = mode === "manual"
      ? (state.lang === "en" ? "Text" : "文字")
      : mode === "webpage"
        ? (state.lang === "en" ? "Web" : "网页")
        : mode === "document"
          ? (state.lang === "en" ? "Document" : "文档")
          : (state.lang === "en" ? "Screenshot" : "截图");
  });
}

function renderDialogFields() {
  renderCaptureForm(state.dialogMode, "dialog");
  $("#captureForm").dataset.captureMode = state.dialogMode;
  $$(".dialog-tabs button").forEach((button) => {
    const active = button.dataset.dialogMode === state.dialogMode;
    button.classList.toggle("is-active", active);
    button.setAttribute("aria-selected", String(active));
    button.tabIndex = active ? 0 : -1;
  });
  updateStaticDynamicLabels();
}

function openCaptureDialog(mode = "manual", editingItem = null) {
  state.editingItemId = editingItem?.id || null;
  if (editingItem) {
    state.richDrafts.dialog = sanitizeRichHtml(
      editingItem.bodyHtml || editingItem.body.map((paragraph) => `<p>${escapeHtml(paragraph)}</p>`).join(""),
    );
    state.notes.dialog = "";
  }
  state.dialogMode = mode;
  renderDialogFields();
  const dialog = $("#captureDialog");
  if (!dialog.open) dialog.showModal();
  const tabs = $(".dialog-tabs", dialog);
  const heading = $(".dialog-heading h2", dialog);
  const submit = $(".dialog-actions .primary-button", dialog);
  if (tabs) tabs.hidden = Boolean(editingItem);
  if (editingItem) {
    const titleInput = $('#dialog-title');
    if (titleInput) titleInput.value = editingItem.title;
    if (heading) heading.textContent = t("editKnowledgeTitle");
    if (submit) submit.textContent = t("saveChanges");
  } else {
    if (heading) heading.textContent = t("captureDialogTitle");
    if (submit) submit.textContent = t("saveAuto");
  }
  if (mode === "screenshot") {
    window.requestAnimationFrame(() => $('[data-paste-zone="dialog"] [data-paste-input]')?.focus());
  }
}

function openEditDialog() {
  const item = items.find((candidate) => candidate.id === state.currentDetailId);
  if (!item) return;
  openCaptureDialog("manual", item);
}

function addItem({
  title,
  body,
  bodyHtml = "",
  category = t("uncategorized"),
  tags = ["新收录"],
  source = "manual",
  sourceLabel = "手动录入",
  status = "processing",
  finalStatus = "done",
  finalCategory = "产品设计",
  attachments = [],
}) {
  const id = `k${Date.now()}`;
  const newItem = {
    id,
    title,
    summary: body.length > 72 ? `${body.slice(0, 72)}…` : body,
    body: body.split(/\n{2,}/).map((part) => part.trim()).filter(Boolean),
    bodyHtml,
    category,
    tags,
    source,
    sourceLabel,
    status,
    updated: "刚刚",
    depth: "surface",
    confidence: 0.54,
    attachments,
  };
  items.unshift(newItem);
  updateAllCounts();
  renderHomeResults();
  renderDepthMap();
  renderQueuePreview();
  renderKnowledgeTable();
  renderProcessing();
  showToast(t("saved"));
  window.setTimeout(() => {
    newItem.status = finalStatus;
    newItem.category = category === t("uncategorized") ? finalCategory : category;
    newItem.tags = tags.length ? tags : ["自动标签"];
    updateAllCounts();
    renderHomeResults();
    renderQueuePreview();
    renderKnowledgeTable();
    renderProcessing();
  }, 1500);
  return newItem;
}

function saveExternalResult(resultId) {
  const result = externalResults.find((candidate) => candidate.id === resultId);
  if (!result) return;
  addItem({
    title: result.title,
    body: result.summary,
    source: "webpage",
    sourceLabel: result.domain,
    status: "review",
    finalStatus: "review",
    tags: ["互联网结果", "待复核"],
  });
  state.externalVisible = false;
  renderHomeResults();
  showToast(t("internetSaved"));
}

function handleCaptureSubmit(form, target) {
  const mode = target === "dialog" ? state.dialogMode : state.captureMode;
  const formData = new FormData(form);
  const note = String(formData.get("note") || "").trim();
  let editedItemId = null;
  if (mode === "manual") {
    const title = String(formData.get("title") || "").trim();
    const editor = getRichEditor(target);
    const bodyHtml = sanitizeRichHtml(editor?.innerHTML || "");
    const body = String(editor?.innerText || "").trim();
    if (!title || !body) return;
    const editingItem = items.find((candidate) => candidate.id === state.editingItemId);
    if (editingItem) {
      editedItemId = editingItem.id;
      editingItem.title = title;
      editingItem.summary = body.length > 72 ? `${body.slice(0, 72)}…` : body;
      editingItem.body = body.split(/\n{2,}/).map((part) => part.trim()).filter(Boolean);
      editingItem.bodyHtml = bodyHtml;
      editingItem.updated = "刚刚";
      updateAllCounts();
      renderHomeResults();
      renderKnowledgeTable();
    } else {
      addItem({
        title,
        body,
        bodyHtml,
        source: "manual",
        sourceLabel: "手动录入",
        tags: ["手动录入"],
      });
    }
    state.richDrafts[target] = "";
  } else if (mode === "webpage") {
    const url = String(formData.get("url") || "").trim();
    if (!url) return;
    let host = "网页";
    try {
      host = new URL(url).hostname;
    } catch {
      host = "网页";
    }
    addItem({
      title: note || `网页收藏：${host}`,
      body: `正在分析 ${host} 的单页渲染截图。原型中会模拟自动摘要、分类和标签。`,
      source: "webpage",
      sourceLabel: host,
      status: "processing",
      finalStatus: "review",
      tags: ["网页截图", "自动整理"],
    });
  } else if (mode === "document") {
    const file = state.selectedFiles[target] || formData.get("file");
    if (!file || file.size === 0) {
      showToast(t("fileMissing"));
      return;
    }
    const image = isImageFile(file);
    const attachmentUrl = image ? URL.createObjectURL(file) : null;
    addItem({
      title: image ? "新的图片知识" : "新的文档知识",
      body: note || (image ? "图片正在提取可见文字、扫描密钥和自动归类。" : "文档正在提取文字、扫描密钥和自动归类。"),
      source: "document",
      sourceLabel: file.name,
      status: "processing",
      finalStatus: "review",
      tags: [image ? "图片文字识别" : "文档解析", "安全扫描"],
      attachments: image
        ? [{ kind: "image", name: file.name, url: attachmentUrl, safety: "clean" }]
        : [],
    });
    state.selectedFiles[target] = null;
  } else {
    const pasted = state.pastedScreenshots[target];
    if (!pasted?.file) {
      showToast(t("screenshotPasteEmpty"));
      return;
    }
    const attachmentUrl = pasted.file instanceof Blob
      ? URL.createObjectURL(pasted.file)
      : pasted.previewUrl;
    addItem({
      title: "新的截图知识",
      body: note || "剪贴板截图正在做 OCR、密钥扫描和自动归类。",
      source: "screenshot",
      sourceLabel: pasted.file.name || "剪贴板截图",
      status: "processing",
      finalStatus: "review",
      tags: ["截图分析", "剪贴板粘贴", "安全扫描"],
      attachments: [{ kind: "image", name: pasted.file.name, url: attachmentUrl, safety: "clean" }],
    });
    clearPastedScreenshot(target);
  }
  state.notes[target] = "";
  if (target === "dialog") {
    $("#captureDialog").close();
  } else {
    renderCapturePage();
  }
  if (editedItemId) {
    renderDetail(editedItemId);
    showView("detail");
  }
  renderHomeResults();
}

function showToast(message) {
  const toast = $("#toast");
  toast.textContent = message;
  toast.classList.add("is-visible");
  window.clearTimeout(showToast.timer);
  showToast.timer = window.setTimeout(() => toast.classList.remove("is-visible"), 2600);
}

function showView(view) {
  if (view === "detail" && !state.currentDetailId) view = "library";
  state.view = view;
  const activeView = $(`#view-${view}`);
  const canvas = $("main.canvas");
  if (activeView && canvas && canvas.firstElementChild !== activeView) {
    canvas.insertBefore(activeView, canvas.firstElementChild);
  }
  $$(".view").forEach((section) => {
    section.hidden = section.id !== `view-${view}`;
  });
  $$(".rail-item").forEach((button) => {
    button.classList.toggle("is-active", button.dataset.view === view);
  });
  if (view === "library") renderKnowledgeTable();
  if (view === "processing") renderProcessing();
  if (view === "trash") renderTrash();
  if (view === "capture") renderCapturePage();
  if (view === "profile") renderAccountProfile();
  if (view === "home") renderHomeResults();
  if (view === "detail") renderDetail(state.currentDetailId);
  window.scrollTo({ top: 0, behavior: "smooth" });
}

function restoreTrashEntry(entryId) {
  const index = trashItems.findIndex((entry) => entry.id === entryId);
  if (index < 0) return;
  const [entry] = trashItems.splice(index, 1);
  if (!items.some((item) => item.id === entry.item.id)) items.unshift(entry.item);
  updateAllCounts();
  renderTrash();
  renderHomeResults();
  renderKnowledgeTable();
  showToast(t("restored"));
}

function accountInitial(name) {
  return Array.from(String(name).trim())[0]?.toUpperCase() || "识";
}

function renderAccountProfile() {
  const initial = accountInitial(state.accountName);
  $("#accountMenuButton").textContent = initial;
  $("#accountAvatarLarge").textContent = initial;
  $("#accountDisplayName").textContent = state.accountName;
  $("#accountEmail").textContent = state.accountEmail;
  $("#accountMenuButton").setAttribute(
    "aria-label",
    t("accountOpenMenu", { name: state.accountName }),
  );
  $("#accountMenu").setAttribute("aria-label", t("accountMenuLabel"));
  $(".account-menu-summary", $("#accountMenu")).setAttribute(
    "aria-label",
    t("accountCurrentAccount"),
  );
  $("#profileAvatar").textContent = initial;
  $("#profileSummaryName").textContent = state.accountName;
  $("#profileSummaryEmail").textContent = state.accountEmail;
  $("#profileEmail").value = state.accountEmail;
  $("#profileDisplayName").value = state.accountName;
}

function setAccountMenuOpen(open) {
  const menu = $("#accountMenu");
  const button = $("#accountMenuButton");
  menu.hidden = !open;
  button.setAttribute("aria-expanded", String(open));
  if (open) {
    $('[role="menuitem"]', menu)?.focus();
  } else {
    button.focus();
  }
}

function normalizePrototypeName(value) {
  return value.trim().replace(/\s+/gu, " ");
}

function hasUnsafePrototypeControl(value) {
  return Array.from(value).some((character) => {
    const codePoint = character.codePointAt(0);
    return character === "<" ||
      character === ">" ||
      ((/\p{Cc}|\p{Cf}/u.test(character)) &&
        codePoint !== 0x200c &&
        codePoint !== 0x200d);
  });
}

function savePrototypeProfile(event) {
  event.preventDefault();
  const value = normalizePrototypeName($("#profileDisplayName").value);
  const length = Array.from(value).length;
  if (length < 1 || length > 40 || hasUnsafePrototypeControl(value)) {
    showToast(t("profileInvalid"));
    return;
  }
  state.accountName = value;
  renderAccountProfile();
  $("#profileSaveStatus").textContent = t("profileSaved");
  syncProfileButtons();
  showToast(t("profileSaved"));
}

function syncProfileButtons() {
  const dirty =
    normalizePrototypeName($("#profileDisplayName").value) !== state.accountName;
  $("#profileSaveButton").disabled = !dirty;
  $("#profileCancelButton").disabled = !dirty;
  if (dirty) $("#profileSaveStatus").textContent = "";
}

function bindEvents() {
  document.addEventListener("click", (event) => {
    const accountButton = event.target.closest("#accountMenuButton");
    if (accountButton) {
      setAccountMenuOpen($("#accountMenu").hidden);
      return;
    }
    if (!event.target.closest(".account-control")) {
      const menu = $("#accountMenu");
      if (menu && !menu.hidden) {
        menu.hidden = true;
        $("#accountMenuButton").setAttribute("aria-expanded", "false");
      }
    }
    const captureFallback = event.target.closest("[data-switch-page], [data-switch-dialog]");
    if (captureFallback) {
      const target = captureFallback.hasAttribute("data-switch-dialog") ? "dialog" : "page";
      switchCaptureMode("document", target);
      return;
    }
    const clearScreenshot = event.target.closest("[data-clear-screenshot]");
    if (clearScreenshot) {
      clearPastedScreenshot(clearScreenshot.dataset.clearScreenshot);
      return;
    }
    const clearFile = event.target.closest("[data-clear-file]");
    if (clearFile) {
      const target = clearFile.dataset.clearFile;
      state.selectedFiles[target] = null;
      const container = target === "dialog" ? $("#dialogFields") : $("#captureContent");
      const input = $("[data-file-input]", container);
      if (input) input.value = "";
      paintSelectedFile(target, null);
      return;
    }
    const pasteZone = event.target.closest("[data-paste-zone]");
    if (pasteZone) {
      $("[data-paste-input]", pasteZone)?.focus();
      return;
    }
    const viewButton = event.target.closest("[data-view]");
    if (viewButton) {
      if (!$("#accountMenu").hidden) {
        $("#accountMenu").hidden = true;
        $("#accountMenuButton").setAttribute("aria-expanded", "false");
      }
      showView(viewButton.dataset.view);
      return;
    }
    const openCapture = event.target.closest("[data-open-capture]");
    if (openCapture) {
      openCaptureDialog("manual");
      return;
    }
    const itemButton = event.target.closest("[data-item-id]");
    if (itemButton) {
      openDetail(itemButton.dataset.itemId);
      return;
    }
    const saveExternal = event.target.closest("[data-save-external]");
    if (saveExternal) {
      saveExternalResult(saveExternal.dataset.saveExternal);
      return;
    }
    const review = event.target.closest("[data-review-id]");
    if (review) {
      const item = items.find((candidate) => candidate.id === review.dataset.reviewId);
      if (item?.status === "failed") {
        item.status = "processing";
        renderQueuePreview();
        renderProcessing();
        updateAllCounts();
        showToast(t("retryStarted"));
        window.setTimeout(() => {
          item.status = "done";
          updateAllCounts();
          renderQueuePreview();
          renderProcessing();
        }, 1900);
      } else {
        openDetail(review.dataset.reviewId);
      }
      return;
    }
    const openItem = event.target.closest("[data-open-id]");
    if (openItem) {
      openDetail(openItem.dataset.openId);
      return;
    }
    const restore = event.target.closest("[data-restore-id]");
    if (restore) {
      restoreTrashEntry(restore.dataset.restoreId);
    }
  });

  document.addEventListener("paste", (event) => {
    const dialog = $("#captureDialog");
    const target = dialog.open
      ? (state.dialogMode === "screenshot" ? "dialog" : null)
      : (state.view === "capture" && state.captureMode === "screenshot" ? "page" : null);
    if (!target) return;
    const imageItem = [...(event.clipboardData?.items || [])]
      .find((item) => item.kind === "file" && item.type.startsWith("image/"));
    if (!imageItem) return;
    event.preventDefault();
    const file = imageItem.getAsFile();
    if (file) {
      setPastedScreenshot(target, file);
      const input = $(`[data-paste-zone="${target}"] [data-paste-input]`);
      if (input) input.value = "";
    }
  });

  document.addEventListener("keydown", (event) => {
    if (event.key === "Escape" && !$("#accountMenu").hidden) {
      setAccountMenuOpen(false);
      return;
    }
    const tab = event.target.closest(".capture-tab, .dialog-tabs button");
    if (!tab || !["ArrowLeft", "ArrowRight", "Home", "End"].includes(event.key)) return;
    const tabs = [...tab.parentElement.querySelectorAll(".capture-tab, .dialog-tabs button")];
    if (tabs.length < 2) return;
    event.preventDefault();
    const current = tabs.indexOf(tab);
    const nextIndex = event.key === "Home"
      ? 0
      : event.key === "End"
        ? tabs.length - 1
        : (current + (event.key === "ArrowRight" ? 1 : -1) + tabs.length) % tabs.length;
    const next = tabs[nextIndex];
    if (next.classList.contains("capture-tab")) {
      switchCaptureMode(next.dataset.captureMode, "page");
    } else {
      state.dialogMode = next.dataset.dialogMode;
      renderDialogFields();
    }
    next.focus();
  });

  document.addEventListener("selectionchange", () => {
    const dialog = $("#captureDialog");
    const target = dialog.open && state.dialogMode === "manual"
      ? "dialog"
      : (!dialog.open && state.view === "capture" && state.captureMode === "manual" ? "page" : null);
    if (target) updateRichToolbarState(target);
  });

  $("#searchForm").addEventListener("submit", (event) => {
    event.preventDefault();
    state.query = $("#searchInput").value;
    state.externalVisible = false;
    renderHomeResults();
  });

  $("#internetTrigger").addEventListener("click", () => {
    state.externalVisible = true;
    const container = $("#externalResults");
    container.hidden = false;
    container.innerHTML = `<div class="loading-state">${t("internetLoading")}</div>`;
    window.setTimeout(() => renderExternalResults(), 850);
  });

  $("#languageToggle").addEventListener("click", () => {
    setLanguage(state.lang === "zh" ? "en" : "zh");
  });

  $("#accountLogout").addEventListener("click", () => {
    setAccountMenuOpen(false);
    $("#logoutDialog").showModal();
    $("#cancelLogout").focus();
  });
  $("#cancelLogout").addEventListener("click", () => $("#logoutDialog").close());
  $("#confirmLogout").addEventListener("click", () => {
    $("#logoutDialog").close();
    showToast(t("accountSignedOut"));
  });
  $("#profileForm").addEventListener("submit", savePrototypeProfile);
  $("#profileDisplayName").addEventListener("input", syncProfileButtons);
  $("#profileCancelButton").addEventListener("click", () => {
    renderAccountProfile();
    $("#profileSaveStatus").textContent = "";
    syncProfileButtons();
  });

  $("#detailBack").addEventListener("click", () => showView("library"));
  $("#editItemButton").addEventListener("click", openEditDialog);
  $("#reanalyzeButton").addEventListener("click", () => {
    showToast(t("reanalysis"));
  });

  $("#libraryFilters").addEventListener("click", (event) => {
    const chip = event.target.closest("[data-filter]");
    if (!chip) return;
    state.libraryFilter = chip.dataset.filter;
    $$(".filter-chip", $("#libraryFilters")).forEach((candidate) => candidate.classList.toggle("is-active", candidate === chip));
    renderKnowledgeTable();
  });

  $$(".capture-tab").forEach((tab) => {
    tab.addEventListener("click", () => {
      switchCaptureMode(tab.dataset.captureMode, "page");
    });
  });

  $$(".dialog-tabs button").forEach((tab) => {
    tab.addEventListener("click", () => {
      switchCaptureMode(tab.dataset.dialogMode, "dialog");
    });
  });

  $("#captureDialog").addEventListener("click", (event) => {
    if (event.target === $("#captureDialog")) $("#captureDialog").close();
  });
  $("#captureDialog").addEventListener("close", () => {
    const dialog = $("#captureDialog");
    const tabs = $(".dialog-tabs", dialog);
    const heading = $(".dialog-heading h2", dialog);
    const submit = $(".dialog-actions .primary-button", dialog);
    if (state.editingItemId) {
      state.richDrafts.dialog = "";
      state.notes.dialog = "";
    }
    state.editingItemId = null;
    if (tabs) tabs.hidden = false;
    if (heading) heading.textContent = t("captureDialogTitle");
    if (submit) submit.textContent = t("saveAuto");
  });

  document.addEventListener("submit", (event) => {
    const form = event.target.closest("[data-capture-form]");
    if (!form) return;
    if (event.submitter && event.submitter.value === "cancel") return;
    event.preventDefault();
    const target = form.dataset.captureForm;
    handleCaptureSubmit(form, target);
  });
}

function initialize() {
  bindEvents();
  setLanguage("zh");
  const params = new URLSearchParams(window.location.search);
  const requestedView = params.get("view");
  const requestedMode = params.get("mode");
  const requestedDialog = params.get("dialog");
  if (["manual", "webpage", "document", "screenshot"].includes(requestedMode)) {
    state.captureMode = requestedMode;
  }
  renderHomeResults();
  renderDepthMap();
  renderQueuePreview();
  renderProcessing();
  renderTrash();
  renderCapturePage();
  renderAccountProfile();
  syncProfileButtons();
  $$(".capture-tab").forEach((tab) => {
    tab.classList.toggle("is-active", tab.dataset.captureMode === state.captureMode);
  });
  if (["home", "library", "capture", "processing", "trash", "settings", "profile"].includes(requestedView)) {
    showView(requestedView);
  }
  if (["manual", "webpage", "document", "screenshot"].includes(requestedDialog)) {
    openCaptureDialog(requestedDialog);
  }
  if (params.get("demoScreenshot") === "1" && state.captureMode === "screenshot") {
    seedDemoPastedScreenshot();
    renderCapturePage();
  }
  updateStaticDynamicLabels();
  console.assert(
    Object.values(categoryCounts()).reduce((sum, count) => sum + count, 0) === items.length,
    "Category counts must reconcile with the knowledge total.",
  );
  console.assert(
    pendingItems().length === items.filter((item) => ["review", "processing", "failed"].includes(item.status)).length,
    "Pending queue count must come from item states.",
  );
}

initialize();
