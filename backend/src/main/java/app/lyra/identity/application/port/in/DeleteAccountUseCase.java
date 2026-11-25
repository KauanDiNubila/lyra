package app.lyra.identity.application.port.in;

import app.lyra.shared.domain.UserId;

public interface DeleteAccountUseCase {

    void delete(UserId userId);
}
