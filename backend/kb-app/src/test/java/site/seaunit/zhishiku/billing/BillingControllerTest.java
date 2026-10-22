package site.seaunit.zhishiku.billing;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import site.seaunit.zhishiku.billing.BillingDtos.CatalogResponse;
import site.seaunit.zhishiku.billing.BillingDtos.CreditAccountResponse;
import site.seaunit.zhishiku.billing.BillingDtos.PaymentOrderResponse;
import site.seaunit.zhishiku.billing.BillingDtos.RefundPolicy;
import site.seaunit.zhishiku.security.SecurityConfig;

@WebMvcTest(BillingController.class)
@Import({SecurityConfig.class, site.seaunit.zhishiku.common.ApiExceptionHandler.class})
class BillingControllerTest {
  @Autowired private MockMvc mvc;

  @MockBean private BillingService service;
  @MockBean private AuthenticationManager authenticationManager;

  @Test
  @WithMockUser(username = "owner@example.com")
  void exposesCatalogForAuthenticatedOwner() throws Exception {
    when(service.catalog())
        .thenReturn(
            new CatalogResponse(
                "test",
                false,
                "2026-10-mvp",
                List.of(),
                List.of(),
                new RefundPolicy("https://seaunit.site/kb/billing", "3 days", true, true)));

    mvc.perform(get("/api/v1/billing/catalog"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.environment").value("test"))
        .andExpect(jsonPath("$.billingEnabled").value(false))
        .andExpect(jsonPath("$.versionCode").value("2026-10-mvp"));
  }

  @Test
  @WithMockUser(username = "owner@example.com")
  void returnsAccountForAuthenticatedOwner() throws Exception {
    when(service.account(any()))
        .thenReturn(
            new CreditAccountResponse(100, 0, 100, 0, Instant.parse("2026-10-22T00:00:00Z")));

    mvc.perform(get("/api/v1/billing/account"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.availablePoints").value(100));
  }

  @Test
  @WithMockUser(username = "owner@example.com")
  void createsOrderWithIdempotencyKey() throws Exception {
    UUID orderId = UUID.randomUUID();
    when(service.createOrder(any(), any(), any()))
        .thenReturn(
            new PaymentOrderResponse(
                orderId,
                "kb_pack_9",
                "test",
                900,
                "CNY",
                400,
                null,
                "PENDING",
                null,
                null,
                null,
                Instant.now(),
                Instant.now()));

    mvc.perform(
            post("/api/v1/billing/orders")
                .with(csrf())
                .header("Idempotency-Key", "order-request-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sku\":\"kb_pack_9\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.sku").value("kb_pack_9"))
        .andExpect(jsonPath("$.points").value(400));
  }
}
