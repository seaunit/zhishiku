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
      "系统先获得渲染截图，再进行 OCR、密钥扫描与自动归类；截图能力失败时回退到用户上传截图。",
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
    authVisionDesc: "网页渲染图与用户上传截图",
    authSearchTitle: "DeepSeek Search · 查询",
    authSearchDesc: "仅在你主动继续互联网搜索时发送查询词",
    settingsLanguageTitle: "默认语言",
    settingsLanguageDesc: "界面默认中文，用户内容不会被自动翻译。",
    settingsRetentionTitle: "回收站保留",
    settingsRetentionDesc: "删除后 30 天永久清理，备份会按声明周期淘汰。",
    retentionValue: "30 天",
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
    formFileHint: "原型中不会真正上传文件",
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
    authVisionDesc: "Rendered pages and user-uploaded screenshots",
    authSearchTitle: "DeepSeek Search · Query",
    authSearchDesc: "Only the query is sent when you explicitly continue to internet search",
    settingsLanguageTitle: "Default language",
    settingsLanguageDesc: "The interface defaults to Chinese. User content is never translated automatically.",
    settingsRetentionTitle: "Trash retention",
    settingsRetentionDesc: "Deleted items purge after 30 days. Backups expire on the declared schedule.",
    retentionValue: "30 days",
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
    formFileHint: "This prototype does not upload the file",
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
  currentDetailId: null,
  externalVisible: false,
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
      <div class="detail-body">
        ${item.body.map((paragraph) => `<p>${escapeHtml(paragraph)}</p>`).join("")}
      </div>
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
        <div class="field-group">
          <label for="${bodyId}">${t("formBody")}</label>
          <textarea class="text-area" id="${bodyId}" name="body" placeholder="粘贴文字、想法或摘录……" required></textarea>
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
        <p>${state.lang === "en" ? "If rendering fails, you can upload a screenshot yourself." : "如果渲染失败，可以改为上传页面截图。"}</p>
      </aside>
    `;
  } else {
    const isScreenshot = mode === "screenshot";
    container.innerHTML = `
      ${openTag}
        <label class="file-drop" for="${fileId}">
          <svg><use href="#${isScreenshot ? "icon-image" : "icon-file"}"></use></svg>
          <p>${isScreenshot ? (state.lang === "en" ? "Drop or choose a screenshot" : "拖入或选择一张截图") : (state.lang === "en" ? "Drop or choose a document" : "拖入或选择一个文档")}</p>
          <small class="table-label">${t("formFileHint")}</small>
        </label>
        <input id="${fileId}" name="file" type="file" hidden accept="${isScreenshot ? "image/*" : ".pdf,.doc,.docx,.txt,.md"}" />
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
        <h3>${state.lang === "en" ? "Fail closed" : "安全优先"}</h3>
        <p>${t("safetyNote")}</p>
        <h3>${state.lang === "en" ? "Original file" : "原始文件"}</h3>
        <p>${state.lang === "en" ? "Screenshots keep the original only when no secret is detected. Otherwise only a redacted derivative remains." : "截图未检测到密钥时保留原图；检测到密钥时只保留脱敏派生图。"}</p>
      </aside>
    `;
    const fileInput = $(`#${fileId}`);
    fileInput?.addEventListener("change", () => {
      const file = fileInput.files?.[0];
      if (file) {
        $(".file-drop p", container).textContent = file.name;
      }
    });
  }
}

function renderCapturePage() {
  renderCaptureForm(state.captureMode, "page");
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
    button.classList.toggle("is-active", button.dataset.dialogMode === state.dialogMode);
  });
  updateStaticDynamicLabels();
}

function openCaptureDialog(mode = "manual") {
  state.dialogMode = mode;
  renderDialogFields();
  const dialog = $("#captureDialog");
  if (!dialog.open) dialog.showModal();
}

function addItem({
  title,
  body,
  category = "待整理",
  tags = ["新收录"],
  source = "manual",
  sourceLabel = "手动录入",
  status = "processing",
  finalStatus = "done",
  finalCategory = "产品设计",
}) {
  const id = `k${Date.now()}`;
  const newItem = {
    id,
    title,
    summary: body.length > 72 ? `${body.slice(0, 72)}…` : body,
    body: [body],
    category,
    tags,
    source,
    sourceLabel,
    status,
    updated: "刚刚",
    depth: "surface",
    confidence: 0.54,
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
    newItem.category = category === "待整理" ? finalCategory : category;
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
  if (mode === "manual") {
    const title = String(formData.get("title") || "").trim();
    const body = String(formData.get("body") || "").trim();
    if (!title || !body) return;
    addItem({ title, body, source: "manual", sourceLabel: "手动录入", category: "待整理", tags: ["待整理"] });
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
  } else {
    const file = formData.get("file");
    if (file && file.size === 0) return;
    const isScreenshot = mode === "screenshot";
    addItem({
      title: isScreenshot ? "新的截图知识" : "新的文档知识",
      body: note || (isScreenshot ? "截图正在做 OCR、密钥扫描和自动归类。" : "文档正在提取文字、扫描密钥和自动归类。"),
      source: isScreenshot ? "screenshot" : "document",
      sourceLabel: file?.name || (isScreenshot ? "截图" : "文档"),
      status: "processing",
      finalStatus: "review",
      tags: [isScreenshot ? "截图分析" : "文档解析", "安全扫描"],
    });
  }
  if (target === "dialog") $("#captureDialog").close();
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

function bindEvents() {
  document.addEventListener("click", (event) => {
    const viewButton = event.target.closest("[data-view]");
    if (viewButton) {
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

  $("#detailBack").addEventListener("click", () => showView("library"));
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
      state.captureMode = tab.dataset.captureMode;
      $$(".capture-tab").forEach((candidate) => candidate.classList.toggle("is-active", candidate === tab));
      renderCapturePage();
    });
  });

  $$(".dialog-tabs button").forEach((tab) => {
    tab.addEventListener("click", () => {
      state.dialogMode = tab.dataset.dialogMode;
      renderDialogFields();
    });
  });

  $("#captureDialog").addEventListener("click", (event) => {
    if (event.target === $("#captureDialog")) $("#captureDialog").close();
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
  renderHomeResults();
  renderDepthMap();
  renderQueuePreview();
  renderProcessing();
  renderTrash();
  renderCapturePage();
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
