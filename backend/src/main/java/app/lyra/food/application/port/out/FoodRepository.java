package app.lyra.food.application.port.out;

import app.lyra.food.domain.Food;
import app.lyra.food.domain.FoodSearchQuery;
import app.lyra.shared.domain.UserId;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FoodRepository {

    void save(UserId owner, Food food);

    Optional<Food> findById(UserId user, UUID id);

    List<Food> findAllById(UserId user, Collection<UUID> ids);

    List<Food> search(UserId user, FoodSearchQuery query, int limit);

    List<Food> findCustom(UserId user);
}
