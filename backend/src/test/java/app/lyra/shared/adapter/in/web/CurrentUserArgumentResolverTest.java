package app.lyra.shared.adapter.in.web;

import app.lyra.shared.domain.InvalidInputException;
import app.lyra.shared.domain.UserId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Method;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CurrentUserArgumentResolverTest {

    private final CurrentUserArgumentResolver resolver = new CurrentUserArgumentResolver();

    @AfterEach
    void clearTheContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void supportsOnlyAParameterOfTheUserIdType() throws Exception {
        assertThat(resolver.supportsParameter(parameter("userId", 0))).isTrue();
        assertThat(resolver.supportsParameter(parameter("text", 0))).isFalse();
    }

    @Test
    void givesTheIdOfTheUserThatIsSignedIn() throws Exception {
        UUID uuid = UUID.randomUUID();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(uuid.toString(), "n/a", AuthorityUtils.NO_AUTHORITIES));

        UserId id = resolver.resolveArgument(parameter("userId", 0), null, null, null);

        assertThat(id).isEqualTo(new UserId(uuid));
    }

    @Test
    void failsWhenNobodyIsSignedIn() throws Exception {
        MethodParameter parameter = parameter("userId", 0);

        assertThatThrownBy(() -> resolver.resolveArgument(parameter, null, null, null))
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }

    @Test
    void failsForAnAnonymousVisitor() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "key", "anonymous", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
        MethodParameter parameter = parameter("userId", 0);

        assertThatThrownBy(() -> resolver.resolveArgument(parameter, null, null, null))
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }

    @Test
    void failsWhenThePrincipalIsNotAnIdOfOurs() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("someone", "n/a", AuthorityUtils.NO_AUTHORITIES));
        MethodParameter parameter = parameter("userId", 0);

        assertThatThrownBy(() -> resolver.resolveArgument(parameter, null, null, null))
                .isInstanceOf(InvalidInputException.class);
    }

    private static MethodParameter parameter(String method, int index) throws Exception {
        for (Method candidate : Handlers.class.getDeclaredMethods()) {
            if (candidate.getName().equals(method)) {
                return new MethodParameter(candidate, index);
            }
        }
        throw new IllegalArgumentException(method);
    }

    @SuppressWarnings("unused")
    private static final class Handlers {

        void userId(UserId id) {
        }

        void text(String id) {
        }
    }
}
