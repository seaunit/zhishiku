package site.seaunit.zhishiku.profile;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/profile")
public class ProfileController {
  private final ProfileService profileService;

  public ProfileController(ProfileService profileService) {
    this.profileService = profileService;
  }

  @GetMapping
  ProfileResponse get(Authentication authentication) {
    return profileService.get(authentication);
  }

  @PatchMapping
  ProfileResponse update(
      @Valid @RequestBody UpdateProfileRequest request, Authentication authentication) {
    return profileService.update(authentication, request.displayName());
  }
}
