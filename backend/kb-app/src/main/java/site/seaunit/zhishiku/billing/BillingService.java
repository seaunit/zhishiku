package site.seaunit.zhishiku.billing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;
import site.seaunit.zhishiku.billing.BillingDtos.AiRateResponse;
import site.seaunit.zhishiku.billing.BillingDtos.AutoChargePolicyResponse;
import site.seaunit.zhishiku.billing.BillingDtos.BillingTaskResponse;
import site.seaunit.zhishiku.billing.BillingDtos.CatalogResponse;
import site.seaunit.zhishiku.billing.BillingDtos.CheckoutBridgeRequest;
import site.seaunit.zhishiku.billing.BillingDtos.CheckoutBridgeResponse;
import site.seaunit.zhishiku.billing.BillingDtos.CheckoutSessionResponse;
import site.seaunit.zhishiku.billing.BillingDtos.ConfirmQuoteRequest;
import site.seaunit.zhishiku.billing.BillingDtos.CreateOrderRequest;
import site.seaunit.zhishiku.billing.BillingDtos.CreateQuoteRequest;
import site.seaunit.zhishiku.billing.BillingDtos.CreditAccountResponse;
import site.seaunit.zhishiku.billing.BillingDtos.FinanceSummaryResponse;
import site.seaunit.zhishiku.billing.BillingDtos.LedgerEntryResponse;
import site.seaunit.zhishiku.billing.BillingDtos.LedgerPage;
import site.seaunit.zhishiku.billing.BillingDtos.ManualAdjustmentRequest;
import site.seaunit.zhishiku.billing.BillingDtos.NormalizedStatusResponse;
import site.seaunit.zhishiku.billing.BillingDtos.NormalizedWaffoEvent;
import site.seaunit.zhishiku.billing.BillingDtos.OrderPage;
import site.seaunit.zhishiku.billing.BillingDtos.PaymentOrderResponse;
import site.seaunit.zhishiku.billing.BillingDtos.PointPackResponse;
import site.seaunit.zhishiku.billing.BillingDtos.PreflightRequest;
import site.seaunit.zhishiku.billing.BillingDtos.PreflightResponse;
import site.seaunit.zhishiku.billing.BillingDtos.QuoteImageResourceResponse;
import site.seaunit.zhishiku.billing.BillingDtos.QuoteLineRequest;
import site.seaunit.zhishiku.billing.BillingDtos.QuoteLineResponse;
import site.seaunit.zhishiku.billing.BillingDtos.QuoteResponse;
import site.seaunit.zhishiku.billing.BillingDtos.RefundCaseResponse;
import site.seaunit.zhishiku.billing.BillingDtos.RefundPolicy;
import site.seaunit.zhishiku.billing.BillingDtos.RefundResolutionRequest;
import site.seaunit.zhishiku.billing.BillingDtos.UpdateAutoChargePolicyRequest;
import site.seaunit.zhishiku.config.AppProperties;
import site.seaunit.zhishiku.core.billing.BillingModel;
import site.seaunit.zhishiku.core.billing.BillingModel.AiRate;
import site.seaunit.zhishiku.core.billing.BillingModel.CatalogSnapshot;
import site.seaunit.zhishiku.core.billing.BillingModel.Environment;
import site.seaunit.zhishiku.core.billing.BillingModel.Feature;
import site.seaunit.zhishiku.core.billing.BillingModel.LedgerOperation;
import site.seaunit.zhishiku.core.billing.BillingModel.OrderStatus;
import site.seaunit.zhishiku.core.billing.BillingModel.PointPack;
import site.seaunit.zhishiku.core.billing.BillingModel.QuoteLine;
import site.seaunit.zhishiku.core.billing.BillingModel.TaskStatus;
import site.seaunit.zhishiku.core.billing.BillingModel.Unit;
import site.seaunit.zhishiku.owner.CurrentOwnerService;
import site.seaunit.zhishiku.owner.OwnerAccountEntity;

@Service
public class BillingService {
  private static final Duration PREFLIGHT_TTL = Duration.ofMinutes(20);
  private static final int MAX_PAGE_SIZE = 100;

  private final JdbcTemplate jdbc;
  private final ObjectMapper mapper;
  private final CurrentOwnerService owners;
  private final BillingBridgeClient bridge;
  private final AppProperties.Billing config;
  private final Clock clock;
  private final Environment environment;

  public BillingService(
      JdbcTemplate jdbc,
      ObjectMapper mapper,
      CurrentOwnerService owners,
      BillingBridgeClient bridge,
      AppProperties properties,
      Clock clock) {
    this.jdbc = jdbc;
    this.mapper = mapper;
    this.owners = owners;
    this.bridge = bridge;
    this.config = properties.billing();
    this.clock = clock;
    this.environment = Environment.from(config.environment());
    if (environment == Environment.PRODUCTION && properties.webImport().allowUnbilledAi()) {
      throw new IllegalStateException("Production AI processing cannot bypass billing.");
    }
    validateConfiguration();
  }

  @PostConstruct
  void initializeEnvironmentGuard() {
    jdbc.update(
        """
        INSERT INTO billing_environment_guard (singleton_id, environment, initialized_at)
        VALUES (1, ?, ?)
        ON CONFLICT (singleton_id) DO NOTHING
        """,
        environment.wireValue(),
        timestamp(clock.instant()));
    String stored =
        jdbc.queryForObject(
            "SELECT environment FROM billing_environment_guard WHERE singleton_id = 1",
            String.class);
    if (!environment.wireValue().equals(stored)) {
      throw new IllegalStateException(
          "The physical billing database belongs to a different environment.");
    }
  }

  private void validateConfiguration() {
    Environment databaseEnvironment = Environment.from(config.databaseEnvironment());
    if (environment != databaseEnvironment) {
      throw new IllegalStateException(
          "APP_ENV and BILLING_DB_ENV must identify the same physical billing environment.");
    }
    if (environment == Environment.PRODUCTION) {
      String refundUrl = config.refundSupportUrl();
      if (refundUrl == null
          || refundUrl.isBlank()
          || refundUrl.contains(".invalid")
          || (!refundUrl.startsWith("https://") && !refundUrl.startsWith("mailto:"))) {
        throw new IllegalStateException("Production refund support URL is not approved.");
      }
    }
  }

  public boolean enabled() {
    return config.enabled();
  }

  @Transactional
  public CreditAccountResponse ensureAccount(UUID ownerId) {
    Optional<CreditAccountResponse> existing = findAccount(ownerId);
    if (existing.isPresent()) {
      return existing.get();
    }
    Instant now = clock.instant();
    jdbc.update(
        """
        INSERT INTO billing_credit_account
          (owner_id, available_points, reserved_points, trial_points, purchased_points,
           lifetime_granted, lifetime_settled, version, updated_at)
        VALUES (?, ?, 0, ?, 0, ?, 0, 1, ?)
        ON CONFLICT (owner_id) DO NOTHING
        """,
        ownerId,
        Math.max(0, config.trialPoints()),
        Math.max(0, config.trialPoints()),
        Math.max(0, config.trialPoints()),
        timestamp(now));
    if (config.trialPoints() > 0) {
      List<UUID> batches =
          jdbc.query(
              "SELECT id FROM billing_credit_batch WHERE owner_id = ? AND source_type = 'trial'",
              (rs, row) -> rs.getObject(1, UUID.class),
              ownerId);
      if (batches.isEmpty()) {
        UUID batchId = UUID.randomUUID();
        jdbc.update(
            """
            INSERT INTO billing_credit_batch
              (id, owner_id, source_type, order_id, environment, initial_points,
               remaining_points, reserved_points, status, priority_at, version,
               created_at, updated_at)
            VALUES (?, ?, 'trial', NULL, ?, ?, ?, 0, 'active', ?, 1, ?, ?)
            """,
            batchId,
            ownerId,
            environment.wireValue(),
            config.trialPoints(),
            config.trialPoints(),
            timestamp(now),
            timestamp(now),
            timestamp(now));
        appendLedger(
            ownerId,
            batchId,
            "batch",
            LedgerOperation.TRIAL_GRANT,
            config.trialPoints(),
            0,
            0,
            config.trialPoints(),
            0,
            "trial",
            ownerId.toString(),
            "Trial points",
            now);
      }
    }
    return findAccount(ownerId)
        .orElseThrow(() -> new IllegalStateException("Credit account could not be created."));
  }

  @Transactional
  public CreditAccountResponse account(Authentication authentication) {
    OwnerAccountEntity owner = owners.require(authentication);
    return ensureAccount(owner.getId());
  }

  public CatalogResponse catalog() {
    CatalogSnapshot catalog = loadCatalog();
    List<PointPackResponse> packs =
        catalog.packs().stream()
            .map(
                pack ->
                    new PointPackResponse(
                        pack.sku(),
                        "en".equals(Locale.getDefault().getLanguage())
                            ? pack.nameEn()
                            : pack.nameZh(),
                        pack.nameZh(),
                        pack.nameEn(),
                        pack.priceMinor(),
                        pack.currency(),
                        pack.points(),
                        pack.oneTime()))
            .toList();
    List<AiRateResponse> rates =
        catalog.rates().values().stream()
            .sorted(Comparator.comparing(rate -> rate.feature().ordinal()))
            .map(
                rate ->
                    new AiRateResponse(
                        rate.feature().wireValue(),
                        rate.unit().wireValue(),
                        rate.pointsPerUnit(),
                        "en".equals(Locale.getDefault().getLanguage())
                            ? rate.descriptionEn()
                            : rate.descriptionZh(),
                        rate.descriptionZh(),
                        rate.descriptionEn()))
            .toList();
    return new CatalogResponse(
        environment.wireValue(),
        config.enabled(),
        catalog.versionCode(),
        packs,
        rates,
        new RefundPolicy(config.refundSupportUrl(), config.refundResponseTarget(), true, true));
  }

  @Transactional
  public LedgerPage ledger(Authentication authentication, int limit, String cursor) {
    OwnerAccountEntity owner = owners.require(authentication);
    ensureAccount(owner.getId());
    int pageSize = Math.min(Math.max(limit, 1), MAX_PAGE_SIZE);
    long cursorId = parseCursor(cursor);
    List<LedgerEntryResponse> items =
        jdbc.query(
            """
            SELECT id, operation_type, available_delta, reserved_delta, consumed_delta,
                   available_after, reserved_after, reference_type, reference_id,
                   description_safe, created_at
            FROM billing_ledger_entry
            WHERE owner_id = ? AND id < ?
            ORDER BY id DESC
            LIMIT ?
            """,
            (rs, row) ->
                new LedgerEntryResponse(
                    rs.getLong("id") + "",
                    rs.getString("operation_type"),
                    rs.getInt("available_delta"),
                    rs.getInt("reserved_delta"),
                    rs.getInt("consumed_delta"),
                    rs.getInt("available_after"),
                    rs.getInt("reserved_after"),
                    rs.getString("reference_type"),
                    rs.getString("reference_id"),
                    rs.getString("description_safe"),
                    instant(rs, "created_at")),
            owner.getId(),
            cursorId <= 0 ? Long.MAX_VALUE : cursorId,
            pageSize + 1);
    boolean hasMore = items.size() > pageSize;
    List<LedgerEntryResponse> page = hasMore ? items.subList(0, pageSize) : items;
    String next = hasMore ? page.get(page.size() - 1).id() : null;
    return new LedgerPage(List.copyOf(page), next);
  }

