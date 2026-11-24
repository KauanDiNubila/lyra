package app.lyra.identity.adapter.in.web;

import app.lyra.shared.domain.UserId;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

import java.util.List;

class SignedInOidcUser extends DefaultOidcUser {

    private static final long serialVersionUID = 1L;

    private final String userId;

    SignedInOidcUser(UserId userId, OidcIdToken idToken) {
        super(List.of(), idToken);
        this.userId = userId.toString();
    }

    @Override
    public String getName() {
        return userId;
    }
}
