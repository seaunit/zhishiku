package site.seaunit.zhishiku.webimport;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import site.seaunit.zhishiku.webimport.WebImportDtos.WebPageImportRequest;
import site.seaunit.zhishiku.webimport.WebImportDtos.WebPageImportResponse;

@RestController
@RequestMapping("/api/v1/web-imports")
public class WebPageImportController {
  private final WebPageImportService service;

  public WebPageImportController(WebPageImportService service) {
    this.service = service;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  WebPageImportResponse create(
      @Valid @RequestBody WebPageImportRequest request,
      @RequestHeader("Idempotency-Key") String idempotencyKey,
      Authentication authentication) {
    return service.importPage(
        request.url(),
        request.externalAiConsent(),
        request.billingTaskId(),
        idempotencyKey,
        authentication);
  }
}
