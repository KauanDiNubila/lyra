package archfixture.food.adapter.in.web;

import archfixture.food.application.port.in.SearchFoods;
import archfixture.food.domain.Food;

public class FoodController {

    private final SearchFoods searchFoods;

    public FoodController(SearchFoods searchFoods) {
        this.searchFoods = searchFoods;
    }

    public Food get(String name) {
        return searchFoods.byName(name);
    }
}
