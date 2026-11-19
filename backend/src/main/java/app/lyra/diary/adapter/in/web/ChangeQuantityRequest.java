package app.lyra.diary.adapter.in.web;

import app.lyra.food.domain.InvalidInputException;
import app.lyra.food.domain.Quantity;

record ChangeQuantityRequest(QuantityJson quantity) {

    Quantity toQuantity() {
        if (quantity == null) {
            throw new InvalidInputException("quantity is required");
        }
        return quantity.toDomain();
    }
}
