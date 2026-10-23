package site.seaunit.zhishiku.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import site.seaunit.zhishiku.billing.BillingDtos.CheckoutBridgeRequest;
import site.seaunit.zhishiku.billing.BillingDtos.NormalizedWaffoEvent;
import site.seaunit.zhishiku.billing.BillingDtos.QuoteLineRequest;
import site.seaunit.zhishiku.config.AppProperties;
import site.seaunit.zhishiku.core.billing.BillingModel.Feature;
import site.seaunit.zhishiku.core.billing.BillingModel.OrderStatus;
import site.seaunit.zhishiku.owner.CurrentOwnerService;
import site.seaunit.zhishiku.owner.OwnerAccountEntity;

class BillingP0PolicyTest {
  private static final UUID OWNER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID PREFLIGHT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final Instant NOW = Instant.parse("2026-10-22T08:00:00Z");

  private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
  private final BillingService service =
      new BillingService(
          jdbc,
          new ObjectMapper(),
          mock(CurrentOwnerService.class),
          mock(BillingBridgeClient.class),
          properties(),
          Clock.fixed(NOW, ZoneOffset.UTC));

  @Test
  void operationPolicyRejectsCheapReplacementLines() {
    List<QuoteLineRequest> cheapLines =
        List.of(new QuoteLineRequest(Feature.INTERNET_SEARCH.wireValue(), 1));

    assertThatThrownBy(
            () ->
                BillingService.validateRequestedOperationLines(
                    "webpage_import", cheapLines, 1, null, 0))
        .isInstanceOf(BillingException.class)
        .extracting(error -> ((BillingException) error).getCode())
        .isEqualTo("QUOTE_LINES_INVALID");
  }

  @Test
  void operationPolicyRequiresExactFeaturesAndQuantities() {
    BillingService.validateRequestedOperationLines(
        "webpage_import",
        List.of(
            new QuoteLineRequest(Feature.WEBPAGE_VISION.wireValue(), 2),
            new QuoteLineRequest(Feature.CLASSIFY.wireValue(), 1)),
        2,
        null,
        0);

    assertThatThrownBy(
            () ->
                BillingService.validateRequestedOperationLines(
                    "webpage_import",
                    List.of(
                        new QuoteLineRequest(Feature.WEBPAGE_VISION.wireValue(), 1),
                        new QuoteLineRequest(Feature.CLASSIFY.wireValue(), 1)),
                    2,
                    null,
                    0))
        .isInstanceOf(BillingException.class)
        .extracting(error -> ((BillingException) error).getCode())
        .isEqualTo("QUOTE_LINES_INVALID");

    assertThatThrownBy(
            () ->
                BillingService.validateRequestedOperationLines(
                    "webpage_import",
                    List.of(
                        new QuoteLineRequest(Feature.WEBPAGE_VISION.wireValue(), 2),
                        new QuoteLineRequest(Feature.CLASSIFY.wireValue(), 1),
                        new QuoteLineRequest(Feature.AI_ANSWER.wireValue(), 1)),
                    2,
                    null,
                    0))
        .isInstanceOf(BillingException.class)
        .extracting(error -> ((BillingException) error).getCode())
        .isEqualTo("QUOTE_LINES_INVALID");
  }

  @Test
  void acceptsTheExistingFrontendQuoteCombinations() {
    BillingService.validateRequestedOperationLines(
        "text_import",
        List.of(new QuoteLineRequest(Feature.AI_TEXT.wireValue(), 1)),
        null,
        null,
        0);
    BillingService.validateRequestedOperationLines(
        "webpage_import",
        List.of(
            new QuoteLineRequest(Feature.WEBPAGE_VISION.wireValue(), 1),
            new QuoteLineRequest(Feature.CLASSIFY.wireValue(), 1)),
        1,
        null,
        0);
    BillingService.validateRequestedOperationLines(
        "document_import",
        List.of(
            new QuoteLineRequest(Feature.DOCUMENT_EXTRACT.wireValue(), 1),
            new QuoteLineRequest(Feature.CLASSIFY.wireValue(), 1)),
        null,
        8_000,
        0);
    BillingService.validateRequestedOperationLines(
        "screenshot_import",
        List.of(
            new QuoteLineRequest(Feature.SCREENSHOT_OCR.wireValue(), 1),
            new QuoteLineRequest(Feature.CLASSIFY.wireValue(), 1)),
        null,
        null,
        0);
    BillingService.validateRequestedOperationLines(
        "internet_search",
        List.of(new QuoteLineRequest(Feature.INTERNET_SEARCH.wireValue(), 1)),
        null,
        null,
        0);
    BillingService.validateRequestedOperationLines(
        "knowledge_chat",
        List.of(new QuoteLineRequest(Feature.AI_ANSWER.wireValue(), 1)),
        null,
        null,
        0);
    BillingService.validateRequestedOperationLines(
        "internet_import",
        List.of(
            new QuoteLineRequest(Feature.WEBPAGE_VISION.wireValue(), 1),
            new QuoteLineRequest(Feature.CLASSIFY.wireValue(), 1)),
        null,
        null,
        0);
  }

