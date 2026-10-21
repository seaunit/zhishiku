package site.seaunit.zhishiku.richtext;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RichDocumentRepository extends JpaRepository<RichDocumentEntity, UUID> {}
