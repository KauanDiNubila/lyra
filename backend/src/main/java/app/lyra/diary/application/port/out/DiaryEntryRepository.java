package app.lyra.diary.application.port.out;

import app.lyra.diary.domain.DiaryEntry;
import app.lyra.shared.domain.UserId;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DiaryEntryRepository {

    void lockDay(UserId user, LocalDate date);

    void save(UserId owner, DiaryEntry entry);

    Optional<DiaryEntry> findById(UserId user, UUID id);

    List<DiaryEntry> findByDate(UserId user, LocalDate date);

    List<DiaryEntry> findAll(UserId user);

    void deleteById(UserId user, UUID id);
}
