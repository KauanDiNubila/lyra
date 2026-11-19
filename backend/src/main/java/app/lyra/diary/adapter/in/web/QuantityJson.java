package app.lyra.diary.adapter.in.web;

import app.lyra.food.domain.MeasurementUnit;
import app.lyra.food.domain.Quantity;

import java.math.BigDecimal;

record QuantityJson(BigDecimal amount, MeasurementUnit unit) {

    static QuantityJson from(Quantity quantity) {
        return new QuantityJson(quantity.amount(), quantity.unit());
    }

    Quantity toDomain() {
        return new Quantity(amount, unit);
    }
}
