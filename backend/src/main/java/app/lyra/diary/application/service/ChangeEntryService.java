package app.lyra.diary.application.service;

import app.lyra.diary.application.port.in.ChangeEntryUseCase;
import app.lyra.diary.application.port.out.DiaryEntryRepository;
import app.lyra.diary.domain.DiaryEntry;
import app.lyra.diary.domain.DiaryEntryNotFoundException;
import app.lyra.food.application.port.in.FindFoodUseCase;
import app.lyra.food.domain.Food;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
class ChangeEntryService implements ChangeEntryUseCase {

    private final FindFoodUseCase findFood;
    private final DiaryEntryRepository diaryEntryRepository;

    @Override
    public DiaryEntry change(UUID entryId, ChangeEntryCommand command) {
        DiaryEntry entry = diaryEntryRepository.findById(entryId).orElseThrow(() -> new DiaryEntryNotFoundException(entryId));
        DiaryEntry changed = entry;
        if (command.foodId() != null) {
            Food food = findFood.findById(command.foodId());
            changed = changed.withFood(food, command.quantity());
        } else if (command.quantity() != null) {
            changed = changed.withQuantity(command.quantity());
        }
        if (command.mealName() != null) {
            changed = changed.withMealName(command.mealName());
        }
        diaryEntryRepository.save(changed);
        return changed;
    }
}
