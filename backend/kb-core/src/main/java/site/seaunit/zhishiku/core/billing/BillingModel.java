package site.seaunit.zhishiku.core.billing;

import java.time.Duration;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class BillingModel {
  private BillingModel() {}

  public enum Environment {
    TEST("test"),
    PRODUCTION("production");

    private final String wireValue;

    Environment(String wireValue) {
      this.wireValue = wireValue;
    }

    public String wireValue() {
      return wireValue;
    }

    public static Environment from(String value) {
      return switch (Objects.requireNonNullElse(value, "").strip().toLowerCase()) {
        case "test" -> TEST;
        case "production", "prod" -> PRODUCTION;
        default -> throw new IllegalArgumentException("Unsupported billing environment.");
      };
    }
  }

  public enum Feature {
    CLASSIFY("classify", Unit.ENTRY, 5, "non_empty_classification"),
    AI_TEXT("ai_text", Unit.ENTRY, 5, "non_empty_text_knowledge"),
    WEBPAGE_VISION("webpage_vision", Unit.VIEW, 10, "visible_text_extracted"),
    DOCUMENT_EXTRACT("document_extract", Unit.PER_10K_CHARS, 10, "document_text_extracted"),
    SCREENSHOT_OCR("screenshot_ocr", Unit.IMAGE, 10, "screenshot_text_extracted"),
    INTERNET_SEARCH("internet_search", Unit.QUERY, 3, "search_completed"),
    AI_ANSWER("ai_answer", Unit.ANSWER, 2, "answer_displayed");

    private final String wireValue;
    private final Unit unit;
    private final int defaultPoints;
    private final String successRule;

    Feature(String wireValue, Unit unit, int defaultPoints, String successRule) {
      this.wireValue = wireValue;
      this.unit = unit;
      this.defaultPoints = defaultPoints;
      this.successRule = successRule;
    }

    public String wireValue() {
      return wireValue;
    }

    public Unit unit() {
      return unit;
    }

    public int defaultPoints() {
      return defaultPoints;
    }

    public String successRule() {
      return successRule;
    }

    public static Feature from(String value) {
      for (Feature feature : values()) {
        if (feature.wireValue.equals(value)) {
          return feature;
        }
      }
      throw new IllegalArgumentException("Unsupported billing feature.");
    }
  }

  public enum Unit {
    ENTRY("entry"),
    VIEW("view"),
    PER_10K_CHARS("per_10k_chars"),
    IMAGE("image"),
    QUERY("query"),
    ANSWER("answer");

    private final String wireValue;

    Unit(String wireValue) {
      this.wireValue = wireValue;
    }

    public String wireValue() {
      return wireValue;
    }
  }

  public enum OrderStatus {
    PENDING,
    CONFIRMING,
    PAID,
    CLOSED,
    REFUNDING,
    REFUNDED,
    EXCEPTION
  }

  public enum TaskStatus {
    QUOTED,
    CONFIRMED,
    RESERVED,
    RUNNING,
    SETTLED,
    RELEASED,
    EXPIRED,
    INSUFFICIENT,
    CANCELLED,
    MANUAL_REVIEW
  }

  public enum LedgerOperation {
    TRIAL_GRANT("trial_grant"),
    PURCHASE_GRANT("purchase_grant"),
    RESERVE("reserve"),
    RESERVE_RELEASE("reserve_release"),
    SETTLE("settle"),
    REFUND_RECOVER("refund_recover"),
    MANUAL_ADJUSTMENT("manual_adjustment");

    private final String wireValue;

    LedgerOperation(String wireValue) {
      this.wireValue = wireValue;
    }

    public String wireValue() {
      return wireValue;
    }
  }

  public record PointPack(
      String sku,
      String nameZh,
      String nameEn,
      int priceMinor,
      int points,
      String currency,
      boolean oneTime) {}

  public record AiRate(
      Feature feature,
      Unit unit,
      int pointsPerUnit,
      String successRule,
      String descriptionZh,
      String descriptionEn) {}

  public record QuoteLine(Feature feature, Unit unit, int quantity, int unitPoints, int maxPoints) {}

  public record CatalogSnapshot(
      java.util.UUID versionId,
      String versionCode,
      Environment environment,
      List<PointPack> packs,
      Map<Feature, AiRate> rates) {
    public CatalogSnapshot {
      packs = List.copyOf(packs);
      rates = Map.copyOf(new EnumMap<>(rates));
    }
  }

  public static final Duration QUOTE_TTL = Duration.ofMinutes(15);
  public static final Duration TASK_TTL = Duration.ofMinutes(30);
  public static final int TRIAL_POINTS = 100;

  public static int documentQuantity(int characters) {
    if (characters <= 0) {
      return 1;
    }
    return Math.max(1, (characters + 9_999) / 10_000);
  }

  public static int quotePoints(AiRate rate, int quantity) {
    if (quantity < 1 || quantity > 1000) {
      throw new IllegalArgumentException("Quote quantity is outside the supported range.");
    }
    return Math.multiplyExact(rate.pointsPerUnit(), quantity);
  }

  public static void transitionOrder(OrderStatus from, OrderStatus to) {
    boolean allowed =
        switch (from) {
          case PENDING -> to == OrderStatus.CONFIRMING || to == OrderStatus.CLOSED;
          case CONFIRMING -> to == OrderStatus.PAID || to == OrderStatus.EXCEPTION;
          case PAID -> to == OrderStatus.REFUNDING || to == OrderStatus.EXCEPTION;
          case REFUNDING -> to == OrderStatus.REFUNDED || to == OrderStatus.EXCEPTION;
          case CLOSED -> to == OrderStatus.CONFIRMING;
          case EXCEPTION, REFUNDED -> false;
        };
    if (!allowed) {
      throw new IllegalStateException("Illegal order transition " + from + " -> " + to);
    }
  }

  public static void transitionTask(TaskStatus from, TaskStatus to) {
    boolean allowed =
        switch (from) {
          case QUOTED -> to == TaskStatus.CONFIRMED || to == TaskStatus.EXPIRED || to == TaskStatus.CANCELLED;
          case CONFIRMED -> to == TaskStatus.RESERVED || to == TaskStatus.INSUFFICIENT || to == TaskStatus.EXPIRED;
          case RESERVED -> to == TaskStatus.RUNNING || to == TaskStatus.RELEASED || to == TaskStatus.EXPIRED;
          case RUNNING -> to == TaskStatus.SETTLED
              || to == TaskStatus.RELEASED
              || to == TaskStatus.MANUAL_REVIEW;
          case SETTLED, RELEASED, EXPIRED, INSUFFICIENT, CANCELLED, MANUAL_REVIEW -> false;
        };
    if (!allowed) {
      throw new IllegalStateException("Illegal billing task transition " + from + " -> " + to);
    }
  }
}
