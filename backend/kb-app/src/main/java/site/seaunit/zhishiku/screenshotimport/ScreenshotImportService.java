package site.seaunit.zhishiku.screenshotimport;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import site.seaunit.zhishiku.billing.BillingService;
import site.seaunit.zhishiku.consent.AiConsentService;
import site.seaunit.zhishiku.core.billing.BillingModel.Feature;
import site.seaunit.zhishiku.idempotency.IdempotencyService;
import site.seaunit.zhishiku.knowledge.KnowledgeDtos.BodySourceRequest;
import site.seaunit.zhishiku.knowledge.KnowledgeDtos.CreateKnowledgeRequest;
import site.seaunit.zhishiku.knowledge.KnowledgeDtos.KnowledgeItemResponse;
import site.seaunit.zhishiku.knowledge.KnowledgeService;
import site.seaunit.zhishiku.screenshotimport.ScreenshotImportDtos.ScreenshotImportResponse;
import site.seaunit.zhishiku.webimport.AiTextSecretRedactor;
import site.seaunit.zhishiku.webimport.ImportedKnowledgeMapper;
import site.seaunit.zhishiku.webimport.OrganizedKnowledge;
import site.seaunit.zhishiku.webimport.ScreenshotOrganizer;
import site.seaunit.zhishiku.webimport.WebImportBillingRequiredException;
import site.seaunit.zhishiku.webimport.WebImportConsentRequiredException;
import site.seaunit.zhishiku.webimport.WebImportUnavailableException;
import site.seaunit.zhishiku.webimport.WebPageOrganizationException;

@Service
public class ScreenshotImportService {
  private final ScreenshotTextExtractor extractor;
  private final ScreenshotOrganizer organizer;
  private final AiTextSecretRedactor secretRedactor;
  private final boolean allowUnbilledAi;
  private final KnowledgeService knowledgeService;
  private final ImportedKnowledgeMapper knowledgeMapper;
  private final BillingService billingService;
  private final AiConsentService aiConsents;
  private final IdempotencyService idempotency;
  private final Cache<String, ScreenshotImportResponse> idempotentResponses =
      Caffeine.newBuilder().expireAfterWrite(Duration.ofMinutes(10)).maximumSize(10_000).build();

  public ScreenshotImportService(
      ScreenshotTextExtractor extractor,
      ScreenshotOrganizer organizer,
      AiTextSecretRedactor secretRedactor,
      @Value("${app.web-import.allow-unbilled-ai:false}") boolean allowUnbilledAi,
      KnowledgeService knowledgeService,
      ImportedKnowledgeMapper knowledgeMapper,
      BillingService billingService,
      AiConsentService aiConsents,
      IdempotencyService idempotency) {
    this.extractor = extractor;
    this.organizer = organizer;
    this.secretRedactor = secretRedactor;
    this.allowUnbilledAi = allowUnbilledAi;
    this.knowledgeService = knowledgeService;
    this.knowledgeMapper = knowledgeMapper;
    this.billingService = billingService;
    this.aiConsents = aiConsents;
    this.idempotency = idempotency;
  }

  @Transactional
  public ScreenshotImportResponse importScreenshot(
      MultipartFile file,
      String userNote,
      boolean externalAiConsent,
      String idempotencyKey,
      Authentication authentication) {
    return importScreenshot(
        file, userNote, externalAiConsent, null, idempotencyKey, authentication);
  }

