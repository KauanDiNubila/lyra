package app.lyra.diary.application.service;

import app.lyra.diary.application.port.in.DeleteEntryUseCase;
import app.lyra.diary.application.port.out.DiaryEntryRepository;
import app.lyra.diary.domain.DiaryEntryNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
class DeleteEntryService implements DeleteEntryUseCase {

    private final DiaryEntryRepository diaryEntryRepository;

    @Override
    public void delete(UUID entryId) {
        diaryEntryRepository.findById(entryId).orElseThrow(() -> new DiaryEntryNotFoundException(entryId));
        diaryEntryRepository.deleteById(entryId);
    }
}
