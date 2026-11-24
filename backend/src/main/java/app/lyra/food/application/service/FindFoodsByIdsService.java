package app.lyra.food.application.service;

import app.lyra.food.application.port.in.FindFoodsByIdsUseCase;
import app.lyra.food.application.port.out.FoodRepository;
import app.lyra.food.domain.Food;
import app.lyra.food.domain.FoodNotFoundException;
import app.lyra.shared.domain.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
class FindFoodsByIdsService implements FindFoodsByIdsUseCase {

    private final FoodRepository foodRepository;

    @Override
    public Map<UUID, Food> findByIds(UserId userId, Collection<UUID> ids) {
        Set<UUID> distinctIds = new LinkedHashSet<>(ids);
        Map<UUID, Food> found = foodRepository.findAllById(distinctIds).stream()
                .collect(Collectors.toMap(Food::getId, Function.identity()));
        for (UUID id : distinctIds) {
            if (!found.containsKey(id)) {
                throw new FoodNotFoundException(id);
            }
        }
        return found;
    }
}
