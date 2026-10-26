package site.seaunit.zhishiku.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import site.seaunit.zhishiku.admin.AdminAccountDtos.AdminUser;
import site.seaunit.zhishiku.admin.AdminAccountDtos.AdminUserPage;
import site.seaunit.zhishiku.common.ApiExceptionHandler;
import site.seaunit.zhishiku.security.SecurityConfig;

@WebMvcTest(AdminAccountController.class)
@Import({SecurityConfig.class, ApiExceptionHandler.class})
class AdminAccountControllerTest {
  private static final UUID OWNER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final String IDEMPOTENCY_KEY = "admin-controller-request-0001";

  @Autowired private MockMvc mvc;

  @MockBean private AdminAccountService service;
  @MockBean private AuthenticationManager authenticationManager;

  @Test
  void rejectsAnonymousManagementRequests() throws Exception {
    mvc.perform(get("/api/v1/admin/users")).andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(username = "a115846")
  void listsAccountsWithPointAndConsumptionFields() throws Exception {
    when(service.list(any(), eq("alice"), eq(1), eq(20), eq("all")))
        .thenReturn(new AdminUserPage(List.of(user()), 1, 20, 1, 1, 9, 2, 340));

    mvc.perform(get("/api/v1/admin/users").param("query", "alice"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].loginName").value("alice@example.com"))
        .andExpect(jsonPath("$.items[0].availablePoints").value(37))
        .andExpect(jsonPath("$.items[0].lifetimeConsumed").value(19));
  }

  @Test
  @WithMockUser(username = "alice@example.com")
  void returnsForbiddenWithoutLeakingAccountData() throws Exception {
    when(service.list(any(), eq(""), eq(1), eq(20), eq("all")))
        .thenThrow(
            new AdminAccountException(
                HttpStatus.FORBIDDEN, "ADMIN_REQUIRED", "Administrator access is required."));

    mvc.perform(get("/api/v1/admin/users"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("ADMIN_REQUIRED"))
        .andExpect(jsonPath("$.items").doesNotExist());
  }

  @Test
  @WithMockUser(username = "a115846")
  void updatesStatusOnlyWithCsrfAndAnIdempotencyKey() throws Exception {
    when(service.updateStatus(any(), eq(OWNER_ID), any(), eq(IDEMPOTENCY_KEY))).thenReturn(user());

    mvc.perform(
            patch("/api/v1/admin/users/{id}/status", OWNER_ID)
                .with(csrf())
                .header("Idempotency-Key", IDEMPOTENCY_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"enabled":false,"reason":"Policy review"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("enabled"));

    verify(service).updateStatus(any(), eq(OWNER_ID), any(), eq(IDEMPOTENCY_KEY));
  }

  @Test
  @WithMockUser(username = "a115846")
  void grantsBoundedPointsWithCsrfAndIdempotency() throws Exception {
    when(service.grantPoints(any(), eq(OWNER_ID), any(), eq(IDEMPOTENCY_KEY))).thenReturn(user());

    mvc.perform(
            post("/api/v1/admin/users/{id}/point-grants", OWNER_ID)
                .with(csrf())
                .header("Idempotency-Key", IDEMPOTENCY_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"points":120,"reason":"Service compensation"}
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.availablePoints").value(37));
  }

  @Test
  @WithMockUser(username = "a115846")
  void rejectsOutOfRangeGrants() throws Exception {
    mvc.perform(
            post("/api/v1/admin/users/{id}/point-grants", OWNER_ID)
                .with(csrf())
                .header("Idempotency-Key", IDEMPOTENCY_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"points":100001,"reason":"Too many"}
                    """))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
  }

  private static AdminUser user() {
    return new AdminUser(
        OWNER_ID,
        "alice@example.com",
        "alice@example.com",
        "Alice",
        "USER",
        "enabled",
        Instant.parse("2026-10-20T00:00:00Z"),
        8,
        37,
        2,
        56,
        19);
  }
}
