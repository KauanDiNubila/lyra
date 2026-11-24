package app.lyra.plan.application.service;

import app.lyra.food.application.port.in.FindFoodsByIdsUseCase;
import app.lyra.food.domain.Food;
import app.lyra.food.domain.MeasurementUnit;
import app.lyra.plan.domain.DietPlan;
import app.lyra.plan.domain.PlannedItem;
import app.lyra.plan.domain.PlannedMeal;
import app.lyra.shared.domain.InvalidInputException;
import app.lyra.shared.domain.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class PlanFoodChecker {

    private final FindFoodsByIdsUseCase findFoods;

    void check(UserId userId, DietPlan plan) {
        Map<UUID, Food> foods = findFoods.findByIds(userId, plan.foodIds());
        for (PlannedMeal meal : plan.getMeals()) {
            for (PlannedItem item : meal.items()) {
                Food food = foods.get(item.foodId());
                MeasurementUnit expected = food.getReferenceAmount().unit();
                if (item.quantity().unit() != expected) {
                    throw new InvalidInputException("Quantity of " + food.getName() + " must be in " + expected);
                }
            }
        }
    }
}
