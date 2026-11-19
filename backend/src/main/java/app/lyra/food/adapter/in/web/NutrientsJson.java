package app.lyra.food.adapter.in.web;

import app.lyra.food.domain.Nutrients;

import java.math.BigDecimal;

record NutrientsJson(
        BigDecimal kcal,
        BigDecimal protein,
        BigDecimal carbohydrate,
        BigDecimal fat,
        BigDecimal fiber,
        BigDecimal sodium) {

    static NutrientsJson from(Nutrients nutrients) {
        return new NutrientsJson(nutrients.kcal(), nutrients.protein(), nutrients.carbohydrate(), nutrients.fat(),
                nutrients.fiber(), nutrients.sodium());
    }

    Nutrients toDomain() {
        return new Nutrients(kcal, protein, carbohydrate, fat, fiber, sodium);
    }
}
