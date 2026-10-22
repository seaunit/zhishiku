package site.seaunit.zhishiku.billing;

import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.seaunit.zhishiku.billing.BillingDtos.NormalizedWaffoEvent;
import site.seaunit.zhishiku.config.AppProperties;

@RestController
@RequestMapping("/api/internal/billing")
public class BillingInternalController {
  private final BillingService service;
  private final AppProperties.Billing config;

  public BillingInternalController(BillingService service, AppProperties properties) {
    this.service = service;
    this.config = properties.billing();
  }

  @PostMapping("/waffo-events")
  NormalizedWaffoEvent event(
      @RequestHeader(name = "X-Internal-Secret", required = false) String secret,
      @Valid @RequestBody NormalizedWaffoEvent event) {
    if (!constantTimeEquals(config.bridgeToAppSecret(), secret)) {
      throw new BillingException(
          HttpStatus.UNAUTHORIZED, "INTERNAL_AUTH_REQUIRED", "Internal authentication failed.");
    }
    return service.processWaffoEvent(event);
  }

  private static boolean constantTimeEquals(String expected, String actual) {
    if (expected == null || actual == null) {
      return false;
    }
    return MessageDigest.isEqual(
        expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
  }
}
