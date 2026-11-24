package app.lyra.identity.application.port.in;

import app.lyra.identity.domain.AuthProvider;
import app.lyra.identity.domain.User;

public interface SignInUseCase {

    User signIn(SignInCommand command);

    record SignInCommand(AuthProvider provider, String providerSubject, String email, String name) {
    }
}
