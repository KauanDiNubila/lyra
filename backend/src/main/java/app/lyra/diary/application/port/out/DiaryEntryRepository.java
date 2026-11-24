package app.lyra.diary.application.port.out;

import app.lyra.diary.domain.DiaryEntry;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DiaryEntryRepository {

    void lockDay(LocalDate date);

    void save(DiaryEntry entry);

    Optional<DiaryEntry> findById(UUID id);

    List<DiaryEntry> findByDate(LocalDate date);

    void deleteById(UUID id);
}
