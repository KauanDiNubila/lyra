package app.lyra.diary.application.service;

import app.lyra.diary.application.port.out.DiaryEntryRepository;
import app.lyra.diary.domain.DiaryEntry;
import app.lyra.shared.domain.UserId;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

class InMemoryDiaryEntryRepository implements DiaryEntryRepository {

    private record Stored(UserId owner, DiaryEntry entry) {
    }

    private final Map<UUID, Stored> entries = new LinkedHashMap<>();

    @Override
    public void lockDay(UserId user, LocalDate date) {
    }

    @Override
    public void save(UserId owner, DiaryEntry entry) {
        entries.put(entry.getId(), new Stored(owner, entry));
    }

    @Override
    public Optional<DiaryEntry> findById(UserId user, UUID id) {
        return Optional.ofNullable(entries.get(id)).filter(stored -> stored.owner().equals(user)).map(Stored::entry);
    }

    @Override
    public List<DiaryEntry> findByDate(UserId user, LocalDate date) {
        return entries.values().stream()
                .filter(stored -> stored.owner().equals(user) && stored.entry().getDate().equals(date))
                .map(Stored::entry)
                .toList();
    }

    @Override
    public void deleteById(UserId user, UUID id) {
        findById(user, id).ifPresent(entry -> entries.remove(id));
    }
}
