package site.seaunit.zhishiku.knowledge;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KnowledgeItemRepository extends JpaRepository<KnowledgeItemEntity, UUID> {
  Optional<KnowledgeItemEntity> findByIdAndOwnerIdAndLifecycleState(
      UUID id, UUID ownerId, String lifecycleState);

  List<KnowledgeItemEntity> findByOwnerIdAndLifecycleStateOrderByUpdatedAtDesc(
      UUID ownerId, String lifecycleState, Pageable pageable);
}
