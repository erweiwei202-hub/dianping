package cn.dianping.platform.identity;

import java.io.Serial;
import java.io.Serializable;

public record SessionPrincipal(long userId, String username, String displayName, UserRole role) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public static SessionPrincipal from(UserAccount user) {
        return new SessionPrincipal(user.getId(), user.getUsername(), user.getDisplayName(), user.getRole());
    }
}
