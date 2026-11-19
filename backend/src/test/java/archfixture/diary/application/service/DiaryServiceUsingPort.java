package archfixture.diary.application.service;

import archfixture.food.application.port.in.SearchFoods;
import archfixture.food.domain.Food;

public class DiaryServiceUsingPort {

    private final SearchFoods searchFoods;

    public DiaryServiceUsingPort(SearchFoods searchFoods) {
        this.searchFoods = searchFoods;
    }

    public Food find(String name) {
        return searchFoods.byName(name);
    }
}
