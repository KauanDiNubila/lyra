package app.lyra.food.application.port.in;

import app.lyra.food.domain.Food;

import java.util.List;

public interface SearchFoodsUseCase {

    List<Food> search(String query);
}