  @Transactional
  public PaymentOrderResponse createOrder(
      Authentication authentication, CreateOrderRequest request, String idempotencyKey) {
    OwnerAccountEntity owner = owners.require(authentication);
    ensureAccount(owner.getId());
    CatalogSnapshot catalog = loadCatalog();
    PointPack pack =
        catalog.packs().stream()
            .filter(candidate -> candidate.sku().equals(request.sku()))
            .findFirst()
            .orElseThrow(
                () ->
                    new BillingException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "SKU_UNAVAILABLE",
                        "The selected pack is unavailable."));
    Optional<PaymentOrderResponse> pending =
        jdbc
            .query(
                """
            SELECT * FROM billing_payment_order
            WHERE owner_id = ? AND environment = ? AND sku_snapshot = ? AND status = 'PENDING'
            ORDER BY created_at DESC LIMIT 1
            """,
                (rs, row) -> order(rs),
                owner.getId(),
                environment.wireValue(),
                pack.sku())
            .stream()
            .findFirst();
    if (pending.isPresent()) {
      return pending.get();
    }
    UUID orderId = UUID.randomUUID();
    String key =
        idempotencyKey == null || idempotencyKey.isBlank()
            ? "order-" + orderId
            : idempotencyKey.strip();
    Instant now = clock.instant();
    UUID packId = loadPackId(catalog.versionId(), pack.sku());
    try {
      jdbc.update(
          """
          INSERT INTO billing_payment_order
            (id, owner_id, pack_id, sku_snapshot, catalog_version_id, environment,
             amount_minor, currency, points_snapshot, status, next_check_at,
             reconciliation_attempts, idempotency_key, version, created_at, updated_at)
          VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDING', ?, 0, ?, 1, ?, ?)
          """,
          orderId,
          owner.getId(),
          packId,
          pack.sku(),
          catalog.versionId(),
          environment.wireValue(),
          pack.priceMinor(),
          pack.currency(),
          pack.points(),
          timestamp(now.plus(Duration.ofMinutes(15))),
          key,
          timestamp(now),
          timestamp(now));
    } catch (DuplicateKeyException error) {
      return findPendingOrder(owner.getId(), pack.sku())
          .orElseThrow(
              () ->
                  new BillingException(
                      HttpStatus.CONFLICT,
                      "ORDER_CONFLICT",
                      "Another order for this pack already exists."));
    }
    return getOrder(authentication, orderId);
  }

  @Transactional
  public PaymentOrderResponse getOrder(Authentication authentication, UUID orderId) {
    OwnerAccountEntity owner = owners.require(authentication);
    return jdbc
        .query(
            "SELECT * FROM billing_payment_order WHERE id = ? AND owner_id = ?",
            (rs, row) -> order(rs),
            orderId,
            owner.getId())
        .stream()
        .findFirst()
        .orElseThrow(
            () ->
                new BillingException(
                    HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "Order was not found."));
  }

  @Transactional
  public OrderPage orders(Authentication authentication, int limit, String cursor) {
    OwnerAccountEntity owner = owners.require(authentication);
    int pageSize = Math.min(Math.max(limit, 1), MAX_PAGE_SIZE);
    Instant before = cursor == null || cursor.isBlank() ? clock.instant() : Instant.parse(cursor);
    List<PaymentOrderResponse> rows =
        jdbc.query(
            """
            SELECT * FROM billing_payment_order
            WHERE owner_id = ? AND created_at < ?
            ORDER BY created_at DESC LIMIT ?
            """,
            (rs, row) -> order(rs),
            owner.getId(),
            timestamp(before),
            pageSize + 1);
    boolean more = rows.size() > pageSize;
    List<PaymentOrderResponse> page = more ? rows.subList(0, pageSize) : rows;
    String next = more ? page.get(page.size() - 1).createdAt().toString() : null;
    return new OrderPage(List.copyOf(page), next);
  }

  @Transactional
  public CheckoutSessionResponse checkout(Authentication authentication, UUID orderId) {
    requireBillingEnabled();
    OwnerAccountEntity owner = owners.require(authentication);
    PaymentOrderRow order = lockOrder(orderId, owner.getId());
    if (!OrderStatus.PENDING.equals(order.status())) {
      throw new BillingException(
          HttpStatus.CONFLICT, "ORDER_NOT_PAYABLE", "This order cannot open a new checkout.");
    }
    Optional<CheckoutSessionResponse> existing =
        jdbc
            .query(
                """
            SELECT order_id, checkout_url, expires_at, environment
            FROM billing_checkout_session
            WHERE order_id = ? AND status = 'open' AND expires_at > ?
            ORDER BY created_at DESC LIMIT 1
            """,
                (rs, row) ->
                    new CheckoutSessionResponse(
                        rs.getObject("order_id", UUID.class),
                        rs.getString("checkout_url"),
                        instant(rs, "expires_at"),
                        rs.getString("environment")),
                orderId,
                timestamp(clock.instant()))
            .stream()
            .findFirst();
    if (existing.isPresent()) {
      return existing.get();
    }
    jdbc.update(
        """
        UPDATE billing_checkout_session
        SET status = 'expired', updated_at = ?
        WHERE order_id = ? AND status = 'open' AND expires_at <= ?
        """,
        timestamp(clock.instant()),
        orderId,
        timestamp(clock.instant()));
    CheckoutBridgeResponse response =
        bridge.checkout(
            checkoutBridgeRequest(
                order, owner, checkoutSuccessUrl(config.checkoutSuccessUrl(), orderId)),
            orderId.toString());
    Instant now = clock.instant();
    jdbc.update(
        """
        INSERT INTO billing_checkout_session
          (id, order_id, environment, provider_session_id, checkout_url, status,
           expires_at, created_at, updated_at)
        VALUES (?, ?, ?, ?, ?, 'open', ?, ?, ?)
        """,
        UUID.randomUUID(),
        orderId,
        environment.wireValue(),
        response.sessionId(),
        response.checkoutUrl(),
        timestamp(response.expiresAt()),
        timestamp(now),
        timestamp(now));
    jdbc.update(
        """
        UPDATE billing_payment_order
        SET checkout_url = ?, provider_session_id = ?, provider_product_id = ?,
            version = version + 1, updated_at = ?
        WHERE id = ?
        """,
        response.checkoutUrl(),
        response.sessionId(),
        response.productId(),
        timestamp(now),
        orderId);
    return new CheckoutSessionResponse(
        orderId, response.checkoutUrl(), response.expiresAt(), response.environment());
  }

  static String checkoutSuccessUrl(String baseUrl, UUID orderId) {
    return UriComponentsBuilder.fromUriString(baseUrl)
        .replaceQueryParam("orderId", orderId)
        .build()
        .toUriString();
  }

  static CheckoutBridgeRequest checkoutBridgeRequest(
      PaymentOrderRow order, OwnerAccountEntity owner, String successUrl) {
    return new CheckoutBridgeRequest(
        order.id(),
        order.sku(),
        order.amountMinor(),
        order.currency(),
        owner.getId().toString(),
        owner.getEmailNormalized(),
        successUrl);
  }

  @Transactional
  public AutoChargePolicyResponse autoCharge(Authentication authentication) {
    OwnerAccountEntity owner = owners.require(authentication);
    ensureAutoPolicy(owner.getId());
    return jdbc.query(
            """
            SELECT feature_scope, enabled, max_points_per_task, max_points_per_day,
                   timezone, consent_text_version, version, effective_at
            FROM billing_auto_charge_policy
            WHERE owner_id = ? AND revoked_at IS NULL
            """,
            (rs, row) ->
                new AutoChargePolicyResponse(
                    rs.getString("feature_scope"),
                    rs.getBoolean("enabled"),
                    rs.getInt("max_points_per_task"),
                    rs.getInt("max_points_per_day"),
                    rs.getString("timezone"),
                    rs.getString("consent_text_version"),
                    rs.getInt("version"),
                    instant(rs, "effective_at")),
            owner.getId())
        .get(0);
  }

  @Transactional
  public AutoChargePolicyResponse updateAutoCharge(
      Authentication authentication, UpdateAutoChargePolicyRequest request) {
    OwnerAccountEntity owner = owners.require(authentication);
    ensureAutoPolicy(owner.getId());
    int updated =
        jdbc.update(
            """
            UPDATE billing_auto_charge_policy
            SET enabled = ?, max_points_per_task = ?, max_points_per_day = ?,
                consent_text_version = ?, effective_at = ?, version = version + 1
            WHERE owner_id = ? AND feature_scope = 'classify'
              AND revoked_at IS NULL AND version = ?
            """,
            request.enabled(),
            request.maxPointsPerTask(),
            request.maxPointsPerDay(),
            request.consentTextVersion(),
            timestamp(clock.instant()),
            owner.getId(),
            request.expectedVersion());
    if (updated != 1) {
      throw new BillingException(
          HttpStatus.CONFLICT, "AUTO_CHARGE_VERSION_CONFLICT", "Auto-charge policy changed.");
    }
    return autoCharge(authentication);
  }

  @Transactional
  public PreflightResponse preflight(Authentication authentication, PreflightRequest request) {
    OwnerAccountEntity owner = owners.require(authentication);
    String kind = request.kind().strip().toLowerCase(Locale.ROOT);
    if (!Set.of("document", "screenshot", "webpage").contains(kind)) {
      throw new BillingException(
          HttpStatus.UNPROCESSABLE_ENTITY, "PREFLIGHT_KIND_INVALID", "Unsupported preflight kind.");
    }
    int viewCount = request.viewCount() == null ? 1 : request.viewCount();
    int characters =
        switch (kind) {
          case "document" ->
              Math.min(
                  60_000,
                  Math.max(10_000, Math.max((int) Math.ceil(request.sizeBytes() / 8.0), 10_000)));
          case "screenshot" -> 0;
          default -> 0;
        };
    UUID id = UUID.randomUUID();
    Instant now = clock.instant();
    String basis =
        switch (kind) {
          case "document" -> "document_chars";
          case "screenshot" -> "screenshot_image";
          default -> "webpage_view";
        };
    Instant expiresAt = now.plus(PREFLIGHT_TTL);
    jdbc.update(
        """
        INSERT INTO billing_preflight
          (id, owner_id, environment, kind, media_type, size_bytes,
           measured_characters_upper_bound, view_count, pricing_basis, expires_at, created_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        id,
        owner.getId(),
        environment.wireValue(),
        kind,
        request.mediaType(),
        request.sizeBytes(),
        characters,
        viewCount,
        basis,
        timestamp(expiresAt),
        timestamp(now));
    return new PreflightResponse(id, characters, viewCount, basis, expiresAt);
  }

  @Transactional
  public QuoteResponse createQuote(Authentication authentication, CreateQuoteRequest request) {
    OwnerAccountEntity owner = owners.require(authentication);
    CreditAccountResponse account = ensureAccount(owner.getId());
    CatalogSnapshot catalog = loadCatalog();
    String operation = request.operation().strip();
    String requiredPreflightKind = requiredPreflightKind(operation);
    PreflightRow preflight = null;
    if (requiredPreflightKind != null) {
      if (request.preflightId() == null) {
        throw new BillingException(
            HttpStatus.UNPROCESSABLE_ENTITY,
            "PREFLIGHT_REQUIRED",
            "A matching preflight is required for this operation.");
      }
      preflight = requirePreflight(owner.getId(), request.preflightId(), requiredPreflightKind);
    } else if (request.preflightId() != null) {
      throw new BillingException(
          HttpStatus.UNPROCESSABLE_ENTITY,
          "PREFLIGHT_NOT_ALLOWED",
          "This operation does not accept a preflight.");
    }
    List<AssetRow> imageAssets = resolveUniqueAssets(owner.getId(), request.imageAssetIds());
    Integer viewCount = preflight == null ? null : preflight.viewCount();
    Integer characters = preflight == null ? null : preflight.charactersUpperBound();
    validateRequestedOperationLines(
        operation, request.lines(), viewCount, characters, imageAssets.size());
    List<ResolvedLine> resolvedLines =
        resolveLines(catalog, operation, viewCount, characters, imageAssets.size());
    int maxPoints = resolvedLines.stream().mapToInt(line -> line.quoteLine().maxPoints()).sum();
    if (maxPoints <= 0) {
      throw new BillingException(
          HttpStatus.UNPROCESSABLE_ENTITY, "QUOTE_EMPTY", "Quote has no billable steps.");
    }
    UUID taskId = UUID.randomUUID();
    Instant now = clock.instant();
    Instant expiresAt = now.plus(Duration.ofSeconds(Math.max(60, config.taskTtlSeconds())));
    String idempotencyKey = "quote-" + taskId;
    boolean autoEligible = request.useAutoCharge() && autoChargeEligible(owner.getId(), maxPoints);
    jdbc.update(
        """
        INSERT INTO billing_task
          (id, owner_id, job_id, item_id, catalog_version_id, operation, environment,
           status, max_points, settled_points, auto_charge_applied, idempotency_key,
           expires_at, version, created_at, updated_at)
        VALUES (?, ?, NULL, ?, ?, ?, ?, 'QUOTED', ?, 0, ?, ?, ?, 1, ?, ?)
        """,
        taskId,
        owner.getId(),
        request.itemId(),
        catalog.versionId(),
        operation,
        environment.wireValue(),
        maxPoints,
        autoEligible,
        idempotencyKey,
        timestamp(expiresAt),
        timestamp(now),
        timestamp(now));
    for (ResolvedLine line : resolvedLines) {
      QuoteLine quote = line.quoteLine();
      jdbc.update(
          """
          INSERT INTO billing_quote_line
            (id, billing_task_id, feature, unit, quantity, unit_points, max_line_points,
             success_rule_code, success_state, settled_points)
          VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'pending', 0)
          """,
          UUID.randomUUID(),
          taskId,
          quote.feature().wireValue(),
          quote.unit().wireValue(),
          quote.quantity(),
          quote.unitPoints(),
          quote.maxPoints(),
          line.successRule());
    }
    List<QuoteImageResourceResponse> resources = insertImageResources(taskId, imageAssets);
    return new QuoteResponse(
        taskId,
        "QUOTED",
        resolvedLines.stream()
            .map(line -> quoteResponse(line.quoteLine(), line.successRule()))
            .toList(),
        resources,
        maxPoints,
        account.availablePoints(),
        account.availablePoints() - maxPoints,
        expiresAt,
        autoEligible);
  }

  @Transactional
  public BillingTaskResponse confirmQuote(
      Authentication authentication, UUID taskId, ConfirmQuoteRequest request) {
    requireBillingEnabled();
    OwnerAccountEntity owner = owners.require(authentication);
    TaskRow task = lockTask(taskId, owner.getId());
    if (task.status() != TaskStatus.QUOTED && task.status() != TaskStatus.CONFIRMED) {
      if (task.status() == TaskStatus.RESERVED
          || task.status() == TaskStatus.RUNNING
          || task.status() == TaskStatus.SETTLED) {
        return taskResponse(task);
      }
      throw new BillingException(
          HttpStatus.CONFLICT, "TASK_NOT_CONFIRMABLE", "Billing task is no longer confirmable.");
    }
    if (!Boolean.TRUE.equals(request.explicitConsent())
        || request.expectedMaxPoints() != task.maxPoints()) {
      throw new BillingException(
          HttpStatus.UNPROCESSABLE_ENTITY,
          "QUOTE_CONFIRMATION_INVALID",
          "Quote confirmation does not match the issued quote.");
    }
    Instant now = clock.instant();
    if (!now.isBefore(task.expiresAt())) {
      jdbc.update(
          "UPDATE billing_task SET status = 'EXPIRED', version = version + 1, updated_at = ? WHERE id = ?",
          timestamp(now),
          taskId);
      throw new BillingException(HttpStatus.CONFLICT, "QUOTE_EXPIRED", "Quote has expired.");
    }
    jdbc.update(
        "UPDATE billing_task SET status = 'CONFIRMED', confirmed_at = ?, version = version + 1, updated_at = ? WHERE id = ?",
        timestamp(now),
        timestamp(now),
        taskId);
    boolean auto = task.autoChargeApplied() && autoChargeEligible(owner.getId(), task.maxPoints());
    reserve(owner.getId(), taskId, task.maxPoints(), auto);
    return getTask(authentication, taskId);
  }

  @Transactional
  public BillingTaskResponse getTask(Authentication authentication, UUID taskId) {
    OwnerAccountEntity owner = owners.require(authentication);
    TaskRow task = lockTask(taskId, owner.getId());
    return taskResponse(task);
  }

  @Transactional
  public BillingTaskResponse cancelTask(Authentication authentication, UUID taskId) {
    OwnerAccountEntity owner = owners.require(authentication);
    TaskRow task = lockTask(taskId, owner.getId());
    if (task.status() == TaskStatus.RESERVED || task.status() == TaskStatus.RUNNING) {
      releaseReservation(task, "cancelled");
    } else if (task.status() == TaskStatus.QUOTED || task.status() == TaskStatus.CONFIRMED) {
      jdbc.update(
          "UPDATE billing_task SET status = 'CANCELLED', version = version + 1, updated_at = ? WHERE id = ?",
          timestamp(clock.instant()),
          taskId);
    } else {
      throw new BillingException(
          HttpStatus.CONFLICT, "TASK_NOT_CANCELLABLE", "Billing task cannot be cancelled.");
    }
    return taskResponse(lockTask(taskId, owner.getId()));
  }

  // Commit RUNNING before external AI work so a failure release can acquire the task lock.
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public BillingTaskResponse requireReserved(
      Authentication authentication, UUID taskId, String operation) {
    OwnerAccountEntity owner = owners.require(authentication);
    TaskRow task = lockTask(taskId, owner.getId());
    if (!task.operation().equals(operation)
        || (task.status() != TaskStatus.RESERVED && task.status() != TaskStatus.RUNNING)) {
      throw new BillingException(
          HttpStatus.CONFLICT,
          "BILLING_TASK_NOT_RESERVED",
          "Billing task is not reserved for this operation.");
    }
    if (task.status() == TaskStatus.RUNNING) {
      throw new BillingException(
          HttpStatus.CONFLICT, "BILLING_TASK_IN_PROGRESS", "This billing task is already running.");
    }
    if (!clock.instant().isBefore(task.expiresAt())) {
      releaseReservation(task, "expired");
      throw new BillingException(
          HttpStatus.CONFLICT, "BILLING_TASK_EXPIRED", "Billing task has expired.");
    }
    if (task.status() == TaskStatus.RESERVED) {
      jdbc.update(
          "UPDATE billing_task SET status = 'RUNNING', version = version + 1, updated_at = ? WHERE id = ?",
          timestamp(clock.instant()),
          taskId);
    }
    return taskResponse(lockTask(taskId, owner.getId()));
  }

  @Transactional
  public BillingTaskResponse settle(
      UUID ownerId, UUID taskId, Set<Feature> succeededFeatures, String failureReason) {
    TaskRow task = lockTask(taskId, ownerId);
    if (task.status() == TaskStatus.SETTLED || task.status() == TaskStatus.RELEASED) {
      return taskResponse(task);
    }
    if (task.status() != TaskStatus.RUNNING && task.status() != TaskStatus.RESERVED) {
      throw new BillingException(
          HttpStatus.CONFLICT, "BILLING_TASK_NOT_SETTLEABLE", "Billing task cannot be settled.");
    }
    ReservationRow reservation = lockReservation(taskId, ownerId);
    List<AllocationRow> allocations = lockAllocations(reservation.id());
    List<QuoteLineRow> lines = quoteLines(taskId);
    int totalSettled = 0;
    Map<UUID, Integer> settledByBatch = new LinkedHashMap<>();
    Map<UUID, Integer> releasedByBatch = new LinkedHashMap<>();
    int trialSettled = 0;
    int purchasedSettled = 0;
    for (QuoteLineRow line : lines) {
      boolean success = succeededFeatures.contains(line.feature());
      int lineSettled = success ? line.maxPoints() : 0;
      totalSettled += lineSettled;
      jdbc.update(
          "UPDATE billing_quote_line SET success_state = ?, settled_points = ? WHERE id = ?",
          success ? "succeeded" : "failed",
          lineSettled,
          line.id());
    }
    int remainingToSettle = totalSettled;
    int remainingToRelease = reservation.reservedPoints() - totalSettled;
    for (AllocationRow allocation : allocations) {
      int settled = Math.min(remainingToSettle, allocation.reservedPoints());
      int released = allocation.reservedPoints() - settled;
      remainingToSettle -= settled;
      if (released > 0) {
        remainingToRelease -= released;
      }
      settledByBatch.put(allocation.batchId(), settled);
      releasedByBatch.put(allocation.batchId(), released);
      if ("trial".equals(allocation.sourceType())) {
        trialSettled += settled;
      } else {
        purchasedSettled += settled;
      }
      jdbc.update(
          """
          UPDATE billing_reservation_allocation
          SET status = ?, settled_points = ?, released_points = ?
          WHERE id = ?
          """,
          released == 0 ? "settled" : "released",
          settled,
          released,
          allocation.id());
      jdbc.update(
          """
          UPDATE billing_credit_batch
          SET reserved_points = reserved_points - ?,
              remaining_points = remaining_points - ?,
              status = CASE WHEN remaining_points - ? = 0 THEN 'depleted' ELSE status END,
              version = version + 1, updated_at = ?
          WHERE id = ?
          """,
          allocation.reservedPoints(),
          settled,
          settled,
          timestamp(clock.instant()),
          allocation.batchId());
    }
    if (remainingToRelease != 0) {
      throw new IllegalStateException("Reservation allocation does not match task total.");
    }
    AccountRow account = lockAccount(ownerId);
    int availableAfter = account.availablePoints() + reservation.reservedPoints() - totalSettled;
    int reservedAfter = account.reservedPoints() - reservation.reservedPoints();
    jdbc.update(
        """
        UPDATE billing_credit_account
        SET available_points = ?, reserved_points = ?,
            trial_points = trial_points - ?, purchased_points = purchased_points - ?,
            lifetime_settled = lifetime_settled + ?,
            version = version + 1, updated_at = ?
        WHERE owner_id = ?
        """,
        availableAfter,
        reservedAfter,
        trialSettled,
        purchasedSettled,
        totalSettled,
        timestamp(clock.instant()),
        ownerId);
    for (AllocationRow allocation : allocations) {
      int settled = settledByBatch.getOrDefault(allocation.batchId(), 0);
      int released = releasedByBatch.getOrDefault(allocation.batchId(), 0);
      if (settled > 0) {
        appendLedger(
            ownerId,
            allocation.batchId(),
            "batch",
            LedgerOperation.SETTLE,
            0,
            -settled,
            settled,
            availableAfter,
            reservedAfter,
            "ai_task",
            taskId.toString(),
            "AI task settled",
            clock.instant());
      }
      if (released > 0) {
        appendLedger(
            ownerId,
            allocation.batchId(),
            "batch",
            LedgerOperation.RESERVE_RELEASE,
            released,
            -released,
            0,
            availableAfter,
            reservedAfter,
            "ai_task",
            taskId.toString(),
            failureReason == null ? "Unused reservation released" : failureReason,
            clock.instant());
      }
    }
    jdbc.update(
        """
        UPDATE billing_reservation
        SET status = ?, settled_points = ?, released_points = ?,
            version = version + 1, updated_at = ?
        WHERE id = ?
        """,
        totalSettled == 0 ? "released" : "settled",
        totalSettled,
        reservation.reservedPoints() - totalSettled,
        timestamp(clock.instant()),
        reservation.id());
    TaskStatus finalStatus = totalSettled == 0 ? TaskStatus.RELEASED : TaskStatus.SETTLED;
    jdbc.update(
        """
        UPDATE billing_task
        SET status = ?, settled_points = ?, version = version + 1, updated_at = ?
        WHERE id = ?
        """,
        finalStatus.name(),
        totalSettled,
        timestamp(clock.instant()),
        taskId);
    if (task.autoChargeApplied()) {
      adjustDailyAutoUsage(ownerId, -reservation.reservedPoints(), totalSettled);
    }
    return taskResponse(lockTask(taskId, ownerId));
  }

  @Transactional
  public BillingTaskResponse release(UUID ownerId, UUID taskId, String reason) {
    TaskRow task = lockTask(taskId, ownerId);
    if (task.status() == TaskStatus.SETTLED || task.status() == TaskStatus.RELEASED) {
      return taskResponse(task);
    }
    releaseReservation(task, reason);
    return taskResponse(lockTask(taskId, ownerId));
  }

  @Transactional
  public BillingTaskResponse settle(
      Authentication authentication, UUID taskId, Set<Feature> succeededFeatures, String reason) {
    OwnerAccountEntity owner = owners.require(authentication);
    return settle(owner.getId(), taskId, succeededFeatures, reason);
  }

  @Transactional
  public BillingTaskResponse release(Authentication authentication, UUID taskId, String reason) {
    OwnerAccountEntity owner = owners.require(authentication);
    return release(owner.getId(), taskId, reason);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public BillingTaskResponse releaseAfterFailure(
      Authentication authentication, UUID taskId, String reason) {
    OwnerAccountEntity owner = owners.require(authentication);
    return release(owner.getId(), taskId, reason);
  }

  @Transactional
  public List<RefundCaseResponse> refunds(Authentication authentication) {
    OwnerAccountEntity owner = owners.require(authentication);
    return jdbc.query(
        """
        SELECT r.* FROM billing_refund_case r
        JOIN billing_payment_order o ON o.id = r.order_id
        WHERE o.owner_id = ? ORDER BY r.created_at DESC
        """,
        (rs, row) -> refund(rs),
        owner.getId());
  }

  @Transactional
  public RefundCaseResponse refund(Authentication authentication, UUID refundId) {
    OwnerAccountEntity owner = owners.require(authentication);
    return jdbc
        .query(
            """
            SELECT r.* FROM billing_refund_case r
            JOIN billing_payment_order o ON o.id = r.order_id
            WHERE r.id = ? AND o.owner_id = ?
            """,
            (rs, row) -> refund(rs),
            refundId,
            owner.getId())
        .stream()
        .findFirst()
        .orElseThrow(
            () ->
                new BillingException(
                    HttpStatus.NOT_FOUND, "REFUND_NOT_FOUND", "Refund case was not found."));
  }

  @Transactional
  public NormalizedWaffoEvent processWaffoEvent(NormalizedWaffoEvent event) {
    return processWaffoEvent(event, "webhook");
  }

  private NormalizedWaffoEvent processWaffoEvent(NormalizedWaffoEvent event, String source) {
    Environment eventEnvironment = Environment.from(event.environment());
    if (eventEnvironment != environment) {
      recordException(
          null,
          null,
          null,
          null,
          "environment_mismatch",
          Map.of("sourceEventKey", event.sourceEventKey()));
      throw new BillingException(
          HttpStatus.UNPROCESSABLE_ENTITY,
          "BILLING_ENVIRONMENT_MISMATCH",
          "Payment event environment does not match this billing environment.");
    }
    Instant now = clock.instant();
    UUID eventId = UUID.randomUUID();
    try {
      jdbc.update(
          """
          INSERT INTO billing_payment_event
            (id, order_id, environment, source, source_event_key, provider_event_id,
             delivery_id, event_type, signature_status, normalized_status, payload_hash,
             processing_status, received_at)
          VALUES (?, NULL, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'received', ?)
          """,
          eventId,
          environment.wireValue(),
          source,
          event.sourceEventKey(),
          event.providerEventId(),
          event.deliveryId(),
          event.eventType(),
          signatureStatus(event.signatureStatus()),
          event.eventType(),
          event.payloadHash(),
          timestamp(event.receivedAt() == null ? now : event.receivedAt()));
    } catch (DuplicateKeyException duplicate) {
      return event;
    }
    UUID orderId = parseUuid(event.orderId());
    if (orderId == null && event.providerOrderId() != null) {
      orderId =
          jdbc
              .query(
                  "SELECT id FROM billing_payment_order WHERE provider_order_id = ? LIMIT 1",
                  (rs, row) -> rs.getObject(1, UUID.class),
                  event.providerOrderId())
              .stream()
              .findFirst()
              .orElse(null);
    }
    if (orderId == null) {
      jdbc.update(
          "UPDATE billing_payment_event SET processing_status = 'manual_review', processed_at = ? WHERE id = ?",
          timestamp(now),
          eventId);
      recordException(
          null,
          null,
          null,
          null,
          "payment_conflict",
          Map.of("sourceEventKey", event.sourceEventKey()));
      return event;
    }
    PaymentOrderRow orderRow =
        jdbc
            .query(
                "SELECT * FROM billing_payment_order WHERE id = ? FOR UPDATE",
                (rs, row) -> orderRow(rs),
                orderId)
            .stream()
            .findFirst()
            .orElse(null);
    if (orderRow == null) {
      jdbc.update(
          "UPDATE billing_payment_event SET processing_status = 'manual_review', processed_at = ? WHERE id = ?",
          timestamp(now),
          eventId);
      recordException(
          null,
          orderId,
          null,
          null,
          "payment_conflict",
          Map.of("sourceEventKey", event.sourceEventKey()));
      return event;
    }
    if (!validPaymentFacts(event, orderRow, source)) {
      jdbc.update(
          "UPDATE billing_payment_event SET order_id = ?, processing_status = 'manual_review', processed_at = ? WHERE id = ?",
          orderId,
          timestamp(now),
          eventId);
      recordException(
          orderRow.ownerId(),
          orderId,
          null,
          null,
          "amount_mismatch",
          Map.of("sourceEventKey", event.sourceEventKey()));
      return event;
    }
    jdbc.update("UPDATE billing_payment_event SET order_id = ? WHERE id = ?", orderId, eventId);
    switch (event.eventType()) {
      case "paid" -> applyPaid(orderRow, eventId, event, now);
      case "refund" -> applyRefund(orderRow, eventId, event, now);
      case "expired" -> {
        if (orderRow.status() == OrderStatus.PENDING) {
          jdbc.update(
              "UPDATE billing_payment_order SET status = 'CLOSED', closed_at = ?, version = version + 1, updated_at = ? WHERE id = ?",
              timestamp(now),
              timestamp(now),
              orderRow.id());
        }
        markEventProcessed(eventId, "applied", now);
      }
      default -> {
        markEventProcessed(eventId, "manual_review", now);
        recordException(
            orderRow.ownerId(),
            orderRow.id(),
            null,
            null,
            "payment_conflict",
            Map.of("sourceEventKey", event.sourceEventKey()));
      }
    }
    return event;
  }

  @Transactional
  public FinanceSummaryResponse financeSummary(
      Authentication authentication, LocalDate from, LocalDate to) {
    requireAdmin(authentication);
    Instant start = from.atStartOfDay(ZoneId.of("Asia/Shanghai")).toInstant();
    Instant end = to.plusDays(1).atStartOfDay(ZoneId.of("Asia/Shanghai")).toInstant();
    Map<String, Object> summary =
        jdbc.queryForMap(
            """
            SELECT count(*) AS order_count,
                   coalesce(sum(CASE WHEN status IN ('PAID','REFUNDING','REFUNDED') THEN amount_minor ELSE 0 END), 0) AS gross,
                   coalesce(sum(CASE WHEN status IN ('REFUNDED') THEN amount_minor ELSE 0 END), 0) AS refunded
            FROM billing_payment_order
            WHERE environment = ? AND created_at >= ? AND created_at < ?
            """,
            environment.wireValue(),
            timestamp(start),
            timestamp(end));
    long granted =
        number(
            jdbc.queryForObject(
                "SELECT coalesce(sum(initial_points),0) FROM billing_credit_batch WHERE environment = ? AND created_at >= ? AND created_at < ?",
                Long.class,
                environment.wireValue(),
                timestamp(start),
                timestamp(end)));
    long consumed =
        number(
            jdbc.queryForObject(
                "SELECT coalesce(sum(consumed_delta),0) FROM billing_ledger_entry WHERE environment = ? AND operation_type = 'settle' AND created_at >= ? AND created_at < ?",
                Long.class,
                environment.wireValue(),
                timestamp(start),
                timestamp(end)));
    long recovered =
        number(
            jdbc.queryForObject(
                "SELECT coalesce(sum(points),0) FROM billing_refund_recovery WHERE status = 'recovered' AND recovered_at >= ? AND recovered_at < ?",
                Long.class,
                timestamp(start),
                timestamp(end)));
    long unrecovered =
        number(
            jdbc.queryForObject(
                "SELECT coalesce(sum(unrecovered_points),0) FROM billing_refund_case WHERE created_at >= ? AND created_at < ?",
                Long.class,
                timestamp(start),
                timestamp(end)));
    Map<String, Integer> byStatus =
        countMap("status", "billing_payment_order", environment.wireValue(), start, end);
    Map<String, Integer> bySku =
        countMap("sku_snapshot", "billing_payment_order", environment.wireValue(), start, end);
    return new FinanceSummaryResponse(
        from,
        to,
        ((Number) summary.get("order_count")).intValue(),
        ((Number) summary.get("gross")).longValue(),
        ((Number) summary.get("refunded")).longValue(),
        granted,
        consumed,
        recovered,
        unrecovered,
        byStatus,
        bySku);
  }

  @Transactional
  public RefundCaseResponse resolveRefund(
      Authentication authentication, UUID refundId, RefundResolutionRequest request) {
    requireAdmin(authentication);
    Map<String, Object> refundRow =
        jdbc.queryForMap("SELECT * FROM billing_refund_case WHERE id = ? FOR UPDATE", refundId);
    int currentVersion = ((Number) refundRow.get("version")).intValue();
    if (currentVersion != request.expectedVersion()) {
      throw new BillingException(
          HttpStatus.CONFLICT, "REFUND_VERSION_CONFLICT", "Refund case changed.");
    }
    String outcome = request.outcome().strip();
    if (!Set.of("recovered", "partially_recovered", "rejected").contains(outcome)) {
      throw new BillingException(
          HttpStatus.UNPROCESSABLE_ENTITY, "REFUND_OUTCOME_INVALID", "Unsupported refund outcome.");
    }
    Instant now = clock.instant();
    UUID orderId = (UUID) refundRow.get("order_id");
    int alreadyRecovered = ((Number) refundRow.get("recovered_points")).intValue();
    int expectedRecovery = ((Number) refundRow.get("expected_recovery_points")).intValue();
    UUID ownerId =
        jdbc.queryForObject(
            "SELECT owner_id FROM billing_payment_order WHERE id = ?", UUID.class, orderId);
    int actualRecovered =
        "rejected".equals(outcome)
            ? 0
            : recoverRefundPoints(
                ownerId, orderId, refundId, Math.max(0, expectedRecovery - alreadyRecovered), now);
    int totalRecovered = alreadyRecovered + actualRecovered;
    int unrecovered = Math.max(0, expectedRecovery - totalRecovered);
    if ("recovered".equals(outcome) && unrecovered > 0) {
      throw new BillingException(
          HttpStatus.UNPROCESSABLE_ENTITY,
          "REFUND_RECOVERY_INCOMPLETE",
          "Not all expected points can be recovered from the original order batch.");
    }
    jdbc.update(
        """
        UPDATE billing_refund_case
        SET status = ?, recovered_points = ?, unrecovered_points = ?,
            resolution_note_safe = ?, resolved_at = ?, version = version + 1
        WHERE id = ?
        """,
        outcome,
        totalRecovered,
        unrecovered,
        request.resolutionNoteSafe(),
        timestamp(now),
        refundId);
    if ("recovered".equals(outcome)) {
      jdbc.update(
          "UPDATE billing_payment_order SET status = 'REFUNDED', refunded_at = ?, version = version + 1, updated_at = ? WHERE id = ?",
          timestamp(now),
          timestamp(now),
          orderId);
    }
    return refund(refundId);
  }

  @Transactional
  public CreditAccountResponse manualAdjustment(
      Authentication authentication, ManualAdjustmentRequest request) {
    requireAdmin(authentication);
    UUID targetOwner = request.targetOwnerId();
    if (!"credit".equals(request.direction()) && !"debit".equals(request.direction())) {
      throw new BillingException(
          HttpStatus.UNPROCESSABLE_ENTITY,
          "ADJUSTMENT_DIRECTION_INVALID",
          "Adjustment direction must be credit or debit.");
    }
    AccountRow account = lockAccount(targetOwner);
    Instant now = clock.instant();
    if ("credit".equals(request.direction())) {
      UUID batchId = UUID.randomUUID();
      int availableAfter = account.availablePoints() + request.points();
      jdbc.update(
          """
          INSERT INTO billing_credit_batch
            (id, owner_id, source_type, order_id, environment, initial_points,
             remaining_points, reserved_points, status, priority_at, version, created_at, updated_at)
          VALUES (?, ?, 'manual_adjustment', NULL, ?, ?, ?, 0, 'active', ?, 1, ?, ?)
          """,
          batchId,
          targetOwner,
          environment.wireValue(),
          request.points(),
          request.points(),
          timestamp(now),
          timestamp(now),
          timestamp(now));
      jdbc.update(
          """
          UPDATE billing_credit_account
          SET available_points = available_points + ?,
              purchased_points = purchased_points + ?,
              lifetime_granted = lifetime_granted + ?,
              version = version + 1, updated_at = ?
          WHERE owner_id = ?
          """,
          request.points(),
          request.points(),
          request.points(),
          timestamp(now),
          targetOwner);
      appendLedger(
          targetOwner,
          batchId,
          "batch",
          LedgerOperation.MANUAL_ADJUSTMENT,
          request.points(),
          0,
          0,
          availableAfter,
          account.reservedPoints(),
          "adjustment",
          request.referenceId(),
          request.reasonCode() + ": " + nullToEmpty(request.noteSafe()),
          now);
    } else {
      debitManualAdjustment(targetOwner, account, request, now);
    }
    return account(targetOwner);
  }

  private void debitManualAdjustment(
      UUID ownerId, AccountRow account, ManualAdjustmentRequest request, Instant now) {
    List<BatchRow> batches =
        jdbc.query(
            """
            SELECT id, remaining_points, reserved_points, source_type
            FROM billing_credit_batch
            WHERE owner_id = ? AND status = 'active'
              AND remaining_points > reserved_points
            ORDER BY CASE source_type WHEN 'trial' THEN 0 ELSE 1 END, priority_at ASC
            FOR UPDATE
            """,
            (rs, row) ->
                new BatchRow(
                    rs.getObject("id", UUID.class),
                    rs.getInt("remaining_points"),
                    rs.getInt("reserved_points"),
                    rs.getString("source_type")),
            ownerId);
    int remaining = request.points();
    int trialDebited = 0;
    int purchasedDebited = 0;
    int availableAfter = account.availablePoints();
    for (BatchRow batch : batches) {
      int debit =
          Math.min(remaining, Math.max(0, batch.remainingPoints() - batch.reservedPoints()));
      if (debit <= 0) continue;
      availableAfter -= debit;
      if ("trial".equals(batch.sourceType())) {
        trialDebited += debit;
      } else {
        purchasedDebited += debit;
      }
      jdbc.update(
          """
          UPDATE billing_credit_batch
          SET remaining_points = remaining_points - ?,
              status = CASE WHEN remaining_points - ? = 0 THEN 'depleted' ELSE status END,
              version = version + 1, updated_at = ?
          WHERE id = ?
          """,
          debit,
          debit,
          timestamp(now),
          batch.id());
      appendLedger(
          ownerId,
          batch.id(),
          "batch",
          LedgerOperation.MANUAL_ADJUSTMENT,
          -debit,
          0,
          0,
          availableAfter,
          account.reservedPoints(),
          "adjustment",
          request.referenceId(),
          request.reasonCode() + ": " + nullToEmpty(request.noteSafe()),
          now);
      remaining -= debit;
    }
    if (remaining > 0) {
      throw new BillingException(
          HttpStatus.UNPROCESSABLE_ENTITY,
          "ADJUSTMENT_NEGATIVE_BALANCE",
          "Manual debit exceeds the available unreserved points.");
    }
    jdbc.update(
        """
        UPDATE billing_credit_account
        SET available_points = available_points - ?,
            trial_points = trial_points - ?,
            purchased_points = purchased_points - ?,
            version = version + 1, updated_at = ?
        WHERE owner_id = ?
        """,
        request.points(),
        trialDebited,
        purchasedDebited,
        timestamp(now),
        ownerId);
  }

  @Transactional
  public int expireReservations() {
    List<UUID> taskIds =
        jdbc.query(
            """
            SELECT id FROM billing_task
            WHERE status IN ('RESERVED','RUNNING') AND expires_at <= ?
            ORDER BY expires_at ASC LIMIT 50
            """,
            (rs, row) -> rs.getObject(1, UUID.class),
            timestamp(clock.instant()));
    int expired = 0;
    for (UUID taskId : taskIds) {
      JdbcTemplate local = jdbc;
      UUID ownerId =
          local.queryForObject(
              "SELECT owner_id FROM billing_task WHERE id = ?", UUID.class, taskId);
      try {
        release(ownerId, taskId, "reservation expired");
        local.update(
            "UPDATE billing_task SET status = 'EXPIRED' WHERE id = ? AND status = 'RELEASED'",
            taskId);
        expired++;
      } catch (RuntimeException error) {
        recordException(
            ownerId,
            null,
            taskId,
            null,
            "reservation_timeout",
            Map.of("taskId", taskId.toString()));
      }
    }
    return expired;
  }

  @Transactional
  public int reconcilePendingOrders() {
    if (!config.enabled()) {
      return 0;
    }
    List<PaymentOrderRow> orders =
        jdbc.query(
            """
            SELECT * FROM billing_payment_order
            WHERE status IN ('PENDING','CONFIRMING')
              AND (next_check_at IS NULL OR next_check_at <= ?)
            ORDER BY updated_at ASC LIMIT 20
            """,
            (rs, row) -> orderRow(rs),
            timestamp(clock.instant()));
    int processed = 0;
    for (PaymentOrderRow order : orders) {
      if (order.providerSessionId() == null) {
        continue;
      }
      try {
        NormalizedStatusResponse status =
            bridge.orderStatus(
                new BillingDtos.OrderStatusBridgeRequest(order.id(), order.providerSessionId()));
        int attempt =
            jdbc.queryForObject(
                    "SELECT coalesce(max(attempt_number),0)+1 FROM billing_reconciliation_attempt WHERE order_id = ?",
                    Integer.class,
                    order.id())
                .intValue();
        Instant now = clock.instant();
        jdbc.update(
            """
            INSERT INTO billing_reconciliation_attempt
              (id, order_id, environment, attempt_number, result, next_check_at, checked_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """,
            UUID.randomUUID(),
            order.id(),
            environment.wireValue(),
            attempt,
            status.status(),
            timestamp(now.plus(Duration.ofMinutes(Math.min(30, attempt * 2)))),
            timestamp(now));
        if ("paid".equals(status.status()) && status.productMatch()) {
          NormalizedWaffoEvent event =
              new NormalizedWaffoEvent(
                  status.environment(),
                  "paid",
                  "query:" + order.providerSessionId() + ":paid:" + status.payloadHash(),
                  null,
                  null,
                  order.id().toString(),
                  null,
                  order.providerSessionId(),
                  null,
                  status.productId(),
                  true,
                  status.amountMinor(),
                  status.currency(),
                  null,
                  null,
                  "not_applicable",
                  status.payloadHash(),
                  status.checkedAt());
          processWaffoEvent(event, "query");
        } else {
          String orderStatus = null;
          if ("closed".equals(status.status())) {
            orderStatus = "CLOSED";
            jdbc.update(
                "UPDATE billing_payment_order SET closed_at = ? WHERE id = ? AND closed_at IS NULL",
                timestamp(now),
                order.id());
            jdbc.update(
                "UPDATE billing_checkout_session SET status = 'expired', updated_at = ? WHERE order_id = ? AND status = 'open'",
                timestamp(now),
                order.id());
          } else if ("refunded".equals(status.status())) {
            orderStatus = "EXCEPTION";
            recordException(
                order.ownerId(),
                order.id(),
                null,
                null,
                "unexpected_refund_state",
                Map.of("result", status.status()));
          } else if ("mismatch".equals(status.status())
              || ("paid".equals(status.status()) && !status.productMatch())) {
            orderStatus = "EXCEPTION";
            recordException(
                order.ownerId(),
                order.id(),
                null,
                null,
                "reconciliation_mismatch",
                Map.of("result", status.status()));
          } else if ("unavailable".equals(status.status())) {
            orderStatus = "CONFIRMING";
          }
          jdbc.update(
              """
              UPDATE billing_payment_order
              SET reconciliation_attempts = reconciliation_attempts + 1,
                  last_reconciliation_result = ?, next_check_at = ?,
                  status = coalesce(?, status),
                  version = version + 1, updated_at = ?
              WHERE id = ?
              """,
              status.status(),
              timestamp(now.plus(Duration.ofMinutes(Math.min(30, attempt * 2)))),
              orderStatus,
              timestamp(now),
              order.id());
        }
        processed++;
      } catch (RuntimeException error) {
        jdbc.update(
            """
            UPDATE billing_payment_order
            SET reconciliation_attempts = reconciliation_attempts + 1,
                last_reconciliation_result = 'unavailable',
                next_check_at = ?, version = version + 1, updated_at = ?
            WHERE id = ?
            """,
            timestamp(clock.instant().plus(Duration.ofMinutes(5))),
            timestamp(clock.instant()),
            order.id());
      }
    }
    return processed;
  }

  public CatalogSnapshot loadCatalog() {
    UUID catalogId =
        jdbc
            .query(
                """
                SELECT id FROM billing_catalog_version
                WHERE environment = ? AND version_code = ? AND status = 'active'
                """,
                (rs, row) -> rs.getObject(1, UUID.class),
                environment.wireValue(),
                config.catalogVersion())
            .stream()
            .findFirst()
            .orElseThrow(
                () ->
                    new BillingException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "BILLING_CATALOG_UNAVAILABLE",
                        "Billing catalog is unavailable."));
    List<PointPack> packs =
        jdbc.query(
            """
            SELECT sku, name_zh, name_en, price_minor, points, currency
            FROM billing_point_pack
            WHERE catalog_version_id = ? AND environment = ? AND status = 'enabled'
            ORDER BY sort_order
            """,
            (rs, row) ->
                new PointPack(
                    rs.getString("sku"),
                    rs.getString("name_zh"),
                    rs.getString("name_en"),
                    rs.getInt("price_minor"),
                    rs.getInt("points"),
                    rs.getString("currency"),
                    true),
            catalogId,
            environment.wireValue());
    Map<Feature, AiRate> rates = new EnumMap<>(Feature.class);
    jdbc.query(
        """
        SELECT feature, unit, points_per_unit, success_rule_code, description_zh, description_en
        FROM billing_ai_rate WHERE catalog_version_id = ?
        """,
        rs -> {
          Feature feature = Feature.from(rs.getString("feature"));
          rates.put(
              feature,
              new AiRate(
                  feature,
                  Unit.valueOf(rs.getString("unit").toUpperCase(Locale.ROOT)),
                  rs.getInt("points_per_unit"),
                  rs.getString("success_rule_code"),
                  rs.getString("description_zh"),
                  rs.getString("description_en")));
        },
        catalogId);
    return new CatalogSnapshot(catalogId, config.catalogVersion(), environment, packs, rates);
  }

  private void reserve(UUID ownerId, UUID taskId, int maxPoints, boolean autoCharge) {
    AccountRow account = lockAccount(ownerId);
    if (account.availablePoints() < maxPoints) {
      jdbc.update(
          "UPDATE billing_task SET status = 'INSUFFICIENT', version = version + 1, updated_at = ? WHERE id = ?",
          timestamp(clock.instant()),
          taskId);
      throw insufficient(maxPoints, account.availablePoints());
    }
    List<BatchRow> batches =
        jdbc.query(
            """
            SELECT id, remaining_points, reserved_points, source_type
            FROM billing_credit_batch
            WHERE owner_id = ? AND environment = ?
              AND status = 'active' AND remaining_points > reserved_points
            ORDER BY CASE source_type WHEN 'trial' THEN 0 ELSE 1 END, priority_at ASC
            FOR UPDATE
            """,
            (rs, row) ->
                new BatchRow(
                    rs.getObject("id", UUID.class),
                    rs.getInt("remaining_points"),
                    rs.getInt("reserved_points"),
                    rs.getString("source_type")),
            ownerId,
            environment.wireValue());
    int remaining = maxPoints;
    List<BatchAllocation> allocations = new ArrayList<>();
    for (BatchRow batch : batches) {
      if (remaining == 0) {
        break;
      }
      int amount = Math.min(remaining, batch.remainingPoints() - batch.reservedPoints());
      if (amount > 0) {
        allocations.add(new BatchAllocation(batch.id(), amount));
        remaining -= amount;
      }
    }
    if (remaining > 0) {
      jdbc.update(
          "UPDATE billing_task SET status = 'INSUFFICIENT', version = version + 1, updated_at = ? WHERE id = ?",
          timestamp(clock.instant()),
          taskId);
      throw insufficient(maxPoints, account.availablePoints());
    }
    UUID reservationId = UUID.randomUUID();
    Instant now = clock.instant();
    Instant deadline = now.plus(Duration.ofSeconds(config.taskTtlSeconds()));
    jdbc.update(
        """
        INSERT INTO billing_reservation
          (id, billing_task_id, owner_id, status, reserved_points, settled_points,
           released_points, deadline_at, version, created_at, updated_at)
        VALUES (?, ?, ?, 'reserved', ?, 0, 0, ?, 1, ?, ?)
        """,
        reservationId,
        taskId,
        ownerId,
        maxPoints,
        timestamp(deadline),
        timestamp(now),
        timestamp(now));
    int availableAfter = account.availablePoints() - maxPoints;
    int reservedAfter = account.reservedPoints() + maxPoints;
    jdbc.update(
        """
        UPDATE billing_credit_account
        SET available_points = ?, reserved_points = ?, version = version + 1, updated_at = ?
        WHERE owner_id = ?
        """,
        availableAfter,
        reservedAfter,
        timestamp(now),
        ownerId);
    for (BatchAllocation allocation : allocations) {
      jdbc.update(
          """
          INSERT INTO billing_reservation_allocation
            (id, reservation_id, batch_id, status, reserved_points, settled_points, released_points)
          VALUES (?, ?, ?, 'active', ?, 0, 0)
          """,
          UUID.randomUUID(),
          reservationId,
          allocation.batchId(),
          allocation.points());
      jdbc.update(
          """
          UPDATE billing_credit_batch
          SET reserved_points = reserved_points + ?, version = version + 1, updated_at = ?
          WHERE id = ?
          """,
          allocation.points(),
          timestamp(now),
          allocation.batchId());
      appendLedger(
          ownerId,
          allocation.batchId(),
          "batch",
          LedgerOperation.RESERVE,
          -allocation.points(),
          allocation.points(),
          0,
          availableAfter,
          reservedAfter,
          "ai_task",
          taskId.toString(),
          autoCharge ? "Auto-charge reserved" : "AI task reserved",
          now);
    }
    jdbc.update(
        """
        UPDATE billing_task
        SET status = 'RESERVED', auto_charge_applied = ?, expires_at = ?,
            version = version + 1, updated_at = ?
        WHERE id = ?
        """,
        autoCharge,
        timestamp(deadline),
        timestamp(now),
        taskId);
    if (autoCharge) {
      adjustDailyAutoUsage(ownerId, maxPoints, 0);
    }
  }

  private void releaseReservation(TaskRow task, String reason) {
    ReservationRow reservation = lockReservation(task.id(), task.ownerId());
    List<AllocationRow> allocations = lockAllocations(reservation.id());
    AccountRow account = lockAccount(task.ownerId());
    int released = reservation.reservedPoints() - reservation.settledPoints();
    int availableAfter = account.availablePoints() + released;
    int reservedAfter = account.reservedPoints() - released;
    Instant now = clock.instant();
    jdbc.update(
        """
        UPDATE billing_credit_account
        SET available_points = ?, reserved_points = ?, version = version + 1, updated_at = ?
        WHERE owner_id = ?
        """,
        availableAfter,
        reservedAfter,
        timestamp(now),
        task.ownerId());
    for (AllocationRow allocation : allocations) {
      int allocationRelease = allocation.reservedPoints() - allocation.settledPoints();
      if (allocationRelease <= 0) {
        continue;
      }
      jdbc.update(
          """
          UPDATE billing_credit_batch
          SET reserved_points = reserved_points - ?, version = version + 1, updated_at = ?
          WHERE id = ?
          """,
          allocation.reservedPoints(),
          timestamp(now),
          allocation.batchId());
      jdbc.update(
          """
          UPDATE billing_reservation_allocation
          SET status = 'released', released_points = ?
          WHERE id = ?
          """,
          allocationRelease,
          allocation.id());
      appendLedger(
          task.ownerId(),
          allocation.batchId(),
          "batch",
          LedgerOperation.RESERVE_RELEASE,
          allocationRelease,
          -allocationRelease,
          0,
          availableAfter,
          reservedAfter,
          "ai_task",
          task.id().toString(),
          reason,
          now);
    }
    jdbc.update(
        """
        UPDATE billing_reservation
        SET status = 'released', released_points = reserved_points,
            version = version + 1, updated_at = ?
        WHERE id = ?
        """,
        timestamp(now),
        reservation.id());
    jdbc.update(
        """
        UPDATE billing_task
        SET status = 'RELEASED', version = version + 1, updated_at = ?
        WHERE id = ?
        """,
        timestamp(now),
        task.id());
    if (task.autoChargeApplied()) {
      adjustDailyAutoUsage(task.ownerId(), -released, 0);
    }
  }

  private void applyPaid(
      PaymentOrderRow order, UUID eventId, NormalizedWaffoEvent event, Instant now) {
    if (order.status() == OrderStatus.PAID
        || order.status() == OrderStatus.REFUNDING
        || order.status() == OrderStatus.REFUNDED) {
      markEventProcessed(eventId, "ignored_duplicate", now);
      return;
    }
    OrderStatus current = order.status();
    if (current == OrderStatus.PENDING || current == OrderStatus.CLOSED) {
      BillingModel.transitionOrder(current, OrderStatus.CONFIRMING);
    }
    jdbc.update(
        """
        UPDATE billing_payment_order
        SET status = 'PAID', paid_at = ?, provider_order_id = coalesce(?, provider_order_id),
            provider_session_id = coalesce(?, provider_session_id),
            provider_product_id = coalesce(?, provider_product_id),
            version = version + 1, updated_at = ?
        WHERE id = ?
        """,
        timestamp(now),
        event.providerOrderId(),
        event.providerSessionId(),
        event.providerProductId(),
        timestamp(now),
        order.id());
    jdbc.update(
        """
        UPDATE billing_payment_order
        SET status = 'CLOSED', closed_at = ?, version = version + 1, updated_at = ?
        WHERE owner_id = ? AND environment = ? AND sku_snapshot = ? AND status = 'PENDING' AND id <> ?
        """,
        timestamp(now),
        timestamp(now),
        order.ownerId(),
        environment.wireValue(),
        order.sku(),
        order.id());
    UUID batchId = UUID.randomUUID();
    try {
      jdbc.update(
          """
          INSERT INTO billing_credit_batch
            (id, owner_id, source_type, order_id, environment, initial_points,
             remaining_points, reserved_points, status, priority_at, version, created_at, updated_at)
          VALUES (?, ?, 'purchase', ?, ?, ?, ?, 0, 'active', ?, 1, ?, ?)
          """,
          batchId,
          order.ownerId(),
          order.id(),
          environment.wireValue(),
          order.points(),
          order.points(),
          timestamp(now),
          timestamp(now),
          timestamp(now));
    } catch (DuplicateKeyException duplicate) {
      markEventProcessed(eventId, "ignored_duplicate", now);
      return;
    }
    AccountRow account = lockAccount(order.ownerId());
    int availableAfter = account.availablePoints() + order.points();
    int purchasedAfter = account.purchasedPoints() + order.points();
    jdbc.update(
        """
        UPDATE billing_credit_account
        SET available_points = ?, purchased_points = ?, lifetime_granted = lifetime_granted + ?,
            version = version + 1, updated_at = ?
        WHERE owner_id = ?
        """,
        availableAfter,
        purchasedAfter,
        order.points(),
        timestamp(now),
        order.ownerId());
    appendLedger(
        order.ownerId(),
        batchId,
        "batch",
        LedgerOperation.PURCHASE_GRANT,
        order.points(),
        0,
        0,
        availableAfter,
        account.reservedPoints(),
        "order",
        order.id().toString(),
        "Purchased points granted",
        now);
    markEventProcessed(eventId, "applied", now);
  }

  private void applyRefund(
      PaymentOrderRow order, UUID eventId, NormalizedWaffoEvent event, Instant now) {
    if (event.providerRefundId() == null || event.providerRefundId().isBlank()) {
      markEventProcessed(eventId, "manual_review", now);
      recordException(
          order.ownerId(),
          order.id(),
          null,
          null,
          "refund_review",
          Map.of("reason", "missing_refund_id"));
      return;
    }
    int expected = order.points();
    boolean fullRefund =
        event.refundAmountMinor() != null && event.refundAmountMinor() == order.amountMinor();
    UUID refundId = UUID.randomUUID();
    try {
      jdbc.update(
          """
          INSERT INTO billing_refund_case
            (id, order_id, environment, status, provider_refund_id, refund_amount_minor,
             expected_recovery_points, recovered_points, unrecovered_points, reason_code,
             review_due_at, version, created_at)
          VALUES (?, ?, ?, ?, ?, ?, ?, 0, 0, ?, ?, 1, ?)
          """,
          refundId,
          order.id(),
          environment.wireValue(),
          fullRefund ? "recovering" : "manual_review",
          event.providerRefundId(),
          event.refundAmountMinor() == null ? 0 : event.refundAmountMinor(),
          expected,
          event.reasonCode(),
          timestamp(now.plus(Duration.ofDays(3))),
          timestamp(now));
    } catch (DuplicateKeyException duplicate) {
      markEventProcessed(eventId, "ignored_duplicate", now);
      return;
    }
    jdbc.update(
        "UPDATE billing_payment_order SET status = 'REFUNDING', version = version + 1, updated_at = ? WHERE id = ?",
        timestamp(now),
        order.id());
    if (fullRefund) {
      autoRecoverFullRefund(order, refundId, now);
    } else {
      recordException(
          order.ownerId(),
          order.id(),
          null,
          refundId,
          "refund_review",
          Map.of("providerRefundId", event.providerRefundId()));
    }
    markEventProcessed(eventId, "applied", now);
  }

  private void autoRecoverFullRefund(PaymentOrderRow order, UUID refundId, Instant now) {
    List<BatchRow> batches =
        jdbc.query(
            """
            SELECT id, remaining_points, reserved_points, source_type
            FROM billing_credit_batch WHERE order_id = ? FOR UPDATE
            """,
            (rs, row) ->
                new BatchRow(
                    rs.getObject("id", UUID.class),
                    rs.getInt("remaining_points"),
                    rs.getInt("reserved_points"),
                    rs.getString("source_type")),
            order.id());
    if (batches.isEmpty()) {
      jdbc.update(
          "UPDATE billing_refund_case SET status = 'manual_review', unrecovered_points = expected_recovery_points WHERE id = ?",
          refundId);
      return;
    }
    BatchRow batch = batches.get(0);
    int recoverable = Math.max(0, batch.remainingPoints() - batch.reservedPoints());
    recoverRefundPoints(order.ownerId(), order.id(), refundId, recoverable, now);
    jdbc.update(
        "UPDATE billing_credit_batch SET status = 'frozen', version = version + 1, updated_at = ? WHERE id = ?",
        timestamp(now),
        batch.id());
    int unrecovered = order.points() - recoverable;
    String status = unrecovered == 0 ? "recovered" : "partially_recovered";
    jdbc.update(
        """
        UPDATE billing_refund_case
        SET status = ?, recovered_points = ?, unrecovered_points = ?,
            resolved_at = ?, review_owner = coalesce(review_owner, 'system-auto'),
            resolution_note_safe = ?, version = version + 1
        WHERE id = ?
        """,
        status,
        recoverable,
        unrecovered,
        unrecovered == 0 ? timestamp(now) : null,
        unrecovered == 0
            ? "Full refund recovered from unused order points."
            : "Consumed or reserved points require manual review.",
        refundId);
    jdbc.update(
        "UPDATE billing_payment_order SET status = ?, refunded_at = ?, version = version + 1, updated_at = ? WHERE id = ?",
        unrecovered == 0 ? "REFUNDED" : "EXCEPTION",
        timestamp(now),
        timestamp(now),
        order.id());
  }

  private int recoverRefundPoints(
      UUID ownerId, UUID orderId, UUID refundId, int requested, Instant now) {
    if (requested <= 0) {
      return 0;
    }
    List<BatchRow> batches =
        jdbc.query(
            "SELECT id, remaining_points, reserved_points, source_type FROM billing_credit_batch WHERE order_id = ? FOR UPDATE",
            (rs, row) ->
                new BatchRow(
                    rs.getObject("id", UUID.class),
                    rs.getInt("remaining_points"),
                    rs.getInt("reserved_points"),
                    rs.getString("source_type")),
            orderId);
    AccountRow account = lockAccount(ownerId);
    int remaining = requested;
    int availableAfter = account.availablePoints();
    int purchasedRecovered = 0;
    for (BatchRow batch : batches) {
      int recoverable =
          Math.min(remaining, Math.max(0, batch.remainingPoints() - batch.reservedPoints()));
      if (recoverable <= 0) {
        continue;
      }
      availableAfter -= recoverable;
      purchasedRecovered += recoverable;
      jdbc.update(
          """
          UPDATE billing_credit_batch
          SET remaining_points = remaining_points - ?, version = version + 1, updated_at = ?
          WHERE id = ?
          """,
          recoverable,
          timestamp(now),
          batch.id());
      jdbc.update(
          """
          INSERT INTO billing_refund_recovery
            (id, refund_case_id, batch_id, points, status, created_at, recovered_at)
          VALUES (?, ?, ?, ?, 'recovered', ?, ?)
          """,
          UUID.randomUUID(),
          refundId,
          batch.id(),
          recoverable,
          timestamp(now),
          timestamp(now));
      appendLedger(
          ownerId,
          batch.id(),
          "batch",
          LedgerOperation.REFUND_RECOVER,
          -recoverable,
          0,
          0,
          availableAfter,
          account.reservedPoints(),
          "refund",
          refundId.toString(),
          "Refund recovered unused points",
          now);
      remaining -= recoverable;
    }
    jdbc.update(
        """
        UPDATE billing_credit_account
        SET available_points = ?, purchased_points = purchased_points - ?,
            version = version + 1, updated_at = ?
        WHERE owner_id = ?
        """,
        availableAfter,
        purchasedRecovered,
        timestamp(now),
        ownerId);
    return requested - remaining;
  }

  private BillingTaskResponse taskResponse(TaskRow task) {
    List<QuoteLineRow> lines = quoteLines(task.id());
    Integer reservedValue =
        jdbc.queryForObject(
            "SELECT coalesce(sum(reserved_points - settled_points),0) FROM billing_reservation_allocation WHERE reservation_id = (SELECT id FROM billing_reservation WHERE billing_task_id = ?)",
            Integer.class,
            task.id());
    int reserved = reservedValue == null ? 0 : reservedValue;
    return new BillingTaskResponse(
        task.id(),
        task.status().name(),
        lines.stream().map(QuoteLineRow::response).toList(),
        task.maxPoints(),
        task.settledPoints(),
        Math.max(0, reserved),
        task.expiresAt(),
        task.autoChargeApplied(),
        task.createdAt(),
        task.updatedAt());
  }

  private void appendLedger(
      UUID ownerId,
      UUID batchId,
      String scope,
      LedgerOperation operation,
      int availableDelta,
      int reservedDelta,
      int consumedDelta,
      int availableAfter,
      int reservedAfter,
      String referenceType,
      String referenceId,
      String description,
      Instant now) {
    jdbc.update(
        """
        INSERT INTO billing_ledger_entry
          (owner_id, batch_id, entry_scope, operation_type, available_delta, reserved_delta,
           consumed_delta, available_after, reserved_after, reference_type, reference_id,
           environment, metadata_safe, description_safe, created_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, '{}'::jsonb, ?, ?)
        """,
        ownerId,
        batchId,
        scope,
        operation.wireValue(),
        availableDelta,
        reservedDelta,
        consumedDelta,
        availableAfter,
        reservedAfter,
        referenceType,
        referenceId,
        environment.wireValue(),
        description,
        timestamp(now));
  }

  private void recordException(
      UUID ownerId,
      UUID orderId,
      UUID taskId,
      UUID refundId,
      String category,
      Map<String, Object> details) {
    Instant now = clock.instant();
    jdbc.update(
        """
        INSERT INTO billing_exception
          (id, owner_id, order_id, billing_task_id, refund_case_id, category, status,
           safe_details, due_at, created_at)
        VALUES (?, ?, ?, ?, ?, ?, 'open', ?::jsonb, ?, ?)
        """,
        UUID.randomUUID(),
        ownerId,
        orderId,
        taskId,
        refundId,
        category,
        json(details),
        timestamp(now.plus(Duration.ofDays(3))),
        timestamp(now));
  }

  private void markEventProcessed(UUID eventId, String status, Instant now) {
    jdbc.update(
        "UPDATE billing_payment_event SET processing_status = ?, processed_at = ? WHERE id = ?",
        status,
        timestamp(now),
        eventId);
  }

  static boolean validPaymentFacts(
      NormalizedWaffoEvent event, PaymentOrderRow order, String source) {
    boolean signatureAccepted =
        ("webhook".equals(source) && "valid".equals(event.signatureStatus()))
            || ("query".equals(source) && "not_applicable".equals(event.signatureStatus()));
    if (!signatureAccepted) {
      return false;
    }
    if (!event.productMatch()) {
      return false;
    }
    if (!"paid".equals(event.eventType()) && !"refund".equals(event.eventType())) {
      return true;
    }
    if ("paid".equals(event.eventType())) {
      return event.amountMinor() != null
          && event.amountMinor() == order.amountMinor()
          && order.currency().equalsIgnoreCase(event.currency())
          && event.providerProductId() != null
          && order.providerProductId() != null
          && event.providerProductId().equals(order.providerProductId());
    }
    return event.refundAmountMinor() != null
        && event.refundAmountMinor() > 0
        && event.refundAmountMinor() <= order.amountMinor()
        && order.currency().equalsIgnoreCase(event.currency())
        && event.providerProductId() != null
        && order.providerProductId() != null
        && event.providerProductId().equals(order.providerProductId())
        && Set.of(OrderStatus.PAID, OrderStatus.REFUNDING, OrderStatus.REFUNDED)
            .contains(order.status());
  }

  private void requireBillingEnabled() {
    if (!config.enabled()) {
      throw new BillingException(
          HttpStatus.SERVICE_UNAVAILABLE, "BILLING_DISABLED", "Billing is currently disabled.");
    }
  }

  private void requireAdmin(Authentication authentication) {
    OwnerAccountEntity owner = owners.require(authentication);
    Set<String> admins =
        Set.of(config.adminEmails() == null ? new String[0] : config.adminEmails().split(","))
            .stream()
            .map(String::strip)
            .filter(value -> !value.isBlank())
            .map(value -> value.toLowerCase(Locale.ROOT))
            .collect(java.util.stream.Collectors.toUnmodifiableSet());
    if (!admins.contains(owner.getEmailNormalized())) {
      throw new BillingException(
          HttpStatus.FORBIDDEN, "ADMIN_REQUIRED", "Administrator access is required.");
    }
  }

  private CreditAccountResponse account(UUID ownerId) {
    return findAccount(ownerId)
        .orElseThrow(() -> new IllegalStateException("Credit account is unavailable."));
  }

  private Optional<CreditAccountResponse> findAccount(UUID ownerId) {
    return jdbc
        .query(
            """
            SELECT available_points, reserved_points, trial_points, purchased_points, updated_at
            FROM billing_credit_account WHERE owner_id = ?
            """,
            (rs, row) ->
                new CreditAccountResponse(
                    rs.getInt("available_points"),
                    rs.getInt("reserved_points"),
                    rs.getInt("trial_points"),
                    rs.getInt("purchased_points"),
                    instant(rs, "updated_at")),
            ownerId)
        .stream()
        .findFirst();
  }

  private AccountRow lockAccount(UUID ownerId) {
    return jdbc
        .query(
            """
            SELECT available_points, reserved_points, trial_points, purchased_points
            FROM billing_credit_account WHERE owner_id = ? FOR UPDATE
            """,
            (rs, row) ->
                new AccountRow(
                    rs.getInt("available_points"),
                    rs.getInt("reserved_points"),
                    rs.getInt("trial_points"),
                    rs.getInt("purchased_points")),
            ownerId)
        .stream()
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("Credit account is unavailable."));
  }

  private PaymentOrderRow lockOrder(UUID orderId, UUID ownerId) {
    return jdbc
        .query(
            "SELECT * FROM billing_payment_order WHERE id = ? AND owner_id = ? FOR UPDATE",
            (rs, row) -> orderRow(rs),
            orderId,
            ownerId)
        .stream()
        .findFirst()
        .orElseThrow(
            () ->
                new BillingException(
                    HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "Order was not found."));
  }

  private TaskRow lockTask(UUID taskId, UUID ownerId) {
    return jdbc
        .query(
            "SELECT * FROM billing_task WHERE id = ? AND owner_id = ? FOR UPDATE",
            (rs, row) -> taskRow(rs),
            taskId,
            ownerId)
        .stream()
        .findFirst()
        .orElseThrow(
            () ->
                new BillingException(
                    HttpStatus.NOT_FOUND, "BILLING_TASK_NOT_FOUND", "Billing task was not found."));
  }

  private ReservationRow lockReservation(UUID taskId, UUID ownerId) {
    return jdbc
        .query(
            "SELECT * FROM billing_reservation WHERE billing_task_id = ? AND owner_id = ? FOR UPDATE",
            (rs, row) ->
                new ReservationRow(
                    rs.getObject("id", UUID.class),
                    rs.getInt("reserved_points"),
                    rs.getInt("settled_points"),
                    rs.getInt("released_points")),
            taskId,
            ownerId)
        .stream()
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("Reservation is unavailable."));
  }

  private List<AllocationRow> lockAllocations(UUID reservationId) {
    return jdbc.query(
        """
        SELECT a.id, a.batch_id, b.source_type, a.reserved_points,
               a.settled_points, a.released_points
        FROM billing_reservation_allocation a
        JOIN billing_credit_batch b ON b.id = a.batch_id
        WHERE a.reservation_id = ?
        ORDER BY id FOR UPDATE
        """,
        (rs, row) ->
            new AllocationRow(
                rs.getObject("id", UUID.class),
                rs.getObject("batch_id", UUID.class),
                rs.getString("source_type"),
                rs.getInt("reserved_points"),
                rs.getInt("settled_points"),
                rs.getInt("released_points")),
        reservationId);
  }

  private List<QuoteLineRow> quoteLines(UUID taskId) {
    return jdbc.query(
        """
        SELECT id, feature, unit, quantity, unit_points, max_line_points,
               success_rule_code, success_state, settled_points
        FROM billing_quote_line WHERE billing_task_id = ? ORDER BY id
        """,
        (rs, row) ->
            new QuoteLineRow(
                rs.getObject("id", UUID.class),
                Feature.from(rs.getString("feature")),
                Unit.valueOf(rs.getString("unit").toUpperCase(Locale.ROOT)),
                rs.getInt("quantity"),
                rs.getInt("unit_points"),
                rs.getInt("max_line_points"),
                rs.getString("success_rule_code"),
                rs.getString("success_state"),
                rs.getInt("settled_points")),
        taskId);
  }

  private UUID loadPackId(UUID catalogId, String sku) {
    return jdbc.queryForObject(
        "SELECT id FROM billing_point_pack WHERE catalog_version_id = ? AND sku = ?",
        UUID.class,
        catalogId,
        sku);
  }

  private Optional<PaymentOrderResponse> findPendingOrder(UUID ownerId, String sku) {
    return jdbc
        .query(
            """
            SELECT * FROM billing_payment_order
            WHERE owner_id = ? AND environment = ? AND sku_snapshot = ? AND status = 'PENDING'
            ORDER BY created_at DESC LIMIT 1
            """,
            (rs, row) -> order(rs),
            ownerId,
            environment.wireValue(),
            sku)
        .stream()
        .findFirst();
  }

  private void ensureAutoPolicy(UUID ownerId) {
    Integer count =
        jdbc.queryForObject(
            "SELECT count(*) FROM billing_auto_charge_policy WHERE owner_id = ? AND revoked_at IS NULL",
            Integer.class,
            ownerId);
    if (count != null && count > 0) {
      return;
    }
    jdbc.update(
        """
        INSERT INTO billing_auto_charge_policy
          (id, owner_id, feature_scope, enabled, max_points_per_task, max_points_per_day,
           timezone, consent_text_version, effective_at, version)
        VALUES (?, ?, 'classify', FALSE, 15, 100, 'Asia/Shanghai', 'billing-auto-v1', ?, 1)
        """,
        UUID.randomUUID(),
        ownerId,
        timestamp(clock.instant()));
  }

  private boolean autoChargeEligible(UUID ownerId, int maxPoints) {
    ensureAutoPolicy(ownerId);
    Map<String, Object> policy =
        jdbc.queryForMap(
            """
            SELECT enabled, max_points_per_task, max_points_per_day, timezone
            FROM billing_auto_charge_policy
            WHERE owner_id = ? AND feature_scope = 'classify' AND revoked_at IS NULL
            """,
            ownerId);
    if (!Boolean.TRUE.equals(policy.get("enabled"))
        || maxPoints > ((Number) policy.get("max_points_per_task")).intValue()) {
      return false;
    }
    LocalDate usageDate =
        ZonedDateTime.now(clock.withZone(ZoneId.of(policy.get("timezone").toString())))
            .toLocalDate();
    int used =
        jdbc
            .query(
                "SELECT auto_settled_points + auto_reserved_points FROM billing_daily_auto_usage WHERE owner_id = ? AND usage_date = ?",
                (rs, row) -> rs.getInt(1),
                ownerId,
                usageDate)
            .stream()
            .findFirst()
            .orElse(0);
    return used + maxPoints <= ((Number) policy.get("max_points_per_day")).intValue();
  }

  private void adjustDailyAutoUsage(UUID ownerId, int reservedDelta, int settledDelta) {
    String timezone =
        jdbc.queryForObject(
            "SELECT timezone FROM billing_auto_charge_policy WHERE owner_id = ? AND revoked_at IS NULL",
            String.class,
            ownerId);
    LocalDate date = ZonedDateTime.now(clock.withZone(ZoneId.of(timezone))).toLocalDate();
    jdbc.update(
        """
        INSERT INTO billing_daily_auto_usage
          (id, owner_id, usage_date, auto_reserved_points, auto_settled_points, updated_at)
        VALUES (?, ?, ?, greatest(0, ?), ?, ?)
        ON CONFLICT (owner_id, usage_date)
        DO UPDATE SET auto_reserved_points = greatest(
                          0,
                          billing_daily_auto_usage.auto_reserved_points
                            + EXCLUDED.auto_reserved_points
                        ),
                      auto_settled_points = billing_daily_auto_usage.auto_settled_points
                        + EXCLUDED.auto_settled_points,
                      updated_at = EXCLUDED.updated_at
        """,
        UUID.randomUUID(),
        ownerId,
        date,
        reservedDelta,
        settledDelta,
        timestamp(clock.instant()));
  }

  private Map<String, Integer> countMap(
      String column, String table, String environmentValue, Instant start, Instant end) {
    Map<String, Integer> result = new LinkedHashMap<>();
    jdbc.query(
        "SELECT "
            + column
            + " AS key, count(*) AS total FROM "
            + table
            + " WHERE environment = ? AND created_at >= ? AND created_at < ? GROUP BY "
            + column,
        rs -> {
          result.put(rs.getString("key"), rs.getInt("total"));
        },
        environmentValue,
        timestamp(start),
        timestamp(end));
    return result;
  }

  private void recoverRefundPoints(
      UUID ownerId, UUID orderId, UUID refundId, int points, Instant now, boolean force) {
    recoverRefundPoints(ownerId, orderId, refundId, points, now);
  }

  private BillingException insufficient(int required, int available) {
    return new BillingException(
        HttpStatus.PAYMENT_REQUIRED,
        "INSUFFICIENT_POINTS",
        "Insufficient points for this AI operation.",
        Map.of(
            "requiredPoints", required,
            "availablePoints", available,
            "shortfallPoints", Math.max(0, required - available)));
  }

  static Map<Feature, Integer> operationRequirements(
      String operation,
      Integer preflightViewCount,
      Integer preflightCharacters,
      int uniqueImageCount) {
    Map<Feature, Integer> requirements = new EnumMap<>(Feature.class);
    String normalizedOperation = operation == null ? "" : operation.strip();
    switch (normalizedOperation) {
      case "manual_auto_classify", "internet_import" -> requirements.put(Feature.CLASSIFY, 1);
      case "text_import", "reanalyze" -> requirements.put(Feature.AI_TEXT, 1);
      case "webpage_import" -> {
        requirements.put(Feature.WEBPAGE_VISION, requirePositive(preflightViewCount));
        requirements.put(Feature.CLASSIFY, 1);
      }
      case "document_import" -> {
        requirements.put(
            Feature.DOCUMENT_EXTRACT,
            BillingModel.documentQuantity(requireNonNegative(preflightCharacters)));
        requirements.put(Feature.CLASSIFY, 1);
      }
      case "screenshot_import" ->
          requirements.put(Feature.SCREENSHOT_OCR, Math.max(1, uniqueImageCount));
      case "internet_search" -> requirements.put(Feature.INTERNET_SEARCH, 1);
      case "internet_search_with_answer" -> {
        requirements.put(Feature.INTERNET_SEARCH, 1);
        requirements.put(Feature.AI_ANSWER, 1);
      }
      default ->
          throw new BillingException(
              HttpStatus.UNPROCESSABLE_ENTITY,
              "OPERATION_INVALID",
              "Unsupported billing operation.");
    }
    if (Set.of("webpage_import", "document_import", "screenshot_import", "reanalyze")
            .contains(normalizedOperation)
        && uniqueImageCount > 0) {
      requirements.put(Feature.SCREENSHOT_OCR, uniqueImageCount);
    }
    if ("screenshot_import".equals(normalizedOperation)) {
      requirements.put(Feature.CLASSIFY, 1);
    }
    return Map.copyOf(requirements);
  }

  static void validateRequestedOperationLines(
      String operation,
      List<QuoteLineRequest> lines,
      Integer preflightViewCount,
      Integer preflightCharacters,
      int uniqueImageCount) {
    Map<Feature, Integer> requirements =
        operationRequirements(operation, preflightViewCount, preflightCharacters, uniqueImageCount);
    if (uniqueImageCount > 0 && !requirements.containsKey(Feature.SCREENSHOT_OCR)) {
      throw invalidQuoteLines();
    }
    if (lines == null || lines.size() != requirements.size()) {
      throw invalidQuoteLines();
    }
    Set<Feature> features = new LinkedHashSet<>();
    for (QuoteLineRequest line : lines) {
      Feature feature;
      try {
        feature = Feature.from(line.feature());
      } catch (RuntimeException error) {
        throw invalidQuoteLines();
      }
      Integer requiredQuantity = requirements.get(feature);
      if (!features.add(feature)
          || requiredQuantity == null
          || line.quantity() != requiredQuantity) {
        throw invalidQuoteLines();
      }
    }
    if (!features.equals(requirements.keySet())) {
      throw invalidQuoteLines();
    }
  }

  private List<ResolvedLine> resolveLines(
      CatalogSnapshot catalog,
      String operation,
      Integer preflightViewCount,
      Integer preflightCharacters,
      int uniqueImageCount) {
    List<ResolvedLine> result = new ArrayList<>();
    Map<Feature, Integer> requirements =
        operationRequirements(operation, preflightViewCount, preflightCharacters, uniqueImageCount);
    for (var requirement : requirements.entrySet()) {
      Feature feature = requirement.getKey();
      AiRate rate = catalog.rates().get(feature);
      if (rate == null) {
        throw new BillingException(
            HttpStatus.UNPROCESSABLE_ENTITY, "RATE_UNAVAILABLE", "Billing rate is unavailable.");
      }
      int quantity = requirement.getValue();
      result.add(
          new ResolvedLine(
              new QuoteLine(
                  feature,
                  rate.unit(),
                  quantity,
                  rate.pointsPerUnit(),
                  BillingModel.quotePoints(rate, quantity)),
              rate.successRule()));
    }
    return result;
  }

  private List<AssetRow> resolveUniqueAssets(UUID ownerId, List<UUID> assetIds) {
    if (assetIds == null || assetIds.isEmpty()) {
      return List.of();
    }
    List<AssetRow> resolved = new ArrayList<>();
    for (UUID assetId : assetIds) {
      resolved.add(resolveAsset(ownerId, assetId));
    }
    return uniqueAssets(resolved);
  }

  static List<AssetRow> uniqueAssets(List<AssetRow> assets) {
    Map<String, AssetRow> unique = new LinkedHashMap<>();
    for (AssetRow asset : assets) {
      unique.putIfAbsent(asset.contentHash(), asset);
    }
    return List.copyOf(unique.values());
  }

  private List<QuoteImageResourceResponse> insertImageResources(
      UUID taskId, List<AssetRow> assets) {
    List<QuoteImageResourceResponse> result = new ArrayList<>();
    for (AssetRow asset : assets) {
      result.add(
          new QuoteImageResourceResponse(
              asset.id(), asset.contentHash(), asset.mediaType(), asset.sizeBytes()));
      jdbc.update(
          """
          INSERT INTO billing_quote_image_resource
            (id, billing_task_id, asset_id, content_hash, media_type,
             normalized_size_bytes, counted_quantity, created_at)
          VALUES (?, ?, ?, ?, ?, ?, 1, ?)
          """,
          UUID.randomUUID(),
          taskId,
          asset.id(),
          asset.contentHash(),
          asset.mediaType(),
          asset.sizeBytes(),
          timestamp(clock.instant()));
    }
    return List.copyOf(result);
  }

  PreflightRow requirePreflight(UUID ownerId, UUID preflightId, String expectedKind) {
    if (preflightId == null) {
      throw invalidPreflight();
    }
    PreflightRow row =
        jdbc
            .query(
                """
                SELECT owner_id, environment, kind, measured_characters_upper_bound,
                       view_count, expires_at
                FROM billing_preflight WHERE id = ?
                """,
                (rs, index) ->
                    new PreflightRow(
                        rs.getObject("owner_id", UUID.class),
                        rs.getString("environment"),
                        rs.getString("kind"),
                        rs.getInt("measured_characters_upper_bound"),
                        rs.getInt("view_count"),
                        instant(rs, "expires_at")),
                preflightId)
            .stream()
            .findFirst()
            .orElseThrow(BillingService::invalidPreflight);
    if (!ownerId.equals(row.ownerId())
        || !environment.wireValue().equals(row.environment())
        || !expectedKind.equals(row.kind())
        || !clock.instant().isBefore(row.expiresAt())) {
      throw invalidPreflight();
    }
    return row;
  }

  private AssetRow resolveAsset(UUID ownerId, UUID assetId) {
    List<AssetRow> uploads =
        jdbc.query(
            """
            SELECT id, content_hash, media_type, size_bytes
            FROM rich_image_upload
            WHERE id = ? AND owner_id = ? AND status IN ('ready','attached')
              AND scan_state = 'clean' AND redaction_state IN ('none','redacted')
            """,
            (rs, row) ->
                new AssetRow(
                    rs.getObject("id", UUID.class),
                    rs.getString("content_hash"),
                    rs.getString("media_type"),
                    rs.getLong("size_bytes")),
            assetId,
            ownerId);
    if (!uploads.isEmpty()) {
      return uploads.get(0);
    }
    return jdbc
        .query(
            """
            SELECT id, sha256 AS content_hash, media_type, size_bytes
            FROM attachment
            WHERE id = ? AND owner_id = ? AND scan_state = 'clean'
              AND redaction_state IN ('none','redacted','not_required')
            """,
            (rs, row) ->
                new AssetRow(
                    rs.getObject("id", UUID.class),
                    rs.getString("content_hash"),
                    rs.getString("media_type"),
                    rs.getLong("size_bytes")),
            assetId,
            ownerId)
        .stream()
        .findFirst()
        .orElseThrow(
            () ->
                new BillingException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "BILLING_IMAGE_ASSET_INVALID",
                    "Billing image asset is unavailable or unsafe."));
  }

  private QuoteLineResponse quoteResponse(QuoteLine line, String successRule) {
    return new QuoteLineResponse(
        line.feature().wireValue(),
        line.unit().wireValue(),
        line.quantity(),
        line.unitPoints(),
        line.maxPoints(),
        successRule);
  }

  private PaymentOrderResponse order(ResultSet rs) throws SQLException {
    return new PaymentOrderResponse(
        rs.getObject("id", UUID.class),
        rs.getString("sku_snapshot"),
        rs.getString("environment"),
        rs.getInt("amount_minor"),
        rs.getString("currency"),
        rs.getInt("points_snapshot"),
        rs.getString("provider_product_id"),
        rs.getString("status"),
        rs.getString("checkout_url"),
        instant(rs, "paid_at"),
        instant(rs, "closed_at"),
        instant(rs, "created_at"),
        instant(rs, "updated_at"));
  }

  private PaymentOrderRow orderRow(ResultSet rs) throws SQLException {
    return new PaymentOrderRow(
        rs.getObject("id", UUID.class),
        rs.getObject("owner_id", UUID.class),
        rs.getString("sku_snapshot"),
        rs.getInt("amount_minor"),
        rs.getString("currency"),
        rs.getInt("points_snapshot"),
        OrderStatus.valueOf(rs.getString("status")),
        rs.getString("provider_product_id"),
        rs.getString("provider_session_id"));
  }

  private TaskRow taskRow(ResultSet rs) throws SQLException {
    return new TaskRow(
        rs.getObject("id", UUID.class),
        rs.getObject("owner_id", UUID.class),
        rs.getString("operation"),
        TaskStatus.valueOf(rs.getString("status")),
        rs.getInt("max_points"),
        rs.getInt("settled_points"),
        rs.getBoolean("auto_charge_applied"),
        instant(rs, "expires_at"),
        instant(rs, "created_at"),
        instant(rs, "updated_at"));
  }

  private RefundCaseResponse refund(ResultSet rs) throws SQLException {
    return new RefundCaseResponse(
        rs.getObject("id", UUID.class),
        rs.getObject("order_id", UUID.class),
        rs.getString("status"),
        rs.getInt("refund_amount_minor"),
        rs.getInt("expected_recovery_points"),
        rs.getInt("recovered_points"),
        rs.getInt("unrecovered_points"),
        instant(rs, "review_due_at"),
        rs.getInt("version"),
        rs.getString("resolution_note_safe"),
        instant(rs, "created_at"),
        instant(rs, "resolved_at"));
  }

  private RefundCaseResponse refund(UUID refundId) {
    return jdbc
        .query("SELECT * FROM billing_refund_case WHERE id = ?", (rs, row) -> refund(rs), refundId)
        .stream()
        .findFirst()
        .orElseThrow(
            () ->
                new BillingException(
                    HttpStatus.NOT_FOUND, "REFUND_NOT_FOUND", "Refund case was not found."));
  }

  private static String signatureStatus(String value) {
    return value != null && Set.of("valid", "invalid", "not_applicable").contains(value)
        ? value
        : "invalid";
  }

  private static String requiredPreflightKind(String operation) {
    return switch (operation) {
      case "webpage_import" -> "webpage";
      case "document_import" -> "document";
      case "screenshot_import" -> "screenshot";
      default -> null;
    };
  }

  private static int requirePositive(Integer value) {
    if (value == null || value < 1) {
      throw invalidPreflight();
    }
    return value;
  }

  private static int requireNonNegative(Integer value) {
    if (value == null || value < 0) {
      throw invalidPreflight();
    }
    return value;
  }

  private static BillingException invalidQuoteLines() {
    return new BillingException(
        HttpStatus.UNPROCESSABLE_ENTITY,
        "QUOTE_LINES_INVALID",
        "Quote lines do not match the selected billing operation.");
  }

  private static BillingException invalidPreflight() {
    return new BillingException(
        HttpStatus.UNPROCESSABLE_ENTITY,
        "PREFLIGHT_INVALID",
        "The preflight is unavailable for this account and operation.");
  }

  private void validateConfigurationUsesBilling() {
    // Kept as a separate helper so startup validation remains deterministic for tests.
  }

  private long parseCursor(String cursor) {
    if (cursor == null || cursor.isBlank()) {
      return 0;
    }
    try {
      return Long.parseLong(cursor);
    } catch (NumberFormatException error) {
      throw new BillingException(
          HttpStatus.BAD_REQUEST, "CURSOR_INVALID", "Ledger cursor is invalid.");
    }
  }

  private UUID parseUuid(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return UUID.fromString(value);
    } catch (IllegalArgumentException error) {
      return null;
    }
  }

  private String json(Object value) {
    try {
      return mapper.writeValueAsString(value);
    } catch (JsonProcessingException error) {
      throw new IllegalStateException("Billing metadata could not be serialized.", error);
    }
  }

  private static long number(Long value) {
    return value == null ? 0 : value;
  }

  private static Timestamp timestamp(Instant value) {
    return value == null ? null : Timestamp.from(value);
  }

  private static Instant instant(ResultSet rs, String column) throws SQLException {
    Timestamp value = rs.getTimestamp(column);
    return value == null ? null : value.toInstant();
  }

  private static String nullToEmpty(String value) {
    return value == null ? "" : value;
  }

  private record ResolvedLine(QuoteLine quoteLine, String successRule) {}

  private record BatchAllocation(UUID batchId, int points) {}

  private record AccountRow(
      int availablePoints, int reservedPoints, int trialPoints, int purchasedPoints) {}

  private record BatchRow(UUID id, int remainingPoints, int reservedPoints, String sourceType) {}

  private record AllocationRow(
      UUID id,
      UUID batchId,
      String sourceType,
      int reservedPoints,
      int settledPoints,
      int releasedPoints) {}

  private record ReservationRow(
      UUID id, int reservedPoints, int settledPoints, int releasedPoints) {}

  private record TaskRow(
      UUID id,
      UUID ownerId,
      String operation,
      TaskStatus status,
      int maxPoints,
      int settledPoints,
      boolean autoChargeApplied,
      Instant expiresAt,
      Instant createdAt,
      Instant updatedAt) {}

  private record QuoteLineRow(
      UUID id,
      Feature feature,
      Unit unit,
      int quantity,
      int unitPoints,
      int maxPoints,
      String successRule,
      String successState,
      int settledPoints) {
    QuoteLineResponse response() {
      return new QuoteLineResponse(
          feature.wireValue(), unit.wireValue(), quantity, unitPoints, maxPoints, successRule);
    }
  }

  record PaymentOrderRow(
      UUID id,
      UUID ownerId,
      String sku,
      int amountMinor,
      String currency,
      int points,
      OrderStatus status,
      String providerProductId,
      String providerSessionId) {}

  record PreflightRow(
      UUID ownerId,
      String environment,
      String kind,
      int charactersUpperBound,
      int viewCount,
      Instant expiresAt) {}

  record AssetRow(UUID id, String contentHash, String mediaType, long sizeBytes) {}
}
