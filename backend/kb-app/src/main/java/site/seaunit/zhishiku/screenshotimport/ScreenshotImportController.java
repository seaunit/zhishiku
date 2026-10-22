package site.seaunit.zhishiku.screenshotimport;

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
import site.seaunit.zhishiku.screenshotimport.ScreenshotImportDtos.ScreenshotImportResponse;

@RestController
@RequestMapping("/api/v1/screenshot-imports")
public class ScreenshotImportController {
  private final ScreenshotImportService service;

  public ScreenshotImportController(ScreenshotImportService service) {
    this.service = service;
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  ScreenshotImportResponse create(
      @RequestPart("file") MultipartFile file,
      @RequestParam(defaultValue = "false") boolean externalAiConsent,
      @RequestParam(required = false) String note,
      @RequestParam(required = false) UUID billingTaskId,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
      Authentication authentication) {
    return service.importScreenshot(
        file, note, externalAiConsent, billingTaskId, idempotencyKey, authentication);
  }
}
