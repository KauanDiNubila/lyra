package app.lyra.plan.application.service;

import app.lyra.food.application.port.in.FindFoodUseCase;
import app.lyra.food.domain.Food;
import app.lyra.food.domain.MeasurementUnit;
import app.lyra.plan.domain.PlannedItem;
import app.lyra.plan.domain.PlannedMeal;
import app.lyra.shared.domain.InvalidInputException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class PlanFoodChecker {

    private final FindFoodUseCase findFood;

    void check(List<PlannedMeal> meals) {
        Map<UUID, Food> foods = new HashMap<>();
        for (PlannedMeal meal : meals) {
            for (PlannedItem item : meal.items()) {
                Food food = foods.computeIfAbsent(item.foodId(), findFood::findById);
                MeasurementUnit expected = food.getReferenceAmount().unit();
                if (item.quantity().unit() != expected) {
                    throw new InvalidInputException("Quantity of " + food.getName() + " must be in " + expected);
                }
            }
        }
    }
}
