package site.seaunit.zhishiku.richtext;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RichImageUploadRepository extends JpaRepository<RichImageUploadEntity, UUID> {
  Optional<RichImageUploadEntity> findByIdAndOwnerId(UUID id, UUID ownerId);

  List<RichImageUploadEntity> findByOwnerIdAndIdIn(UUID ownerId, Collection<UUID> ids);

  Optional<RichImageUploadEntity>
      findFirstByOwnerIdAndContentHashAndStatusAndScanStateOrderByCreatedAtAsc(
          UUID ownerId, String contentHash, String status, String scanState);
}
