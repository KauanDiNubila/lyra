package app.lyra.diary.domain;

import app.lyra.food.domain.Food;
import app.lyra.food.domain.InvalidInputException;
import app.lyra.food.domain.MeasurementUnit;
import app.lyra.food.domain.Nutrients;
import app.lyra.food.domain.Quantity;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DiaryEntryTest {

    private static final LocalDate DATE = LocalDate.of(2026, 11, 19);
    private static final Quantity HUNDRED_GRAMS = grams("100");
    private static final Nutrients RICE = new Nutrients(new BigDecimal("128.2585"), new BigDecimal("2.5208"),
            new BigDecimal("28.0598"), new BigDecimal("0.227"), null, null);
    private static final Nutrients ONES = new Nutrients(BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, null, null);

    @Test
    void macrosScaleWithTheQuantityEaten() {
        Macros macros = riceEntry("150").macros();

        assertThat(macros.kcal()).isEqualByComparingTo("192.38775");
        assertThat(macros.protein()).isEqualByComparingTo("3.7812");
        assertThat(macros.carbohydrate()).isEqualByComparingTo("42.0897");
        assertThat(macros.fat()).isEqualByComparingTo("0.3405");
    }

    @Test
    void eatingMoreAddsExactlyTheExtraMacros() {
        BigDecimal planned = riceEntry("150").macros().kcal();
        BigDecimal eaten = riceEntry("250").macros().kcal();

        assertThat(eaten).isEqualByComparingTo("320.64625");
        assertThat(eaten.subtract(planned)).isEqualByComparingTo("128.2585");
    }

    @Test
    void worksWithAReferenceThatIsNotOneHundredGrams() {
        Quantity onePortion = new Quantity(BigDecimal.ONE, MeasurementUnit.PORTION);
        Food milkshake = Food.custom("Milkshake", onePortion, new Nutrients(new BigDecimal("590.5"), new BigDecimal("13"),
                new BigDecimal("98.25"), new BigDecimal("16"), null, null));

        Macros macros = DiaryEntry.log(milkshake, DATE, "Lanche", new Quantity(new BigDecimal("2"), MeasurementUnit.PORTION)).macros();

        assertThat(macros.kcal()).isEqualByComparingTo("1181");
        assertThat(macros.carbohydrate()).isEqualByComparingTo("196.5");
    }

    @Test
    void dividesOnceWithThirtyFourDigits() {
        DiaryEntry entry = new DiaryEntry(UUID.randomUUID(), DATE, "Almoço", UUID.randomUUID(), "Arroz", grams("3"), ONES, grams("1"));

        assertThat(entry.macros().kcal()).isEqualTo(new BigDecimal("0.3333333333333333333333333333333333"));
    }

    @Test
    void keepsACopyOfTheFoodWhenItIsLogged() {
        Food rice = Food.custom("Arroz", HUNDRED_GRAMS, RICE);

        DiaryEntry entry = DiaryEntry.log(rice, DATE, "  Almoço ", grams("150"));

        assertThat(entry.getFoodId()).isEqualTo(rice.getId());
        assertThat(entry.getFoodName()).isEqualTo("Arroz");
        assertThat(entry.getFoodReference()).isEqualTo(HUNDRED_GRAMS);
        assertThat(entry.getNutrients()).isEqualTo(RICE);
        assertThat(entry.getQuantity()).isEqualTo(grams("150"));
        assertThat(entry.getDate()).isEqualTo(DATE);
        assertThat(entry.getMealName()).isEqualTo("Almoço");
    }

    @Test
    void rejectsAQuantityOfZero() {
        assertThatThrownBy(() -> riceEntry("0"))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage("Quantity must be positive");
    }

    @Test
    void rejectsAUnitDifferentFromTheFoodReference() {
        Food rice = Food.custom("Arroz", HUNDRED_GRAMS, RICE);
        Quantity milliliters = new Quantity(new BigDecimal("150"), MeasurementUnit.MILLILITER);

        assertThatThrownBy(() -> DiaryEntry.log(rice, DATE, "Almoço", milliliters))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage("Quantity must be in GRAM");
    }

    @Test
    void rejectsAFoodReferenceOfZero() {
        assertThatThrownBy(() -> new DiaryEntry(UUID.randomUUID(), DATE, "Almoço", UUID.randomUUID(), "Arroz", grams("0"), RICE, grams("1")))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage("Food reference amount must be positive");
    }

    @Test
    void rejectsABlankOrTooLongMealName() {
        Food rice = Food.custom("Arroz", HUNDRED_GRAMS, RICE);

        for (String invalid : new String[]{null, "", "   ", "x".repeat(51)}) {
            assertThatThrownBy(() -> DiaryEntry.log(rice, DATE, invalid, grams("100")))
                    .isInstanceOf(InvalidInputException.class)
                    .hasMessage("Meal name must have between 1 and 50 characters");
        }
    }

    @Test
    void acceptsAMealNameOfFiftyCharacters() {
        Food rice = Food.custom("Arroz", HUNDRED_GRAMS, RICE);

        assertThat(DiaryEntry.log(rice, DATE, "x".repeat(50), grams("100")).getMealName()).hasSize(50);
    }

    @Test
    void requiresEveryOtherField() {
        UUID id = UUID.randomUUID();
        UUID foodId = UUID.randomUUID();
        List<ThrowingCallable> missing = List.of(
                () -> new DiaryEntry(null, DATE, "Almoço", foodId, "Arroz", HUNDRED_GRAMS, RICE, grams("1")),
                () -> new DiaryEntry(id, null, "Almoço", foodId, "Arroz", HUNDRED_GRAMS, RICE, grams("1")),
                () -> new DiaryEntry(id, DATE, "Almoço", null, "Arroz", HUNDRED_GRAMS, RICE, grams("1")),
                () -> new DiaryEntry(id, DATE, "Almoço", foodId, null, HUNDRED_GRAMS, RICE, grams("1")),
                () -> new DiaryEntry(id, DATE, "Almoço", foodId, " ", HUNDRED_GRAMS, RICE, grams("1")),
                () -> new DiaryEntry(id, DATE, "Almoço", foodId, "Arroz", null, RICE, grams("1")),
                () -> new DiaryEntry(id, DATE, "Almoço", foodId, "Arroz", HUNDRED_GRAMS, null, grams("1")),
                () -> new DiaryEntry(id, DATE, "Almoço", foodId, "Arroz", HUNDRED_GRAMS, RICE, null));

        for (ThrowingCallable create : missing) {
            assertThatThrownBy(create).isInstanceOf(InvalidInputException.class);
        }
    }

    @Test
    void withQuantityChangesOnlyTheQuantity() {
        DiaryEntry entry = riceEntry("150");

        DiaryEntry changed = entry.withQuantity(grams("250"));

        assertThat(changed.getQuantity()).isEqualTo(grams("250"));
        assertThat(changed.getId()).isEqualTo(entry.getId());
        assertThat(changed.getDate()).isEqualTo(entry.getDate());
        assertThat(changed.getMealName()).isEqualTo(entry.getMealName());
        assertThat(changed.getFoodId()).isEqualTo(entry.getFoodId());
        assertThat(changed.getFoodName()).isEqualTo(entry.getFoodName());
        assertThat(changed.getNutrients()).isEqualTo(entry.getNutrients());
        assertThat(entry.getQuantity()).isEqualTo(grams("150"));
    }

    @Test
    void withQuantityRejectsZeroAndAnotherUnit() {
        DiaryEntry entry = riceEntry("150");
        Quantity milliliters = new Quantity(new BigDecimal("150"), MeasurementUnit.MILLILITER);

        assertThatThrownBy(() -> entry.withQuantity(grams("0")))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage("Quantity must be positive");
        assertThatThrownBy(() -> entry.withQuantity(milliliters))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage("Quantity must be in GRAM");
    }

    private static DiaryEntry riceEntry(String grams) {
        return DiaryEntry.log(Food.custom("Arroz", HUNDRED_GRAMS, RICE), DATE, "Almoço", grams(grams));
    }

    private static Quantity grams(String amount) {
        return new Quantity(new BigDecimal(amount), MeasurementUnit.GRAM);
    }
}
