package site.seaunit.zhishiku.billing;

import java.time.Duration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import site.seaunit.zhishiku.billing.BillingDtos.BridgeHealthResponse;
import site.seaunit.zhishiku.billing.BillingDtos.CheckoutBridgeRequest;
import site.seaunit.zhishiku.billing.BillingDtos.CheckoutBridgeResponse;
import site.seaunit.zhishiku.billing.BillingDtos.NormalizedStatusResponse;
import site.seaunit.zhishiku.billing.BillingDtos.OrderStatusBridgeRequest;
import site.seaunit.zhishiku.config.AppProperties;

@Component
public class BillingBridgeClient {
  private final RestClient client;
  private final AppProperties.Billing config;

  public BillingBridgeClient(RestClient.Builder builder, AppProperties properties) {
    this.config = properties.billing();
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(Duration.ofSeconds(5));
    requestFactory.setReadTimeout(Duration.ofSeconds(15));
    this.client = builder.baseUrl(config.bridgeUrl()).requestFactory(requestFactory).build();
  }

  public BridgeHealthResponse health() {
    try {
      return client.get().uri("/health").retrieve().body(BridgeHealthResponse.class);
    } catch (RuntimeException error) {
      throw unavailable();
    }
  }

  public CheckoutBridgeResponse checkout(CheckoutBridgeRequest request, String idempotencyKey) {
    requireInternalSecret();
    try {
      return client
          .post()
          .uri("/checkout")
          .header("X-Internal-Secret", config.appToBridgeSecret())
          .header("Idempotency-Key", idempotencyKey)
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON)
          .body(request)
          .retrieve()
          .body(CheckoutBridgeResponse.class);
    } catch (RuntimeException error) {
      throw unavailable();
    }
  }

  public NormalizedStatusResponse orderStatus(OrderStatusBridgeRequest request) {
    requireInternalSecret();
    try {
      return client
          .post()
          .uri("/orders/status")
          .header("X-Internal-Secret", config.appToBridgeSecret())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON)
          .body(request)
          .retrieve()
          .body(NormalizedStatusResponse.class);
    } catch (RuntimeException error) {
      throw unavailable();
    }
  }

  private void requireInternalSecret() {
    if (config.appToBridgeSecret() == null || config.appToBridgeSecret().isBlank()) {
      throw new BillingException(
          HttpStatus.SERVICE_UNAVAILABLE,
          "BILLING_BRIDGE_NOT_CONFIGURED",
          "Waffo bridge credentials are not configured.");
    }
  }

  private static BillingException unavailable() {
    return new BillingException(
        HttpStatus.SERVICE_UNAVAILABLE,
        "BILLING_BRIDGE_UNAVAILABLE",
        "Waffo payment service is temporarily unavailable.");
  }
}
