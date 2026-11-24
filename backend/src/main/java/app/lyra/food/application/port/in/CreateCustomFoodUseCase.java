package app.lyra.food.application.port.in;

import app.lyra.food.domain.Food;
import app.lyra.food.domain.Nutrients;
import app.lyra.food.domain.Quantity;
import app.lyra.shared.domain.UserId;

public interface CreateCustomFoodUseCase {

    Food create(UserId userId, String name, Quantity referenceAmount, Nutrients nutrients);
}
