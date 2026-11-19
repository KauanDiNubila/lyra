package app.lyra.diary.application.service;

import app.lyra.diary.application.port.in.ChangeEntryQuantityUseCase;
import app.lyra.diary.application.port.out.DiaryEntryRepository;
import app.lyra.diary.domain.DiaryEntry;
import app.lyra.diary.domain.DiaryEntryNotFoundException;
import app.lyra.food.domain.Quantity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
class ChangeEntryQuantityService implements ChangeEntryQuantityUseCase {

    private final DiaryEntryRepository diaryEntryRepository;

    @Override
    public DiaryEntry changeQuantity(UUID entryId, Quantity quantity) {
        DiaryEntry entry = diaryEntryRepository.findById(entryId).orElseThrow(() -> new DiaryEntryNotFoundException(entryId));
        DiaryEntry changed = entry.withQuantity(quantity);
        diaryEntryRepository.save(changed);
        return changed;
    }
}
