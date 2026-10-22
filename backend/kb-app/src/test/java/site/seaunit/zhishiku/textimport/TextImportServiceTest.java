package site.seaunit.zhishiku.textimport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.core.Authentication;
import site.seaunit.zhishiku.billing.BillingService;
import site.seaunit.zhishiku.core.billing.BillingModel.Feature;
import site.seaunit.zhishiku.knowledge.KnowledgeDtos.CreateKnowledgeRequest;
import site.seaunit.zhishiku.knowledge.KnowledgeDtos.KnowledgeItemResponse;
import site.seaunit.zhishiku.knowledge.KnowledgeService;
import site.seaunit.zhishiku.textimport.TextImportDtos.TextImportRequest;
import site.seaunit.zhishiku.webimport.AiTextSecretRedactor;
import site.seaunit.zhishiku.webimport.ImportedKnowledgeMapper;
import site.seaunit.zhishiku.webimport.OrganizedKnowledge;
import site.seaunit.zhishiku.webimport.TextOrganizer;
import site.seaunit.zhishiku.webimport.WebImportBillingRequiredException;
import site.seaunit.zhishiku.webimport.WebImportConsentRequiredException;

class TextImportServiceTest {
  private final TextOrganizer organizer = mock(TextOrganizer.class);
  private final KnowledgeService knowledgeService = mock(KnowledgeService.class);
  private final Authentication authentication = mock(Authentication.class);
  private final ImportedKnowledgeMapper mapper = new ImportedKnowledgeMapper(new ObjectMapper());
  private final BillingService billingService = mock(BillingService.class);
  private final TextImportService service =
      new TextImportService(
          organizer,
          new AiTextSecretRedactor(),
          true,
          knowledgeService,
          mapper,
          billingService,
          mock(site.seaunit.zhishiku.consent.AiConsentService.class),
          mock(site.seaunit.zhishiku.idempotency.IdempotencyService.class));

  @Test
  void redactsTextAndCreatesOrganizedKnowledge() {
    when(organizer.organize(
            eq("text"),
            argThat(text -> text.contains("[REDACTED]") && !text.contains("test-secret-value"))))
        .thenReturn(List.of(new OrganizedKnowledge("整理主题", "整理正文", "摘要", "资料", List.of("文字"))));
    when(knowledgeService.createOrganized(any(), any(), any())).thenReturn(response());

    var result =
        service.importText(
            new TextImportRequest(
                "这里有一段足够长的 AI 辅助文字内容，用于整理知识。password=test-secret-value", true, "备注"),
            authentication);

    assertThat(result.items()).hasSize(1);
    ArgumentCaptor<CreateKnowledgeRequest> request =
        ArgumentCaptor.forClass(CreateKnowledgeRequest.class);
    verify(knowledgeService).createOrganized(request.capture(), any(), any());
    assertThat(request.getValue().bodySource().kind()).isEqualTo("text");
    assertThat(request.getValue().note()).contains("来源格式：AI 文字").contains("备注");
  }

  @Test
  void requiresExplicitAiConsent() {
    try {
      service.importText(
          new TextImportRequest("足够长的 AI 整理文字内容".repeat(5), false, ""), authentication);
    } catch (WebImportConsentRequiredException expected) {
      assertThat(expected.getMessage()).contains("consent");
    }
    verify(organizer, never()).organize(any(), any());
  }

  @Test
  void refusesUnbilledAi() {
    TextImportService unbilled =
        new TextImportService(
            organizer,
            new AiTextSecretRedactor(),
            false,
            knowledgeService,
            mapper,
            billingService,
            mock(site.seaunit.zhishiku.consent.AiConsentService.class),
            mock(site.seaunit.zhishiku.idempotency.IdempotencyService.class));
    try {
      unbilled.importText(
          new TextImportRequest("足够长的 AI 整理文字内容".repeat(5), true, ""), authentication);
    } catch (WebImportBillingRequiredException expected) {
      assertThat(expected.getMessage()).contains("billing");
    }
    verify(organizer, never()).organize(any(), any());
  }

  @Test
  void requiresReservedTaskAndSettlesSuccessfulBillingLines() {
    UUID taskId = UUID.randomUUID();
    when(organizer.organize(eq("text"), any()))
        .thenReturn(List.of(new OrganizedKnowledge("主题", "正文", "摘要", "资料", List.of("文字"))));
    when(knowledgeService.createOrganized(any(), any(), any())).thenReturn(response());

    service.importText(
        new TextImportRequest("足够长的 AI 整理文字内容".repeat(5), true, "", taskId), authentication);

    verify(billingService).requireReserved(authentication, taskId, "text_import");
    verify(billingService)
        .settle(
            eq(authentication),
            eq(taskId),
            argThat(features -> features.contains(Feature.AI_TEXT)),
            any());
  }

  private static KnowledgeItemResponse response() {
    Instant now = Instant.parse("2026-10-22T00:00:00Z");
    return new KnowledgeItemResponse(
        UUID.randomUUID(),
        "整理主题",
        "整理正文",
        null,
        1,
        "completed",
        "来源格式：AI 文字",
        "摘要",
        "succeeded",
        "active",
        true,
        null,
        null,
        List.of(),
        1,
        now,
        now);
  }
}
