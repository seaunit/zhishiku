package site.seaunit.zhishiku.knowledge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import site.seaunit.zhishiku.owner.OwnerAccountEntity;
import site.seaunit.zhishiku.richtext.RichDocumentEntity;
import site.seaunit.zhishiku.richtext.RichDocumentRepository;
import site.seaunit.zhishiku.richtext.migration.LegacyTextDocumentConverter;
import site.seaunit.zhishiku.search.richtext.SearchProjectionBuilder;

@ExtendWith(MockitoExtension.class)
class StarterKnowledgeServiceTest {
  @Mock private KnowledgeItemRepository items;
  @Mock private RichDocumentRepository documents;
  @Mock private ClassificationService classifications;
  @Mock private JdbcTemplate jdbc;

  private StarterKnowledgeService service;
  private OwnerAccountEntity owner;

  @BeforeEach
  void setUp() {
    ObjectMapper mapper = new ObjectMapper();
    service =
        new StarterKnowledgeService(
            items,
            documents,
            classifications,
            new SearchProjectionBuilder(mapper),
            new LegacyTextDocumentConverter(),
            jdbc,
            mapper);
    owner = OwnerAccountEntity.create("starter@example.com", "hash", "zh-CN");
  }

  @Test
  void seedsSixOrganizedStarterDocumentsWithRichTextSearchAndClassification() {
    when(items.countByOwnerId(owner.getId())).thenReturn(0L);
    when(items.saveAndFlush(any(KnowledgeItemEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    service.seedForOwner(owner);

    ArgumentCaptor<KnowledgeItemEntity> itemCaptor =
        ArgumentCaptor.forClass(KnowledgeItemEntity.class);
    verify(items, times(6)).saveAndFlush(itemCaptor.capture());
    assertThat(itemCaptor.getAllValues())
        .extracting(KnowledgeItemEntity::getProcessingStatus)
        .containsOnly("succeeded");
    assertThat(itemCaptor.getAllValues())
        .extracting(KnowledgeItemEntity::getTitle)
        .containsExactly(
            "深海工作法：用三层归档减少认知噪音",
            "Agentic RAG 的检索策略笔记",
            "把网页保存为可检索知识的最小路径",
            "产品访谈：知识工作者如何整理收藏",
            "截图中的文字如何被安全识别",
            "一次关于自动标签的评审");
    verify(documents, times(6)).save(any(RichDocumentEntity.class));
    verify(classifications, times(6))
        .assignAi(eq(owner.getId()), any(UUID.class), anyString(), anyList(), eq(1.0));
  }

  @Test
  void doesNotSeedAgainWhenTheAccountAlreadyHasKnowledge() {
    when(items.countByOwnerId(owner.getId())).thenReturn(1L);

    service.seedForOwner(owner);

    verify(items, never()).saveAndFlush(any(KnowledgeItemEntity.class));
    verify(documents, never()).save(any(RichDocumentEntity.class));
  }
}
