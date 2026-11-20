package cn.dianping.platform.common.security;

import java.util.OptionalLong;

public interface CurrentUserProvider {

    long requiredUserId();

    OptionalLong optionalUserId();
}
