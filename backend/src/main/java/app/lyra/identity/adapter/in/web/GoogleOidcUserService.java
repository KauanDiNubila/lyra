package app.lyra.identity.adapter.in.web;

import app.lyra.identity.application.port.in.SignInUseCase;
import app.lyra.identity.application.port.in.SignInUseCase.SignInCommand;
import app.lyra.identity.domain.AuthProvider;
import app.lyra.identity.domain.User;
import app.lyra.shared.domain.InvalidInputException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class GoogleOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

    private final SignInUseCase signIn;

    @Override
    public OidcUser loadUser(OidcUserRequest request) {
        OidcIdToken idToken = request.getIdToken();
        if (!Boolean.TRUE.equals(idToken.getEmailVerified())) {
            throw failure("email_not_verified", "The e-mail of the account is not verified");
        }
        try {
            User user = signIn.signIn(new SignInCommand(AuthProvider.GOOGLE, idToken.getSubject(), idToken.getEmail(),
                    idToken.getFullName()));
            return new SignedInOidcUser(user.getId(), idToken);
        } catch (InvalidInputException cause) {
            throw failure("invalid_profile", cause.getMessage());
        }
    }

    private static OAuth2AuthenticationException failure(String code, String description) {
        return new OAuth2AuthenticationException(new OAuth2Error(code, description, null));
    }
}
