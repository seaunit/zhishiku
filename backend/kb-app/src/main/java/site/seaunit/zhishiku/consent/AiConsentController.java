package site.seaunit.zhishiku.consent;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.seaunit.zhishiku.consent.AiConsentDtos.AiConsentResponse;
import site.seaunit.zhishiku.consent.AiConsentDtos.UpdateAiConsentRequest;

@RestController
@RequestMapping("/api/v1/ai-consents")
public class AiConsentController {
  private final AiConsentService service;

  public AiConsentController(AiConsentService service) {
    this.service = service;
  }

  @GetMapping
  List<AiConsentResponse> list(Authentication authentication) {
    return service.list(authentication);
  }

  @PutMapping("/{scope}")
  AiConsentResponse update(
      Authentication authentication,
      @PathVariable String scope,
      @Valid @RequestBody UpdateAiConsentRequest request) {
    return service.update(authentication, scope, request);
  }
}
