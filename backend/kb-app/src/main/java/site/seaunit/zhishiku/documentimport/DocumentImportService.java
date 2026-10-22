package site.seaunit.zhishiku.documentimport;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
import site.seaunit.zhishiku.documentimport.DocumentImportDtos.DocumentImportResponse;
import site.seaunit.zhishiku.idempotency.IdempotencyService;
import site.seaunit.zhishiku.knowledge.KnowledgeDtos.BodySourceRequest;
import site.seaunit.zhishiku.knowledge.KnowledgeDtos.CreateKnowledgeRequest;
import site.seaunit.zhishiku.knowledge.KnowledgeDtos.KnowledgeItemResponse;
import site.seaunit.zhishiku.knowledge.KnowledgeService;
import site.seaunit.zhishiku.webimport.AiTextSecretRedactor;
import site.seaunit.zhishiku.webimport.DocumentOrganizer;
import site.seaunit.zhishiku.webimport.ImportedKnowledgeMapper;
import site.seaunit.zhishiku.webimport.OrganizedKnowledge;
import site.seaunit.zhishiku.webimport.WebImportBillingRequiredException;
import site.seaunit.zhishiku.webimport.WebImportConsentRequiredException;
import site.seaunit.zhishiku.webimport.WebImportUnavailableException;
import site.seaunit.zhishiku.webimport.WebPageOrganizationException;

@Service
public class DocumentImportService {
  private final DocumentTextExtractor extractor;
  private final DocumentOrganizer organizer;
  private final AiTextSecretRedactor secretRedactor;
  private final boolean allowUnbilledAi;
  private final KnowledgeService knowledgeService;
  private final ImportedKnowledgeMapper knowledgeMapper;
  private final BillingService billingService;
  private final AiConsentService aiConsents;
  private final IdempotencyService idempotency;

  public DocumentImportService(
      DocumentTextExtractor extractor,
      DocumentOrganizer organizer,
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
  public DocumentImportResponse importDocument(
      MultipartFile file,
      String userNote,
      boolean externalAiConsent,
      Authentication authentication) {
    return importDocument(file, userNote, externalAiConsent, null, authentication);
  }

  @Transactional
  public DocumentImportResponse importDocument(
      MultipartFile file,
      String userNote,
      boolean externalAiConsent,
      java.util.UUID billingTaskId,
      Authentication authentication) {
    return importDocument(file, userNote, externalAiConsent, billingTaskId, null, authentication);
  }

  @Transactional
  public DocumentImportResponse importDocument(
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
      throw new DocumentImportException(
          "DOCUMENT_PARSE_FAILED",
          HttpStatus.UNPROCESSABLE_ENTITY,
          "The document could not be read.");
    }
    if (idempotencyKey != null) {
      Optional<DocumentImportResponse> replay =
          idempotency.replay(
              authentication,
              "document_import",
              idempotencyKey,
              requestHash,
              DocumentImportResponse.class);
      if (replay.isPresent()) {
        return replay.get();
      }
    }
    if (!externalAiConsent && !aiConsents.granted(authentication, "document")) {
      throw new WebImportConsentRequiredException(
          "Explicit consent is required before sending document text to external AI.");
    }
    if (billingTaskId == null && !allowUnbilledAi) {
      throw new WebImportBillingRequiredException(
          "Paid AI billing must be enabled before document analysis can run.");
    }
    if (billingTaskId != null) {
      billingService.requireReserved(authentication, billingTaskId, "document_import");
    }
    String note = userNote == null ? "" : userNote.strip();
    if (note.length() > 20_000) {
      throw new DocumentImportException(
          "DOCUMENT_NOTE_TOO_LONG",
          HttpStatus.UNPROCESSABLE_ENTITY,
          "The document note exceeds 20,000 characters.");
    }

    try {
      ExtractedDocument document = extractor.extract(file);
      AiTextSecretRedactor.RedactionResult redacted = secretRedactor.redact(document.text());
      List<OrganizedKnowledge> organized;
      try {
        organized = organizer.organize("document", redacted.text());
      } catch (WebPageOrganizationException error) {
        throw new DocumentImportException(
            "DOCUMENT_ORGANIZATION_FAILED",
            HttpStatus.UNPROCESSABLE_ENTITY,
            "AI could not produce valid document knowledge.");
      } catch (WebImportUnavailableException error) {
        throw new DocumentImportException(
            "DOCUMENT_IMPORT_UNAVAILABLE",
            HttpStatus.SERVICE_UNAVAILABLE,
            "Document AI organization is temporarily unavailable.");
      }
      if (organized.isEmpty()) {
        throw new DocumentImportException(
            "DOCUMENT_ORGANIZATION_FAILED",
            HttpStatus.UNPROCESSABLE_ENTITY,
            "AI could not produce valid document knowledge.");
      }

      String sourceFormat = mediaTypeLabel(document.mediaType());
      Instant capturedAt = Instant.now();
      List<KnowledgeItemResponse> created = new ArrayList<>();
      for (OrganizedKnowledge item : organized) {
        created.add(
            knowledgeMapper.withRedactionApplied(
                knowledgeService.createOrganized(
                    new CreateKnowledgeRequest(
                        item.title(),
                        knowledgeMapper.document(item.body()),
                        knowledgeMapper.note(item, redacted.applied(), note, sourceFormat),
                        new BodySourceRequest("document", null, capturedAt)),
                    item,
                    authentication),
                redacted.applied()));
      }
      if (billingTaskId != null) {
        billingService.settle(
            authentication,
            billingTaskId,
            java.util.Set.of(Feature.DOCUMENT_EXTRACT, Feature.CLASSIFY),
            null);
      }
      DocumentImportResponse response = new DocumentImportResponse(List.copyOf(created));
      if (idempotencyKey != null) {
        idempotency.record(
            authentication, "document_import", idempotencyKey, requestHash, response, 201);
      }
      return response;
    } catch (RuntimeException error) {
      if (billingTaskId != null) {
        billingService.releaseAfterFailure(authentication, billingTaskId, "Document import failed");
      }
      throw error;
    }
  }

  private static String mediaTypeLabel(String mediaType) {
    return switch (mediaType.toLowerCase(Locale.ROOT)) {
      case "application/pdf" -> "PDF";
      case "application/msword" -> "DOC";
      case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> "DOCX";
      case "application/rtf" -> "RTF";
      case "text/markdown" -> "Markdown";
      case "image/jpeg" -> "JPEG 图片";
      case "image/png" -> "PNG 图片";
      case "image/webp" -> "WebP 图片";
      case "image/heic" -> "HEIC 图片";
      case "image/heif" -> "HEIF 图片";
      default -> "TXT";
    };
  }
}
