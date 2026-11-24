package app.lyra.plan.domain;

import app.lyra.food.domain.Quantity;
import app.lyra.shared.domain.InvalidInputException;

import java.util.UUID;

public record PlannedItem(UUID foodId, Quantity quantity) {

    public PlannedItem {
        if (foodId == null || quantity == null) {
            throw new InvalidInputException("A planned item needs a food and a quantity");
        }
        if (quantity.amount().signum() == 0) {
            throw new InvalidInputException("The planned quantity must be positive");
        }
    }
}
