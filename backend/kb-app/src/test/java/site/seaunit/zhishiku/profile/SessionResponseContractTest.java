package site.seaunit.zhishiku.profile;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import site.seaunit.zhishiku.auth.SessionResponse;

class SessionResponseContractTest {
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void exposesDisplayNameWithoutCredentialFields() throws Exception {
    SessionResponse response =
        new SessionResponse(
            UUID.fromString("11111111-1111-1111-1111-111111111111"),
            "alice@example.com",
            "Alice",
            "zh-CN",
            "csrf-token");

    String json = objectMapper.writeValueAsString(response);

    assertThat(json).contains("\"displayName\":\"Alice\"");
    assertThat(json).doesNotContain("passwordHash", "password_hash");
  }
}
