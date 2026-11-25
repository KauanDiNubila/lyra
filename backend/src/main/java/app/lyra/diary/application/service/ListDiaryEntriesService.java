package app.lyra.diary.application.service;

import app.lyra.diary.application.port.in.ListDiaryEntriesUseCase;
import app.lyra.diary.application.port.out.DiaryEntryRepository;
import app.lyra.diary.domain.DiaryEntry;
import app.lyra.shared.domain.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
class ListDiaryEntriesService implements ListDiaryEntriesUseCase {

    private final DiaryEntryRepository diaryEntryRepository;

    @Override
    public List<DiaryEntry> listEntries(UserId userId) {
        return diaryEntryRepository.findAll(userId);
    }
}
