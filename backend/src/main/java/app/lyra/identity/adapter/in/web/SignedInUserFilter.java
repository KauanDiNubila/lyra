package app.lyra.identity.adapter.in.web;

import app.lyra.identity.application.port.in.GetCurrentUserUseCase;
import app.lyra.identity.domain.UserNotFoundException;
import app.lyra.shared.domain.UserId;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
class SignedInUserFilter extends OncePerRequestFilter {

    private final GetCurrentUserUseCase getCurrentUser;
    private final AuthenticationEntryPoint authenticationEntryPoint;
    private final PathPatternRequestMatcher apiPaths = PathPatternRequestMatcher.withDefaults().matcher("/api/**");

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !apiPaths.matches(request);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (isSignedIn(authentication) && !userStillExists(authentication)) {
            new SecurityContextLogoutHandler().logout(request, response, authentication);
            authenticationEntryPoint.commence(request, response, new InsufficientAuthenticationException("The account is gone"));
            return;
        }
        chain.doFilter(request, response);
    }

    private static boolean isSignedIn(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    private boolean userStillExists(Authentication authentication) {
        try {
            getCurrentUser.getUser(UserId.of(authentication.getName()));
            return true;
        } catch (UserNotFoundException gone) {
            return false;
        }
    }
}
