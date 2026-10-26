package site.seaunit.zhishiku.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import site.seaunit.zhishiku.owner.OwnerAccountEntity;
import site.seaunit.zhishiku.owner.OwnerService;

public class OwnerSessionStatusFilter extends OncePerRequestFilter {
  private static final Set<String> PUBLIC_AUTH_PATHS =
      Set.of(
          "/api/v1/auth/csrf",
          "/api/v1/auth/captcha",
          "/api/v1/auth/registration-status",
          "/api/v1/auth/register/email-code",
          "/api/v1/auth/register",
          "/api/v1/auth/login",
          "/api/v1/auth/setup");

  private final OwnerService owners;

  public OwnerSessionStatusFilter(OwnerService owners) {
    this.owners = owners;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return PUBLIC_AUTH_PATHS.contains(request.getServletPath());
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.isAuthenticated()) {
      Optional<OwnerAccountEntity> found = owners.findByLoginName(authentication.getName());
      if (found == null) {
        filterChain.doFilter(request, response);
        return;
      }
      OwnerAccountEntity owner = found.orElse(null);
      if (owner != null && owner.isEnabled()) {
        filterChain.doFilter(request, response);
        return;
      }
      SecurityContextHolder.clearContext();
      HttpSession session = request.getSession(false);
      if (session != null) {
        session.invalidate();
      }
      response.sendError(HttpStatus.UNAUTHORIZED.value());
      return;
    }
    filterChain.doFilter(request, response);
  }
}
