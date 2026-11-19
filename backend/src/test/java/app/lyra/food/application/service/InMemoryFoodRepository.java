package app.lyra.food.application.service;

import app.lyra.food.application.port.out.FoodRepository;
import app.lyra.food.domain.Food;
import app.lyra.food.domain.FoodSearchQuery;
import app.lyra.food.domain.SearchText;

import java.util.ArrayList;
import java.util.List;

class InMemoryFoodRepository implements FoodRepository {

    private final List<Food> foods = new ArrayList<>();

    @Override
    public void save(Food food) {
        foods.add(food);
    }

    @Override
    public List<Food> search(FoodSearchQuery query, int limit) {
        return foods.stream()
                .filter(food -> query.terms().stream().allMatch(SearchText.normalize(food.getName())::contains))
                .limit(limit)
                .toList();
    }

    List<Food> all() {
        return List.copyOf(foods);
    }
}
