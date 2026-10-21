package site.seaunit.zhishiku.owner;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class CurrentOwnerService {
  private final OwnerService ownerService;

  public CurrentOwnerService(OwnerService ownerService) {
    this.ownerService = ownerService;
  }

  public OwnerAccountEntity require(Authentication authentication) {
    if (authentication == null || !authentication.isAuthenticated()) {
      throw new IllegalStateException("Authentication is required.");
    }
    return ownerService
        .findByEmail(authentication.getName())
        .orElseThrow(() -> new IllegalStateException("Owner account is unavailable."));
  }
}
