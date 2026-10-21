package site.seaunit.zhishiku.attachment;

import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.seaunit.zhishiku.owner.CurrentOwnerService;
import site.seaunit.zhishiku.storage.ObjectStorage;

@RestController
@RequestMapping("/api/v1/attachments")
public class AttachmentController {
  private final AttachmentRepository attachments;
  private final CurrentOwnerService currentOwnerService;
  private final ObjectStorage storage;

  public AttachmentController(
      AttachmentRepository attachments,
      CurrentOwnerService currentOwnerService,
      ObjectStorage storage) {
    this.attachments = attachments;
    this.currentOwnerService = currentOwnerService;
    this.storage = storage;
  }

  @GetMapping({"/{attachmentId}", "/{attachmentId}/content"})
  ResponseEntity<Resource> read(@PathVariable UUID attachmentId, Authentication authentication) {
    var owner = currentOwnerService.require(authentication);
    AttachmentEntity attachment =
        attachments
            .findByIdAndOwnerId(attachmentId, owner.getId())
            .orElseThrow(() -> new IllegalArgumentException("Attachment was not found."));
    if (!"clean".equals(attachment.getScanState())
        || !("none".equals(attachment.getRedactionState())
            || "redacted".equals(attachment.getRedactionState())
            || "not_required".equals(attachment.getRedactionState()))) {
      throw new IllegalArgumentException("Attachment is not safe to read.");
    }
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(attachment.getMediaType()))
        .cacheControl(CacheControl.noStore())
        .body(storage.read(attachment.getStorageKey()));
  }
}
