package site.seaunit.zhishiku.attachment;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttachmentRepository extends JpaRepository<AttachmentEntity, UUID> {
  Optional<AttachmentEntity> findByIdAndOwnerId(UUID id, UUID ownerId);
}
