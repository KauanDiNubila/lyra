package app.lyra.plan.adapter.in.web;

import app.lyra.plan.domain.DailyTargets;
import app.lyra.plan.domain.PlannedItem;
import app.lyra.plan.domain.PlannedMeal;
import app.lyra.shared.domain.InvalidInputException;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

record PlanRequest(String name, TargetsJson targets, List<PlanMealRequest> meals) {

    DailyTargets toTargets() {
        return required(targets, "targets are required").toDomain();
    }

    List<PlannedMeal> toMeals() {
        return required(meals, "meals are required").stream()
                .map(meal -> required(meal, "every meal needs its data").toDomain())
                .toList();
    }

    record PlanMealRequest(String name, LocalTime time, List<PlanItemRequest> items) {

        PlannedMeal toDomain() {
            List<PlannedItem> plannedItems = items == null
                    ? null
                    : items.stream().map(item -> required(item, "every item needs its data").toDomain()).toList();
            return new PlannedMeal(name, time, plannedItems);
        }
    }

    record PlanItemRequest(UUID foodId, QuantityJson quantity) {

        PlannedItem toDomain() {
            return new PlannedItem(foodId, quantity == null ? null : quantity.toDomain());
        }
    }

    private static <T> T required(T value, String message) {
        if (value == null) {
            throw new InvalidInputException(message);
        }
        return value;
    }
}
