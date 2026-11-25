package app.lyra.food.application.service;

import app.lyra.food.application.port.in.ListCustomFoodsUseCase;
import app.lyra.food.application.port.out.FoodRepository;
import app.lyra.food.domain.Food;
import app.lyra.shared.domain.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
class ListCustomFoodsService implements ListCustomFoodsUseCase {

    private final FoodRepository foodRepository;

    @Override
    public List<Food> listCustomFoods(UserId userId) {
        return foodRepository.findCustom(userId);
    }
}
