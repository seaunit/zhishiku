package site.seaunit.zhishiku.profile;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.seaunit.zhishiku.owner.CurrentOwnerService;
import site.seaunit.zhishiku.owner.OwnerAccountEntity;
import site.seaunit.zhishiku.owner.OwnerService;

@Service
public class ProfileService {
  private final CurrentOwnerService currentOwnerService;
  private final OwnerService ownerService;

  public ProfileService(CurrentOwnerService currentOwnerService, OwnerService ownerService) {
    this.currentOwnerService = currentOwnerService;
    this.ownerService = ownerService;
  }

  @Transactional(readOnly = true)
  public ProfileResponse get(Authentication authentication) {
    return ProfileResponse.from(currentOwnerService.require(authentication));
  }

  @Transactional
  public ProfileResponse update(Authentication authentication, String displayName) {
    OwnerAccountEntity current = currentOwnerService.require(authentication);
    try {
      return ProfileResponse.from(
          ownerService.updateDisplayName(current.getEmailNormalized(), displayName));
    } catch (IllegalArgumentException error) {
      throw new InvalidDisplayNameException(error.getMessage());
    }
  }
}
