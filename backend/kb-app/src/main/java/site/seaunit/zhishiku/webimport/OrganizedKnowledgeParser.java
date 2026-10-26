package site.seaunit.zhishiku.webimport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Pattern;

final class OrganizedKnowledgeParser {
  private static final Pattern REASONING_BLOCK =
      Pattern.compile("(?is)^\\s*<think>.*?</think>\\s*");
  private static final Pattern URL_TOKEN = Pattern.compile("(?i)(?:[a-z][a-z0-9+.-]*://|//)\\S+");
  private static final Pattern BARE_DOMAIN_TOKEN =
      Pattern.compile("(?i)(?:[a-z0-9-]+\\.)+[a-z]{2,}(?:/[^\\s)]*)?");
  private static final Pattern MARKDOWN_LINK = Pattern.compile("!?\\[[^\\]]*\\]\\([^)]*\\)");

  private OrganizedKnowledgeParser() {}

  static List<OrganizedKnowledge> parse(ObjectMapper mapper, int maxItems, String content) {
    try {
      String json = stripCodeFence(content);
      JsonNode items = mapper.readTree(json).path("items");
      if (!items.isArray()) {
        throw new WebPageOrganizationException("AI page organization returned an invalid result.");
      }
      LinkedHashSet<String> seenTitles = new LinkedHashSet<>();
      List<OrganizedKnowledge> result = new ArrayList<>();
      for (JsonNode item : items) {
        String title = safeText(item.path("title").asText(""), 300);
        String body = safeText(item.path("body").asText(""), 10_000);
        if (title.isBlank()
            || body.isBlank()
            || !hasNonUrlText(body)
            || !seenTitles.add(title.toLowerCase())) {
          continue;
        }
        String summary = safeText(item.path("summary").asText(""), 500);
        String category = safeText(item.path("category").asText(""), 100);
        List<String> tags = new ArrayList<>();
        for (JsonNode tag : item.path("tags")) {
          String value = safeText(tag.asText(""), 50);
          if (!value.isBlank() && !tags.contains(value) && tags.size() < 8) {
            tags.add(value);
          }
        }
        if (summary.isBlank() || category.isBlank() || tags.isEmpty()) {
          throw new WebPageOrganizationException(
              "AI page organization returned incomplete knowledge metadata.");
        }
        result.add(new OrganizedKnowledge(title, body, summary, category, tags));
        if (result.size() >= maxItems) {
          break;
        }
      }
      if (result.isEmpty()) {
        throw new WebPageOrganizationException(
            "AI page organization returned no usable knowledge.");
      }
      return result;
    } catch (WebPageOrganizationException error) {
      throw error;
    } catch (Exception error) {
      throw new WebPageOrganizationException("AI page organization returned malformed JSON.");
    }
  }

  private static String stripCodeFence(String content) {
    String json = content == null ? "" : content.strip();
    json = REASONING_BLOCK.matcher(json).replaceFirst("").strip();
    if (!json.startsWith("```")) {
      return json;
    }
    int firstLine = json.indexOf('\n');
    int lastFence = json.lastIndexOf("```");
    return firstLine >= 0 && lastFence > firstLine
        ? json.substring(firstLine + 1, lastFence).strip()
        : "";
  }

  private static String safeText(String value, int maxLength) {
    String normalized =
        value
            .replaceAll("[\\p{Cc}&&[^\\n\\t]]", " ")
            .replaceAll("[\\t ]+", " ")
            .replaceAll("\\n{3,}", "\n\n")
            .strip();
    return normalized.substring(0, Math.min(normalized.length(), maxLength));
  }

  private static boolean hasNonUrlText(String value) {
    String withoutMarkdown = MARKDOWN_LINK.matcher(value).replaceAll(" ");
    String withoutProtocolUrls = URL_TOKEN.matcher(withoutMarkdown).replaceAll(" ");
    String withoutUrls = BARE_DOMAIN_TOKEN.matcher(withoutProtocolUrls).replaceAll(" ").strip();
    return withoutUrls.codePoints().filter(Character::isLetterOrDigit).count() >= 8;
  }
}
