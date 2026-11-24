package app.lyra.identity.domain;

import app.lyra.shared.domain.NotFoundException;
import app.lyra.shared.domain.UserId;

public class UserNotFoundException extends NotFoundException {

    public UserNotFoundException(UserId id) {
        super("User not found: " + id);
    }
}
