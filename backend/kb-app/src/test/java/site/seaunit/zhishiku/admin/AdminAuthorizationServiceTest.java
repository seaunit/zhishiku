package site.seaunit.zhishiku.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.TestingAuthenticationToken;
import site.seaunit.zhishiku.owner.CurrentOwnerService;
import site.seaunit.zhishiku.owner.OwnerAccountEntity;

class AdminAuthorizationServiceTest {
  private final CurrentOwnerService owners = mock(CurrentOwnerService.class);
  private final AdminAuthorizationService authorization = new AdminAuthorizationService(owners);

  @Test
  void acceptsTheConfiguredAdministratorOnly() {
    OwnerAccountEntity admin =
        OwnerAccountEntity.createAdmin("a115846", "a115846@admin.seaunit.local", "hash", "zh-CN");
    TestingAuthenticationToken authentication = new TestingAuthenticationToken("a115846", "hash");
    when(owners.require(authentication)).thenReturn(admin);

    assertThat(authorization.require(authentication)).isSameAs(admin);
  }

  @Test
  void rejectsARegularAccountEvenWhenItUsesTheSameDisplayName() {
    OwnerAccountEntity user = OwnerAccountEntity.create("a115846@example.com", "hash", "zh-CN");
    TestingAuthenticationToken authentication = new TestingAuthenticationToken("a115846", "hash");
    when(owners.require(authentication)).thenReturn(user);

    assertThatThrownBy(() -> authorization.require(authentication))
        .isInstanceOfSatisfying(
            AdminAccountException.class,
            error -> {
              assertThat(error.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
              assertThat(error.getCode()).isEqualTo("ADMIN_REQUIRED");
            });
  }
}
