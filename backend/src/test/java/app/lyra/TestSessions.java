package app.lyra;

import app.lyra.shared.domain.UserId;
import jakarta.servlet.http.Cookie;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

public final class TestSessions {

    private static final String CSRF_TOKEN = "test-csrf-token";

    private TestSessions() {
    }

    public static RequestPostProcessor of(UserId id) {
        RequestPostProcessor session = user(id.toString());
        return request -> withCsrfToken().postProcessRequest(session.postProcessRequest(request));
    }

    public static RequestPostProcessor withCsrfToken() {
        return request -> {
            request.setCookies(new Cookie("XSRF-TOKEN", CSRF_TOKEN));
            request.addHeader("X-XSRF-TOKEN", CSRF_TOKEN);
            return request;
        };
    }

    public static void signIn(UserId id) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(id.toString(), "n/a", AuthorityUtils.NO_AUTHORITIES));
    }

    public static void signOut() {
        SecurityContextHolder.clearContext();
    }
}
