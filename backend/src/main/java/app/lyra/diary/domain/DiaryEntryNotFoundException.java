package app.lyra.diary.domain;

import app.lyra.shared.domain.NotFoundException;

import java.util.UUID;

public class DiaryEntryNotFoundException extends NotFoundException {

    public DiaryEntryNotFoundException(UUID id) {
        super("Diary entry not found: " + id);
    }
}
