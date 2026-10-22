package site.seaunit.zhishiku.knowledge;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.seaunit.zhishiku.billing.BillingService;
import site.seaunit.zhishiku.consent.AiConsentService;
import site.seaunit.zhishiku.core.billing.BillingModel.Feature;
import site.seaunit.zhishiku.idempotency.IdempotencyService;
import site.seaunit.zhishiku.knowledge.KnowledgeDtos.KnowledgeItemResponse;
import site.seaunit.zhishiku.knowledge.KnowledgeDtos.ReanalyzeKnowledgeRequest;
import site.seaunit.zhishiku.owner.CurrentOwnerService;
import site.seaunit.zhishiku.owner.OwnerAccountEntity;
import site.seaunit.zhishiku.webimport.AiTextSecretRedactor;
import site.seaunit.zhishiku.webimport.OrganizedKnowledge;
import site.seaunit.zhishiku.webimport.TextOrganizer;
import site.seaunit.zhishiku.webimport.WebImportBillingRequiredException;
import site.seaunit.zhishiku.webimport.WebImportConsentRequiredException;
import site.seaunit.zhishiku.webimport.WebPageOrganizationException;

@Service
public class KnowledgeReanalysisService {
  private final CurrentOwnerService owners;
  private final KnowledgeItemRepository items;
  private final KnowledgeService knowledge;
  private final ClassificationService classifications;
  private final TextOrganizer organizer;
  private final AiTextSecretRedactor secretRedactor;
  private final BillingService billing;
  private final AiConsentService consents;
  private final IdempotencyService idempotency;
  private final boolean allowUnbilledAi;

  public KnowledgeReanalysisService(
      CurrentOwnerService owners,
      KnowledgeItemRepository items,
      KnowledgeService knowledge,
      ClassificationService classifications,
      TextOrganizer organizer,
      AiTextSecretRedactor secretRedactor,
      BillingService billing,
      AiConsentService consents,
      IdempotencyService idempotency,
      @Value("${app.web-import.allow-unbilled-ai:false}") boolean allowUnbilledAi) {
    this.owners = owners;
    this.items = items;
    this.knowledge = knowledge;
    this.classifications = classifications;
    this.organizer = organizer;
    this.secretRedactor = secretRedactor;
    this.billing = billing;
    this.consents = consents;
    this.idempotency = idempotency;
    this.allowUnbilledAi = allowUnbilledAi;
  }

  @Transactional
  public KnowledgeItemResponse reanalyze(
      UUID itemId,
      ReanalyzeKnowledgeRequest request,
      String idempotencyKey,
      Authentication authentication) {
    if (!request.externalAiConsent() && !consents.granted(authentication, "text")) {
      throw new WebImportConsentRequiredException(
          "Explicit consent is required before sending knowledge to external AI.");
    }
    if (request.billingTaskId() == null && !allowUnbilledAi) {
      throw new WebImportBillingRequiredException(
          "Paid AI billing must be enabled before knowledge can be reanalyzed.");
    }
    String requestHash =
        IdempotencyService.hash(
            itemId + "\n" + request.externalAiConsent() + "\n" + request.billingTaskId());
    Optional<KnowledgeItemResponse> replay =
        idempotency.replay(
            authentication,
            "knowledge_reanalyze",
            idempotencyKey,
            requestHash,
            KnowledgeItemResponse.class);
    if (replay.isPresent()) {
      return replay.get();
    }
    if (request.billingTaskId() != null) {
      billing.requireReserved(authentication, request.billingTaskId(), "reanalyze");
    }
    OwnerAccountEntity owner = owners.require(authentication);
    KnowledgeItemEntity item =
        items
            .findByIdAndOwnerIdAndLifecycleState(itemId, owner.getId(), "active")
            .orElseThrow(() -> new IllegalArgumentException("Knowledge item was not found."));
    try {
      String source = item.getTitle() + "\n" + item.getBody() + "\n" + item.getNote();
      AiTextSecretRedactor.RedactionResult redacted = secretRedactor.redact(source);
      List<OrganizedKnowledge> organized =
          organizer.organize("existing knowledge", redacted.text());
      if (organized.isEmpty()) {
        throw new WebPageOrganizationException("AI returned no usable classification.");
      }
      OrganizedKnowledge result = organized.get(0);
      classifications.assignAi(owner.getId(), itemId, result.category(), result.tags(), 0.9);
      item.updateGeneratedSummary(result.summary());
      items.saveAndFlush(item);
      if (request.billingTaskId() != null) {
        billing.settle(authentication, request.billingTaskId(), Set.of(Feature.CLASSIFY), null);
      }
      KnowledgeItemResponse response = knowledge.detail(itemId, authentication);
      idempotency.record(
          authentication, "knowledge_reanalyze", idempotencyKey, requestHash, response, 200);
      return response;
    } catch (RuntimeException error) {
      if (request.billingTaskId() != null) {
        billing.releaseAfterFailure(authentication, request.billingTaskId(), "Reanalysis failed");
      }
      throw error;
    }
  }
}
