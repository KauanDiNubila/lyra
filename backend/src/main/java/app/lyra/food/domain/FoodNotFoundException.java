package app.lyra.food.domain;

import java.util.UUID;

public class FoodNotFoundException extends RuntimeException {

    public FoodNotFoundException(UUID id) {
        super("Food not found: " + id);
    }
}
