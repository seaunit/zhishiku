package site.seaunit.zhishiku.knowledge;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import site.seaunit.zhishiku.knowledge.KnowledgeDtos.CreateKnowledgeRequest;
import site.seaunit.zhishiku.knowledge.KnowledgeDtos.KnowledgeItemPage;
import site.seaunit.zhishiku.knowledge.KnowledgeDtos.KnowledgeItemResponse;
import site.seaunit.zhishiku.knowledge.KnowledgeDtos.KnowledgeSummaryResponse;
import site.seaunit.zhishiku.knowledge.KnowledgeDtos.SearchResponse;
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
      Authentication authentication) {
    return service.search(query, limit, authentication);
  }

  @GetMapping("/knowledge-items")
  KnowledgeItemPage list(
      @RequestParam(name = "limit", defaultValue = "20") int limit, Authentication authentication) {
    List<KnowledgeSummaryResponse> items = service.list(limit, authentication);
    return new KnowledgeItemPage(items, null);
  }

  @PostMapping("/knowledge-items")
  @ResponseStatus(HttpStatus.CREATED)
  KnowledgeItemResponse create(
      @Valid @RequestBody CreateKnowledgeRequest request, Authentication authentication) {
    return service.create(request, authentication);
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
}
