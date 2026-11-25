package app.lyra.diary.adapter.in.web;

import app.lyra.diary.application.port.in.QuickAddUseCase.QuickAddCommand;
import app.lyra.food.domain.Nutrients;
import app.lyra.shared.domain.InvalidInputException;

import java.math.BigDecimal;
import java.time.LocalDate;

record QuickAddRequest(String mealName, String name, BigDecimal portions, MacrosJson macros) {

    QuickAddCommand toCommand(LocalDate date) {
        if (macros == null) {
            throw new InvalidInputException("macros are required");
        }
        BigDecimal eaten = portions == null ? BigDecimal.ONE : portions;
        Nutrients nutrients = new Nutrients(macros.kcal(), macros.protein(), macros.carbohydrate(), macros.fat(), null, null);
        return new QuickAddCommand(date, mealName, name, eaten, nutrients);
    }
}
