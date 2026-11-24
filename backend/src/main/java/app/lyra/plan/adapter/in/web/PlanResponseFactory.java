package app.lyra.plan.adapter.in.web;

import app.lyra.food.application.port.in.FindFoodsByIdsUseCase;
import app.lyra.food.domain.Food;
import app.lyra.plan.domain.DietPlan;
import app.lyra.plan.domain.PlannedItem;
import app.lyra.plan.domain.PlannedMeal;
import app.lyra.shared.domain.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class PlanResponseFactory {

    private final FindFoodsByIdsUseCase findFoods;

    PlanResponse of(UserId userId, DietPlan plan) {
        Map<UUID, Food> foods = findFoods.findByIds(userId, plan.foodIds());
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
        return new PlanResponse.PlanItemResponse(
                item.foodId(), foods.get(item.foodId()).getName(), QuantityJson.from(item.quantity()));
    }
}