  @Test
  void documentAndImageQuantitiesComeFromServerFacts() {
    Map<Feature, Integer> document =
        BillingService.operationRequirements("document_import", null, 20_001, 1);
    Map<Feature, Integer> screenshot =
        BillingService.operationRequirements("screenshot_import", null, null, 3);

    assertThat(document)
        .containsEntry(Feature.DOCUMENT_EXTRACT, 3)
        .containsEntry(Feature.CLASSIFY, 1);
    assertThat(screenshot)
        .containsEntry(Feature.SCREENSHOT_OCR, 3)
        .containsEntry(Feature.CLASSIFY, 1);
  }

  @Test
  void internetResultImportChargesVisionAndClassification() {
    assertThat(BillingService.operationRequirements("internet_import", null, null, 0))
        .containsEntry(Feature.WEBPAGE_VISION, 1)
        .containsEntry(Feature.CLASSIFY, 1);
  }

  @Test
  void answerAndInternetAnswerUseOnePointPerSuccessfulStep() {
    assertThat(Feature.AI_ANSWER.defaultPoints()).isEqualTo(1);
    assertThat(Feature.INTERNET_SEARCH.defaultPoints()).isEqualTo(1);
    assertThat(BillingService.operationRequirements("knowledge_chat", null, null, 0))
        .containsExactly(Map.entry(Feature.AI_ANSWER, 1));
    assertThat(BillingService.operationRequirements("internet_search_with_answer", null, null, 0))
        .containsEntry(Feature.INTERNET_SEARCH, 1)
        .containsEntry(Feature.AI_ANSWER, 1);
  }

  @Test
  void duplicateImageContentCountsOnlyOnce() {
    List<BillingService.AssetRow> assets =
        List.of(
            new BillingService.AssetRow(UUID.randomUUID(), "a".repeat(64), "image/png", 10),
            new BillingService.AssetRow(UUID.randomUUID(), "a".repeat(64), "image/png", 10),
            new BillingService.AssetRow(UUID.randomUUID(), "b".repeat(64), "image/jpeg", 20));

    assertThat(BillingService.uniqueAssets(assets)).hasSize(2);
  }

  @Test
  void imageAssetsCannotBeAttachedWithoutTheirBilledOcrLines() {
    assertThatThrownBy(
            () ->
                BillingService.validateRequestedOperationLines(
                    "text_import",
                    List.of(new QuoteLineRequest(Feature.AI_TEXT.wireValue(), 1)),
                    null,
                    null,
                    1))
        .isInstanceOf(BillingException.class)
        .extracting(error -> ((BillingException) error).getCode())
        .isEqualTo("QUOTE_LINES_INVALID");
  }

  @Test
  void preflightMustMatchOwnerEnvironmentKindAndExpiry() {
    BillingService.PreflightRow valid =
        new BillingService.PreflightRow(
            OWNER_ID, "test", "document", 20_001, 1, NOW.plusSeconds(60));
    stubPreflight(valid);
    assertThat(service.requirePreflight(OWNER_ID, PREFLIGHT_ID, "document")).isEqualTo(valid);

    assertPreflightRejected(
        new BillingService.PreflightRow(
            UUID.randomUUID(), "test", "document", 20_001, 1, NOW.plusSeconds(60)));
    assertPreflightRejected(
        new BillingService.PreflightRow(
            OWNER_ID, "production", "document", 20_001, 1, NOW.plusSeconds(60)));
    assertPreflightRejected(
        new BillingService.PreflightRow(
            OWNER_ID, "test", "webpage", 20_001, 1, NOW.plusSeconds(60)));
    assertPreflightRejected(
        new BillingService.PreflightRow(OWNER_ID, "test", "document", 20_001, 1, NOW));
  }

