package site.seaunit.zhishiku.knowledge;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.seaunit.zhishiku.knowledge.KnowledgeDtos.KnowledgeItemResponse;
import site.seaunit.zhishiku.knowledge.KnowledgeDtos.ReanalyzeKnowledgeRequest;

@RestController
@RequestMapping("/api/v1/knowledge-items/{itemId}/reanalyze")
public class KnowledgeReanalysisController {
  private final KnowledgeReanalysisService service;

  public KnowledgeReanalysisController(KnowledgeReanalysisService service) {
    this.service = service;
  }

  @PostMapping
  KnowledgeItemResponse reanalyze(
      Authentication authentication,
      @PathVariable UUID itemId,
      @Valid @RequestBody ReanalyzeKnowledgeRequest request,
      @RequestHeader("Idempotency-Key") String idempotencyKey) {
    return service.reanalyze(itemId, request, idempotencyKey, authentication);
  }
}
