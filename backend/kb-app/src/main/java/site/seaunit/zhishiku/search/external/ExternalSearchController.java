package site.seaunit.zhishiku.search.external;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import site.seaunit.zhishiku.search.external.ExternalSearchDtos.ExternalSearchRequest;
import site.seaunit.zhishiku.search.external.ExternalSearchDtos.ExternalSearchResponse;
import site.seaunit.zhishiku.search.external.ExternalSearchDtos.ExternalSearchSaveResponse;
import site.seaunit.zhishiku.search.external.ExternalSearchDtos.SaveExternalCandidateRequest;

@RestController
@RequestMapping("/api/v1/search/external")
public class ExternalSearchController {
  private final ExternalSearchService service;

  public ExternalSearchController(ExternalSearchService service) {
    this.service = service;
  }

  @PostMapping
  ExternalSearchResponse search(
      @Valid @RequestBody ExternalSearchRequest request,
      @RequestHeader("Idempotency-Key") String idempotencyKey,
      Authentication authentication) {
    return service.search(request, idempotencyKey, authentication);
  }

  @PostMapping("/{candidateId}/save")
  @ResponseStatus(HttpStatus.CREATED)
  ExternalSearchSaveResponse save(
      @PathVariable UUID candidateId,
      @Valid @RequestBody SaveExternalCandidateRequest request,
      @RequestHeader("Idempotency-Key") String idempotencyKey,
      Authentication authentication) {
    return service.save(
        candidateId,
        request.externalAiConsent(),
        request.billingTaskId(),
        idempotencyKey,
        authentication);
  }
}
