package site.seaunit.zhishiku.richtext.provenance;

import java.util.List;

public record RichTextConversionReport(
    List<String> preservedFeatures,
    List<String> downgradedFeatures,
    List<String> removedFeatures,
    List<String> rejectedFeatures,
    String converterVersion) {
  public RichTextConversionReport {
    preservedFeatures = List.copyOf(preservedFeatures);
    downgradedFeatures = List.copyOf(downgradedFeatures);
    removedFeatures = List.copyOf(removedFeatures);
    rejectedFeatures = List.copyOf(rejectedFeatures);
  }
}
