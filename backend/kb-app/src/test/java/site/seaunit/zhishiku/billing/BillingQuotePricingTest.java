package site.seaunit.zhishiku.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.core.Authentication;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import site.seaunit.zhishiku.billing.BillingDtos.CreateQuoteRequest;
import site.seaunit.zhishiku.billing.BillingDtos.QuoteLineRequest;
import site.seaunit.zhishiku.billing.BillingDtos.QuoteResponse;
import site.seaunit.zhishiku.config.AppProperties;
import site.seaunit.zhishiku.owner.CurrentOwnerService;
import site.seaunit.zhishiku.owner.OwnerAccountEntity;

class BillingQuotePricingTest {
  private static final Instant NOW = Instant.parse("2026-10-22T08:00:00Z");

  private static PostgreSQLContainer<?> postgres;
  private static BillingService service;

  @BeforeAll
  static void startPostgresWhenDockerIsAvailable() {
    String externalJdbcUrl = System.getenv("SHIHAI_TEST_DATABASE_URL");
    String jdbcUrl;
    String username;
    String password;
    if (externalJdbcUrl != null && !externalJdbcUrl.isBlank()) {
      jdbcUrl = externalJdbcUrl;
      username = System.getenv().getOrDefault("SHIHAI_TEST_DATABASE_USERNAME", "postgres");
      password = System.getenv().getOrDefault("SHIHAI_TEST_DATABASE_PASSWORD", "");
    } else {
      assumeTrue(DockerClientFactory.instance().isDockerAvailable());
      postgres = new PostgreSQLContainer<>("postgres:18-alpine");
      postgres.start();
      jdbcUrl = postgres.getJdbcUrl();
      username = postgres.getUsername();
      password = postgres.getPassword();
    }
    Flyway.configure()
        .dataSource(jdbcUrl, username, password)
        .locations("classpath:db/migration")
        .load()
        .migrate();

    DriverManagerDataSource dataSource = new DriverManagerDataSource(jdbcUrl, username, password);
    JdbcTemplate jdbc = new JdbcTemplate(dataSource);
    OwnerAccountEntity owner =
        OwnerAccountEntity.create("owner-" + UUID.randomUUID() + "@example.com", "hash", "zh-CN");
    jdbc.update(
        """
        INSERT INTO owner_account
          (id, email_normalized, display_name, email_verified_at, password_hash, locale,
           created_at, updated_at, version)
        VALUES (?, ?, 'owner', now(), 'hash', 'zh-CN', now(), now(), 1)
        """,
        owner.getId(),
        owner.getEmailNormalized());

    CurrentOwnerService owners = mock(CurrentOwnerService.class);
    when(owners.require(any())).thenReturn(owner);
    AppProperties properties = mock(AppProperties.class);
    when(properties.billing())
        .thenReturn(
            new AppProperties.Billing(
                "test",
                "test",
                false,
                "http://127.0.0.1:8091",
                "",
                "",
                "2026-10-mvp",
                "http://127.0.0.1:4173/kb/billing/return",
                "http://127.0.0.1:8082/kb/api/internal/billing/waffo-events",
                "https://support.example.invalid/refund",
                "3 个工作日",
                "",
                86400,
                1800,
                100));
    service =
        new BillingService(
            jdbc,
            new ObjectMapper(),
            owners,
            mock(BillingBridgeClient.class),
            properties,
            Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @AfterAll
  static void stopPostgres() {
    if (postgres != null) {
      postgres.stop();
    }
  }

  @Test
  void publiclyQuotesInternetSearchAtOnePoint() {
    QuoteResponse quote =
        service.createQuote(
            mock(Authentication.class),
            new CreateQuoteRequest(
                "internet_search",
                null,
                null,
                List.of(),
                List.of(new QuoteLineRequest("internet_search", 1)),
                false));

    assertThat(quote.maxPoints()).isEqualTo(1);
    assertThat(quote.lines())
        .singleElement()
        .satisfies(
            line -> {
              assertThat(line.feature()).isEqualTo("internet_search");
              assertThat(line.quantity()).isEqualTo(1);
              assertThat(line.pointsPerUnit()).isEqualTo(1);
              assertThat(line.maxPoints()).isEqualTo(1);
            });
  }
}
