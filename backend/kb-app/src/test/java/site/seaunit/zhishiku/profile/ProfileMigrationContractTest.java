package site.seaunit.zhishiku.profile;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ProfileMigrationContractTest {
  @Test
  void addsBackfillsAndConstrainsDisplayName() throws Exception {
    URL resource = getClass().getResource("/db/migration/V0007__account_profile.sql");

    assertThat(resource).isNotNull();
    String sql = Files.readString(Path.of(resource.toURI()));
    assertThat(sql)
        .contains("ADD COLUMN display_name")
        .contains("split_part(email_normalized, '@', 1)")
        .contains("ALTER COLUMN display_name SET NOT NULL")
        .contains("char_length(btrim(display_name)) BETWEEN 1 AND 40");
  }
}
