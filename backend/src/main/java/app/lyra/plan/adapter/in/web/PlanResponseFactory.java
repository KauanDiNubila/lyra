package app.lyra.plan.adapter.in.web;

import app.lyra.food.application.port.in.FindFoodUseCase;
import app.lyra.food.domain.Food;
import app.lyra.plan.domain.DietPlan;
import app.lyra.plan.domain.PlannedItem;
import app.lyra.plan.domain.PlannedMeal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class PlanResponseFactory {

    private final FindFoodUseCase findFood;

    PlanResponse of(DietPlan plan) {
        Map<UUID, Food> foods = new HashMap<>();
        return new PlanResponse(
                plan.getId(),
                plan.getName(),
                plan.isActive(),
                TargetsJson.from(plan.getTargets()),
                plan.getMeals().stream().map(meal -> mealOf(meal, foods)).toList());
    }

    private PlanResponse.PlanMealResponse mealOf(PlannedMeal meal, Map<UUID, Food> foods) {
        return new PlanResponse.PlanMealResponse(
                meal.name(),
                meal.time(),
                meal.items().stream().map(item -> itemOf(item, foods)).toList());
    }

    private PlanResponse.PlanItemResponse itemOf(PlannedItem item, Map<UUID, Food> foods) {
        Food food = foods.computeIfAbsent(item.foodId(), findFood::findById);
        return new PlanResponse.PlanItemResponse(item.foodId(), food.getName(), QuantityJson.from(item.quantity()));
    }
}
