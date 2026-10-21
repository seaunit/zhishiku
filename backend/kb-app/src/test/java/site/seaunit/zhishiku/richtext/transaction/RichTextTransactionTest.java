package site.seaunit.zhishiku.richtext.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import site.seaunit.zhishiku.core.richtext.PlainTextProjector;
import site.seaunit.zhishiku.core.richtext.RichTextValidator;
import site.seaunit.zhishiku.richtext.provenance.RichTextConversionReport;
import site.seaunit.zhishiku.richtext.provenance.RichTextSourceCommand;
import site.seaunit.zhishiku.richtext.provenance.RichTextSourcePolicy;

class RichTextTransactionTest {
  private static final UUID ASSET_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private final ObjectMapper mapper = new ObjectMapper();
  private final RichTextTransactionWriter writer = mock(RichTextTransactionWriter.class);
  private final RichTextSavePlanner planner =
      new RichTextSavePlanner(RichTextValidator.defaults(), new PlainTextProjector(), mapper);
  private final RichTextAtomicSaveService service =
      new RichTextAtomicSaveService(writer, new RichTextSourcePolicy());

  @Test
  void plansDocumentProjectionHashAndUniqueImageBudgetBeforeWriting() throws Exception {
    RichTextSavePlan plan = planner.prepare(document(), Map.of(ASSET_ID, 1_024L));

    assertThat(plan.schemaVersion()).isEqualTo(1);
    assertThat(plan.plainText()).contains("标题", "正文", "图片说明", "替代文本");
    assertThat(plan.documentHash()).matches("[a-f0-9]{64}");
    assertThat(plan.imageCount()).isEqualTo(1);
    assertThat(plan.totalImageBytes()).isEqualTo(1_024L);
    assertThat(plan.document().path("content").get(1).path("attrs").path("assetId").asText())
        .isEqualTo(ASSET_ID.toString());
  }

  @Test
  void rejectsStaleVersionWithoutCallingTheAtomicWriter() throws Exception {
    UUID ownerId = UUID.randomUUID();
    UUID itemId = UUID.randomUUID();
    RichTextSavePlan plan = planner.prepare(document(), Map.of(ASSET_ID, 1_024L));
    when(writer.currentVersion(itemId, ownerId)).thenReturn(8L);

    assertThatThrownBy(() -> service.save(ownerId, itemId, 7L, plan, source(), report()))
        .isInstanceOf(RichTextVersionConflictException.class)
        .hasMessageContaining("8");
    verify(writer, never()).commit(any());
  }

  @Test
  void commitsDocumentSourceImagesAndSearchRevisionAsOneCommand() throws Exception {
    UUID ownerId = UUID.randomUUID();
    UUID itemId = UUID.randomUUID();
    RichTextSavePlan plan = planner.prepare(document(), Map.of(ASSET_ID, 1_024L));
    when(writer.currentVersion(itemId, ownerId)).thenReturn(7L);

    service.save(ownerId, itemId, 7L, plan, source(), report());

    verify(writer).commit(any(RichTextCommitCommand.class));
  }

  @Test
  void saveBoundaryIsTransactional() throws Exception {
    assertThat(
            RichTextAtomicSaveService.class
                .getMethod(
                    "save",
                    UUID.class,
                    UUID.class,
                    long.class,
                    RichTextSavePlan.class,
                    RichTextSourceCommand.class,
                    RichTextConversionReport.class)
                .isAnnotationPresent(
                    org.springframework.transaction.annotation.Transactional.class))
        .isTrue();
  }

  private com.fasterxml.jackson.databind.JsonNode document() throws Exception {
    return mapper.readTree(
        """
        {"type":"doc","content":[
          {"type":"heading","attrs":{"level":2},"content":[
            {"type":"text","text":"标题"}
          ]},
          {"type":"image","attrs":{
            "assetId":"11111111-1111-1111-1111-111111111111",
            "nodeId":"node-1","alt":"替代文本","caption":"图片说明",
            "displayWidthPx":720,"aspectRatio":1.5,
            "alignment":"center","decorative":false
          }},
          {"type":"paragraph","content":[{"type":"text","text":"正文"}]}
        ]}
        """);
  }

  private static RichTextSourceCommand source() {
    return new RichTextSourceCommand(
        "clipboard",
        null,
        Instant.parse("2026-10-21T10:00:00Z"),
        Instant.parse("2026-10-21T10:01:00Z"),
        "clipboard-v1",
        "a".repeat(64));
  }

  private static RichTextConversionReport report() {
    return new RichTextConversionReport(
        java.util.List.of("heading"),
        java.util.List.of(),
        java.util.List.of("iframe"),
        java.util.List.of(),
        "clipboard-v1");
  }
}
