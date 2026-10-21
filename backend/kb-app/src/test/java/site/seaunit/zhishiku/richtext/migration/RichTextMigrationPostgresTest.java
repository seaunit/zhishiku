package site.seaunit.zhishiku.richtext.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import site.seaunit.zhishiku.search.richtext.SearchProjectionBuilder;

class RichTextMigrationPostgresTest {
  static PostgreSQLContainer<?> POSTGRES;

  @BeforeAll
  static void startPostgresWhenDockerIsAvailable() {
    assumeTrue(DockerClientFactory.instance().isDockerAvailable());
    POSTGRES = new PostgreSQLContainer<>("postgres:18-alpine");
    POSTGRES.start();
  }

  @AfterAll
  static void stopPostgres() {
    if (POSTGRES != null) {
      POSTGRES.stop();
    }
  }

  @Test
  void migratesFreshDatabaseAndRepeatedExecutionIsIdempotent() throws Exception {
    var configuration =
        Flyway.configure()
            .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
            .cleanDisabled(false)
            .locations("classpath:db/migration");

    configuration.load().clean();
    assertThat(configuration.target("0004").load().migrate().migrationsExecuted).isEqualTo(2);
    assertThat(configuration.load().migrate().migrationsExecuted).isEqualTo(1);
    assertThat(configuration.load().migrate().migrationsExecuted).isZero();
    configuration.load().clean();
    assertThat(configuration.load().migrate().migrationsExecuted).isEqualTo(3);

    try (Connection connection = POSTGRES.createConnection("");
        Statement statement = connection.createStatement()) {
      assertThat(
              queryLong(
                  statement,
                  "SELECT count(*) FROM information_schema.tables WHERE table_name = 'rich_document_migration'"))
          .isEqualTo(1);
      assertThat(
              queryLong(
                  statement,
                  "SELECT count(*) FROM information_schema.columns WHERE table_name = 'rich_image_reference' AND column_name = 'status'"))
          .isEqualTo(1);
      assertThat(
              queryLong(
                  statement,
                  "SELECT count(*) FROM pg_indexes WHERE indexname = 'rich_image_reference_cleanup_idx'"))
          .isEqualTo(1);
      assertThat(
              queryLong(
                  statement,
                  "SELECT count(*) FROM information_schema.columns WHERE table_name = 'rich_document' AND column_name = 'version'"))
          .isEqualTo(1);
      try (ResultSet result =
          statement.executeQuery(
              "SELECT data_type FROM information_schema.columns WHERE table_name = 'rich_document' AND column_name = 'document'")) {
        assertThat(result.next()).isTrue();
        assertThat(result.getString(1)).isEqualTo("jsonb");
      }
    }
  }

  @Test
  void legacyTextMigrationIsIdempotentAndRecordsOneRevisionAndSource() {
    var configuration =
        Flyway.configure()
            .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
            .cleanDisabled(false)
            .locations("classpath:db/migration");
    configuration.load().clean();
    configuration.load().migrate();

    DriverManagerDataSource dataSource =
        new DriverManagerDataSource(
            POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    JdbcTemplate jdbc = new JdbcTemplate(dataSource);
    UUID ownerId = UUID.randomUUID();
    UUID itemId = UUID.randomUUID();
    String body = "第一段\n保留换行\n\n链接 https://example.com/path";
    jdbc.update(
        """
        INSERT INTO owner_account
          (id, instance_key, email_normalized, password_hash, locale,
           created_at, updated_at, version)
        VALUES (?, 1, 'owner@example.com', 'hash', 'zh-CN', now(), now(), 1)
        """,
        ownerId);
    jdbc.update(
        """
        INSERT INTO knowledge_item
          (id, owner_id, title, body, note, processing_status, lifecycle_state,
           body_migration_state, search_revision, created_at, updated_at, version)
        VALUES (?, ?, '旧文本', ?, '旧条目', 'not_requested', 'active',
                'completed', 1, now(), now(), 1)
        """,
        itemId,
        ownerId,
        body);
    LegacyTextMigrationService service =
        new LegacyTextMigrationService(
            jdbc,
            new ObjectMapper(),
            new LegacyTextDocumentConverter(),
            new SearchProjectionBuilder(new ObjectMapper()),
            new DataSourceTransactionManager(dataSource));

    assertThat(service.enqueueMissing(10)).isEqualTo(1);
    assertThat(service.migrate(itemId)).isTrue();
    assertThat(service.migrate(itemId)).isTrue();

    assertThat(queryLong(jdbc, "SELECT count(*) FROM rich_document WHERE item_id = ?", itemId))
        .isEqualTo(1);
    assertThat(
            queryLong(
                jdbc, "SELECT count(*) FROM rich_document_revision WHERE item_id = ?", itemId))
        .isEqualTo(1);
    assertThat(
            queryLong(jdbc, "SELECT count(*) FROM rich_document_source WHERE item_id = ?", itemId))
        .isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT body FROM knowledge_item WHERE id = ?", String.class, itemId))
        .isEqualTo(body);
    assertThat(
            jdbc.queryForObject(
                "SELECT status FROM rich_document_migration WHERE item_id = ?",
                String.class,
                itemId))
        .isEqualTo("migrated");
  }

  private static long queryLong(Statement statement, String sql) throws Exception {
    try (ResultSet result = statement.executeQuery(sql)) {
      result.next();
      return result.getLong(1);
    }
  }

  private static long queryLong(JdbcTemplate jdbc, String sql, Object... args) {
    return jdbc.queryForObject(sql, Long.class, args);
  }
}
