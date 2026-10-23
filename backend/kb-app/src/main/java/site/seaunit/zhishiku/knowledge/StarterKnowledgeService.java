package site.seaunit.zhishiku.knowledge;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.seaunit.zhishiku.owner.OwnerAccountEntity;
import site.seaunit.zhishiku.richtext.RichDocumentEntity;
import site.seaunit.zhishiku.richtext.RichDocumentRepository;
import site.seaunit.zhishiku.richtext.migration.LegacyTextDocumentConverter;
import site.seaunit.zhishiku.search.richtext.SearchProjection;
import site.seaunit.zhishiku.search.richtext.SearchProjectionBuilder;

@Service
public class StarterKnowledgeService {
  private static final String STARTER_CONVERTER_VERSION = "starter-v1";
  private static final List<StarterKnowledge> STARTER_KNOWLEDGE =
      List.of(
          new StarterKnowledge(
              "深海工作法：用三层归档减少认知噪音",
              "将知识按使用频率和决策距离分成表层、中层和深海。表层保存七天内要继续处理的材料，"
                  + "中层放正在推进的项目，深海保存低频但长期重要的资料。\n\n"
                  + "归档的目的不是把内容藏起来，而是让每一层只承担一种注意力成本。"
                  + "当需要快速行动时看表层，当需要推进项目时看中层，当需要追溯依据时再潜入深海。",
              "用深度而不是文件夹数量组织知识。",
              "产品设计",
              List.of("知识管理", "分类")),
          new StarterKnowledge(
              "Agentic RAG 的检索策略笔记",
              "Agentic RAG 先判断问题需要哪些实体、约束和同义表达，再执行多轮召回与重排。"
                  + "它适合处理只靠关键词匹配会漏掉的自然语言问题。\n\n"
                  + "关键不是让模型随意扩展搜索，而是把候选正文重新交给模型阅读，"
                  + "并坚持引用实际支持结论的知识条目。",
              "先查询、再阅读、后回答，降低单次字符串匹配造成的漏检。",
              "人工智能",
              List.of("RAG", "检索")),
          new StarterKnowledge(
              "把网页保存为可检索知识的最小路径",
              "提交一个公开网页后，识海只读取这一页的可见内容，识别正文、图片和图表，"
                  + "再由 AI 整理成可检索的结构化知识。\n\n"
                  + "系统不会跟随页面中的链接，也不会遍历站点。最终保存的是经过整理的正文，"
                  + "而不是一条只有网址的收藏。",
              "单页读取与 AI 整理，让网页收藏真正进入知识库。",
              "工程实践",
              List.of("网页采集", "OCR")),
          new StarterKnowledge(
              "产品访谈：知识工作者如何整理收藏",
              "访谈中的共同问题是：收藏很快，但再次使用时找不到上下文。"
                  + "真正消耗时间的不是保存，而是回忆当时为什么保存、它属于哪个主题、"
                  + "以及它和当前任务有什么关系。\n\n"
                  + "因此知识工具需要同时解决录入、自动分类、全文检索和对话式复用，"
                  + "而不是只提供一个收藏按钮。",
              "知识整理的难点不是收藏，而是未来能否重新找到并使用。",
              "研究",
              List.of("访谈", "知识工作")),
          new StarterKnowledge(
              "截图中的文字如何被安全识别",
              "截图会先经过格式、尺寸和元数据检查，再进行 OCR 文字识别。"
                  + "识别出的文本会扫描可能的密钥、令牌和密码，并在发给外部 AI 前完成脱敏。\n\n"
                  + "截图本身只用于本次识别，不会成为默认附件。"
                  + "系统最终保存的是经过整理、可搜索的知识内容。",
              "截图先扫描再 OCR，脱敏后才能由 AI 整理入库。",
              "工程实践",
              List.of("截图", "OCR")),
          new StarterKnowledge(
              "一次关于自动标签的评审",
              "自动标签的价值是减少整理负担，但用户纠正过的字段必须被锁定。"
                  + "后续 AI 重新分析可以补充新标签，却不能覆盖用户确认过的分类和名称。\n\n"
                  + "这条规则把自动化与可控性放在一起：系统先给出合理结果，"
                  + "用户只需要在关键位置做最终决定。",
              "自动分类必须尊重用户锁定，避免每次重新分析都覆盖人工判断。",
              "产品设计",
              List.of("标签", "评审")));

