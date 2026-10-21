package site.seaunit.zhishiku.richtext.migration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class LegacyTextDocumentConverter {
  private static final Pattern LINK = Pattern.compile("https?://[^\\s<>\"']+");
  private static final Set<Character> TRAILING_PUNCTUATION =
      Set.of('.', ',', ';', ':', '!', '?', ')', ']', '}', '。', '，', '；', '：', '！', '？', '）', '】');

  private final ObjectMapper mapper = new ObjectMapper();

  public Conversion convert(String rawText) {
    String plainText = normalize(rawText);
    ObjectNode root = mapper.createObjectNode();
    root.put("type", "doc");
    ArrayNode content = root.putArray("content");
    String[] paragraphs = plainText.split("\\n{2,}", -1);
    for (String paragraphText : paragraphs) {
      ObjectNode paragraph = content.addObject();
      paragraph.put("type", "paragraph");
      ArrayNode inline = paragraph.putArray("content");
      String[] lines = paragraphText.split("\\n", -1);
      for (int index = 0; index < lines.length; index++) {
        appendInline(inline, lines[index]);
        if (index < lines.length - 1) {
          inline.addObject().put("type", "hardBreak");
        }
      }
    }
    return new Conversion(root, plainText, sha256(plainText));
  }

  private static String normalize(String rawText) {
    if (rawText == null) {
      return "";
    }
    return rawText.replace("\r\n", "\n").replace('\r', '\n').replace("\u0000", "");
  }

  private static void appendInline(ArrayNode inline, String line) {
    Matcher matcher = LINK.matcher(line);
    int cursor = 0;
    while (matcher.find()) {
      String candidate = matcher.group();
      int end = matcher.end();
      while (!candidate.isEmpty()
          && TRAILING_PUNCTUATION.contains(candidate.charAt(candidate.length() - 1))) {
        candidate = candidate.substring(0, candidate.length() - 1);
        end--;
      }
      if (matcher.start() > cursor) {
        inline.addObject().put("type", "text").put("text", line.substring(cursor, matcher.start()));
      }
      ObjectNode link = inline.addObject();
      link.put("type", "text");
      link.put("text", candidate);
      ArrayNode marks = link.putArray("marks");
      ObjectNode mark = marks.addObject();
      mark.put("type", "link");
      mark.putObject("attrs").put("href", candidate);
      cursor = end;
    }
    if (cursor < line.length()) {
      inline.addObject().put("type", "text").put("text", line.substring(cursor));
    }
  }

  private static String sha256(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException error) {
      throw new IllegalStateException("SHA-256 is unavailable.", error);
    }
  }

  public record Conversion(JsonNode document, String plainText, String sourceHash) {}
}
