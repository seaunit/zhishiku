package site.seaunit.zhishiku.search.richtext;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Objects;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import site.seaunit.zhishiku.core.richtext.PlainTextProjector;

@Component
public class SearchProjectionBuilder {
  private static final Pattern CONTROL = Pattern.compile("[\\p{Cc}&&[^\\n\\t]]");

  private final PlainTextProjector projector;

  public SearchProjectionBuilder(com.fasterxml.jackson.databind.ObjectMapper ignoredMapper) {
    this.projector = new PlainTextProjector();
    Objects.requireNonNull(ignoredMapper, "objectMapper");
  }

  public SearchProjection project(String title, JsonNode document, String note) {
    String titleText = clean(title);
    String bodyText = clean(projector.project(document));
    String noteText = clean(note);
    String imageText = "";
    String searchText =
        String.join(
                "\n\n",
                java.util.stream.Stream.of(titleText, bodyText, noteText)
                    .filter(value -> !value.isBlank())
                    .toList())
            .strip();
    return new SearchProjection(titleText, bodyText, noteText, imageText, searchText, 0);
  }

  public SearchProjection withRevision(SearchProjection projection, long searchRevision) {
    return new SearchProjection(
        projection.titleText(),
        projection.bodyText(),
        projection.noteText(),
        projection.imageText(),
        projection.searchText(),
        searchRevision);
  }

  private static String clean(String value) {
    if (value == null) {
      return "";
    }
    return CONTROL.matcher(value.toString()).replaceAll("").strip();
  }
}
