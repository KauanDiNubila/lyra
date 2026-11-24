package app.lyra.food.application.service;

import app.lyra.food.application.port.in.CreateCustomFoodUseCase;
import app.lyra.food.application.port.out.FoodRepository;
import app.lyra.food.domain.Food;
import app.lyra.food.domain.Nutrients;
import app.lyra.food.domain.Quantity;
import app.lyra.shared.domain.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class CustomFoodService implements CreateCustomFoodUseCase {

    private final FoodRepository foodRepository;

    @Override
    public Food create(UserId userId, String name, Quantity referenceAmount, Nutrients nutrients) {
        Food food = Food.custom(name, referenceAmount, nutrients);
        foodRepository.save(food);
        return food;
    }
}
