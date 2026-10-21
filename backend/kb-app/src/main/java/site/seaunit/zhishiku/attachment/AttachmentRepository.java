package site.seaunit.zhishiku.attachment;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttachmentRepository extends JpaRepository<AttachmentEntity, UUID> {
  Optional<AttachmentEntity> findByIdAndOwnerId(UUID id, UUID ownerId);

  List<AttachmentEntity> findByOwnerIdAndIdIn(UUID ownerId, Collection<UUID> ids);

  Optional<AttachmentEntity> findFirstByOwnerIdAndSha256AndScanStateOrderByCreatedAtAsc(
      UUID ownerId, String sha256, String scanState);
}
