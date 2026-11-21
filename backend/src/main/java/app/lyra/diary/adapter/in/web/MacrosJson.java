package app.lyra.diary.adapter.in.web;

import app.lyra.diary.domain.Macros;

import java.math.BigDecimal;

record MacrosJson(BigDecimal kcal, BigDecimal protein, BigDecimal carbohydrate, BigDecimal fat) {

    static MacrosJson from(Macros macros) {
        return new MacrosJson(macros.kcal(), macros.protein(), macros.carbohydrate(), macros.fat());
    }
}
