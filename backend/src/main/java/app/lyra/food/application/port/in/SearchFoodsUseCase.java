package app.lyra.food.application.port.in;

import app.lyra.food.domain.Food;
import app.lyra.shared.domain.UserId;

import java.util.List;

public interface SearchFoodsUseCase {

    List<Food> search(UserId userId, String query);
}
