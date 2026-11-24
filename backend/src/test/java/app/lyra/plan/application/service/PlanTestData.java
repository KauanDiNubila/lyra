package app.lyra.plan.application.service;

import app.lyra.food.domain.Food;
import app.lyra.food.domain.MeasurementUnit;
import app.lyra.food.domain.Nutrients;
import app.lyra.food.domain.Quantity;
import app.lyra.plan.domain.DailyTargets;
import app.lyra.plan.domain.PlannedItem;
import app.lyra.plan.domain.PlannedMeal;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

final class PlanTestData {

    static final Food RICE = Food.custom("Arroz", new Quantity(new BigDecimal("100"), MeasurementUnit.GRAM),
            new Nutrients(BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, null, null));

    private PlanTestData() {
    }

    static PlannedItem riceInGrams(String grams) {
        return new PlannedItem(RICE.getId(), new Quantity(new BigDecimal(grams), MeasurementUnit.GRAM));
    }

    static PlannedItem riceInPortions(String portions) {
        return new PlannedItem(RICE.getId(), new Quantity(new BigDecimal(portions), MeasurementUnit.PORTION));
    }

    static PlannedMeal meal(String name, PlannedItem... items) {
        return new PlannedMeal(name, null, List.of(items));
    }

    static List<PlannedMeal> lunchAndDinner() {
        return List.of(meal("Almoço", riceInGrams("150")), meal("Jantar", riceInGrams("100")));
    }

    static DailyTargets targets() {
        return new DailyTargets(new BigDecimal("2000"), new BigDecimal("100"), new BigDecimal("250"), new BigDecimal("70"));
    }

    static UUID unknownId() {
        return UUID.randomUUID();
    }
}
