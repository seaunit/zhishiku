package site.seaunit.zhishiku.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.TestingAuthenticationToken;
import site.seaunit.zhishiku.admin.AdminAccountDtos.AdminUser;
import site.seaunit.zhishiku.admin.AdminAccountDtos.GrantPointsRequest;
import site.seaunit.zhishiku.admin.AdminAccountDtos.UpdateAccountStatusRequest;
import site.seaunit.zhishiku.billing.BillingService;
import site.seaunit.zhishiku.idempotency.IdempotencyService;
import site.seaunit.zhishiku.owner.OwnerAccountEntity;
import site.seaunit.zhishiku.owner.OwnerAccountRepository;
import site.seaunit.zhishiku.webimport.AiTextSecretRedactor;

class AdminAccountServiceTest {
  private static final UUID TARGET_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
  private static final String KEY = "admin-service-request-0001";

  private final OwnerAccountRepository accounts = mock(OwnerAccountRepository.class);
  private final AdminAuthorizationService authorization = mock(AdminAuthorizationService.class);
  private final AdminAuditRepository audit = mock(AdminAuditRepository.class);
  private final IdempotencyService idempotency = mock(IdempotencyService.class);
  private final BillingService billing = mock(BillingService.class);
  private final AiTextSecretRedactor secretRedactor = new AiTextSecretRedactor();
  private final Deque<AdminUser> summaryResponses = new ArrayDeque<>();
  private final TestingAuthenticationToken authentication =
      new TestingAuthenticationToken("a115846", "hash");
  private final AdminAccountService service =
      new AdminAccountService(
          mock(JdbcTemplate.class),
          accounts,
          authorization,
          audit,
          idempotency,
          billing,
          secretRedactor) {
        @Override
        AdminUser summary(UUID ownerId) {
          return summaryResponses.removeFirst();
        }
      };

  @Test
  void disablesAnOrdinaryAccountAndWritesASafeAuditEvent() {
    OwnerAccountEntity actor = administrator();
    OwnerAccountEntity target = OwnerAccountEntity.create("user@example.com", "hash", "zh-CN");
    AdminUser after = summary("disabled", 10, 20);
    summaryResponses.add(after);
    when(authorization.require(authentication)).thenReturn(actor);
    when(idempotency.replay(any(), any(), any(), any(), any())).thenReturn(Optional.empty());
    when(accounts.findById(TARGET_ID)).thenReturn(Optional.of(target));
    when(accounts.saveAndFlush(target)).thenReturn(target);

    AdminUser response =
        service.updateStatus(
            authentication, TARGET_ID, new UpdateAccountStatusRequest(false, "Policy review"), KEY);

    assertThat(response).isSameAs(after);
    assertThat(target.isEnabled()).isFalse();
    verify(audit)
        .append(
            actor.getId(),
            target.getId(),
            "account_disabled",
            "Policy review",
            java.util.Map.of("status", "enabled", "role", "USER"),
            java.util.Map.of("status", "disabled", "role", "USER"),
            KEY);
    verify(idempotency)
        .record(
            eq(authentication),
            eq("admin-account-status"),
            eq(KEY),
            anyString(),
            eq(after),
            eq(200));
  }

  @Test
  void refusesToDisableAnyAdministrator() {
    when(authorization.require(authentication)).thenReturn(administrator());
    when(idempotency.replay(any(), any(), any(), any(), any())).thenReturn(Optional.empty());
    when(accounts.findById(TARGET_ID)).thenReturn(Optional.of(administrator()));

    assertThatThrownBy(
            () ->
                service.updateStatus(
                    authentication, TARGET_ID, new UpdateAccountStatusRequest(false, "No"), KEY))
        .isInstanceOfSatisfying(
            AdminAccountException.class,
            error -> {
              assertThat(error.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
              assertThat(error.getCode()).isEqualTo("ADMIN_ACCOUNT_PROTECTED");
            });
    verify(accounts, never()).saveAndFlush(any());
  }

  @Test
  void grantsPointsThroughTheExistingManualAdjustmentLedger() {
    OwnerAccountEntity actor = administrator();
    OwnerAccountEntity target = OwnerAccountEntity.create("user@example.com", "hash", "zh-CN");
    AdminUser before = summary("enabled", 10, 20);
    AdminUser after = summary("enabled", 22, 32);
    summaryResponses.add(before);
    summaryResponses.add(after);
    when(authorization.require(authentication)).thenReturn(actor);
    when(idempotency.replay(any(), any(), any(), any(), any())).thenReturn(Optional.empty());
    when(accounts.findById(TARGET_ID)).thenReturn(Optional.of(target));

    AdminUser response =
        service.grantPoints(
            authentication, TARGET_ID, new GrantPointsRequest(12, "Service compensation"), KEY);

    assertThat(response).isSameAs(after);
    verify(billing).ensureAccount(target.getId());
    verify(billing)
        .manualAdjustment(
            eq(authentication),
            eq(
                new site.seaunit.zhishiku.billing.BillingDtos.ManualAdjustmentRequest(
                    target.getId(), 12, "credit", "admin_grant", KEY, "Service compensation")));
    verify(audit)
        .append(
            actor.getId(),
            target.getId(),
            "points_granted",
            "Service compensation",
            java.util.Map.of("availablePoints", 10, "lifetimeGranted", 20),
            java.util.Map.of("availablePoints", 22, "lifetimeGranted", 32, "points", 12),
            KEY);
  }

  @Test
  void returnsThePersistedIdempotentResultWithoutRepeatingTheGrant() {
    AdminUser persisted = summary("enabled", 22, 32);
    when(authorization.require(authentication)).thenReturn(administrator());
    when(idempotency.replay(any(), any(), any(), any(), any())).thenReturn(Optional.of(persisted));

    AdminUser response =
        service.grantPoints(
            authentication, TARGET_ID, new GrantPointsRequest(12, "Service compensation"), KEY);

    assertThat(response).isSameAs(persisted);
    verify(billing, never()).manualAdjustment(any(), any());
    verify(audit, never()).append(any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  void redactsSecretsBeforeAuditAndLedgerWrites() {
    OwnerAccountEntity actor = administrator();
    OwnerAccountEntity target = OwnerAccountEntity.create("user@example.com", "hash", "zh-CN");
    summaryResponses.add(summary("enabled", 10, 20));
    summaryResponses.add(summary("enabled", 15, 25));
    when(authorization.require(authentication)).thenReturn(actor);
    when(idempotency.replay(any(), any(), any(), any(), any())).thenReturn(Optional.empty());
    when(accounts.findById(TARGET_ID)).thenReturn(Optional.of(target));

    service.grantPoints(
        authentication, TARGET_ID, new GrantPointsRequest(5, "api_key=very-secret-value"), KEY);

    verify(audit)
        .append(
            actor.getId(),
            target.getId(),
            "points_granted",
            "api_key=[REDACTED]",
            java.util.Map.of("availablePoints", 10, "lifetimeGranted", 20),
            java.util.Map.of("availablePoints", 15, "lifetimeGranted", 25, "points", 5),
            KEY);
  }

  private static OwnerAccountEntity administrator() {
    return OwnerAccountEntity.createAdmin(
        "a115846", "a115846@admin.seaunit.local", "hash", "zh-CN");
  }

  private static AdminUser summary(String status, int availablePoints, int lifetimeGranted) {
    return new AdminUser(
        TARGET_ID,
        "user@example.com",
        "user@example.com",
        "User",
        "USER",
        status,
        Instant.parse("2026-10-20T00:00:00Z"),
        3,
        availablePoints,
        2,
        lifetimeGranted,
        9);
  }
}
