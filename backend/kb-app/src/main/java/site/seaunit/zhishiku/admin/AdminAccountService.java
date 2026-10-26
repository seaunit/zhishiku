package site.seaunit.zhishiku.admin;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.seaunit.zhishiku.admin.AdminAccountDtos.AdminUser;
import site.seaunit.zhishiku.admin.AdminAccountDtos.AdminUserPage;
import site.seaunit.zhishiku.admin.AdminAccountDtos.GrantPointsRequest;
import site.seaunit.zhishiku.admin.AdminAccountDtos.UpdateAccountStatusRequest;
import site.seaunit.zhishiku.billing.BillingDtos.ManualAdjustmentRequest;
import site.seaunit.zhishiku.billing.BillingService;
import site.seaunit.zhishiku.idempotency.IdempotencyService;
import site.seaunit.zhishiku.owner.OwnerAccountEntity;
import site.seaunit.zhishiku.owner.OwnerAccountRepository;
import site.seaunit.zhishiku.webimport.AiTextSecretRedactor;

@Service
public class AdminAccountService {
  private static final int MAX_LIMIT = 100;
  private static final String STATUS_OPERATION = "admin-account-status";
  private static final String GRANT_OPERATION = "admin-points-grant";

  private final JdbcTemplate jdbc;
  private final OwnerAccountRepository accounts;
  private final AdminAuthorizationService authorization;
  private final AdminAuditRepository audit;
  private final IdempotencyService idempotency;
  private final BillingService billing;
  private final AiTextSecretRedactor secretRedactor;

  public AdminAccountService(
      JdbcTemplate jdbc,
      OwnerAccountRepository accounts,
      AdminAuthorizationService authorization,
      AdminAuditRepository audit,
      IdempotencyService idempotency,
      BillingService billing,
      AiTextSecretRedactor secretRedactor) {
    this.jdbc = jdbc;
    this.accounts = accounts;
    this.authorization = authorization;
    this.audit = audit;
    this.idempotency = idempotency;
    this.billing = billing;
    this.secretRedactor = secretRedactor;
  }

