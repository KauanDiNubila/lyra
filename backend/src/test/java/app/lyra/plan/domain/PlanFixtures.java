package app.lyra.plan.domain;

import app.lyra.food.domain.MeasurementUnit;
import app.lyra.food.domain.Quantity;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

final class PlanFixtures {

    static final UUID RICE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private PlanFixtures() {
    }

    static PlannedItem item(String grams) {
        return new PlannedItem(RICE_ID, new Quantity(new BigDecimal(grams), MeasurementUnit.GRAM));
    }

    static PlannedMeal meal(String name) {
        return new PlannedMeal(name, null, List.of(item("150")));
    }

    static DailyTargets targets() {
        return new DailyTargets(new BigDecimal("2000"), new BigDecimal("100"), new BigDecimal("250"), new BigDecimal("70"));
    }
}
