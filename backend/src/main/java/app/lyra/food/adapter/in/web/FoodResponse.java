package app.lyra.food.adapter.in.web;

import app.lyra.food.domain.Food;
import app.lyra.food.domain.FoodSource;

import java.util.UUID;

record FoodResponse(UUID id, String name, FoodSource source, QuantityJson referenceAmount, NutrientsJson nutrients) {

    static FoodResponse from(Food food) {
        return new FoodResponse(food.getId(), food.getName(), food.getSource(),
                QuantityJson.from(food.getReferenceAmount()), NutrientsJson.from(food.getNutrients()));
    }
}
