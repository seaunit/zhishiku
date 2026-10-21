package site.seaunit.zhishiku.profile;

import java.util.UUID;
import site.seaunit.zhishiku.owner.OwnerAccountEntity;

public record ProfileResponse(UUID ownerId, String email, String displayName, String locale) {
  static ProfileResponse from(OwnerAccountEntity owner) {
    return new ProfileResponse(
        owner.getId(), owner.getEmailNormalized(), owner.getDisplayName(), owner.getLocale());
  }
}
