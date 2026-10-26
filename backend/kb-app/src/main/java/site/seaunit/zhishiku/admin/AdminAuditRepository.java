package site.seaunit.zhishiku.admin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AdminAuditRepository {
  private final JdbcTemplate jdbc;
  private final ObjectMapper mapper;
  private final Clock clock;

  public AdminAuditRepository(JdbcTemplate jdbc, ObjectMapper mapper, Clock clock) {
    this.jdbc = jdbc;
    this.mapper = mapper;
    this.clock = clock;
  }

  public void append(
      UUID actorOwnerId,
      UUID targetOwnerId,
      String action,
      String reason,
      Map<String, ?> previousState,
      Map<String, ?> nextState,
      String idempotencyKey) {
    jdbc.update(
        """
        INSERT INTO admin_audit_event
          (id, actor_owner_id, target_owner_id, action, reason_safe,
           previous_state, next_state, idempotency_key, created_at)
        VALUES (?, ?, ?, ?, ?, ?::jsonb, ?::jsonb, ?, ?)
        """,
        UUID.randomUUID(),
        actorOwnerId,
        targetOwnerId,
        action,
        reason.strip(),
        json(previousState),
        json(nextState),
        idempotencyKey.strip(),
        Timestamp.from(clock.instant()));
  }

  private String json(Map<String, ?> value) {
    try {
      return mapper.writeValueAsString(value);
    } catch (JsonProcessingException error) {
      throw new IllegalStateException("Administrator audit state could not be serialized.", error);
    }
  }
}
