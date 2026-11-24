package app.lyra.diary.application.service;

import app.lyra.diary.application.port.in.LogEntryUseCase;
import app.lyra.diary.application.port.out.DiaryEntryRepository;
import app.lyra.diary.domain.DiaryEntry;
import app.lyra.food.application.port.in.FindFoodUseCase;
import app.lyra.food.domain.Food;
import app.lyra.shared.domain.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class LogEntryService implements LogEntryUseCase {

    private final FindFoodUseCase findFood;
    private final DiaryEntryRepository diaryEntryRepository;

    @Override
    public DiaryEntry log(UserId userId, LogEntryCommand command) {
        Food food = findFood.findById(userId, command.foodId());
        DiaryEntry entry = DiaryEntry.log(food, command.date(), command.mealName(), command.quantity());
        diaryEntryRepository.save(entry);
        return entry;
    }
}
