package app.lyra.diary.application.port.in;

import java.util.UUID;

public interface DeleteEntryUseCase {

    void delete(UUID entryId);
}
