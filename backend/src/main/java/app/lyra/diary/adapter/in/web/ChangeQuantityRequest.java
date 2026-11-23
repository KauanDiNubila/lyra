package app.lyra.diary.adapter.in.web;

import app.lyra.food.domain.Quantity;
import app.lyra.shared.domain.InvalidInputException;

record ChangeQuantityRequest(QuantityJson quantity) {

    Quantity toQuantity() {
        if (quantity == null) {
            throw new InvalidInputException("quantity is required");
        }
        return quantity.toDomain();
    }
}
