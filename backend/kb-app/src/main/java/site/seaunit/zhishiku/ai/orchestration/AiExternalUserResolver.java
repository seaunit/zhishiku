package site.seaunit.zhishiku.ai.orchestration;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import site.seaunit.zhishiku.owner.CurrentOwnerService;

@Component
public class AiExternalUserResolver {
  private final CurrentOwnerService currentOwners;

  public AiExternalUserResolver(CurrentOwnerService currentOwners) {
    this.currentOwners = currentOwners;
  }

  public String resolve() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || !authentication.isAuthenticated()
        || authentication instanceof AnonymousAuthenticationToken) {
      return "system";
    }
    try {
      return currentOwners.require(authentication).getId().toString();
    } catch (RuntimeException error) {
      return hash(authentication.getName());
    }
  }

  private static String hash(String value) {
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256")
              .digest((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest, 0, 16);
    } catch (NoSuchAlgorithmException error) {
      throw new IllegalStateException("SHA-256 is unavailable.", error);
    }
  }
}
