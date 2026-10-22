package site.seaunit.zhishiku.knowledge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import site.seaunit.zhishiku.attachment.AttachmentRepository;
import site.seaunit.zhishiku.idempotency.IdempotencyException;
import site.seaunit.zhishiku.idempotency.IdempotencyService;
import site.seaunit.zhishiku.knowledge.KnowledgeDtos.BulkRecycleResponse;
import site.seaunit.zhishiku.owner.CurrentOwnerService;
import site.seaunit.zhishiku.owner.OwnerAccountEntity;
import site.seaunit.zhishiku.richtext.RichDocumentRepository;
import site.seaunit.zhishiku.richtext.RichImageUploadRepository;
import site.seaunit.zhishiku.richtext.conflict.RichTextConflictPolicy;
import site.seaunit.zhishiku.richtext.lifecycle.RichTextLifecycleService;
import site.seaunit.zhishiku.richtext.provenance.SourceProvenancePolicy;
import site.seaunit.zhishiku.search.richtext.SearchProjectionBuilder;

@ExtendWith(MockitoExtension.class)
class KnowledgeServiceBulkRecycleTest {
  private static final UUID FIRST_ITEM_ID = UUID.fromString("123e4567-e89b-42d3-a456-426614174711");
  private static final UUID SECOND_ITEM_ID =
      UUID.fromString("123e4567-e89b-42d3-a456-426614174712");
  private static final String IDEMPOTENCY_KEY = "bulk-recycle-request-0001";
  private static final String SORTED_REQUEST_HASH =
      "dddd7126b7321c76056f70ebe942e5fc74d6505a48fd3af94a0c77859132e284";
  private static final String SINGLE_ITEM_REQUEST_HASH =
      "3da0900bc2172c5ef8026288f09b07d4be4499af79a3619a402fe8b8ffdb986e";

  @Mock private CurrentOwnerService currentOwnerService;
  @Mock private KnowledgeItemRepository items;
  @Mock private RichTextLifecycleService lifecycle;
  @Mock private IdempotencyService idempotency;
  @Mock private Authentication authentication;

  private KnowledgeService service;

  @BeforeEach
  void setUp() {
    service =
        new KnowledgeService(
            currentOwnerService,
            items,
            org.mockito.Mockito.mock(RichDocumentRepository.class),
            org.mockito.Mockito.mock(RichImageUploadRepository.class),
            org.mockito.Mockito.mock(AttachmentRepository.class),
            org.mockito.Mockito.mock(JdbcTemplate.class),
            new ObjectMapper(),
            org.mockito.Mockito.mock(RichTextConflictPolicy.class),
            new SourceProvenancePolicy(),
            org.mockito.Mockito.mock(SearchProjectionBuilder.class),
            lifecycle,
            org.mockito.Mockito.mock(ClassificationService.class),
            idempotency);
  }

  @Test
  void recyclesSortedDistinctOwnedItemsAndRecordsTheResponse() {
    OwnerAccountEntity owner = OwnerAccountEntity.create("alice@example.com", "hash", "zh-CN");
    BulkRecycleResponse response = new BulkRecycleResponse(2);
    when(idempotency.replay(
            authentication,
            "knowledge_bulk_recycle",
            IDEMPOTENCY_KEY,
            SORTED_REQUEST_HASH,
            BulkRecycleResponse.class))
        .thenReturn(Optional.empty());
    when(currentOwnerService.require(authentication)).thenReturn(owner);
    when(items.countByIdInAndOwnerId(List.of(FIRST_ITEM_ID, SECOND_ITEM_ID), owner.getId()))
        .thenReturn(2L);

    assertThat(
            service.bulkRecycle(
                List.of(SECOND_ITEM_ID, FIRST_ITEM_ID, SECOND_ITEM_ID),
                IDEMPOTENCY_KEY,
                authentication))
        .isEqualTo(response);

    InOrder lockOrder = inOrder(items, lifecycle);
    lockOrder
        .verify(items)
        .countByIdInAndOwnerId(List.of(FIRST_ITEM_ID, SECOND_ITEM_ID), owner.getId());
    lockOrder.verify(lifecycle).recycle(owner.getId(), FIRST_ITEM_ID);
    lockOrder.verify(lifecycle).recycle(owner.getId(), SECOND_ITEM_ID);
    verify(idempotency)
        .record(
            authentication,
            "knowledge_bulk_recycle",
            IDEMPOTENCY_KEY,
            SORTED_REQUEST_HASH,
            response,
            200);
  }

