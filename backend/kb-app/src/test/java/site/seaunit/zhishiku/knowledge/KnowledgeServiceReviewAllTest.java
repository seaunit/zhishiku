package site.seaunit.zhishiku.knowledge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import site.seaunit.zhishiku.attachment.AttachmentRepository;
import site.seaunit.zhishiku.idempotency.IdempotencyService;
import site.seaunit.zhishiku.owner.CurrentOwnerService;
import site.seaunit.zhishiku.owner.OwnerAccountEntity;
import site.seaunit.zhishiku.richtext.RichDocumentRepository;
import site.seaunit.zhishiku.richtext.RichImageUploadRepository;
import site.seaunit.zhishiku.richtext.conflict.RichTextConflictPolicy;
import site.seaunit.zhishiku.richtext.lifecycle.RichTextLifecycleService;
import site.seaunit.zhishiku.richtext.provenance.SourceProvenancePolicy;
import site.seaunit.zhishiku.search.richtext.SearchProjectionBuilder;

class KnowledgeServiceReviewAllTest {
  @Test
  void reviewsEveryLockedItemForTheAuthenticatedAccountOnly() {
    CurrentOwnerService owners = mock(CurrentOwnerService.class);
    KnowledgeItemRepository items = mock(KnowledgeItemRepository.class);
    Authentication authentication = mock(Authentication.class);
    OwnerAccountEntity owner = OwnerAccountEntity.create("owner@example.com", "hash", "zh-CN");
    KnowledgeItemEntity first =
        KnowledgeItemEntity.create(owner.getId(), "第一条", "正文", "", "review");
    KnowledgeItemEntity second =
        KnowledgeItemEntity.create(owner.getId(), "第二条", "正文", "", "review");
    when(owners.require(authentication)).thenReturn(owner);
    when(items.findReviewableByOwnerIdForUpdate(owner.getId(), "active", "review"))
        .thenReturn(List.of(first, second));
    KnowledgeService service = service(owners, items);

    var response = service.reviewAll(authentication);

    assertThat(response.reviewed()).isEqualTo(2);
    assertThat(first.getProcessingStatus()).isEqualTo("succeeded");
    assertThat(second.getProcessingStatus()).isEqualTo("succeeded");
    verify(items).findReviewableByOwnerIdForUpdate(owner.getId(), "active", "review");
    verify(items).saveAll(List.of(first, second));
  }

  @Test
  void repeatedReviewAllIsSafeAndReturnsZeroWhenNothingRemains() {
    CurrentOwnerService owners = mock(CurrentOwnerService.class);
    KnowledgeItemRepository items = mock(KnowledgeItemRepository.class);
    Authentication authentication = mock(Authentication.class);
    OwnerAccountEntity owner = OwnerAccountEntity.create("owner@example.com", "hash", "zh-CN");
    when(owners.require(authentication)).thenReturn(owner);
    when(items.findReviewableByOwnerIdForUpdate(owner.getId(), "active", "review"))
        .thenReturn(List.of());
    KnowledgeService service = service(owners, items);

    var response = service.reviewAll(authentication);

    assertThat(response.reviewed()).isZero();
    verify(items).findReviewableByOwnerIdForUpdate(owner.getId(), "active", "review");
  }

  private static KnowledgeService service(
      CurrentOwnerService owners, KnowledgeItemRepository items) {
    return new KnowledgeService(
        owners,
        items,
        mock(RichDocumentRepository.class),
        mock(RichImageUploadRepository.class),
        mock(AttachmentRepository.class),
        mock(JdbcTemplate.class),
        new ObjectMapper(),
        mock(RichTextConflictPolicy.class),
        mock(SourceProvenancePolicy.class),
        mock(SearchProjectionBuilder.class),
        mock(RichTextLifecycleService.class),
        mock(ClassificationService.class),
        mock(IdempotencyService.class));
  }
}
