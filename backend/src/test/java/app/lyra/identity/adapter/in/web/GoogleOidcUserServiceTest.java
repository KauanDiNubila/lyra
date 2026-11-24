package app.lyra.identity.adapter.in.web;

import app.lyra.identity.application.port.in.SignInUseCase;
import app.lyra.identity.application.port.in.SignInUseCase.SignInCommand;
import app.lyra.identity.domain.AuthProvider;
import app.lyra.identity.domain.User;
import app.lyra.shared.domain.InvalidInputException;
import app.lyra.shared.domain.UserId;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GoogleOidcUserServiceTest {

    private final List<SignInCommand> commands = new ArrayList<>();
    private final UserId userId = UserId.random();
    private final SignInUseCase signIn = command -> {
        commands.add(command);
        return new User(userId, AuthProvider.GOOGLE, command.providerSubject(), command.email(), command.name(), Instant.now());
    };
    private final GoogleOidcUserService service = new GoogleOidcUserService(signIn);

    @Test
    void signsTheUserInWithWhatGoogleSaid() {
        service.loadUser(request(idToken("sub-1", "ana@example.com", true, "Ana Souza")));

        assertThat(commands).containsExactly(
                new SignInCommand(AuthProvider.GOOGLE, "sub-1", "ana@example.com", "Ana Souza"));
    }

    @Test
    void theNameOfThePrincipalIsTheIdOfOurUser() {
        OidcUser principal = service.loadUser(request(idToken("sub-1", "ana@example.com", true, "Ana")));

        assertThat(principal.getName()).isEqualTo(userId.toString());
        assertThat(principal.getEmail()).isEqualTo("ana@example.com");
        assertThat(principal.getSubject()).isEqualTo("sub-1");
    }

    @Test
    void refusesAnAccountWhoseEmailIsNotVerified() {
        OidcUserRequest unverified = request(idToken("sub-1", "ana@example.com", false, "Ana"));

        assertThatThrownBy(() -> service.loadUser(unverified))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .extracting(cause -> ((OAuth2AuthenticationException) cause).getError().getErrorCode())
                .isEqualTo("email_not_verified");
        assertThat(commands).isEmpty();
    }

    @Test
    void refusesAnAccountWithoutTheVerifiedClaim() {
        OidcIdToken withoutClaim = baseToken("sub-1").claim("email", "ana@example.com").build();

        assertThatThrownBy(() -> service.loadUser(request(withoutClaim)))
                .isInstanceOf(OAuth2AuthenticationException.class);
        assertThat(commands).isEmpty();
    }

    @Test
    void refusesAProfileTheDomainRejects() {
        GoogleOidcUserService strict = new GoogleOidcUserService(command -> {
            throw new InvalidInputException("The e-mail is required");
        });
        OidcUserRequest request = request(idToken("sub-1", "ana@example.com", true, "Ana"));

        assertThatThrownBy(() -> strict.loadUser(request))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .hasMessageContaining("The e-mail is required");
    }

    private static OidcUserRequest request(OidcIdToken idToken) {
        ClientRegistration registration = ClientRegistration.withRegistrationId("google")
                .clientId("client-id")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://localhost/login/oauth2/code/google")
                .authorizationUri("http://localhost/authorize")
                .tokenUri("http://localhost/token")
                .build();
        Instant now = Instant.now();
        OAuth2AccessToken accessToken = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "token", now, now.plusSeconds(60));
        return new OidcUserRequest(registration, accessToken, idToken);
    }

    private static OidcIdToken idToken(String subject, String email, boolean emailVerified, String name) {
        return baseToken(subject).claim("email", email).claim("email_verified", emailVerified).claim("name", name).build();
    }

    private static OidcIdToken.Builder baseToken(String subject) {
        Instant now = Instant.now();
        return OidcIdToken.withTokenValue("id-token").subject(subject).issuedAt(now).expiresAt(now.plusSeconds(60));
    }
}
