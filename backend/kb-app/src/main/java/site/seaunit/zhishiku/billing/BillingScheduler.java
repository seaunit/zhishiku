package site.seaunit.zhishiku.billing;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.billing", name = "enabled", havingValue = "true")
public class BillingScheduler {
  private final BillingService service;

  public BillingScheduler(BillingService service) {
    this.service = service;
  }

  @Scheduled(fixedDelayString = "${app.billing.expire-scan-millis:60000}")
  public void expireReservations() {
    service.expireReservations();
  }

  @Scheduled(fixedDelayString = "${app.billing.reconcile-scan-millis:60000}")
  public void reconcileOrders() {
    service.reconcilePendingOrders();
  }
}
