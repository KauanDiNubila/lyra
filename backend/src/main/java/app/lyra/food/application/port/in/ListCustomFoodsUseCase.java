package app.lyra.food.application.port.in;

import app.lyra.food.domain.Food;
import app.lyra.shared.domain.UserId;

import java.util.List;

public interface ListCustomFoodsUseCase {

    List<Food> listCustomFoods(UserId userId);
}