  private final KnowledgeItemRepository items;
  private final RichDocumentRepository documents;
  private final ClassificationService classifications;
  private final SearchProjectionBuilder searchProjections;
  private final LegacyTextDocumentConverter legacyConverter;
  private final JdbcTemplate jdbc;
  private final ObjectMapper mapper;

  public StarterKnowledgeService(
      KnowledgeItemRepository items,
      RichDocumentRepository documents,
      ClassificationService classifications,
      SearchProjectionBuilder searchProjections,
      LegacyTextDocumentConverter legacyConverter,
      JdbcTemplate jdbc,
      ObjectMapper mapper) {
    this.items = items;
    this.documents = documents;
    this.classifications = classifications;
    this.searchProjections = searchProjections;
    this.legacyConverter = legacyConverter;
    this.jdbc = jdbc;
    this.mapper = mapper;
  }

  @Transactional
  public void seedForOwner(OwnerAccountEntity owner) {
    if (items.countByOwnerId(owner.getId()) > 0) {
      return;
    }
    STARTER_KNOWLEDGE.forEach(document -> create(owner, document));
  }

  private void create(OwnerAccountEntity owner, StarterKnowledge source) {
    LegacyTextDocumentConverter.Conversion conversion = legacyConverter.convert(source.body());
    KnowledgeItemEntity item =
        KnowledgeItemEntity.create(
            owner.getId(), source.title(), conversion.plainText(), source.summary(), "succeeded");
    item.updateGeneratedSummary(source.summary());
    item = items.saveAndFlush(item);
    RichDocumentEntity document =
        RichDocumentEntity.create(
            item.getId(),
            conversion.document(),
            conversion.plainText(),
            conversion.sourceHash(),
            0,
            0,
            item.getVersion());
    documents.save(document);

    Timestamp now = Timestamp.from(Instant.now());
    UUID sourceId = UUID.randomUUID();
    jdbc.update(
        """
        INSERT INTO rich_document_source
          (id, item_id, source_kind, source_url, source_host, captured_at,
           converted_at, converter_version, content_hash, created_at)
        VALUES (?, ?, 'native', NULL, NULL, NULL, ?, ?, ?, ?)
        """,
        sourceId,
        item.getId(),
        now,
        STARTER_CONVERTER_VERSION,
        conversion.sourceHash(),
        now);
    jdbc.update(
        """
        INSERT INTO rich_document_conversion_report
          (id, source_id, preserved_features, downgraded_features,
           removed_features, rejected_features, converter_version, created_at)
        VALUES (?, ?, ARRAY[]::text[], ARRAY[]::text[], ARRAY[]::text[],
                ARRAY[]::text[], ?, ?)
        """,
        UUID.randomUUID(),
        sourceId,
        STARTER_CONVERTER_VERSION,
        now);
    jdbc.update(
        """
        INSERT INTO rich_document_revision
          (id, item_id, revision_no, schema_version, document, plain_text,
           document_hash, reason, source_id, created_at)
        VALUES (?, ?, 1, 1, ?::jsonb, ?, ?, 'create', ?, ?)
        """,
        UUID.randomUUID(),
        item.getId(),
        writeJson(conversion.document()),
        conversion.plainText(),
        conversion.sourceHash(),
        sourceId,
        now);

    SearchProjection projection =
        searchProjections.withRevision(
            searchProjections.project(
                item.getTitle(), conversion.document(), item.getNote()),
            item.getSearchRevision());
    jdbc.update(
        """
        INSERT INTO search_document
          (item_id, owner_id, title_text, body_text, note_text, image_text,
           search_text, search_revision, updated_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        item.getId(),
        owner.getId(),
        projection.titleText(),
        projection.bodyText(),
        projection.noteText(),
        projection.imageText(),
        projection.searchText(),
        projection.expectedSearchRevision(),
        now);
    classifications.assignAi(
        owner.getId(), item.getId(), source.category(), source.tags(), 1.0);
  }

  private String writeJson(com.fasterxml.jackson.databind.JsonNode document) {
    try {
      return mapper.writeValueAsString(document);
    } catch (com.fasterxml.jackson.core.JsonProcessingException error) {
      throw new IllegalStateException("Starter knowledge could not be serialized.", error);
    }
  }

  private record StarterKnowledge(
      String title,
      String body,
      String summary,
      String category,
      List<String> tags) {}
}