  @Test
  void returnsCompletedReplayWithoutTouchingItemsOrOwnerData() {
    BulkRecycleResponse replayed = new BulkRecycleResponse(2);
    when(idempotency.replay(
            authentication,
            "knowledge_bulk_recycle",
            IDEMPOTENCY_KEY,
            SORTED_REQUEST_HASH,
            BulkRecycleResponse.class))
        .thenReturn(Optional.of(replayed));

    assertThat(
            service.bulkRecycle(
                List.of(SECOND_ITEM_ID, FIRST_ITEM_ID), IDEMPOTENCY_KEY, authentication))
        .isSameAs(replayed);

    verifyNoInteractions(currentOwnerService, items, lifecycle);
    verify(idempotency, never()).record(any(), any(), any(), any(), any(), anyInt());
  }

  @Test
  void rejectsAConflictingCanonicalBodyBeforeOwnerOrItemLookup() {
    when(idempotency.replay(
            authentication,
            "knowledge_bulk_recycle",
            IDEMPOTENCY_KEY,
            SINGLE_ITEM_REQUEST_HASH,
            BulkRecycleResponse.class))
        .thenThrow(
            new IdempotencyException(
                "IDEMPOTENCY_CONFLICT",
                "The idempotency key was already used for a different request."));

    assertThatThrownBy(
            () -> service.bulkRecycle(List.of(FIRST_ITEM_ID), IDEMPOTENCY_KEY, authentication))
        .isInstanceOf(IdempotencyException.class)
        .extracting("code")
        .isEqualTo("IDEMPOTENCY_CONFLICT");

    verifyNoInteractions(currentOwnerService, items, lifecycle);
  }

  @Test
  void keepsIdempotencyAndOwnerBoundaryScopedToTheAuthenticatedAccount() {
    Authentication aliceAuthentication = org.mockito.Mockito.mock(Authentication.class);
    Authentication bobAuthentication = org.mockito.Mockito.mock(Authentication.class);
    OwnerAccountEntity bob = OwnerAccountEntity.create("bob@example.com", "hash", "zh-CN");
    BulkRecycleResponse aliceReplay = new BulkRecycleResponse(1);
    when(idempotency.replay(
            aliceAuthentication,
            "knowledge_bulk_recycle",
            IDEMPOTENCY_KEY,
            SORTED_REQUEST_HASH,
            BulkRecycleResponse.class))
        .thenReturn(Optional.of(aliceReplay));
    when(idempotency.replay(
            bobAuthentication,
            "knowledge_bulk_recycle",
            IDEMPOTENCY_KEY,
            SORTED_REQUEST_HASH,
            BulkRecycleResponse.class))
        .thenReturn(Optional.empty());
    when(currentOwnerService.require(bobAuthentication)).thenReturn(bob);
    when(items.countByIdInAndOwnerId(List.of(FIRST_ITEM_ID, SECOND_ITEM_ID), bob.getId()))
        .thenReturn(2L);

    assertThat(
            service.bulkRecycle(
                List.of(FIRST_ITEM_ID, SECOND_ITEM_ID), IDEMPOTENCY_KEY, aliceAuthentication))
        .isSameAs(aliceReplay);
    assertThat(
            service.bulkRecycle(
                List.of(FIRST_ITEM_ID, SECOND_ITEM_ID), IDEMPOTENCY_KEY, bobAuthentication))
        .isEqualTo(new BulkRecycleResponse(2));

    verify(lifecycle).recycle(bob.getId(), FIRST_ITEM_ID);
    verify(lifecycle).recycle(bob.getId(), SECOND_ITEM_ID);
    verify(idempotency)
        .record(
            eq(bobAuthentication),
            eq("knowledge_bulk_recycle"),
            eq(IDEMPOTENCY_KEY),
            eq(SORTED_REQUEST_HASH),
            eq(new BulkRecycleResponse(2)),
            eq(200));
  }

  @Test
  void rejectsTheWholeBatchBeforeRecyclingWhenAnyItemIsOutsideTheAccount() {
    OwnerAccountEntity owner = OwnerAccountEntity.create("alice@example.com", "hash", "zh-CN");
    when(idempotency.replay(
            authentication,
            "knowledge_bulk_recycle",
            IDEMPOTENCY_KEY,
            SORTED_REQUEST_HASH,
            BulkRecycleResponse.class))
        .thenReturn(Optional.empty());
    when(currentOwnerService.require(authentication)).thenReturn(owner);
    when(items.countByIdInAndOwnerId(List.of(FIRST_ITEM_ID, SECOND_ITEM_ID), owner.getId()))
        .thenReturn(1L);

    assertThatThrownBy(
            () ->
                service.bulkRecycle(
                    List.of(FIRST_ITEM_ID, SECOND_ITEM_ID), IDEMPOTENCY_KEY, authentication))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Knowledge item was not found.");

    verifyNoInteractions(lifecycle);
  }
}
