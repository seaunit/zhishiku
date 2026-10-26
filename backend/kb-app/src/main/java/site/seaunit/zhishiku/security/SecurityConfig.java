package site.seaunit.zhishiku.security;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import site.seaunit.zhishiku.owner.OwnerService;

@Configuration
public class SecurityConfig {
  private final OwnerSessionStatusFilter ownerSessionStatusFilter;

  public SecurityConfig(ObjectProvider<OwnerService> ownerService) {
    OwnerService resolved = ownerService.getIfAvailable();
    this.ownerSessionStatusFilter =
        resolved == null ? null : new OwnerSessionStatusFilter(resolved);
  }

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    CookieCsrfTokenRepository csrf = CookieCsrfTokenRepository.withHttpOnlyFalse();
    csrf.setCookieCustomizer(builder -> builder.path("/kb").sameSite("Lax"));

    http.csrf(
            config ->
                config
                    .csrfTokenRepository(csrf)
                    .ignoringRequestMatchers("/api/internal/billing/**"))
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        "/api/v1/auth/csrf",
                        "/api/v1/auth/captcha",
                        "/api/v1/auth/registration-status",
                        "/api/v1/auth/register/email-code",
                        "/api/v1/auth/register",
                        "/api/v1/auth/login",
                        "/api/v1/auth/setup",
                        "/api/internal/billing/waffo-events",
                        "/api/actuator/health",
                        "/api/actuator/health/**",
                        "/v3/api-docs/**",
                        "/swagger-ui/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .formLogin(form -> form.disable())
        .httpBasic(basic -> basic.disable())
        .logout(logout -> logout.disable())
        .exceptionHandling(
            errors ->
                errors
                    .authenticationEntryPoint(
                        (request, response, error) ->
                            response.sendError(HttpStatus.UNAUTHORIZED.value()))
                    .accessDeniedHandler(
                        (request, response, error) ->
                            response.sendError(HttpStatus.FORBIDDEN.value())))
        .headers(headers -> headers.frameOptions(frame -> frame.deny()));
    if (ownerSessionStatusFilter != null) {
      http.addFilterAfter(ownerSessionStatusFilter, SecurityContextHolderFilter.class);
    }
    return http.build();
  }

  @Bean
  AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
      throws Exception {
    return configuration.getAuthenticationManager();
  }
}
