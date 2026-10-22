package site.seaunit.zhishiku.knowledge;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface KnowledgeItemRepository extends JpaRepository<KnowledgeItemEntity, UUID> {
  Optional<KnowledgeItemEntity> findByIdAndOwnerIdAndLifecycleState(
      UUID id, UUID ownerId, String lifecycleState);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      SELECT item
      FROM KnowledgeItemEntity item
      WHERE item.id = :id
        AND item.ownerId = :ownerId
        AND item.lifecycleState = :lifecycleState
      """)
  Optional<KnowledgeItemEntity> findByIdAndOwnerIdAndLifecycleStateForUpdate(
      @Param("id") UUID id,
      @Param("ownerId") UUID ownerId,
      @Param("lifecycleState") String lifecycleState);

  List<KnowledgeItemEntity> findByOwnerIdAndLifecycleStateOrderByUpdatedAtDesc(
      UUID ownerId, String lifecycleState, Pageable pageable);

  long countByIdInAndOwnerId(Collection<UUID> ids, UUID ownerId);
}
