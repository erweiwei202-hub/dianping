package cn.dianping.platform.identity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class IdentityDtos {

    private IdentityDtos() {
    }

    public record RegisterRequest(
            @NotBlank
            @Size(min = 3, max = 64)
            @Pattern(regexp = "[A-Za-z0-9_.-]+")
            String username,
            @NotBlank
            @Size(min = 10, max = 72)
            String password,
            @NotBlank
            @Size(max = 80)
            String displayName) {
    }

    public record LoginRequest(
            @NotBlank @Size(max = 64) String username,
            @NotBlank @Size(max = 72) String password) {
    }

    public record UserResponse(long id, String username, String displayName, UserRole role, Instant createdAt) {
        public static UserResponse from(UserAccount user) {
            return new UserResponse(user.getId(), user.getUsername(), user.getDisplayName(), user.getRole(), user.getCreatedAt());
        }
    }
}
