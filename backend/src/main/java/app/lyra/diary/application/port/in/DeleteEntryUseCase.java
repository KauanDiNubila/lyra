package app.lyra.diary.application.port.in;
import app.lyra.shared.domain.UserId;

import java.util.UUID;

public interface DeleteEntryUseCase {

    void delete(UserId userId, UUID entryId);
}
