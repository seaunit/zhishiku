package site.seaunit.zhishiku.billing;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class BillingMigrationContractTest {
  @Test
  void billingMigrationContainsAuthoritativeTablesAndAppendOnlyLedger() throws Exception {
    String sql =
        new ClassPathResource("db/migration/V0009__billing.sql")
            .getContentAsString(StandardCharsets.UTF_8);

    assertThat(sql)
        .contains("CREATE TABLE billing_payment_order")
        .contains("CREATE TABLE billing_payment_event")
        .contains("CREATE TABLE billing_credit_batch")
        .contains("CREATE TABLE billing_ledger_entry")
        .contains("CREATE TABLE billing_task")
        .contains("CREATE TABLE billing_reservation")
        .contains("CREATE TABLE billing_refund_case")
        .contains("CREATE TABLE billing_idempotency")
        .contains("billing_ledger_no_update")
        .contains("UNIQUE (environment, source_event_key)")
        .contains("UNIQUE (owner_id, idempotency_key)");
  }

  @Test
  void onePointAnswerMigrationOnlyUpdatesTheActiveCatalog() throws Exception {
    String sql =
        new ClassPathResource("db/migration/V0016__ai_answer_one_point.sql")
            .getContentAsString(StandardCharsets.UTF_8);

    assertThat(sql)
        .contains("UPDATE billing_ai_rate")
        .contains("SET points_per_unit = 1")
        .contains("catalog.status = 'active'")
        .contains("rate.feature = 'ai_answer'");
  }
}
