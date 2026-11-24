package app.lyra.diary.domain;

import app.lyra.food.domain.Quantity;
import app.lyra.shared.domain.InvalidInputException;

import java.util.UUID;

public record Planned(UUID foodId, String foodName, Quantity quantity, Macros macros, int slot) {

    public Planned {
        if (foodId == null || foodName == null || foodName.isBlank() || quantity == null || macros == null) {
            throw new InvalidInputException("A planned item needs a food, a quantity and its macros");
        }
        if (quantity.amount().signum() == 0) {
            throw new InvalidInputException("The planned quantity must be positive");
        }
        if (slot < 0) {
            throw new InvalidInputException("The planned slot must be zero or positive");
        }
    }
}
