package site.seaunit.zhishiku.webimport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import site.seaunit.zhishiku.config.AppProperties;

@Component
public class DeepSeekKnowledgeOrganizer
    implements WebPageOrganizer,
        DocumentOrganizer,
        ScreenshotOrganizer,
        TextOrganizer,
        InternetResultOrganizer {
  private static final Pattern URL_TOKEN = Pattern.compile("(?i)(?:[a-z][a-z0-9+.-]*://|//)\\S+");
  private static final Pattern BARE_DOMAIN_TOKEN =
      Pattern.compile("(?i)(?:[a-z0-9-]+\\.)+[a-z]{2,}(?:/[^\\s)]*)?");
  private static final Pattern MARKDOWN_LINK = Pattern.compile("!?\\[[^\\]]*\\]\\([^)]*\\)");
  private static final String SYSTEM_PROMPT =
      """
      You convert one {{SOURCE_KIND}} into self-contained knowledge notes.
      Return JSON only: {"items":[{"title":"...","body":"...","summary":"...","category":"...","tags":["..."]}]}.
      Create between 1 and %d notes. Split only when the source contains distinct topics.
      Preserve factual meaning and the source language. Do not invent details.
      Body must be a polished Markdown subset: ##/### headings, short paragraphs, bullet or numbered
      lists, > blockquotes and fenced code where useful. Do not use HTML, scripts, tables, remote
      image syntax, Markdown image URLs, or URLs-only filler. Write for readable study notes, not a
      chat summary.
      """;

  private final AppProperties.Ai config;
  private final int maxItems;
  private final ObjectMapper mapper;
  private final RestClient client;

  public DeepSeekKnowledgeOrganizer(
      AppProperties properties, ObjectMapper mapper, RestClient.Builder builder) {
    this.config = properties.ai();
    this.maxItems = Math.max(1, properties.webImport().maxItems());
    this.mapper = mapper;
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    int timeout = Math.max(10, config.timeoutSeconds());
    requestFactory.setConnectTimeout(Duration.ofSeconds(timeout));
    requestFactory.setReadTimeout(Duration.ofSeconds(timeout));
    this.client = builder.requestFactory(requestFactory).build();
  }

  @Override
  public List<OrganizedKnowledge> organize(String sourceLabel, String text) {
    if (config.apiKey() == null || config.apiKey().isBlank()) {
      throw new WebImportUnavailableException("AI page organization is not configured.");
    }
    Map<String, Object> request = new LinkedHashMap<>();
    request.put("model", config.model());
    request.put(
        "messages",
        List.of(
            Map.of(
                "role",
                "system",
                "content",
                SYSTEM_PROMPT.replace("{{SOURCE_KIND}}", sourceLabel).formatted(maxItems)),
            Map.of("role", "user", "content", "Extracted source text:\n" + text)));
    request.put("response_format", Map.of("type", "json_object"));
    request.put("temperature", 0.2);
    request.put("max_tokens", 8000);

    try {
      JsonNode response =
          client
              .post()
              .uri(config.endpoint())
              .header(HttpHeaders.AUTHORIZATION, "Bearer " + config.apiKey())
              .contentType(MediaType.APPLICATION_JSON)
              .accept(MediaType.APPLICATION_JSON)
              .body(request)
              .retrieve()
              .body(JsonNode.class);
      if (response == null) {
        throw new WebImportUnavailableException("AI page organization returned no response.");
      }
      String content = response.path("choices").path(0).path("message").path("content").asText("");
      if (content.isBlank()) {
        throw new WebPageOrganizationException("AI page organization returned no knowledge.");
      }
      return parse(content);
    } catch (WebImportUnavailableException error) {
      throw error;
    } catch (WebPageOrganizationException error) {
      throw error;
    } catch (RuntimeException error) {
      throw new WebImportUnavailableException("AI page organization is temporarily unavailable.");
    }
  }

  private List<OrganizedKnowledge> parse(String content) {
    try {
      String json = content.strip();
      if (json.startsWith("```")) {
        int firstLine = json.indexOf('\n');
        int lastFence = json.lastIndexOf("```");
        json =
            firstLine >= 0 && lastFence > firstLine ? json.substring(firstLine + 1, lastFence) : "";
      }
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
