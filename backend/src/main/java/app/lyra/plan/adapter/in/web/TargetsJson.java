package app.lyra.plan.adapter.in.web;

import app.lyra.plan.domain.DailyTargets;

import java.math.BigDecimal;

record TargetsJson(BigDecimal kcal, BigDecimal protein, BigDecimal carbohydrate, BigDecimal fat) {

    static TargetsJson from(DailyTargets targets) {
        return new TargetsJson(targets.kcal(), targets.protein(), targets.carbohydrate(), targets.fat());
    }

    DailyTargets toDomain() {
        return new DailyTargets(kcal, protein, carbohydrate, fat);
    }
}
