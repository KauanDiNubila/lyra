package app.lyra.food.application.port.in;

import app.lyra.food.domain.Food;
import app.lyra.shared.domain.UserId;

import java.util.UUID;

public interface FindFoodUseCase {

    Food findById(UserId userId, UUID id);
}
