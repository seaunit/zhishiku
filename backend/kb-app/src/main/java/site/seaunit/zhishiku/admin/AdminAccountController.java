package site.seaunit.zhishiku.admin;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import site.seaunit.zhishiku.admin.AdminAccountDtos.AdminUser;
import site.seaunit.zhishiku.admin.AdminAccountDtos.AdminUserPage;
import site.seaunit.zhishiku.admin.AdminAccountDtos.GrantPointsRequest;
import site.seaunit.zhishiku.admin.AdminAccountDtos.UpdateAccountStatusRequest;

@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminAccountController {
  private final AdminAccountService service;

  public AdminAccountController(AdminAccountService service) {
    this.service = service;
  }

  @GetMapping
  AdminUserPage list(
      Authentication authentication,
      @RequestParam(defaultValue = "") String query,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int limit,
      @RequestParam(defaultValue = "all") String status) {
    return service.list(authentication, query, page, limit, status);
  }

  @GetMapping("/{ownerId}")
  AdminUser get(Authentication authentication, @PathVariable UUID ownerId) {
    return service.get(authentication, ownerId);
  }

  @PatchMapping("/{ownerId}/status")
  AdminUser updateStatus(
      Authentication authentication,
      @PathVariable UUID ownerId,
      @Valid @RequestBody UpdateAccountStatusRequest request,
      @RequestHeader("Idempotency-Key") String idempotencyKey) {
    return service.updateStatus(authentication, ownerId, request, idempotencyKey);
  }

  @PostMapping("/{ownerId}/point-grants")
  @ResponseStatus(HttpStatus.CREATED)
  AdminUser grantPoints(
      Authentication authentication,
      @PathVariable UUID ownerId,
      @Valid @RequestBody GrantPointsRequest request,
      @RequestHeader("Idempotency-Key") String idempotencyKey) {
    return service.grantPoints(authentication, ownerId, request, idempotencyKey);
  }
}
