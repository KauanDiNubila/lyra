package app.lyra.diary.application.port.in;

import app.lyra.diary.domain.DiaryEntry;
import app.lyra.shared.domain.UserId;

import java.util.List;

public interface ListDiaryEntriesUseCase {

    List<DiaryEntry> listEntries(UserId userId);
}