  @Transactional(readOnly = true)
  public AdminUserPage list(
      Authentication authentication, String query, int page, int limit, String status) {
    authorization.require(authentication);
    int normalizedPage = Math.max(page, 1);
    int normalizedLimit = Math.min(Math.max(limit, 1), MAX_LIMIT);
    String normalizedQuery = query == null ? "" : query.strip();
    String normalizedStatus =
        "enabled".equals(status) || "disabled".equals(status) ? status : "all";
    String pattern = likePattern(normalizedQuery);
    long total =
        jdbc.queryForObject(
            """
            SELECT count(*)
            FROM owner_account o
            WHERE (? = ''
                   OR o.email_normalized ILIKE ? ESCAPE '\\'
                   OR o.display_name ILIKE ? ESCAPE '\\'
                   OR coalesce(o.username, '') ILIKE ? ESCAPE '\\')
              AND (? = 'all' OR o.account_status = ?)
            """,
            Long.class,
            normalizedQuery,
            pattern,
            pattern,
            pattern,
            normalizedStatus,
            normalizedStatus);
    SummaryStats summary =
        jdbc.queryForObject(
            """
            SELECT count(*) AS total_accounts,
                   count(*) FILTER (WHERE o.account_status = 'enabled') AS enabled_accounts,
                   count(*) FILTER (WHERE o.account_status = 'disabled') AS disabled_accounts,
                   coalesce(sum(b.lifetime_settled), 0) AS total_consumed
            FROM owner_account o
            LEFT JOIN billing_credit_account b ON b.owner_id = o.id
            """,
            (rs, row) ->
                new SummaryStats(
                    rs.getLong("total_accounts"),
                    rs.getLong("enabled_accounts"),
                    rs.getLong("disabled_accounts"),
                    rs.getLong("total_consumed")));
    List<AdminUser> items =
        jdbc.query(
            summarySql()
                + """
                WHERE (? = ''
                       OR o.email_normalized ILIKE ? ESCAPE '\\'
                       OR o.display_name ILIKE ? ESCAPE '\\'
                       OR coalesce(o.username, '') ILIKE ? ESCAPE '\\')
                  AND (? = 'all' OR o.account_status = ?)
                ORDER BY o.created_at DESC, o.id DESC
                LIMIT ? OFFSET ?
                """,
            this::mapSummary,
            normalizedQuery,
            pattern,
            pattern,
            pattern,
            normalizedStatus,
            normalizedStatus,
            normalizedLimit,
            (long) (normalizedPage - 1) * normalizedLimit);
    int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / normalizedLimit);
    return new AdminUserPage(
        items,
        normalizedPage,
        normalizedLimit,
        total,
        totalPages,
        summary.enabled(),
        summary.disabled(),
        summary.consumed());
  }

  @Transactional(readOnly = true)
  public AdminUser get(Authentication authentication, UUID ownerId) {
    authorization.require(authentication);
    return summary(ownerId);
  }

  @Transactional
  public AdminUser updateStatus(
      Authentication authentication,
      UUID ownerId,
      UpdateAccountStatusRequest request,
      String idempotencyKey) {
    OwnerAccountEntity actor = authorization.require(authentication);
    String reason = safeReason(request.reason());
    String requestHash =
        IdempotencyService.hash("status|" + ownerId + "|" + request.enabled() + "|" + reason);
    var replay =
        idempotency.replay(
            authentication, STATUS_OPERATION, idempotencyKey, requestHash, AdminUser.class);
    if (replay.isPresent()) {
      return replay.get();
    }
    OwnerAccountEntity target = requireTarget(ownerId);
    if (target.isAdmin()) {
      throw new AdminAccountException(
          HttpStatus.UNPROCESSABLE_ENTITY,
          "ADMIN_ACCOUNT_PROTECTED",
          "Administrator accounts cannot be disabled or enabled from user management.");
    }
    String previous = target.getAccountStatus();
    target.updateEnabled(request.enabled());
    accounts.saveAndFlush(target);
    String next = target.getAccountStatus();
    audit.append(
        actor.getId(),
        target.getId(),
        request.enabled() ? "account_enabled" : "account_disabled",
        reason,
        Map.of("status", previous, "role", target.getAccountRole()),
        Map.of("status", next, "role", target.getAccountRole()),
        idempotencyKey);
    AdminUser response = summary(target.getId());
    idempotency.record(
        authentication, STATUS_OPERATION, idempotencyKey, requestHash, response, 200);
    return response;
  }

  @Transactional
  public AdminUser grantPoints(
      Authentication authentication,
      UUID ownerId,
      GrantPointsRequest request,
      String idempotencyKey) {
    OwnerAccountEntity actor = authorization.require(authentication);
    String reason = safeReason(request.reason());
    String requestHash =
        IdempotencyService.hash("grant|" + ownerId + "|" + request.points() + "|" + reason);
    var replay =
        idempotency.replay(
            authentication, GRANT_OPERATION, idempotencyKey, requestHash, AdminUser.class);
    if (replay.isPresent()) {
      return replay.get();
    }
    OwnerAccountEntity target = requireTarget(ownerId);
    if (target.isAdmin()) {
      throw new AdminAccountException(
          HttpStatus.UNPROCESSABLE_ENTITY,
          "ADMIN_ACCOUNT_PROTECTED",
          "Administrator accounts cannot receive manual point grants.");
    }
    AdminUser before = summary(target.getId());
    billing.ensureAccount(target.getId());
    billing.manualAdjustment(
        authentication,
        new ManualAdjustmentRequest(
            target.getId(), request.points(), "credit", "admin_grant", idempotencyKey, reason));
    AdminUser after = summary(target.getId());
    audit.append(
        actor.getId(),
        target.getId(),
        "points_granted",
        reason,
        Map.of(
            "availablePoints",
            before.availablePoints(),
            "lifetimeGranted",
            before.lifetimeGranted()),
        Map.of(
            "availablePoints",
            after.availablePoints(),
            "lifetimeGranted",
            after.lifetimeGranted(),
            "points",
            request.points()),
        idempotencyKey);
    idempotency.record(authentication, GRANT_OPERATION, idempotencyKey, requestHash, after, 201);
    return after;
  }

  private OwnerAccountEntity requireTarget(UUID ownerId) {
    return accounts
        .findById(ownerId)
        .orElseThrow(
            () ->
                new AdminAccountException(
                    HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "The account was not found."));
  }

  AdminUser summary(UUID ownerId) {
    return jdbc.query(summarySql() + "WHERE o.id = ?", this::mapSummary, ownerId).stream()
        .findFirst()
        .orElseThrow(
            () ->
                new AdminAccountException(
                    HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "The account was not found."));
  }

  private String summarySql() {
    return """
        SELECT o.id, o.username, o.email_normalized, o.display_name,
               o.account_role, o.account_status, o.created_at,
               coalesce(k.knowledge_count, 0) AS knowledge_count,
               coalesce(b.available_points, 0) AS available_points,
               coalesce(b.reserved_points, 0) AS reserved_points,
               coalesce(b.lifetime_granted, 0) AS lifetime_granted,
               coalesce(b.lifetime_settled, 0) AS lifetime_consumed
        FROM owner_account o
        LEFT JOIN billing_credit_account b ON b.owner_id = o.id
        LEFT JOIN (
          SELECT owner_id, count(*) AS knowledge_count
          FROM knowledge_item
          WHERE lifecycle_state = 'active'
          GROUP BY owner_id
        ) k ON k.owner_id = o.id
        """;
  }

  private AdminUser mapSummary(ResultSet rs, int row) throws SQLException {
    String username = rs.getString("username");
    String email = rs.getString("email_normalized");
    Timestamp createdAt = rs.getTimestamp("created_at");
    Instant created = createdAt == null ? null : createdAt.toInstant();
    return new AdminUser(
        rs.getObject("id", UUID.class),
        username == null || username.isBlank() ? email : username,
        email,
        rs.getString("display_name"),
        rs.getString("account_role"),
        rs.getString("account_status"),
        created,
        rs.getLong("knowledge_count"),
        rs.getInt("available_points"),
        rs.getInt("reserved_points"),
        rs.getInt("lifetime_granted"),
        rs.getInt("lifetime_consumed"));
  }

  private static String likePattern(String query) {
    String escaped =
        query.toLowerCase().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    return "%" + escaped + "%";
  }

  private String safeReason(String reason) {
    String redacted = secretRedactor.redact(reason == null ? "" : reason).text().strip();
    if (redacted.codePointCount(0, redacted.length()) <= 300) {
      return redacted;
    }
    return redacted.substring(0, redacted.offsetByCodePoints(0, 300));
  }

  private record SummaryStats(long total, long enabled, long disabled, long consumed) {}
}
