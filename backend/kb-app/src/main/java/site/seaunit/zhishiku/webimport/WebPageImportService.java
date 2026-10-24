package site.seaunit.zhishiku.webimport;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.seaunit.zhishiku.billing.BillingService;
import site.seaunit.zhishiku.consent.AiConsentService;
import site.seaunit.zhishiku.core.billing.BillingModel.Feature;
import site.seaunit.zhishiku.idempotency.IdempotencyService;
import site.seaunit.zhishiku.knowledge.DuplicateKnowledge;
import site.seaunit.zhishiku.knowledge.DuplicateKnowledgeException;
import site.seaunit.zhishiku.knowledge.KnowledgeDtos.BodySourceRequest;
import site.seaunit.zhishiku.knowledge.KnowledgeDtos.CreateKnowledgeRequest;
import site.seaunit.zhishiku.knowledge.KnowledgeDtos.KnowledgeItemResponse;
import site.seaunit.zhishiku.knowledge.KnowledgeDuplicateService;
import site.seaunit.zhishiku.knowledge.KnowledgeService;
import site.seaunit.zhishiku.owner.CurrentOwnerService;
import site.seaunit.zhishiku.richtext.image.ImportImageAssetService;
import site.seaunit.zhishiku.richtext.image.ImportedImageAsset;
import site.seaunit.zhishiku.webimport.WebImportDtos.WebPageImportResponse;

@Service
public class WebPageImportService {
  private final WebPageCaptureGateway capture;
  private final WebPageOrganizer organizer;
  private final AiTextSecretRedactor secretRedactor;
  private final boolean allowUnbilledAi;
  private final KnowledgeService knowledgeService;
  private final KnowledgeDuplicateService knowledgeDuplicates;
  private final CurrentOwnerService currentOwnerService;
  private final ImportedKnowledgeMapper knowledgeMapper;
  private final ImportImageAssetService imageAssets;
  private final BillingService billingService;
  private final AiConsentService aiConsents;
  private final IdempotencyService idempotency;

  public WebPageImportService(
      WebPageCaptureGateway capture,
      WebPageOrganizer organizer,
      AiTextSecretRedactor secretRedactor,
      @Value("${app.web-import.allow-unbilled-ai:false}") boolean allowUnbilledAi,
      KnowledgeService knowledgeService,
      KnowledgeDuplicateService knowledgeDuplicates,
      CurrentOwnerService currentOwnerService,
      ImportedKnowledgeMapper knowledgeMapper,
      ImportImageAssetService imageAssets,
      BillingService billingService,
      AiConsentService aiConsents,
      IdempotencyService idempotency) {
    this.capture = capture;
    this.organizer = organizer;
    this.secretRedactor = secretRedactor;
    this.allowUnbilledAi = allowUnbilledAi;
    this.knowledgeService = knowledgeService;
    this.knowledgeDuplicates = knowledgeDuplicates;
    this.currentOwnerService = currentOwnerService;
    this.knowledgeMapper = knowledgeMapper;
    this.imageAssets = imageAssets;
    this.billingService = billingService;
    this.aiConsents = aiConsents;
    this.idempotency = idempotency;
  }

  @Transactional
  public WebPageImportResponse importPage(
      String requestedUrl, boolean externalAiConsent, Authentication authentication) {
    return importPage(requestedUrl, externalAiConsent, null, authentication);
  }

  @Transactional
  public WebPageImportResponse importPage(
      String requestedUrl,
      boolean externalAiConsent,
      java.util.UUID billingTaskId,
      Authentication authentication) {
    return importPage(requestedUrl, externalAiConsent, billingTaskId, null, authentication);
  }

  @Transactional
  public WebPageImportResponse importPage(
      String requestedUrl,
      boolean externalAiConsent,
      java.util.UUID billingTaskId,
      String idempotencyKey,
      Authentication authentication) {
    String requestHash =
        IdempotencyService.hash(requestedUrl + "\n" + externalAiConsent + "\n" + billingTaskId);
    if (idempotencyKey != null) {
      Optional<WebPageImportResponse> replay =
          idempotency.replay(
              authentication,
              "webpage_import",
              idempotencyKey,
              requestHash,
              WebPageImportResponse.class);
      if (replay.isPresent()) {
        return replay.get();
      }
    }
    if (!externalAiConsent && !aiConsents.granted(authentication, "webpage")) {
      throw new WebImportConsentRequiredException(
          "Explicit consent is required before sending page text to external AI.");
    }
    if (billingTaskId == null && !allowUnbilledAi) {
      throw new WebImportBillingRequiredException(
          "Paid AI billing must be enabled before webpage analysis can run.");
    }
    if (billingTaskId != null) {
      billingService.requireReserved(authentication, billingTaskId, "webpage_import");
    }
    var owner = currentOwnerService.require(authentication);
    knowledgeDuplicates.lockSource(owner.getId(), requestedUrl);
    List<DuplicateKnowledge> existingItems =
        knowledgeDuplicates.findActiveBySourceUrl(owner.getId(), requestedUrl);
    if (!existingItems.isEmpty()) {
      if (billingTaskId != null) {
        billingService.releaseAfterFailure(
            authentication, billingTaskId, "Knowledge source is already imported");
      }
      throw new DuplicateKnowledgeException(existingItems);
    }
    try {
      CapturedWebPage page;
      try {
        page = capture.capture(requestedUrl);
      } catch (IllegalArgumentException error) {
        throw new WebPageUrlException(
            "The webpage URL must be a public HTTP or HTTPS address without credentials.");
      }
      AiTextSecretRedactor.RedactionResult redacted = secretRedactor.redact(page.visibleText());
      List<OrganizedKnowledge> organized = organizer.organize("web page", redacted.text());
      if (organized.isEmpty()) {
        throw new WebPageOrganizationException("AI page organization returned no knowledge.");
      }
      List<KnowledgeItemResponse> created = new ArrayList<>();
      Instant capturedAt = Instant.now();
      List<ImportedImageAsset> preparedImages =
          redacted.applied()
              ? List.of()
              : imageAssets.prepare(
                  page.images(), authentication, organized.get(0).title(), "网页来源图示", false);
      for (int index = 0; index < organized.size(); index++) {
        OrganizedKnowledge note = organized.get(index);
        created.add(
            knowledgeMapper.withRedactionApplied(
                knowledgeService.createOrganized(
                    new CreateKnowledgeRequest(
                        note.title(),
                        knowledgeMapper.document(
                            note.body(), index == 0 ? preparedImages : List.of()),
                        knowledgeMapper.note(note, redacted.applied(), "", ""),
                        new BodySourceRequest("webpage", page.requestedUrl(), capturedAt)),
                    note,
                    authentication),
                redacted.applied()));
      }
      if (billingTaskId != null) {
        billingService.settle(
            authentication,
            billingTaskId,
            java.util.Set.of(Feature.WEBPAGE_VISION, Feature.CLASSIFY),
            null);
      }
      WebPageImportResponse response = new WebPageImportResponse(List.copyOf(created));
      if (idempotencyKey != null) {
        idempotency.record(
            authentication, "webpage_import", idempotencyKey, requestHash, response, 201);
      }
      return response;
    } catch (RuntimeException error) {
      if (billingTaskId != null) {
        billingService.releaseAfterFailure(authentication, billingTaskId, "Webpage import failed");
      }
      throw error;
    }
  }
}
