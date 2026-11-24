package app.lyra.diary.application.service;

import app.lyra.diary.application.port.in.DeleteEntryUseCase;
import app.lyra.diary.application.port.out.DiaryEntryRepository;
import app.lyra.diary.domain.DiaryEntry;
import app.lyra.diary.domain.DiaryEntryNotFoundException;
import app.lyra.shared.domain.InvalidInputException;
import app.lyra.shared.domain.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
class DeleteEntryService implements DeleteEntryUseCase {

    private final DiaryEntryRepository diaryEntryRepository;

    @Override
    public void delete(UserId userId, UUID entryId) {
        DiaryEntry entry = diaryEntryRepository.findById(entryId).orElseThrow(() -> new DiaryEntryNotFoundException(entryId));
        if (entry.getPlanned().isPresent()) {
            throw new InvalidInputException("An entry of the plan cannot be deleted: change its quantity to zero to skip it");
        }
        diaryEntryRepository.deleteById(entryId);
    }
}
