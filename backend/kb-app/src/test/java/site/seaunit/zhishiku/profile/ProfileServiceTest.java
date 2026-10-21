package site.seaunit.zhishiku.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import site.seaunit.zhishiku.owner.CurrentOwnerService;
import site.seaunit.zhishiku.owner.OwnerAccountEntity;
import site.seaunit.zhishiku.owner.OwnerService;

class ProfileServiceTest {
  private final CurrentOwnerService currentOwnerService = mock(CurrentOwnerService.class);
  private final OwnerService ownerService = mock(OwnerService.class);
  private final ProfileService service = new ProfileService(currentOwnerService, ownerService);
  private final Authentication authentication = mock(Authentication.class);

  @Test
  void normalizesUnicodeWhitespaceToSingleSpaces() {
    assertThat(OwnerService.normalizeDisplayName("\u3000 Alice\u3000  Bob  "))
        .isEqualTo("Alice Bob");
  }

  @Test
  void countsUnicodeCodePointsInsteadOfUtf16Units() {
    String fortyCodePoints = "\uD83D\uDE00".repeat(40);

    assertThat(OwnerService.normalizeDisplayName(fortyCodePoints)).isEqualTo(fortyCodePoints);
    assertThatThrownBy(() -> OwnerService.normalizeDisplayName(fortyCodePoints + "a"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsBlankControlAndHtmlLikeDisplayNames() {
    assertThatThrownBy(() -> OwnerService.normalizeDisplayName(" \u3000 "))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> OwnerService.normalizeDisplayName("Alice\nBob"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> OwnerService.normalizeDisplayName("Alice\tBob"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> OwnerService.normalizeDisplayName("Alice<script>"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> OwnerService.normalizeDisplayName("Alice>Bob"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> OwnerService.normalizeDisplayName("Alice\u202Eevil"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void readsOnlyTheAuthenticatedAccount() {
    OwnerAccountEntity owner =
        OwnerAccountEntity.create("alice@example.com", "secret-hash", "zh-CN");
    when(authentication.isAuthenticated()).thenReturn(true);
    when(currentOwnerService.require(authentication)).thenReturn(owner);

    ProfileResponse response = service.get(authentication);

    assertThat(response.ownerId()).isEqualTo(owner.getId());
    assertThat(response.email()).isEqualTo("alice@example.com");
    assertThat(response.displayName()).isEqualTo("alice");
    verify(currentOwnerService).require(authentication);
  }

  @Test
  void updatesOnlyTheAuthenticatedAccountsDisplayName() {
    OwnerAccountEntity owner =
        OwnerAccountEntity.create("alice@example.com", "secret-hash", "zh-CN");
    OwnerAccountEntity updated =
        OwnerAccountEntity.create("alice@example.com", "secret-hash", "zh-CN");
    updated.updateDisplayName("Alice");
    when(currentOwnerService.require(authentication)).thenReturn(owner);
    when(ownerService.updateDisplayName("alice@example.com", "Alice")).thenReturn(updated);

    ProfileResponse response = service.update(authentication, "Alice");

    assertThat(response.displayName()).isEqualTo("Alice");
    verify(ownerService).updateDisplayName("alice@example.com", "Alice");
  }

  @Test
  void convertsDisplayNameValidationFailuresToProfileSpecificErrors() {
    OwnerAccountEntity owner =
        OwnerAccountEntity.create("alice@example.com", "secret-hash", "zh-CN");
    when(currentOwnerService.require(authentication)).thenReturn(owner);
    when(ownerService.updateDisplayName("alice@example.com", "<script>"))
        .thenThrow(new IllegalArgumentException("Display name is invalid."));

    assertThatThrownBy(() -> service.update(authentication, "<script>"))
        .isInstanceOf(InvalidDisplayNameException.class);
  }

  @Test
  void profileResponseContainsNoCredentialOrSessionFields() {
    ProfileResponse response =
        new ProfileResponse(
            UUID.fromString("11111111-1111-1111-1111-111111111111"),
            "alice@example.com",
            "Alice",
            "zh-CN");

    assertThat(response.toString()).doesNotContain("password", "hash", "csrf", "session");
  }
}
