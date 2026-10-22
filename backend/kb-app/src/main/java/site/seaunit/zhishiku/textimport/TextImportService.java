package site.seaunit.zhishiku.textimport;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.seaunit.zhishiku.billing.BillingService;
import site.seaunit.zhishiku.consent.AiConsentService;
import site.seaunit.zhishiku.core.billing.BillingModel.Feature;
import site.seaunit.zhishiku.idempotency.IdempotencyService;
import site.seaunit.zhishiku.knowledge.KnowledgeDtos.BodySourceRequest;
import site.seaunit.zhishiku.knowledge.KnowledgeDtos.CreateKnowledgeRequest;
import site.seaunit.zhishiku.knowledge.KnowledgeDtos.KnowledgeItemResponse;
import site.seaunit.zhishiku.knowledge.KnowledgeService;
import site.seaunit.zhishiku.textimport.TextImportDtos.TextImportRequest;
import site.seaunit.zhishiku.textimport.TextImportDtos.TextImportResponse;
import site.seaunit.zhishiku.webimport.AiTextSecretRedactor;
import site.seaunit.zhishiku.webimport.ImportedKnowledgeMapper;
import site.seaunit.zhishiku.webimport.OrganizedKnowledge;
import site.seaunit.zhishiku.webimport.TextOrganizer;
import site.seaunit.zhishiku.webimport.WebImportBillingRequiredException;
import site.seaunit.zhishiku.webimport.WebImportConsentRequiredException;
import site.seaunit.zhishiku.webimport.WebImportUnavailableException;
import site.seaunit.zhishiku.webimport.WebPageOrganizationException;

@Service
public class TextImportService {
  private static final int MIN_TEXT_CHARS = 30;

  private final TextOrganizer organizer;
  private final AiTextSecretRedactor secretRedactor;
  private final boolean allowUnbilledAi;
  private final KnowledgeService knowledgeService;
  private final ImportedKnowledgeMapper knowledgeMapper;
  private final BillingService billingService;
  private final AiConsentService aiConsents;
  private final IdempotencyService idempotency;

  public TextImportService(
      TextOrganizer organizer,
      AiTextSecretRedactor secretRedactor,
      @Value("${app.web-import.allow-unbilled-ai:false}") boolean allowUnbilledAi,
      KnowledgeService knowledgeService,
      ImportedKnowledgeMapper knowledgeMapper,
      BillingService billingService,
      AiConsentService aiConsents,
      IdempotencyService idempotency) {
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
  public TextImportResponse importText(TextImportRequest request, Authentication authentication) {
    return importText(request, null, authentication);
  }

  @Transactional
  public TextImportResponse importText(
      TextImportRequest request, String idempotencyKey, Authentication authentication) {
    String requestHash =
        IdempotencyService.hash(
            request.text()
                + "\n"
                + request.externalAiConsent()
                + "\n"
                + (request.note() == null ? "" : request.note())
                + "\n"
                + request.billingTaskId());
    if (idempotencyKey != null) {
      Optional<TextImportResponse> replay =
          idempotency.replay(
              authentication, "text_import", idempotencyKey, requestHash, TextImportResponse.class);
      if (replay.isPresent()) {
        return replay.get();
      }
    }
    if (!request.externalAiConsent() && !aiConsents.granted(authentication, "text")) {
      throw new WebImportConsentRequiredException(
          "Explicit consent is required before sending text to external AI.");
    }
    if (request.billingTaskId() == null && !allowUnbilledAi) {
      throw new WebImportBillingRequiredException(
          "Paid AI billing must be enabled before text analysis can run.");
    }
    if (request.billingTaskId() != null) {
      billingService.requireReserved(authentication, request.billingTaskId(), "text_import");
    }
    String text = request.text().strip();
    if (text.codePoints().filter(Character::isLetterOrDigit).count() < MIN_TEXT_CHARS) {
      throw new TextImportException(
          "TEXT_NO_CONTENT",
          HttpStatus.UNPROCESSABLE_ENTITY,
          "The text does not contain enough readable content.");
    }
    try {
      AiTextSecretRedactor.RedactionResult redacted = secretRedactor.redact(text);
      List<OrganizedKnowledge> organized;
      try {
        organized = organizer.organize("text", redacted.text());
      } catch (WebPageOrganizationException error) {
        throw new TextImportException(
            "TEXT_ORGANIZATION_FAILED",
            HttpStatus.UNPROCESSABLE_ENTITY,
            "AI could not produce valid text knowledge.");
      } catch (WebImportUnavailableException error) {
        throw new TextImportException(
            "TEXT_IMPORT_UNAVAILABLE",
            HttpStatus.SERVICE_UNAVAILABLE,
            "Text AI organization is temporarily unavailable.");
      }
      if (organized.isEmpty()) {
        throw new TextImportException(
            "TEXT_ORGANIZATION_FAILED",
            HttpStatus.UNPROCESSABLE_ENTITY,
            "AI could not produce valid text knowledge.");
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
                        knowledgeMapper.note(item, redacted.applied(), request.note(), "AI 文字"),
                        new BodySourceRequest("text", null, capturedAt)),
                    item,
                    authentication),
                redacted.applied()));
      }
      if (request.billingTaskId() != null) {
        billingService.settle(
            authentication,
            request.billingTaskId(),
            java.util.Set.of(Feature.AI_TEXT, Feature.CLASSIFY),
            null);
      }
      TextImportResponse response = new TextImportResponse(List.copyOf(created));
      if (idempotencyKey != null) {
        idempotency.record(
            authentication, "text_import", idempotencyKey, requestHash, response, 201);
      }
      return response;
    } catch (RuntimeException error) {
      if (request.billingTaskId() != null) {
        billingService.releaseAfterFailure(
            authentication, request.billingTaskId(), "Text import failed");
      }
      throw error;
    }
  }
}
