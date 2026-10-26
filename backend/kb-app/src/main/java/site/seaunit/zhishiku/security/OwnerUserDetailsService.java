package site.seaunit.zhishiku.security;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import site.seaunit.zhishiku.owner.OwnerService;

@Service
public class OwnerUserDetailsService implements UserDetailsService {
  private final OwnerService ownerService;

  public OwnerUserDetailsService(OwnerService ownerService) {
    this.ownerService = ownerService;
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    var owner =
        ownerService
            .findByLoginName(username)
            .orElseThrow(() -> new UsernameNotFoundException("Owner not found."));
    return User.withUsername(owner.getLoginName())
        .password(owner.getPasswordHash())
        .roles(owner.getAccountRole())
        .disabled(!owner.isEnabled())
        .build();
  }
}
