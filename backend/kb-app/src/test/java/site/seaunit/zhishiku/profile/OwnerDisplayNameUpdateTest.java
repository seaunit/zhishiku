package site.seaunit.zhishiku.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import site.seaunit.zhishiku.config.AppProperties;
import site.seaunit.zhishiku.owner.OwnerAccountEntity;
import site.seaunit.zhishiku.owner.OwnerAccountRepository;
import site.seaunit.zhishiku.owner.OwnerService;

class OwnerDisplayNameUpdateTest {
  private final OwnerAccountRepository repository = mock(OwnerAccountRepository.class);
  private final OwnerService service =
      new OwnerService(repository, mock(PasswordEncoder.class), mock(AppProperties.class));

  @Test
  void normalizesAndPersistsTheCurrentAccountsDisplayName() {
    OwnerAccountEntity owner =
        OwnerAccountEntity.create("alice@example.com", "secret-hash", "zh-CN");
    when(repository.findByEmailNormalized("alice@example.com"))
        .thenReturn(java.util.Optional.of(owner));
    when(repository.save(owner)).thenReturn(owner);

    OwnerAccountEntity updated =
        service.updateDisplayName(" Alice@Example.com ", "\u3000 Alice\u3000  Smith ");

    assertThat(updated.getDisplayName()).isEqualTo("Alice Smith");
    verify(repository).findByEmailNormalized("alice@example.com");
    verify(repository).save(owner);
  }

  @Test
  void rejectsInvalidDisplayNameBeforeChangingTheAccount() {
    OwnerAccountEntity owner =
        OwnerAccountEntity.create("alice@example.com", "secret-hash", "zh-CN");
    when(repository.findByEmailNormalized("alice@example.com"))
        .thenReturn(java.util.Optional.of(owner));

    assertThatThrownBy(() -> service.updateDisplayName("alice@example.com", "<script>"))
        .isInstanceOf(IllegalArgumentException.class);

    assertThat(owner.getDisplayName()).isEqualTo("alice");
    verify(repository, never()).save(any());
  }
}
