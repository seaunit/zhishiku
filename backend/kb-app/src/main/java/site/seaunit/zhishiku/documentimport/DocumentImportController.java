package site.seaunit.zhishiku.documentimport;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import site.seaunit.zhishiku.documentimport.DocumentImportDtos.DocumentImportResponse;

@RestController
@RequestMapping("/api/v1/document-imports")
public class DocumentImportController {
  private final DocumentImportService service;

  public DocumentImportController(DocumentImportService service) {
    this.service = service;
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  DocumentImportResponse create(
      @RequestPart("file") MultipartFile file,
      @RequestParam(defaultValue = "false") boolean externalAiConsent,
      @RequestParam(required = false) String note,
      @RequestParam(required = false) UUID billingTaskId,
      @RequestHeader("Idempotency-Key") String idempotencyKey,
      Authentication authentication) {
    return service.importDocument(
        file, note, externalAiConsent, billingTaskId, idempotencyKey, authentication);
  }
}
