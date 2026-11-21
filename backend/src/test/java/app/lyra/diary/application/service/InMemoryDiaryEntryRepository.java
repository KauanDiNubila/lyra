package app.lyra.diary.application.service;

import app.lyra.diary.application.port.out.DiaryEntryRepository;
import app.lyra.diary.domain.DiaryEntry;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

class InMemoryDiaryEntryRepository implements DiaryEntryRepository {

    private final Map<UUID, DiaryEntry> entries = new LinkedHashMap<>();

    @Override
    public void save(DiaryEntry entry) {
        entries.put(entry.getId(), entry);
    }

    @Override
    public Optional<DiaryEntry> findById(UUID id) {
        return Optional.ofNullable(entries.get(id));
    }

    @Override
    public List<DiaryEntry> findByDate(LocalDate date) {
        return entries.values().stream().filter(entry -> entry.getDate().equals(date)).toList();
    }

    @Override
    public void deleteById(UUID id) {
        entries.remove(id);
    }
}
