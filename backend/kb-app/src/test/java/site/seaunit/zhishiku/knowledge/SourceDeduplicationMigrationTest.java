package site.seaunit.zhishiku.knowledge;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class SourceDeduplicationMigrationTest {
  @Test
  void addsAndBackfillsCanonicalSourceUrlWithLookupIndexes() throws Exception {
    String sql =
        new ClassPathResource("db/migration/V0018__knowledge_source_deduplication.sql")
            .getContentAsString(StandardCharsets.UTF_8);

    assertThat(sql)
        .contains("ADD COLUMN canonical_url TEXT")
        .contains("SET canonical_url = source_url")
        .contains("rich_document_source_canonical_url_idx")
        .contains("rich_document_source_url_idx");
  }
}
