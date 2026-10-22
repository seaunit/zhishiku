package site.seaunit.zhishiku.knowledge;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import site.seaunit.zhishiku.attachment.AttachmentRepository;
import site.seaunit.zhishiku.idempotency.IdempotencyService;
import site.seaunit.zhishiku.knowledge.KnowledgeDtos.BodySourceRequest;
import site.seaunit.zhishiku.knowledge.KnowledgeDtos.CreateKnowledgeRequest;
import site.seaunit.zhishiku.knowledge.KnowledgeDtos.UpdateKnowledgeRequest;
import site.seaunit.zhishiku.owner.CurrentOwnerService;
import site.seaunit.zhishiku.richtext.RichDocumentRepository;
import site.seaunit.zhishiku.richtext.RichImageUploadRepository;
import site.seaunit.zhishiku.richtext.conflict.RichTextConflictPolicy;
import site.seaunit.zhishiku.richtext.lifecycle.RichTextLifecycleService;
import site.seaunit.zhishiku.richtext.provenance.SourceProvenancePolicy;
import site.seaunit.zhishiku.search.richtext.SearchProjectionBuilder;

class KnowledgeServiceManualSourceTest {
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final KnowledgeService service =
      new KnowledgeService(
          mock(CurrentOwnerService.class),
          mock(KnowledgeItemRepository.class),
          mock(RichDocumentRepository.class),
          mock(RichImageUploadRepository.class),
          mock(AttachmentRepository.class),
          mock(org.springframework.jdbc.core.JdbcTemplate.class),
          objectMapper,
          mock(RichTextConflictPolicy.class),
          new SourceProvenancePolicy(),
          mock(SearchProjectionBuilder.class),
          mock(RichTextLifecycleService.class),
          mock(ClassificationService.class),
          mock(IdempotencyService.class));

  @Test
  void rejectsNonManualSourcesAtTheOrdinaryCreationBoundary() {
    var document = objectMapper.createObjectNode().put("type", "doc");
    document.putArray("content").addObject().put("type", "paragraph");

    for (String kind : new String[] {"document", "screenshot", "internet_result", "text"}) {
      assertThatThrownBy(
              () ->
                  service.createManual(
                      new CreateKnowledgeRequest(
                          "标题",
                          document,
                          "",
                          new BodySourceRequest(kind, null, java.time.Instant.now())),
                      mock(Authentication.class)))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("non-manual");
    }
  }

  @Test
  void rejectsNonManualSourcesWhenSavingAnEditConflictCopy() {
    var document = objectMapper.createObjectNode().put("type", "doc");
    document.putArray("content").addObject().put("type", "paragraph");

    assertThatThrownBy(
            () ->
                service.update(
                    java.util.UUID.randomUUID(),
                    new UpdateKnowledgeRequest(
                        1,
                        "标题",
                        document,
                        "",
                        new BodySourceRequest("screenshot", null, java.time.Instant.now()),
                        "save_as_new"),
                    mock(Authentication.class)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("non-manual");
  }
}
