package app.lyra.food.application.service;

import app.lyra.food.application.port.in.FindFoodUseCase;
import app.lyra.food.application.port.out.FoodRepository;
import app.lyra.food.domain.Food;
import app.lyra.food.domain.FoodNotFoundException;
import app.lyra.shared.domain.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
class FindFoodService implements FindFoodUseCase {

    private final FoodRepository foodRepository;

    @Override
    public Food findById(UserId userId, UUID id) {
        return foodRepository.findById(userId, id).orElseThrow(() -> new FoodNotFoundException(id));
    }
}
