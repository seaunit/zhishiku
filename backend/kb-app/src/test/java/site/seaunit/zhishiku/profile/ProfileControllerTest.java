package site.seaunit.zhishiku.profile;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import site.seaunit.zhishiku.security.SecurityConfig;

@WebMvcTest(ProfileController.class)
@Import({SecurityConfig.class, ProfileExceptionHandler.class})
class ProfileControllerTest {
  private static final UUID OWNER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

  @Autowired private MockMvc mvc;

  @MockBean private ProfileService profileService;
  @MockBean private AuthenticationManager authenticationManager;

  @Test
  void rejectsUnauthenticatedProfileReads() throws Exception {
    mvc.perform(get("/api/v1/profile")).andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(username = "alice@example.com")
  void returnsOnlySafeCurrentProfileFields() throws Exception {
    when(profileService.get(argThat(auth -> "alice@example.com".equals(auth.getName()))))
        .thenReturn(new ProfileResponse(OWNER_ID, "alice@example.com", "Alice", "zh-CN"));

    mvc.perform(get("/api/v1/profile"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ownerId").value(OWNER_ID.toString()))
        .andExpect(jsonPath("$.email").value("alice@example.com"))
        .andExpect(jsonPath("$.displayName").value("Alice"))
        .andExpect(jsonPath("$.locale").value("zh-CN"))
        .andExpect(jsonPath("$.passwordHash").doesNotExist())
        .andExpect(jsonPath("$.csrfToken").doesNotExist());
  }

  @Test
  @WithMockUser(username = "alice@example.com")
  void requiresCsrfForProfileUpdates() throws Exception {
    mvc.perform(
            patch("/api/v1/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"displayName":"Alice"}
                    """))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(username = "alice@example.com")
  void updatesTheAuthenticatedAccountWithCsrf() throws Exception {
    when(profileService.update(
            argThat(auth -> "alice@example.com".equals(auth.getName())), eq("Alice")))
        .thenReturn(new ProfileResponse(OWNER_ID, "alice@example.com", "Alice", "zh-CN"));

    mvc.perform(
            patch("/api/v1/profile")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"displayName":"Alice"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.displayName").value("Alice"))
        .andExpect(jsonPath("$.passwordHash").doesNotExist());

    verify(profileService).update(argThat(authenticationMatches("alice@example.com")), eq("Alice"));
  }

  @Test
  @WithMockUser(username = "alice@example.com")
  void returnsStableValidationErrorForUnsafeDisplayNames() throws Exception {
    when(profileService.update(argThat(authenticationMatches("alice@example.com")), eq("<script>")))
        .thenThrow(new InvalidDisplayNameException("Display name is invalid."));

    mvc.perform(
            patch("/api/v1/profile")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"displayName":"<script>"}
                    """))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("DISPLAY_NAME_INVALID"));
  }

  private static org.mockito.ArgumentMatcher<Authentication> authenticationMatches(String name) {
    return authentication -> authentication != null && name.equals(authentication.getName());
  }
}
