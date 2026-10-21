package site.seaunit.zhishiku.owner;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OwnerAccountRepository extends JpaRepository<OwnerAccountEntity, UUID> {
  Optional<OwnerAccountEntity> findByInstanceKey(short instanceKey);

  Optional<OwnerAccountEntity> findByEmailNormalized(String emailNormalized);
}