  @Transactional
  public ScreenshotImportResponse importScreenshot(
      MultipartFile file,
      String userNote,
      boolean externalAiConsent,
      java.util.UUID billingTaskId,
      String idempotencyKey,
      Authentication authentication) {
    String requestHash;
    try {
      requestHash =
          IdempotencyService.hash(
              IdempotencyService.hashBytes(file.getBytes())
                  + "\n"
                  + (userNote == null ? "" : userNote)
                  + "\n"
                  + externalAiConsent
                  + "\n"
                  + billingTaskId);
    } catch (IOException error) {
      throw new ScreenshotImportException(
          "SCREENSHOT_IMAGE_INVALID",
          HttpStatus.UNPROCESSABLE_ENTITY,
          "The screenshot could not be read.");
    }
    Optional<ScreenshotImportResponse> replay =
        idempotency.replay(
            authentication,
            "screenshot_import",
            idempotencyKey,
            requestHash,
            ScreenshotImportResponse.class);
    if (replay.isPresent()) {
      return replay.get();
    }
    if (!externalAiConsent && !aiConsents.granted(authentication, "screenshot")) {
      throw new WebImportConsentRequiredException(
          "Explicit consent is required before sending screenshot text to external AI.");
    }
    if (billingTaskId == null && !allowUnbilledAi) {
      throw new WebImportBillingRequiredException(
          "Paid AI billing must be enabled before screenshot analysis can run.");
    }
    if (billingTaskId != null) {
      billingService.requireReserved(authentication, billingTaskId, "screenshot_import");
    }
    String note = userNote == null ? "" : userNote.strip();
    if (note.length() > 20_000) {
      throw new ScreenshotImportException(
          "SCREENSHOT_NOTE_TOO_LONG",
          HttpStatus.UNPROCESSABLE_ENTITY,
          "The screenshot note exceeds 20,000 characters.");
    }
    String key = idempotencyKey == null ? "" : idempotencyKey.strip();
    if (key.isBlank() || key.length() > 128) {
      throw new ScreenshotImportException(
          "IDEMPOTENCY_KEY_REQUIRED",
          HttpStatus.BAD_REQUEST,
          "A valid Idempotency-Key header is required.");
    }
    String cacheKey =
        authentication.getName() + "\n" + key + "\n" + (billingTaskId == null ? "" : billingTaskId);
    ScreenshotImportResponse response =
        idempotentResponses
            .asMap()
            .computeIfAbsent(
                cacheKey,
                ignored -> importScreenshotOnce(file, note, billingTaskId, authentication));
    idempotency.record(
        authentication, "screenshot_import", idempotencyKey, requestHash, response, 201);
    return response;
  }

  private ScreenshotImportResponse importScreenshotOnce(
      MultipartFile file,
      String note,
      java.util.UUID billingTaskId,
      Authentication authentication) {
    try {
      ExtractedScreenshot screenshot = extractor.extract(file);
      AiTextSecretRedactor.RedactionResult redacted = secretRedactor.redact(screenshot.text());
      List<OrganizedKnowledge> organized;
      try {
        organized = organizer.organize("screenshot", redacted.text());
      } catch (WebPageOrganizationException error) {
        throw new ScreenshotImportException(
            "SCREENSHOT_ORGANIZATION_FAILED",
            HttpStatus.UNPROCESSABLE_ENTITY,
            "AI could not produce valid screenshot knowledge.");
      } catch (WebImportUnavailableException error) {
        throw new ScreenshotImportException(
            "SCREENSHOT_IMPORT_UNAVAILABLE",
            HttpStatus.SERVICE_UNAVAILABLE,
            "Screenshot AI organization is temporarily unavailable.");
      }
      if (organized.isEmpty()) {
        throw new ScreenshotImportException(
            "SCREENSHOT_ORGANIZATION_FAILED",
            HttpStatus.UNPROCESSABLE_ENTITY,
            "AI could not produce valid screenshot knowledge.");
      }

      Instant capturedAt = Instant.now();
      List<KnowledgeItemResponse> created = new ArrayList<>();
      for (OrganizedKnowledge item : organized) {
        created.add(
            knowledgeMapper.withRedactionApplied(
                knowledgeService.createOrganized(
                    new CreateKnowledgeRequest(
                        item.title(),
                        knowledgeMapper.document(item.body()),
                        knowledgeMapper.note(
                            item, redacted.applied(), note, screenshot.formatLabel()),
                        new BodySourceRequest("screenshot", null, capturedAt)),
                    item,
                    authentication),
                redacted.applied()));
      }
      if (billingTaskId != null) {
        billingService.settle(
            authentication,
            billingTaskId,
            java.util.Set.of(Feature.SCREENSHOT_OCR, Feature.CLASSIFY),
            null);
      }
      return new ScreenshotImportResponse(List.copyOf(created));
    } catch (RuntimeException error) {
      if (billingTaskId != null) {
        billingService.releaseAfterFailure(
            authentication, billingTaskId, "Screenshot import failed");
      }
      throw error;
    }
  }
}
