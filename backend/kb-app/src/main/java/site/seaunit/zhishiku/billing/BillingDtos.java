package site.seaunit.zhishiku.billing;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class BillingDtos {
  private BillingDtos() {}

  public record RefundPolicy(
      String supportUrl,
      String responseTarget,
      boolean automaticFullRefundRecovery,
      boolean partialRefundManualReview) {}

  public record PointPackResponse(
      String sku,
      String name,
      String nameZh,
      String nameEn,
      int priceMinor,
      String currency,
      int points,
      boolean oneTime) {}

  public record AiRateResponse(
      String feature,
      String unit,
      int pointsPerUnit,
      String description,
      String descriptionZh,
      String descriptionEn) {}

  public record CatalogResponse(
      String environment,
      String versionCode,
      List<PointPackResponse> packs,
      List<AiRateResponse> aiRates,
      RefundPolicy refundPolicy) {}

  public record CreditAccountResponse(
      int availablePoints,
      int reservedPoints,
      int trialPoints,
      int purchasedPoints,
      Instant updatedAt) {}

  public record LedgerEntryResponse(
      String id,
      String operationType,
      int availableDelta,
      int reservedDelta,
      int consumedDelta,
      int availableAfter,
      int reservedAfter,
      String referenceType,
      String referenceId,
      String description,
      Instant createdAt) {}

  public record LedgerPage(List<LedgerEntryResponse> items, String nextCursor) {}

  public record CreateOrderRequest(@NotBlank @Pattern(regexp = "kb_pack_(9|29|99)") String sku) {}

  public record PaymentOrderResponse(
      UUID id,
      String sku,
      String environment,
      int amountMinor,
      String currency,
      int points,
      String providerProductId,
      String status,
      String checkoutUrl,
      Instant paidAt,
      Instant closedAt,
      Instant createdAt,
      Instant updatedAt) {}

  public record OrderPage(List<PaymentOrderResponse> items, String nextCursor) {}

  public record CheckoutSessionResponse(
      UUID orderId, String checkoutUrl, Instant expiresAt, String environment) {}

  public record AutoChargePolicyResponse(
      String featureScope,
      boolean enabled,
      int maxPointsPerTask,
      int maxPointsPerDay,
      String timezone,
      String consentTextVersion,
      int version,
      Instant updatedAt) {}

  public record UpdateAutoChargePolicyRequest(
      boolean enabled,
      @Min(1) @Max(100) int maxPointsPerTask,
      @Min(1) @Max(500) int maxPointsPerDay,
      @NotBlank String consentTextVersion,
      @Min(1) int expectedVersion) {}

  public record PreflightRequest(
      @NotBlank String kind,
      @NotBlank String mediaType,
      @Min(1) long sizeBytes,
      @Min(1) Integer pageCount,
      @Min(1) @Max(10) Integer viewCount) {}

  public record PreflightResponse(
      UUID preflightId,
      int measuredCharactersUpperBound,
      int viewCount,
      String pricingBasis,
      Instant expiresAt) {}

  public record QuoteLineRequest(@NotBlank String feature, @Min(1) @Max(1000) int quantity) {}

  public record CreateQuoteRequest(
      @NotBlank String operation,
      UUID itemId,
      UUID preflightId,
      @Size(max = 20) List<UUID> imageAssetIds,
      @NotEmpty @Size(max = 6) List<QuoteLineRequest> lines,
      boolean useAutoCharge) {}

  public record QuoteLineResponse(
      String feature,
      String unit,
      int quantity,
      int pointsPerUnit,
      int maxPoints,
      String successRule) {}

  public record QuoteImageResourceResponse(
      UUID assetId, String contentHash, String mediaType, long normalizedSizeBytes) {}

  public record QuoteResponse(
      UUID quoteId,
      String status,
      List<QuoteLineResponse> lines,
      List<QuoteImageResourceResponse> imageResources,
      int maxPoints,
      int availablePoints,
      int projectedRemainingPoints,
      Instant expiresAt,
      boolean autoChargeEligible) {}

  public record ConfirmQuoteRequest(
      @Min(1) int expectedMaxPoints, @NotNull Boolean explicitConsent) {}

  public record BillingTaskResponse(
      UUID id,
      String status,
      List<QuoteLineResponse> lines,
      int maxPoints,
      int settledPoints,
      int reservedPoints,
      Instant expiresAt,
      boolean autoChargeApplied,
      Instant createdAt,
      Instant updatedAt) {}

  public record RefundCaseResponse(
      UUID id,
      UUID orderId,
      String status,
      int refundAmountMinor,
      int expectedRecoveryPoints,
      int recoveredPoints,
      int unrecoveredPoints,
      Instant reviewDueAt,
      int version,
      String resolution,
      Instant createdAt,
      Instant resolvedAt) {}

  public record FinanceSummaryResponse(
      LocalDate from,
      LocalDate to,
      int orderCount,
      long grossAmountMinor,
      long refundedAmountMinor,
      long pointsGranted,
      long pointsConsumed,
      long pointsRefundRecovered,
      long pointsUnrecovered,
      Map<String, Integer> ordersByStatus,
      Map<String, Integer> ordersBySku) {}

  public record RefundResolutionRequest(
      @NotBlank String outcome,
      @Min(0) int recoveredPoints,
      @Min(0) int unrecoveredPoints,
      @NotBlank @Size(max = 2000) String resolutionNoteSafe,
      @Min(1) int expectedVersion) {}

  public record ManualAdjustmentRequest(
      @NotNull UUID targetOwnerId,
      @Min(1) int points,
      @NotBlank String direction,
      @NotBlank String reasonCode,
      @NotBlank @Size(max = 128) String referenceId,
      @Size(max = 2000) String noteSafe) {}

  public record NormalizedWaffoEvent(
      @NotBlank String environment,
      @NotBlank String eventType,
      @NotBlank String sourceEventKey,
      String providerEventId,
      String deliveryId,
      String orderId,
      String providerOrderId,
      String providerSessionId,
      String providerRefundId,
      String providerProductId,
      boolean productMatch,
      Integer amountMinor,
      String currency,
      Integer refundAmountMinor,
      String reasonCode,
      String signatureStatus,
      @NotBlank String payloadHash,
      Instant receivedAt) {}

  public record BridgeHealthResponse(
      boolean ok, boolean configured, String environment, boolean dryRun) {}

  public record CheckoutBridgeRequest(
      UUID orderId,
      String sku,
      int amountMinor,
      String currency,
      String buyerIdentity,
      String successUrl) {}

  public record CheckoutBridgeResponse(
      String sessionId,
      String checkoutUrl,
      Instant expiresAt,
      String environment,
      String productId) {}

  public record OrderStatusBridgeRequest(UUID orderId, String providerSessionId) {}

  public record NormalizedStatusResponse(
      String environment,
      String status,
      Integer amountMinor,
      String currency,
      String productId,
      boolean productMatch,
      Instant checkedAt,
      String payloadHash) {}
}
