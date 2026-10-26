package site.seaunit.zhishiku.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class AdminAccountDtos {
  private AdminAccountDtos() {}

  public record AdminUser(
      UUID ownerId,
      String loginName,
      String email,
      String displayName,
      String role,
      String status,
      Instant createdAt,
      long knowledgeCount,
      int availablePoints,
      int reservedPoints,
      int lifetimeGranted,
      int lifetimeConsumed) {}

  public record AdminUserPage(
      List<AdminUser> items,
      int page,
      int limit,
      long totalItems,
      int totalPages,
      long enabledCount,
      long disabledCount,
      long totalConsumed) {}

  public record UpdateAccountStatusRequest(
      boolean enabled, @NotBlank @Size(max = 300) String reason) {}

  public record GrantPointsRequest(
      @Min(1) @Max(100000) int points, @NotBlank @Size(max = 300) String reason) {}
}
