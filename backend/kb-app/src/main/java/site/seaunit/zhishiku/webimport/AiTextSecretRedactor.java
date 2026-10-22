package site.seaunit.zhishiku.webimport;

import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class AiTextSecretRedactor {
  private static final String REDACTED = "[REDACTED]";
  private static final List<Replacement> REPLACEMENTS =
      List.of(
          new Replacement(
              Pattern.compile(
                  "(?i)(\\b[A-Za-z0-9_-]*(?:api[_-]?key|token|secret|password|passwd|pwd)"
                      + "\\b\\s*[:=]\\s*)([^\\s,;]{6,})"),
              "$1" + REDACTED),
          new Replacement(
              Pattern.compile(
                  "((?:登录)?密码|访问令牌|令牌|接口密钥|API密钥|私钥|密钥)" + "\\s*[:：=]\\s*([^\\s,;，；]{4,})"),
              "$1：" + REDACTED),
          new Replacement(
              Pattern.compile(
                  "(?i)(\\bAuthorization\\s*:\\s*(?:Basic|Bearer)\\s+)"
                      + "([A-Za-z0-9._~+/=-]{8,})"),
              "$1" + REDACTED),
          new Replacement(
              Pattern.compile("(?i)(\\bBearer\\s+)([A-Za-z0-9._~+/=-]{20,})"), "$1" + REDACTED),
          new Replacement(Pattern.compile("\\bsk-[A-Za-z0-9_-]{20,}\\b"), REDACTED),
          new Replacement(Pattern.compile("\\bAKIA[0-9A-Z]{16}\\b"), REDACTED),
          new Replacement(Pattern.compile("\\bAIza[0-9A-Za-z_-]{30,}\\b"), REDACTED),
          new Replacement(Pattern.compile("\\bgh[pousr]_[A-Za-z0-9]{20,}\\b"), REDACTED),
          new Replacement(Pattern.compile("\\bxox[baprs]-[A-Za-z0-9-]{20,}\\b"), REDACTED),
          new Replacement(
              Pattern.compile(
                  "-----BEGIN [A-Z ]*PRIVATE KEY-----[\\s\\S]*?"
                      + "-----END [A-Z ]*PRIVATE KEY-----"),
              REDACTED));

  public RedactionResult redact(String text) {
    String redacted = text == null ? "" : text;
    for (Replacement replacement : REPLACEMENTS) {
      redacted = replacement.pattern().matcher(redacted).replaceAll(replacement.value());
    }
    return new RedactionResult(redacted, !redacted.equals(text));
  }

  public record RedactionResult(String text, boolean applied) {}

  private record Replacement(Pattern pattern, String value) {}
}
