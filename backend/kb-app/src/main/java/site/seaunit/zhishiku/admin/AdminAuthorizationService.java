package site.seaunit.zhishiku.admin;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import site.seaunit.zhishiku.owner.CurrentOwnerService;
import site.seaunit.zhishiku.owner.OwnerAccountEntity;

@Service
public class AdminAuthorizationService {
  private final CurrentOwnerService owners;

  public AdminAuthorizationService(CurrentOwnerService owners) {
    this.owners = owners;
  }

  public OwnerAccountEntity require(Authentication authentication) {
    OwnerAccountEntity owner = owners.require(authentication);
    if (!isConfiguredAdmin(owner)) {
      throw new AdminAccountException(
          HttpStatus.FORBIDDEN, "ADMIN_REQUIRED", "Administrator access is required.");
    }
    return owner;
  }

  public boolean isConfiguredAdmin(OwnerAccountEntity owner) {
    return owner.isAdmin()
        && owner.getUsername() != null
        && OwnerAccountEntity.ADMIN_USERNAME.equalsIgnoreCase(owner.getUsername());
  }
}
