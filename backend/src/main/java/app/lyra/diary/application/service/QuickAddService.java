package app.lyra.diary.application.service;

import app.lyra.diary.application.port.in.QuickAddUseCase;
import app.lyra.diary.application.port.out.DiaryEntryRepository;
import app.lyra.diary.domain.DiaryEntry;
import app.lyra.food.application.port.in.CreateCustomFoodUseCase;
import app.lyra.food.domain.Food;
import app.lyra.food.domain.MeasurementUnit;
import app.lyra.food.domain.Quantity;
import app.lyra.shared.domain.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
class QuickAddService implements QuickAddUseCase {

    private static final Quantity ONE_PORTION = new Quantity(BigDecimal.ONE, MeasurementUnit.PORTION);

    private final CreateCustomFoodUseCase createCustomFood;
    private final DiaryEntryRepository diaryEntryRepository;

    @Override
    @Transactional
    public DiaryEntry quickAdd(UserId userId, QuickAddCommand command) {
        Food food = createCustomFood.create(userId, command.name(), ONE_PORTION, command.nutrients());
        Quantity eaten = new Quantity(command.portions(), MeasurementUnit.PORTION);
        DiaryEntry entry = DiaryEntry.log(food, command.date(), command.mealName(), eaten);
        diaryEntryRepository.save(userId, entry);
        return entry;
    }
}
