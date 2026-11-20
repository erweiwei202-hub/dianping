package cn.dianping.platform.identity;

import cn.dianping.platform.common.security.CurrentUserProvider;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.OptionalLong;

@Component
public class CurrentUserProviderImpl implements CurrentUserProvider {

    @Override
    public long requiredUserId() {
        return optionalUserId().orElseThrow(() -> new IllegalStateException("Authenticated user is required"));
    }

    @Override
    public OptionalLong optionalUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || !(authentication.getPrincipal() instanceof SessionPrincipal principal)) {
            return OptionalLong.empty();
        }
        return OptionalLong.of(principal.userId());
    }
}
