package site.seaunit.zhishiku.billing;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import site.seaunit.zhishiku.billing.BillingDtos.CreditAccountResponse;
import site.seaunit.zhishiku.billing.BillingDtos.FinanceSummaryResponse;
import site.seaunit.zhishiku.billing.BillingDtos.ManualAdjustmentRequest;
import site.seaunit.zhishiku.billing.BillingDtos.RefundCaseResponse;
import site.seaunit.zhishiku.billing.BillingDtos.RefundResolutionRequest;

@RestController
@RequestMapping("/api/v1/admin/billing")
public class BillingAdminController {
  private final BillingService service;

  public BillingAdminController(BillingService service) {
    this.service = service;
  }

  @GetMapping("/summary")
  FinanceSummaryResponse summary(
      Authentication authentication, @RequestParam LocalDate from, @RequestParam LocalDate to) {
    return service.financeSummary(authentication, from, to);
  }

  @PostMapping("/refunds/{refundId}/resolve")
  RefundCaseResponse resolveRefund(
      Authentication authentication,
      @PathVariable UUID refundId,
      @Valid @RequestBody RefundResolutionRequest request) {
    return service.resolveRefund(authentication, refundId, request);
  }

  @PostMapping("/adjustments")
  CreditAccountResponse adjustment(
      Authentication authentication, @Valid @RequestBody ManualAdjustmentRequest request) {
    return service.manualAdjustment(authentication, request);
  }
}
