package site.seaunit.zhishiku.billing;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.web.servlet.MockMvc;
import site.seaunit.zhishiku.billing.BillingDtos.NormalizedWaffoEvent;
import site.seaunit.zhishiku.config.AppProperties;
import site.seaunit.zhishiku.security.SecurityConfig;

@WebMvcTest(BillingInternalController.class)
@Import({SecurityConfig.class, site.seaunit.zhishiku.common.ApiExceptionHandler.class})
class BillingInternalControllerTest {
  @Autowired private MockMvc mvc;

  @MockBean private BillingService service;
  @MockBean private AuthenticationManager authenticationManager;

  @Test
  void rejectsMissingOrIncorrectInternalSecret() throws Exception {
    mvc.perform(
            post("/api/internal/billing/waffo-events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(eventJson()))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("INTERNAL_AUTH_REQUIRED"));

    mvc.perform(
            post("/api/internal/billing/waffo-events")
                .header("X-Internal-Secret", "wrong-secret")
                .contentType(MediaType.APPLICATION_JSON)
                .content(eventJson()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void forwardsVerifiedBridgeEventWithCorrectInternalSecret() throws Exception {
    when(service.processWaffoEvent(any())).thenAnswer(invocation -> invocation.getArgument(0));

    mvc.perform(
            post("/api/internal/billing/waffo-events")
                .header("X-Internal-Secret", "bridge-secret")
                .contentType(MediaType.APPLICATION_JSON)
                .content(eventJson()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sourceEventKey").value("evt_1"));

    verify(service).processWaffoEvent(any(NormalizedWaffoEvent.class));
  }

  private static AppProperties.Billing billingConfig() {
    AppProperties.Billing billing = AppProperties.Billing.disabled();
    return new AppProperties.Billing(
        billing.environment(),
        billing.databaseEnvironment(),
        billing.enabled(),
        billing.bridgeUrl(),
        billing.appToBridgeSecret(),
        "bridge-secret",
        billing.catalogVersion(),
        billing.checkoutSuccessUrl(),
        billing.webhookUrl(),
        billing.refundSupportUrl(),
        billing.refundResponseTarget(),
        billing.adminEmails(),
        billing.idempotencyTtlSeconds(),
        billing.taskTtlSeconds(),
        billing.trialPoints());
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class TestConfig {
    @Bean
    AppProperties appProperties() {
      AppProperties properties = mock(AppProperties.class);
      when(properties.billing()).thenReturn(billingConfig());
      return properties;
    }
  }

  private static String eventJson() {
    return """
        {
          "environment": "test",
          "eventType": "paid",
          "sourceEventKey": "evt_1",
          "providerEventId": "evt_1",
          "deliveryId": "delivery_1",
          "orderId": "123e4567-e89b-42d3-a456-426614174180",
          "providerOrderId": "order_1",
          "providerSessionId": "session_1",
          "providerRefundId": null,
          "providerProductId": "PROD_9",
          "productMatch": true,
          "amountMinor": 900,
          "currency": "CNY",
          "refundAmountMinor": null,
          "reasonCode": null,
          "signatureStatus": "valid",
          "payloadHash": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
          "receivedAt": "2026-10-22T08:00:00Z"
        }
        """;
  }
}
