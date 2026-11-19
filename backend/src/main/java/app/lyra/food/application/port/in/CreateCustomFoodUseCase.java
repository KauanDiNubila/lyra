package app.lyra.food.application.port.in;

import app.lyra.food.domain.Food;
import app.lyra.food.domain.Nutrients;
import app.lyra.food.domain.Quantity;

public interface CreateCustomFoodUseCase {

    Food create(String name, Quantity referenceAmount, Nutrients nutrients);
}
