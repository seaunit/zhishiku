package site.seaunit.zhishiku.richtext.image;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/rich-text/image-uploads")
public class ImageUploadController {
  private final ImageUploadService service;

  public ImageUploadController(ImageUploadService service) {
    this.service = service;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.ACCEPTED)
  ImageUploadResponse create(
      @RequestParam("file") MultipartFile file,
      @RequestParam(value = "itemId", required = false) UUID itemId,
      Authentication authentication) {
    return service.create(file, itemId, authentication);
  }

  @GetMapping("/{uploadId}")
  ImageUploadResponse get(@PathVariable UUID uploadId, Authentication authentication) {
    return service.get(uploadId, authentication);
  }
}
