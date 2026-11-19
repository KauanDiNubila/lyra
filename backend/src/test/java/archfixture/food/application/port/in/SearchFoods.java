package archfixture.food.application.port.in;

import archfixture.food.domain.Food;

public interface SearchFoods {

    Food byName(String name);
}
