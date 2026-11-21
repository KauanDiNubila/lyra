package app.lyra.diary.domain;

import java.math.BigDecimal;

public record Macros(BigDecimal kcal, BigDecimal protein, BigDecimal carbohydrate, BigDecimal fat) {

    public static final Macros ZERO = new Macros(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

    public Macros plus(Macros other) {
        return new Macros(
                kcal.add(other.kcal),
                protein.add(other.protein),
                carbohydrate.add(other.carbohydrate),
                fat.add(other.fat));
    }
}
