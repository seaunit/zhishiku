package site.seaunit.zhishiku.richtext.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class MigrationSchemaContractTest {
  @Test
  void integrityMigrationAddsMigrationStateReferenceStatusAndProvenanceGuards() throws Exception {
    String sql = resource("db/migration/V0005__rich_text_integrity.sql");

    assertThat(sql)
        .contains("CREATE TABLE rich_document_migration")
        .contains("ADD COLUMN status")
        .contains("rich_image_reference_cleanup_idx")
        .contains("rich_document_source_provenance_check")
        .contains("rich_document_migration_status_idx")
        .contains("body_migration_state = 'pending'");
  }

  @Test
  void migrationVersionIsAfterTheAlreadyAppliedRichTextMigration() throws Exception {
    assertThat(resource("db/migration/V0004__rich_text.sql"))
        .contains("CREATE TABLE rich_document")
        .contains("CREATE TABLE rich_image_reference")
        .doesNotContain("rich_document_migration");
  }

  @Test
  void multiAccountMigrationRemovesTheSingleInstanceConstraint() throws Exception {
    String sql = resource("db/migration/V0006__multi_account.sql");

    assertThat(sql)
        .contains("DROP CONSTRAINT IF EXISTS owner_account_instance_key_key")
        .contains("DROP CONSTRAINT IF EXISTS owner_account_instance_key_check")
        .contains("DROP COLUMN IF EXISTS instance_key")
        .contains("ADD COLUMN IF NOT EXISTS email_verified_at")
        .contains("ALTER COLUMN email_verified_at SET NOT NULL");
  }

  private static String resource(String path) throws Exception {
    try (InputStream input =
        MigrationSchemaContractTest.class.getClassLoader().getResourceAsStream(path)) {
      assertThat(input).as(path).isNotNull();
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
