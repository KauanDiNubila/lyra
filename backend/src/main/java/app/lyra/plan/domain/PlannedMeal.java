package app.lyra.plan.domain;

import app.lyra.shared.domain.InvalidInputException;

import java.time.LocalTime;
import java.util.List;

public record PlannedMeal(String name, LocalTime time, List<PlannedItem> items) {

    private static final int MAX_NAME_LENGTH = 50;

    public PlannedMeal {
        name = name == null ? "" : name.trim();
        if (name.isEmpty() || name.length() > MAX_NAME_LENGTH) {
            throw new InvalidInputException("Meal name must have between 1 and " + MAX_NAME_LENGTH + " characters");
        }
        if (items == null || items.isEmpty()) {
            throw new InvalidInputException("A meal needs at least one item");
        }
        items = List.copyOf(items);
    }
}
