package app.lyra;

import app.lyra.shared.domain.UserId;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

public final class TestSessions {

    private TestSessions() {
    }

    public static RequestPostProcessor of(UserId id) {
        return user(id.toString());
    }

    public static void signIn(UserId id) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(id.toString(), "n/a", AuthorityUtils.NO_AUTHORITIES));
    }

    public static void signOut() {
        SecurityContextHolder.clearContext();
    }
}
