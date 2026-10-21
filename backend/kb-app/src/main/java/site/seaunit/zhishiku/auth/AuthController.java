package site.seaunit.zhishiku.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import site.seaunit.zhishiku.auth.captcha.CaptchaChallenge;
import site.seaunit.zhishiku.auth.captcha.CaptchaService;
import site.seaunit.zhishiku.auth.registration.EmailCodeIssued;
import site.seaunit.zhishiku.auth.registration.RegistrationService;
import site.seaunit.zhishiku.owner.OwnerAccountEntity;
import site.seaunit.zhishiku.owner.OwnerService;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
  private static final HttpSessionSecurityContextRepository CONTEXT_REPOSITORY =
      new HttpSessionSecurityContextRepository();

  private final AuthenticationManager authenticationManager;
  private final OwnerService ownerService;
  private final CaptchaService captchaService;
  private final RegistrationService registrationService;
  private final Environment environment;

  public AuthController(
      AuthenticationManager authenticationManager,
      OwnerService ownerService,
      CaptchaService captchaService,
      RegistrationService registrationService,
      Environment environment) {
    this.authenticationManager = authenticationManager;
    this.ownerService = ownerService;
    this.captchaService = captchaService;
    this.registrationService = registrationService;
    this.environment = environment;
  }

  @GetMapping("/csrf")
  CsrfResponse csrf(CsrfToken token) {
    return new CsrfResponse(token.getToken(), token.getHeaderName());
  }

  @GetMapping("/captcha")
  CaptchaResponse captcha() {
    CaptchaChallenge challenge = captchaService.issue();
    boolean exposeAnswer =
        environment.getProperty("app.captcha.expose-answer", Boolean.class, false);
    return new CaptchaResponse(
        challenge.captchaId(),
        challenge.image(),
        challenge.expiresInSeconds(),
        exposeAnswer ? challenge.answer() : null);
  }

  @GetMapping("/registration-status")
  RegistrationStatusResponse registrationStatus() {
    return new RegistrationStatusResponse(registrationService.isRegistrationOpen());
  }

  @PostMapping("/register/email-code")
  @ResponseStatus(HttpStatus.ACCEPTED)
  EmailCodeResponse registerEmailCode(
      @Valid @RequestBody EmailCodeRequest request, HttpServletRequest servletRequest) {
    EmailCodeIssued issued =
        registrationService.issueEmailCode(request.email(), servletRequest.getRemoteAddr());
    boolean exposeCode =
        environment.getProperty("app.registration.expose-code", Boolean.class, false);
    return new EmailCodeResponse(
        issued.expiresInSeconds(), issued.resendAfterSeconds(), exposeCode ? issued.code() : null);
  }

  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  RegistrationResponse register(@Valid @RequestBody RegisterRequest request) {
    String email =
        registrationService.register(
            new RegistrationService.RegisterCommand(
                request.email(),
                request.password(),
                request.confirmPassword(),
                request.emailCode(),
                request.locale()));
    return new RegistrationResponse(email);
  }

  @PostMapping("/login")
  SessionResponse login(
      @Valid @RequestBody LoginRequest request,
      HttpServletRequest servletRequest,
      HttpServletResponse servletResponse,
      CsrfToken csrfToken) {
    if (!captchaService.consume(request.captchaId(), request.captchaCode())) {
      throw new CaptchaValidationException();
    }
    Authentication authentication =
        authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken.unauthenticated(
                request.email().strip().toLowerCase(), request.password()));
    persist(authentication, servletRequest, servletResponse);
    return response(csrfToken, authentication);
  }

  @PostMapping("/setup")
  @ResponseStatus(HttpStatus.CREATED)
  SessionResponse setup(
      @Valid @RequestBody SetupRequest request,
      HttpServletRequest servletRequest,
      HttpServletResponse servletResponse,
      CsrfToken csrfToken) {
    ownerService.setup(
        request.bootstrapToken(), request.email(), request.password(), request.locale());
    Authentication authentication =
        authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken.unauthenticated(
                request.email().strip().toLowerCase(), request.password()));
    persist(authentication, servletRequest, servletResponse);
    return response(csrfToken, authentication);
  }

  @GetMapping("/me")
  SessionResponse me(CsrfToken csrfToken, Authentication authentication) {
    return response(csrfToken, authentication);
  }

  @PostMapping("/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void logout(HttpServletRequest request, HttpServletResponse response) {
    new SecurityContextLogoutHandler()
        .logout(request, response, SecurityContextHolder.getContext().getAuthentication());
  }

  private static void persist(
      Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authentication);
    SecurityContextHolder.setContext(context);
    CONTEXT_REPOSITORY.saveContext(context, request, response);
  }

  private SessionResponse response(CsrfToken token, Authentication authentication) {
    OwnerAccountEntity owner =
        ownerService
            .findByEmail(authentication.getName())
            .orElseThrow(() -> new IllegalStateException("Account is unavailable."));
    return new SessionResponse(
        owner.getId(), owner.getEmailNormalized(), owner.getLocale(), token.getToken());
  }
}
