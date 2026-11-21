package app.lyra.food.application.port.in;

import app.lyra.food.domain.Food;

import java.util.UUID;

public interface FindFoodUseCase {

    Food findById(UUID id);
}
