package archfixture.food.application.service;

import archfixture.food.application.port.in.SearchFoods;
import archfixture.food.domain.Food;

public class FoodSearchService implements SearchFoods {

    @Override
    public Food byName(String name) {
        return new Food(name);
    }
}
