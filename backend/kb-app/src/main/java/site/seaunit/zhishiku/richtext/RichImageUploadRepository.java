package site.seaunit.zhishiku.richtext;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RichImageUploadRepository extends JpaRepository<RichImageUploadEntity, UUID> {
  Optional<RichImageUploadEntity> findByIdAndOwnerId(UUID id, UUID ownerId);
}
