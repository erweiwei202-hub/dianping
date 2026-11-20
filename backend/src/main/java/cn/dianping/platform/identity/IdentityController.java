package cn.dianping.platform.identity;

import cn.dianping.platform.common.api.ApiResponse;
import cn.dianping.platform.common.security.CurrentUserProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class IdentityController {

    private final IdentityService identityService;
    private final AuthenticationManager authenticationManager;
    private final CurrentUserProvider currentUserProvider;
    private final UserAccountRepository userAccountRepository;
    private final SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();

    public IdentityController(
            IdentityService identityService,
            AuthenticationManager authenticationManager,
            CurrentUserProvider currentUserProvider,
            UserAccountRepository userAccountRepository) {
        this.identityService = identityService;
        this.authenticationManager = authenticationManager;
        this.currentUserProvider = currentUserProvider;
        this.userAccountRepository = userAccountRepository;
    }

    @GetMapping("/csrf")
    public ApiResponse<CsrfTokenView> csrf(CsrfToken csrfToken) {
        return ApiResponse.success(new CsrfTokenView(csrfToken.getHeaderName(), csrfToken.getToken()));
    }

    @PostMapping("/register")
    public ApiResponse<IdentityDtos.UserResponse> register(@Valid @RequestBody IdentityDtos.RegisterRequest request) {
        return ApiResponse.success(IdentityDtos.UserResponse.from(identityService.register(request)));
    }

    @PostMapping("/login")
    public ApiResponse<IdentityDtos.UserResponse> login(
            @Valid @RequestBody IdentityDtos.LoginRequest request,
            HttpServletRequest httpServletRequest) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.username().trim().toLowerCase(), request.password()));
        UserAccount user = (UserAccount) authentication.getPrincipal();
        Authentication sessionAuthentication = UsernamePasswordAuthenticationToken.authenticated(
                SessionPrincipal.from(user), null, authentication.getAuthorities());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(sessionAuthentication);
        SecurityContextHolder.setContext(context);
        httpServletRequest.getSession(true).setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        return ApiResponse.success(IdentityDtos.UserResponse.from(user));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response) {
        logoutHandler.logout(request, response, SecurityContextHolder.getContext().getAuthentication());
        return ApiResponse.success(null);
    }

    @GetMapping("/me")
    public ApiResponse<IdentityDtos.UserResponse> me() {
        long userId = currentUserProvider.requiredUserId();
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));
        return ApiResponse.success(IdentityDtos.UserResponse.from(user));
    }

    public record CsrfTokenView(String headerName, String token) {
    }
}
