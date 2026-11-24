package app.lyra.diary.application.service;

import app.lyra.diary.application.port.in.FillDayFromPlanUseCase;
import app.lyra.diary.application.port.in.GetDiaryDayUseCase;
import app.lyra.diary.application.port.out.DiaryEntryRepository;
import app.lyra.diary.domain.DiaryDay;
import app.lyra.diary.domain.DiaryEntry;
import app.lyra.diary.domain.NoActivePlanException;
import app.lyra.food.application.port.in.FindFoodsByIdsUseCase;
import app.lyra.food.domain.Food;
import app.lyra.plan.application.port.in.GetActivePlanUseCase;
import app.lyra.plan.domain.DietPlan;
import app.lyra.plan.domain.PlannedItem;
import app.lyra.plan.domain.PlannedMeal;
import app.lyra.shared.domain.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
class FillDayFromPlanService implements FillDayFromPlanUseCase {

    private final GetActivePlanUseCase getActivePlan;
    private final FindFoodsByIdsUseCase findFoods;
    private final GetDiaryDayUseCase getDiaryDay;
    private final DiaryEntryRepository diaryEntryRepository;

    @Override
    @Transactional
    public DiaryDay fillDay(UserId userId, LocalDate date) {
        DietPlan plan = getActivePlan.getActivePlan(userId).orElseThrow(NoActivePlanException::new);
        diaryEntryRepository.lockDay(date);
        if (!diaryEntryRepository.findByDate(date).isEmpty()) {
            return getDiaryDay.getDay(userId, date);
        }
        Map<UUID, Food> foods = findFoods.findByIds(userId, plan.foodIds());
        int slot = 0;
        for (PlannedMeal meal : plan.getMeals()) {
            for (PlannedItem item : meal.items()) {
                Food food = foods.get(item.foodId());
                diaryEntryRepository.save(DiaryEntry.fromPlan(food, date, meal.name(), item.quantity(), slot));
                slot++;
            }
        }
        return getDiaryDay.getDay(userId, date);
    }
}
