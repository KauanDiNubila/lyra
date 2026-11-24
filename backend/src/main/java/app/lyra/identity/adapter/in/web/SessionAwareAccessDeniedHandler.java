package app.lyra.identity.adapter.in.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
class SessionAwareAccessDeniedHandler implements AccessDeniedHandler {

    private final ProblemDetailsAuthenticationEntryPoint authenticationEntryPoint;
    private final AuthenticationTrustResolverImpl trustResolver = new AuthenticationTrustResolverImpl();
    private final AccessDeniedHandlerImpl forbidden = new AccessDeniedHandlerImpl();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException exception)
            throws IOException, ServletException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || trustResolver.isAnonymous(authentication)) {
            authenticationEntryPoint.commence(request, response, new InsufficientAuthenticationException("No session"));
            return;
        }
        forbidden.handle(request, response, exception);
    }
}
