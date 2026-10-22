package site.seaunit.zhishiku.textimport;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import site.seaunit.zhishiku.textimport.TextImportDtos.TextImportRequest;
import site.seaunit.zhishiku.textimport.TextImportDtos.TextImportResponse;

@RestController
@RequestMapping("/api/v1/text-imports")
public class TextImportController {
  private final TextImportService service;

  public TextImportController(TextImportService service) {
    this.service = service;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  TextImportResponse create(
      @Valid @RequestBody TextImportRequest request,
      @RequestHeader("Idempotency-Key") String idempotencyKey,
      Authentication authentication) {
    return service.importText(request, idempotencyKey, authentication);
  }
}
