package site.seaunit.zhishiku.idempotency;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.seaunit.zhishiku.owner.CurrentOwnerService;
import site.seaunit.zhishiku.owner.OwnerAccountEntity;

@Service
public class IdempotencyService {
  private static final Duration RETENTION = Duration.ofHours(24);

  private final JdbcTemplate jdbc;
  private final ObjectMapper mapper;
  private final CurrentOwnerService owners;

  public IdempotencyService(JdbcTemplate jdbc, ObjectMapper mapper, CurrentOwnerService owners) {
    this.jdbc = jdbc;
    this.mapper = mapper;
    this.owners = owners;
  }

  @Transactional
  public <T> Optional<T> replay(
      Authentication authentication,
      String operation,
      String idempotencyKey,
      String requestHash,
      Class<T> responseType) {
    OwnerAccountEntity owner = owners.require(authentication);
    String key = requireKey(idempotencyKey);
    Instant now = Instant.now();
    int inserted =
        jdbc.update(
            """
            INSERT INTO api_idempotency
              (owner_id, operation, idempotency_key, request_hash, status,
               response_json, status_code, created_at, expires_at)
            VALUES (?, ?, ?, ?, 'in_progress', NULL, NULL, ?, ?)
            ON CONFLICT (owner_id, operation, idempotency_key) DO NOTHING
            """,
            owner.getId(),
            operation,
            key,
            requestHash,
            Timestamp.from(now),
            Timestamp.from(now.plus(RETENTION)));
    if (inserted == 1) {
      return Optional.empty();
    }
    List<RecordRow> rows =
        jdbc.query(
            """
            SELECT request_hash, status, response_json, expires_at
            FROM api_idempotency
            WHERE owner_id = ? AND operation = ? AND idempotency_key = ?
            """,
            (rs, row) ->
                new RecordRow(
                    rs.getString("request_hash"),
                    rs.getString("status"),
                    rs.getString("response_json"),
                    rs.getTimestamp("expires_at").toInstant()),
            owner.getId(),
            operation,
            key);
    RecordRow row =
        rows.stream()
            .findFirst()
            .orElseThrow(
                () ->
                    new IdempotencyException(
                        "IDEMPOTENCY_UNAVAILABLE", "The idempotency record could not be read."));
    if (now.isAfter(row.expiresAt())) {
      jdbc.update(
          "DELETE FROM api_idempotency WHERE owner_id = ? AND operation = ? AND idempotency_key = ?",
          owner.getId(),
          operation,
          key);
      return replay(authentication, operation, key, requestHash, responseType);
    }
    if (!row.requestHash().equals(requestHash)) {
      throw new IdempotencyException(
          "IDEMPOTENCY_CONFLICT", "The idempotency key was already used for a different request.");
    }
    if (!"completed".equals(row.status())) {
      throw new IdempotencyException(
          "REQUEST_IN_PROGRESS", "An identical request is already being processed.");
    }
    try {
      return Optional.of(mapper.readValue(row.responseJson(), responseType));
    } catch (JsonProcessingException error) {
      throw new IllegalStateException("Stored idempotent response is invalid.", error);
    }
  }

  @Transactional
  public void record(
      Authentication authentication,
      String operation,
      String idempotencyKey,
      String requestHash,
      Object response,
      int statusCode) {
    OwnerAccountEntity owner = owners.require(authentication);
    String key = requireKey(idempotencyKey);
    String responseJson;
    try {
      responseJson = mapper.writeValueAsString(response);
    } catch (JsonProcessingException error) {
      throw new IllegalStateException("Response could not be stored for idempotent replay.", error);
    }
    int updated =
        jdbc.update(
            """
            UPDATE api_idempotency
            SET status = 'completed', response_json = ?::jsonb, status_code = ?
            WHERE owner_id = ? AND operation = ? AND idempotency_key = ?
              AND request_hash = ?
            """,
            responseJson,
            statusCode,
            owner.getId(),
            operation,
            key,
            requestHash);
    if (updated != 1) {
      throw new IllegalStateException("Idempotency record could not be completed.");
    }
  }

  public static String hash(String value) {
    return hashBytes(value.getBytes(StandardCharsets.UTF_8));
  }

  public static String hashBytes(byte[] value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value == null ? new byte[0] : value));
    } catch (NoSuchAlgorithmException error) {
      throw new IllegalStateException("SHA-256 is unavailable.", error);
    }
  }

  private static String requireKey(String value) {
    String key = value == null ? "" : value.strip();
    if (key.length() < 16 || key.length() > 128) {
      throw new IdempotencyException(
          "IDEMPOTENCY_KEY_INVALID", "A 16 to 128 character Idempotency-Key header is required.");
    }
    return key;
  }

  private record RecordRow(
      String requestHash, String status, String responseJson, Instant expiresAt) {}
}
