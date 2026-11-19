package app.lyra.food.application.port.out;

import app.lyra.food.domain.Food;
import app.lyra.food.domain.FoodSearchQuery;

import java.util.List;

public interface FoodRepository {

    void save(Food food);

    List<Food> search(FoodSearchQuery query, int limit);
}
