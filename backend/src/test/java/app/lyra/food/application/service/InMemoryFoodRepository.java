package app.lyra.food.application.service;

import app.lyra.food.application.port.out.FoodRepository;
import app.lyra.food.domain.Barcode;
import app.lyra.food.domain.Food;
import app.lyra.food.domain.FoodSearchQuery;
import app.lyra.food.domain.FoodSource;
import app.lyra.food.domain.SearchText;
import app.lyra.shared.domain.UserId;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

class InMemoryFoodRepository implements FoodRepository {

    private record Stored(UserId owner, Food food, Barcode barcode) {

        boolean isVisibleTo(UserId user) {
            return owner == null || owner.equals(user);
        }
    }

    private final List<Stored> foods = new ArrayList<>();

    @Override
    public void save(UserId owner, Food food) {
        if (food.getSource() != FoodSource.CUSTOM) {
            throw new IllegalArgumentException("Only custom foods are saved by a user");
        }
        foods.add(new Stored(owner, food, null));
    }

    void seedTaco(Food food) {
        foods.add(new Stored(null, food, null));
    }

    @Override
    public Optional<Food> findByBarcode(Barcode barcode) {
        return foods.stream().filter(stored -> barcode.equals(stored.barcode())).map(Stored::food).findFirst();
    }

    @Override
    public Food saveProduct(Barcode barcode, Food product) {
        if (product.getSource() != FoodSource.OPEN_FOOD_FACTS) {
            throw new IllegalArgumentException("Only products of the Open Food Facts are saved by barcode");
        }
        return findByBarcode(barcode).orElseGet(() -> {
            foods.add(new Stored(null, product, barcode));
            return product;
        });
    }

    @Override
    public Optional<Food> findById(UserId user, UUID id) {
        return visibleTo(user).filter(food -> food.getId().equals(id)).findFirst();
    }

    @Override
    public List<Food> findAllById(UserId user, Collection<UUID> ids) {
        return visibleTo(user).filter(food -> ids.contains(food.getId())).toList();
    }

    @Override
    public List<Food> search(UserId user, FoodSearchQuery query, int limit) {
        return visibleTo(user)
                .filter(food -> query.terms().stream().allMatch(SearchText.normalize(food.getName())::contains))
                .sorted(Comparator.comparingInt((Food food) -> food.getSource() == FoodSource.CUSTOM ? 0 : 1)
                        .thenComparingInt(food -> food.getName().length()))
                .limit(limit)
                .toList();
    }

    @Override
    public List<Food> findCustom(UserId user) {
        return foods.stream()
                .filter(stored -> user.equals(stored.owner()))
                .map(Stored::food)
                .sorted(Comparator.comparing(Food::getName).thenComparing(Food::getId))
                .toList();
    }

    List<Food> all() {
        return foods.stream().map(Stored::food).toList();
    }

    private Stream<Food> visibleTo(UserId user) {
        return foods.stream().filter(stored -> stored.isVisibleTo(user)).map(Stored::food);
    }
}
