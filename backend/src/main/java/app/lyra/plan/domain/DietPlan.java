package app.lyra.plan.domain;

import app.lyra.shared.domain.InvalidInputException;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class DietPlan {

    private static final int MAX_NAME_LENGTH = 100;

    @EqualsAndHashCode.Include
    private final UUID id;
    private final String name;
    private final DailyTargets targets;
    private final List<PlannedMeal> meals;
    private final boolean active;

    public DietPlan(UUID id, String name, DailyTargets targets, List<PlannedMeal> meals, boolean active) {
        if (id == null || name == null || targets == null || meals == null) {
            throw new InvalidInputException("id, name, targets and meals are required");
        }
        String trimmed = name.trim();
        if (trimmed.isEmpty() || trimmed.length() > MAX_NAME_LENGTH) {
            throw new InvalidInputException("Plan name must have between 1 and " + MAX_NAME_LENGTH + " characters");
        }
        if (meals.isEmpty()) {
            throw new InvalidInputException("A plan needs at least one meal");
        }
        if (meals.stream().map(PlannedMeal::name).distinct().count() != meals.size()) {
            throw new InvalidInputException("Meal names must be different inside a plan");
        }
        this.id = id;
        this.name = trimmed;
        this.targets = targets;
        this.meals = List.copyOf(meals);
        this.active = active;
    }

    public static DietPlan create(String name, DailyTargets targets, List<PlannedMeal> meals) {
        return new DietPlan(UUID.randomUUID(), name, targets, meals, false);
    }

    public DietPlan withContent(String newName, DailyTargets newTargets, List<PlannedMeal> newMeals) {
        return new DietPlan(id, newName, newTargets, newMeals, active);
    }

    public Set<UUID> foodIds() {
        return meals.stream()
                .flatMap(meal -> meal.items().stream())
                .map(PlannedItem::foodId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public DietPlan activated() {
        return new DietPlan(id, name, targets, meals, true);
    }

    public DietPlan deactivated() {
        return new DietPlan(id, name, targets, meals, false);
    }
}
