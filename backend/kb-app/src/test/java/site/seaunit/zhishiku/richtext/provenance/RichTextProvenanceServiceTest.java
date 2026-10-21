package site.seaunit.zhishiku.richtext.provenance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class RichTextProvenanceServiceTest {
  private static final String HASH = "a".repeat(64);
  private final RichTextProvenanceRepository repository = mock(RichTextProvenanceRepository.class);
  private final RichTextProvenanceService service =
      new RichTextProvenanceService(repository, new RichTextSourcePolicy());

  @Test
  void recordsSourceBeforeConversionReport() {
    UUID ownerId = UUID.randomUUID();
    UUID itemId = UUID.randomUUID();
    when(repository.ownsItem(itemId, ownerId)).thenReturn(true);

    service.record(
        ownerId,
        itemId,
        new RichTextSourceCommand(
            "document",
            null,
            Instant.parse("2026-10-21T10:00:00Z"),
            Instant.parse("2026-10-21T10:01:00Z"),
            "document-v1",
            HASH),
        new RichTextConversionReport(
            List.of("heading"), List.of(), List.of("iframe"), List.of(), "document-v1"));

    InOrder order = inOrder(repository);
    order.verify(repository).insertSource(eq(itemId), any());
    order.verify(repository).insertConversionReport(any());
  }

  @Test
  void foreignOrMissingItemNeverWritesProvenance() {
    UUID ownerId = UUID.randomUUID();
    UUID itemId = UUID.randomUUID();
    when(repository.ownsItem(itemId, ownerId)).thenReturn(false);

    assertThatThrownBy(
            () ->
                service.record(
                    ownerId,
                    itemId,
                    new RichTextSourceCommand(
                        "native",
                        null,
                        null,
                        Instant.parse("2026-10-21T10:01:00Z"),
                        "native-v1",
                        HASH),
                    new RichTextConversionReport(
                        List.of(), List.of(), List.of(), List.of(), "native-v1")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("not found");

    verify(repository, never()).insertSource(any(), any());
    verify(repository, never()).insertConversionReport(any());
  }

  @Test
  void sourceAndReportWriteAreDeclaredInOneTransactionBoundary() throws Exception {
    assertThat(
            RichTextProvenanceService.class
                .getMethod(
                    "record",
                    UUID.class,
                    UUID.class,
                    RichTextSourceCommand.class,
                    RichTextConversionReport.class)
                .isAnnotationPresent(
                    org.springframework.transaction.annotation.Transactional.class))
        .isTrue();
  }
}
