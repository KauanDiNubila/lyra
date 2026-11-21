package app.lyra.food.application.port.out;

import app.lyra.food.domain.Food;
import app.lyra.food.domain.FoodSearchQuery;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FoodRepository {

    void save(Food food);

    Optional<Food> findById(UUID id);

    List<Food> search(FoodSearchQuery query, int limit);
}
