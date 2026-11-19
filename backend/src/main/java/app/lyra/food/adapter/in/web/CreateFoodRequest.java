package app.lyra.food.adapter.in.web;

import jakarta.validation.constraints.NotNull;

record CreateFoodRequest(String name, @NotNull QuantityJson referenceAmount, @NotNull NutrientsJson nutrients) {
}
