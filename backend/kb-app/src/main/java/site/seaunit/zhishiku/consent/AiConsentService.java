package site.seaunit.zhishiku.consent;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.seaunit.zhishiku.consent.AiConsentDtos.AiConsentResponse;
import site.seaunit.zhishiku.consent.AiConsentDtos.UpdateAiConsentRequest;
import site.seaunit.zhishiku.owner.CurrentOwnerService;
import site.seaunit.zhishiku.owner.OwnerAccountEntity;

@Service
public class AiConsentService {
  private static final Map<String, ScopeDefinition> SCOPES = definitions();

  private final JdbcTemplate jdbc;
  private final CurrentOwnerService owners;

  public AiConsentService(JdbcTemplate jdbc, CurrentOwnerService owners) {
    this.jdbc = jdbc;
    this.owners = owners;
  }

  @Transactional(readOnly = true)
  public List<AiConsentResponse> list(Authentication authentication) {
    OwnerAccountEntity owner = owners.require(authentication);
    Map<String, AiConsentResponse> saved =
        jdbc.query(
            """
            SELECT scope, service, content_type, granted, consent_version, updated_at
            FROM ai_processing_consent
            WHERE owner_id = ?
            """,
            result -> {
              Map<String, AiConsentResponse> values = new LinkedHashMap<>();
              while (result.next()) {
                values.put(
                    result.getString("scope"),
                    new AiConsentResponse(
                        result.getString("scope"),
                        result.getString("service"),
                        result.getString("content_type"),
                        result.getBoolean("granted"),
                        result.getString("consent_version"),
                        result.getTimestamp("updated_at").toInstant()));
              }
              return values;
            },
            owner.getId());
    return SCOPES.entrySet().stream()
        .map(
            entry ->
                saved.getOrDefault(
                    entry.getKey(),
                    new AiConsentResponse(
                        entry.getKey(),
                        entry.getValue().service(),
                        entry.getValue().contentType(),
                        false,
                        entry.getValue().defaultVersion(),
                        null)))
        .toList();
  }

  @Transactional
  public AiConsentResponse update(
      Authentication authentication, String requestedScope, UpdateAiConsentRequest request) {
    OwnerAccountEntity owner = owners.require(authentication);
    String scope = requestedScope.strip().toLowerCase(Locale.ROOT);
    ScopeDefinition definition = SCOPES.get(scope);
    if (definition == null) {
      throw new IllegalArgumentException("AI consent scope is invalid.");
    }
    String version = request.consentVersion().strip();
    Instant now = Instant.now();
    Timestamp timestamp = Timestamp.from(now);
    jdbc.update(
        """
        INSERT INTO ai_processing_consent
          (owner_id, scope, service, content_type, granted, consent_version,
           granted_at, revoked_at, updated_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT (owner_id, scope) DO UPDATE SET
          service = EXCLUDED.service,
          content_type = EXCLUDED.content_type,
          granted = EXCLUDED.granted,
          consent_version = EXCLUDED.consent_version,
          granted_at = EXCLUDED.granted_at,
          revoked_at = EXCLUDED.revoked_at,
          updated_at = EXCLUDED.updated_at
        """,
        owner.getId(),
        scope,
        definition.service(),
        definition.contentType(),
        request.granted(),
        version,
        request.granted() ? timestamp : null,
        request.granted() ? null : timestamp,
        timestamp);
    return new AiConsentResponse(
        scope, definition.service(), definition.contentType(), request.granted(), version, now);
  }

  @Transactional(readOnly = true)
  public boolean granted(Authentication authentication, String scope) {
    OwnerAccountEntity owner = owners.require(authentication);
    Boolean granted =
        jdbc
            .query(
                "SELECT granted FROM ai_processing_consent WHERE owner_id = ? AND scope = ?",
                (rs, row) -> rs.getBoolean(1),
                owner.getId(),
                scope)
            .stream()
            .findFirst()
            .orElse(false);
    return Boolean.TRUE.equals(granted);
  }

  private static Map<String, ScopeDefinition> definitions() {
    Map<String, ScopeDefinition> values = new LinkedHashMap<>();
    values.put("text", new ScopeDefinition("deepseek:chat", "text", "ai-consent-v1"));
    values.put(
        "webpage", new ScopeDefinition("deepseek:chat", "webpage_screenshot", "ai-consent-v1"));
    values.put("document", new ScopeDefinition("deepseek:chat", "document_text", "ai-consent-v1"));
    values.put(
        "screenshot", new ScopeDefinition("deepseek:chat", "screenshot_ocr", "ai-consent-v1"));
    values.put("search", new ScopeDefinition("deepseek:search", "search_query", "ai-consent-v1"));
    return Map.copyOf(values);
  }

  private record ScopeDefinition(String service, String contentType, String defaultVersion) {}
}
