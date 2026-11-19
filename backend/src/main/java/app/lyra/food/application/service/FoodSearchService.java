package app.lyra.food.application.service;

import app.lyra.food.application.port.in.SearchFoodsUseCase;
import app.lyra.food.application.port.out.FoodRepository;
import app.lyra.food.domain.Food;
import app.lyra.food.domain.FoodSearchQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
class FoodSearchService implements SearchFoodsUseCase {

    static final int MAX_RESULTS = 20;

    private final FoodRepository foodRepository;

    @Override
    public List<Food> search(String query) {
        return foodRepository.search(FoodSearchQuery.of(query), MAX_RESULTS);
    }
}
