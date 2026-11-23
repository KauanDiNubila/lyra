package app.lyra.food.domain;

import app.lyra.shared.domain.NotFoundException;

import java.util.UUID;

public class FoodNotFoundException extends NotFoundException {

    public FoodNotFoundException(UUID id) {
        super("Food not found: " + id);
    }
}
