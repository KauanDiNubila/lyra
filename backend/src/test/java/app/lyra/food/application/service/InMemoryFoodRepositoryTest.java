package app.lyra.food.application.service;

import app.lyra.food.application.port.out.FoodRepository;
import app.lyra.food.application.port.out.FoodRepositoryContract;

class InMemoryFoodRepositoryTest implements FoodRepositoryContract {

    private final InMemoryFoodRepository repository = new InMemoryFoodRepository();

    @Override
    public FoodRepository repository() {
        return repository;
    }
}
