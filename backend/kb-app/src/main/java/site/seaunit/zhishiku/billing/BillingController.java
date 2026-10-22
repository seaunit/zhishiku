package site.seaunit.zhishiku.billing;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import site.seaunit.zhishiku.billing.BillingDtos.AutoChargePolicyResponse;
import site.seaunit.zhishiku.billing.BillingDtos.BillingTaskResponse;
import site.seaunit.zhishiku.billing.BillingDtos.CatalogResponse;
import site.seaunit.zhishiku.billing.BillingDtos.CheckoutSessionResponse;
import site.seaunit.zhishiku.billing.BillingDtos.ConfirmQuoteRequest;
import site.seaunit.zhishiku.billing.BillingDtos.CreateOrderRequest;
import site.seaunit.zhishiku.billing.BillingDtos.CreateQuoteRequest;
import site.seaunit.zhishiku.billing.BillingDtos.CreditAccountResponse;
import site.seaunit.zhishiku.billing.BillingDtos.LedgerPage;
import site.seaunit.zhishiku.billing.BillingDtos.OrderPage;
import site.seaunit.zhishiku.billing.BillingDtos.PaymentOrderResponse;
import site.seaunit.zhishiku.billing.BillingDtos.PreflightRequest;
import site.seaunit.zhishiku.billing.BillingDtos.PreflightResponse;
import site.seaunit.zhishiku.billing.BillingDtos.QuoteResponse;
import site.seaunit.zhishiku.billing.BillingDtos.RefundCaseResponse;
import site.seaunit.zhishiku.billing.BillingDtos.UpdateAutoChargePolicyRequest;

@RestController
@RequestMapping("/api/v1/billing")
public class BillingController {
  private final BillingService service;

  public BillingController(BillingService service) {
    this.service = service;
  }

  @GetMapping("/catalog")
  CatalogResponse catalog() {
    return service.catalog();
  }

  @GetMapping("/account")
  CreditAccountResponse account(Authentication authentication) {
    return service.account(authentication);
  }

  @GetMapping("/ledger")
  LedgerPage ledger(
      Authentication authentication,
      @RequestParam(defaultValue = "20") int limit,
      @RequestParam(required = false) String cursor) {
    return service.ledger(authentication, limit, cursor);
  }

  @GetMapping("/orders")
  OrderPage orders(
      Authentication authentication,
      @RequestParam(defaultValue = "20") int limit,
      @RequestParam(required = false) String cursor) {
    return service.orders(authentication, limit, cursor);
  }

  @PostMapping("/orders")
  @ResponseStatus(HttpStatus.CREATED)
  PaymentOrderResponse createOrder(
      Authentication authentication,
      @Valid @RequestBody CreateOrderRequest request,
      @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey) {
    return service.createOrder(authentication, request, idempotencyKey);
  }

  @GetMapping("/orders/{orderId}")
  PaymentOrderResponse order(Authentication authentication, @PathVariable UUID orderId) {
    return service.getOrder(authentication, orderId);
  }

  @PostMapping("/orders/{orderId}/checkout")
  CheckoutSessionResponse checkout(Authentication authentication, @PathVariable UUID orderId) {
    return service.checkout(authentication, orderId);
  }

  @GetMapping("/auto-charge")
  AutoChargePolicyResponse autoCharge(Authentication authentication) {
    return service.autoCharge(authentication);
  }

  @PatchMapping("/auto-charge")
  AutoChargePolicyResponse updateAutoCharge(
      Authentication authentication, @Valid @RequestBody UpdateAutoChargePolicyRequest request) {
    return service.updateAutoCharge(authentication, request);
  }

  @PostMapping("/preflights")
  @ResponseStatus(HttpStatus.CREATED)
  PreflightResponse preflight(
      Authentication authentication, @Valid @RequestBody PreflightRequest request) {
    return service.preflight(authentication, request);
  }

  @PostMapping("/quotes")
  @ResponseStatus(HttpStatus.CREATED)
  QuoteResponse quote(
      Authentication authentication, @Valid @RequestBody CreateQuoteRequest request) {
    return service.createQuote(authentication, request);
  }

  @PostMapping("/quotes/{quoteId}/confirm")
  BillingTaskResponse confirm(
      Authentication authentication,
      @PathVariable UUID quoteId,
      @Valid @RequestBody ConfirmQuoteRequest request) {
    return service.confirmQuote(authentication, quoteId, request);
  }

  @GetMapping("/tasks/{taskId}")
  BillingTaskResponse task(Authentication authentication, @PathVariable UUID taskId) {
    return service.getTask(authentication, taskId);
  }

  @PostMapping("/tasks/{taskId}/cancel")
  BillingTaskResponse cancel(Authentication authentication, @PathVariable UUID taskId) {
    return service.cancelTask(authentication, taskId);
  }

  @GetMapping("/refunds")
  List<RefundCaseResponse> refunds(Authentication authentication) {
    return service.refunds(authentication);
  }

  @GetMapping("/refunds/{refundId}")
  RefundCaseResponse refund(Authentication authentication, @PathVariable UUID refundId) {
    return service.refund(authentication, refundId);
  }
}
