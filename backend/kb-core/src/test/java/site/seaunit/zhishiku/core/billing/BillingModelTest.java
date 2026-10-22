package site.seaunit.zhishiku.core.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.EnumMap;
import java.util.List;
import org.junit.jupiter.api.Test;
import site.seaunit.zhishiku.core.billing.BillingModel.AiRate;
import site.seaunit.zhishiku.core.billing.BillingModel.Feature;
import site.seaunit.zhishiku.core.billing.BillingModel.OrderStatus;
import site.seaunit.zhishiku.core.billing.BillingModel.TaskStatus;

class BillingModelTest {
  @Test
  void internetSearchDefaultRateMatchesPublishedPrice() {
    assertThat(Feature.INTERNET_SEARCH.defaultPoints()).isEqualTo(1);
  }

  @Test
  void roundsDocumentCharactersUpToTenThousandCharacterUnits() {
    assertThat(BillingModel.documentQuantity(0)).isEqualTo(1);
    assertThat(BillingModel.documentQuantity(1)).isEqualTo(1);
    assertThat(BillingModel.documentQuantity(10_000)).isEqualTo(1);
    assertThat(BillingModel.documentQuantity(10_001)).isEqualTo(2);
  }

  @Test
  void calculatesQuoteLinesAndRejectsImpossibleQuantities() {
    AiRate rate =
        new AiRate(Feature.SCREENSHOT_OCR, BillingModel.Unit.IMAGE, 10, "text", "截图", "Image");
    assertThat(BillingModel.quotePoints(rate, 3)).isEqualTo(30);
    assertThatThrownBy(() -> BillingModel.quotePoints(rate, 0))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void enforcesOrderAndTaskTerminalStates() {
    BillingModel.transitionOrder(OrderStatus.PENDING, OrderStatus.CONFIRMING);
    BillingModel.transitionOrder(OrderStatus.CONFIRMING, OrderStatus.PAID);
    assertThatThrownBy(() -> BillingModel.transitionOrder(OrderStatus.PAID, OrderStatus.PENDING))
        .isInstanceOf(IllegalStateException.class);

    BillingModel.transitionTask(TaskStatus.QUOTED, TaskStatus.CONFIRMED);
    BillingModel.transitionTask(TaskStatus.CONFIRMED, TaskStatus.RESERVED);
    BillingModel.transitionTask(TaskStatus.RESERVED, TaskStatus.RUNNING);
    BillingModel.transitionTask(TaskStatus.RUNNING, TaskStatus.SETTLED);
    assertThatThrownBy(() -> BillingModel.transitionTask(TaskStatus.SETTLED, TaskStatus.RUNNING))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void catalogCopiesMutableCollections() {
    var rates = new EnumMap<Feature, AiRate>(Feature.class);
    rates.put(
        Feature.INTERNET_SEARCH,
        new AiRate(Feature.INTERNET_SEARCH, BillingModel.Unit.QUERY, 3, "query", "搜索", "Search"));
    BillingModel.CatalogSnapshot catalog =
        new BillingModel.CatalogSnapshot(
            java.util.UUID.randomUUID(), "v1", BillingModel.Environment.TEST, List.of(), rates);
    rates.clear();
    assertThat(catalog.rates()).containsKey(Feature.INTERNET_SEARCH);
  }
}
