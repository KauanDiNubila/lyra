package app.lyra.identity.application.port.in;

import app.lyra.identity.domain.User;
import app.lyra.shared.domain.UserId;

public interface GetCurrentUserUseCase {

    User getUser(UserId id);
}
