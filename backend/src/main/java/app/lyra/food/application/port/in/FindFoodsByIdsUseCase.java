package app.lyra.food.application.port.in;

import app.lyra.food.domain.Food;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public interface FindFoodsByIdsUseCase {

    Map<UUID, Food> findByIds(Collection<UUID> ids);
}
