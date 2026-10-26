package site.seaunit.zhishiku.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import site.seaunit.zhishiku.owner.OwnerAccountEntity;
import site.seaunit.zhishiku.owner.OwnerService;

class OwnerSessionStatusFilterTest {
  private final OwnerService owners = mock(OwnerService.class);
  private final OwnerSessionStatusFilter filter = new OwnerSessionStatusFilter(owners);

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void invalidatesASessionWhenTheAccountBecomesDisabled() throws Exception {
    OwnerAccountEntity owner = OwnerAccountEntity.create("user@example.com", "hash", "zh-CN");
    owner.updateEnabled(false);
    when(owners.findByLoginName("user@example.com")).thenReturn(Optional.of(owner));
    SecurityContextHolder.getContext()
        .setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(
                "user@example.com", "hash", java.util.List.of()));
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/knowledge-items");
    request.getSession(true);
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, new MockFilterChain());

    assertThat(response.getStatus()).isEqualTo(401);
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    assertThat(request.getSession(false)).isNull();
  }

  @Test
  void allowsAnEnabledAccountAndPublicAuthenticationPaths() throws Exception {
    OwnerAccountEntity owner = OwnerAccountEntity.create("user@example.com", "hash", "zh-CN");
    when(owners.findByLoginName("user@example.com")).thenReturn(Optional.of(owner));
    SecurityContextHolder.getContext()
        .setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(
                "user@example.com", "hash", java.util.List.of()));
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/knowledge-items");
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(request, response, chain);

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(chain.getRequest()).isNotNull();
  }

  @Test
  void invalidatesASessionWhenTheAccountNoLongerExists() throws Exception {
    when(owners.findByLoginName("removed@example.com")).thenReturn(Optional.empty());
    SecurityContextHolder.getContext()
        .setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(
                "removed@example.com", "hash", java.util.List.of()));
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/knowledge-items");
    request.getSession(true);
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, new MockFilterChain());

    assertThat(response.getStatus()).isEqualTo(401);
    assertThat(request.getSession(false)).isNull();
  }
}