  @Test
  void webhookRequiresExplicitValidSignatureAndMatchingProduct() {
    BillingService.PaymentOrderRow order =
        new BillingService.PaymentOrderRow(
            UUID.randomUUID(),
            OWNER_ID,
            "kb_pack_9",
            900,
            "CNY",
            400,
            OrderStatus.PENDING,
            "PROD_9",
            "SESSION_1");

    assertThat(BillingService.validPaymentFacts(event("valid", "PROD_9", true), order, "webhook"))
        .isTrue();
    assertThat(BillingService.validPaymentFacts(event("invalid", "PROD_9", true), order, "webhook"))
        .isFalse();
    assertThat(BillingService.validPaymentFacts(event(null, "PROD_9", true), order, "webhook"))
        .isFalse();
    assertThat(BillingService.validPaymentFacts(event("valid", null, true), order, "webhook"))
        .isFalse();
    assertThat(BillingService.validPaymentFacts(event("valid", "PROD_29", true), order, "webhook"))
        .isFalse();
    assertThat(
            BillingService.validPaymentFacts(
                event("not_applicable", "PROD_9", true), order, "query"))
        .isTrue();
    assertThat(
            BillingService.validPaymentFacts(event("not_applicable", null, true), order, "query"))
        .isFalse();
  }

  @Test
  void checkoutReturnUrlCarriesTheLocalOrderId() {
    UUID orderId = UUID.fromString("33333333-3333-3333-3333-333333333333");

    assertThat(
            BillingService.checkoutSuccessUrl(
                "https://seaunit.site/kb/billing/return?source=waffo", orderId))
        .isEqualTo("https://seaunit.site/kb/billing/return?source=waffo&orderId=" + orderId);
  }

  @Test
  void checkoutBridgeRequestCarriesTheAuthenticatedAccountEmail() {
    UUID orderId = UUID.fromString("44444444-4444-4444-4444-444444444444");
    OwnerAccountEntity owner = OwnerAccountEntity.create("Buyer@Example.com", "hash", "zh-CN");
    BillingService.PaymentOrderRow order =
        new BillingService.PaymentOrderRow(
            orderId, owner.getId(), "kb_pack_9", 900, "CNY", 400, OrderStatus.PENDING, null, null);

    CheckoutBridgeRequest request =
        BillingService.checkoutBridgeRequest(
            order, owner, "https://seaunit.site/kb/billing/return");

    assertThat(request.buyerIdentity()).isEqualTo(owner.getId().toString());
    assertThat(request.buyerEmail()).isEqualTo("buyer@example.com");
  }

  @Test
  void requireReservedUsesIndependentTransactionBeforeExternalAiWork() throws Exception {
    Method method =
        BillingService.class.getMethod(
            "requireReserved",
            org.springframework.security.core.Authentication.class,
            UUID.class,
            String.class);
    Transactional transactional = method.getAnnotation(Transactional.class);

    assertThat(transactional).isNotNull();
    assertThat(transactional.propagation()).isEqualTo(Propagation.REQUIRES_NEW);
  }

  private void assertPreflightRejected(BillingService.PreflightRow row) {
    stubPreflight(row);
    assertThatThrownBy(() -> service.requirePreflight(OWNER_ID, PREFLIGHT_ID, "document"))
        .isInstanceOf(BillingException.class)
        .extracting(error -> ((BillingException) error).getCode())
        .isEqualTo("PREFLIGHT_INVALID");
  }

  @SuppressWarnings("unchecked")
  private void stubPreflight(BillingService.PreflightRow row) {
    when(jdbc.query(anyString(), any(RowMapper.class), eq(PREFLIGHT_ID))).thenReturn(List.of(row));
  }

  private static NormalizedWaffoEvent event(
      String signatureStatus, String productId, boolean productMatch) {
    return new NormalizedWaffoEvent(
        "test",
        "paid",
        "event-" + UUID.randomUUID(),
        "provider-event",
        "delivery",
        UUID.randomUUID().toString(),
        "provider-order",
        "SESSION_1",
        null,
        productId,
        productMatch,
        900,
        "CNY",
        null,
        null,
        signatureStatus,
        "a".repeat(64),
        NOW);
  }

  private static AppProperties properties() {
    AppProperties properties = mock(AppProperties.class);
    when(properties.billing()).thenReturn(AppProperties.Billing.disabled());
    return properties;
  }
}
