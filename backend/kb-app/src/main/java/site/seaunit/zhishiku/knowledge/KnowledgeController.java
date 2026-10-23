package site.seaunit.zhishiku.knowledge;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import site.seaunit.zhishiku.knowledge.KnowledgeDtos.BulkRecycleRequest;
import site.seaunit.zhishiku.knowledge.KnowledgeDtos.BulkRecycleResponse;
import site.seaunit.zhishiku.knowledge.KnowledgeDtos.CreateKnowledgeRequest;
import site.seaunit.zhishiku.knowledge.KnowledgeDtos.KnowledgeItemPage;
import site.seaunit.zhishiku.knowledge.KnowledgeDtos.KnowledgeItemResponse;
import site.seaunit.zhishiku.knowledge.KnowledgeDtos.SearchResponse;
import site.seaunit.zhishiku.knowledge.KnowledgeDtos.UpdateClassificationRequest;
import site.seaunit.zhishiku.knowledge.KnowledgeDtos.UpdateKnowledgeRequest;

@RestController
@RequestMapping("/api/v1")
public class KnowledgeController {
  private final KnowledgeService service;

  public KnowledgeController(KnowledgeService service) {
    this.service = service;
  }

  @GetMapping("/search")
  SearchResponse search(
      @RequestParam(name = "q", required = false, defaultValue = "") String query,
      @RequestParam(name = "limit", defaultValue = "20") int limit,
      @RequestParam(name = "page", defaultValue = "1") int page,
      @RequestParam(name = "source", defaultValue = "all") String source,
      Authentication authentication) {
    return service.search(query, page, limit, source, authentication);
  }

  @GetMapping("/knowledge-items")
  KnowledgeItemPage list(
      @RequestParam(name = "limit", defaultValue = "20") int limit,
      @RequestParam(name = "page", defaultValue = "1") int page,
      @RequestParam(name = "source", defaultValue = "all") String source,
      @RequestParam(name = "status", defaultValue = "all") String status,
      Authentication authentication) {
    return service.list(page, limit, source, status, authentication);
  }

  @GetMapping({"/knowledge-items/trash", "/trash"})
  KnowledgeItemPage trash(
      @RequestParam(name = "limit", defaultValue = "100") int limit,
      Authentication authentication) {
    return new KnowledgeItemPage(service.listTrash(limit, authentication), null, null);
  }

  @PostMapping("/knowledge-items")
  @ResponseStatus(HttpStatus.CREATED)
  KnowledgeItemResponse create(
      @Valid @RequestBody CreateKnowledgeRequest request, Authentication authentication) {
    return service.createManual(request, authentication);
  }

  @GetMapping("/knowledge-items/{itemId}")
  KnowledgeItemResponse detail(@PathVariable UUID itemId, Authentication authentication) {
    return service.detail(itemId, authentication);
  }

  @PatchMapping("/knowledge-items/{itemId}")
  KnowledgeItemResponse update(
      @PathVariable UUID itemId,
      @Valid @RequestBody UpdateKnowledgeRequest request,
      Authentication authentication) {
    return service.update(itemId, request, authentication);
  }

  @PatchMapping({
    "/knowledge-items/{itemId}/classification",
    "/knowledge-items/{itemId}/classifications"
  })
  KnowledgeItemResponse updateClassification(
      @PathVariable UUID itemId,
      @Valid @RequestBody UpdateClassificationRequest request,
      Authentication authentication) {
    return service.updateClassification(itemId, request, authentication);
  }

  @PostMapping("/knowledge-items/bulk-recycle")
  BulkRecycleResponse bulkRecycle(
      @Valid @RequestBody BulkRecycleRequest request,
      @RequestHeader("Idempotency-Key") String idempotencyKey,
      Authentication authentication) {
    return service.bulkRecycle(request.itemIds(), idempotencyKey, authentication);
  }

  @PostMapping("/knowledge-items/{itemId}/review")
  KnowledgeItemResponse confirmReview(@PathVariable UUID itemId, Authentication authentication) {
    return service.confirmReview(itemId, authentication);
  }

  @DeleteMapping("/knowledge-items/{itemId}")
  ResponseEntity<Void> recycle(@PathVariable UUID itemId, Authentication authentication) {
    service.recycle(itemId, authentication);
    return ResponseEntity.noContent().build();
  }

  @PostMapping({"/knowledge-items/{itemId}/restore", "/trash/{itemId}/restore"})
  ResponseEntity<Void> restore(@PathVariable UUID itemId, Authentication authentication) {
    service.restore(itemId, authentication);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping({"/knowledge-items/{itemId}/permanent", "/trash/{itemId}"})
  ResponseEntity<Void> purge(@PathVariable UUID itemId, Authentication authentication) {
    service.purge(itemId, authentication);
    return ResponseEntity.noContent().build();
  }
}
