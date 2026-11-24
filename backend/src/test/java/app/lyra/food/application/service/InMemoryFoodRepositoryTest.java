package app.lyra.food.application.service;

import app.lyra.food.application.port.out.FoodRepository;
import app.lyra.food.application.port.out.FoodRepositoryContract;
import app.lyra.food.domain.Food;
import app.lyra.shared.domain.UserId;

class InMemoryFoodRepositoryTest implements FoodRepositoryContract {

    private final InMemoryFoodRepository repository = new InMemoryFoodRepository();
    private final UserId owner = UserId.random();
    private final UserId otherOwner = UserId.random();

    @Override
    public FoodRepository repository() {
        return repository;
    }

    @Override
    public void seedTaco(Food food) {
        repository.seedTaco(food);
    }

    @Override
    public UserId owner() {
        return owner;
    }

    @Override
    public UserId otherOwner() {
        return otherOwner;
    }
}
