package app.lyra.food.adapter.in.web;

import app.lyra.food.domain.Nutrients;
import app.lyra.food.domain.Quantity;
import app.lyra.shared.domain.InvalidInputException;

record CreateFoodRequest(String name, QuantityJson referenceAmount, NutrientsJson nutrients) {

    Quantity toReferenceAmount() {
        if (referenceAmount == null) {
            throw new InvalidInputException("reference amount is required");
        }
        return referenceAmount.toDomain();
    }

    Nutrients toNutrients() {
        if (nutrients == null) {
            throw new InvalidInputException("nutrients are required");
        }
        return nutrients.toDomain();
    }
}
