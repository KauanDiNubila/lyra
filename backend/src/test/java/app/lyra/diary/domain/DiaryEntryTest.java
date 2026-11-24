package app.lyra.diary.domain;

import app.lyra.food.domain.Food;
import app.lyra.food.domain.MeasurementUnit;
import app.lyra.food.domain.Nutrients;
import app.lyra.food.domain.Quantity;
import app.lyra.shared.domain.InvalidInputException;
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

    @Test
    void anEntryLoggedByHandHasNoPlanAndIsAnExtra() {
        DiaryEntry entry = riceEntry("150");

        assertThat(entry.getPlanned()).isEmpty();
        assertThat(entry.origin()).isEqualTo(Origin.EXTRA);
    }

    @Test
    void anEntryFromThePlanRemembersWhatWasPlanned() {
        Food rice = Food.custom("Arroz", HUNDRED_GRAMS, RICE);

        DiaryEntry entry = DiaryEntry.fromPlan(rice, DATE, " Almoço ", grams("150"), 3);

        Planned planned = entry.getPlanned().orElseThrow();
        assertThat(planned.foodId()).isEqualTo(rice.getId());
        assertThat(planned.quantity()).isEqualTo(grams("150"));
        assertThat(planned.macros()).isEqualTo(entry.macros());
        assertThat(planned.slot()).isEqualTo(3);
        assertThat(entry.getMealName()).isEqualTo("Almoço");
        assertThat(entry.getFoodId()).isEqualTo(rice.getId());
        assertThat(entry.getQuantity()).isEqualTo(grams("150"));
        assertThat(entry.origin()).isEqualTo(Origin.PLANNED);
    }

    @Test
    void eatingADifferentAmountIsAnAdjustmentAndKeepsThePlannedMacros() {
        DiaryEntry entry = planEntry("150");

        DiaryEntry adjusted = entry.withQuantity(grams("250"));

        assertThat(adjusted.origin()).isEqualTo(Origin.ADJUSTED);
        assertThat(adjusted.getPlanned()).isEqualTo(entry.getPlanned());
        assertThat(adjusted.macros().kcal()).isEqualByComparingTo("320.64625");
        assertThat(adjusted.getPlanned().orElseThrow().macros().kcal()).isEqualByComparingTo("192.38775");
    }

    @Test
    void theSameAmountWrittenWithAnotherScaleIsStillThePlan() {
        assertThat(planEntry("150").withQuantity(grams("150.0")).origin()).isEqualTo(Origin.PLANNED);
    }

    @Test
    void aQuantityOfZeroSkipsAnEntryOfThePlan() {
        DiaryEntry entry = planEntry("150");

        DiaryEntry skipped = entry.withQuantity(grams("0"));

        assertThat(skipped.origin()).isEqualTo(Origin.SKIPPED);
        assertThat(skipped.macros().kcal()).isEqualByComparingTo("0");
        assertThat(skipped.getPlanned()).isEqualTo(entry.getPlanned());
    }

    @Test
    void anotherFoodInThePlaceOfThePlannedOneIsASwap() {
        DiaryEntry entry = planEntry("150");

        DiaryEntry swapped = entry.withFood(chicken(), grams("120"));

        assertThat(swapped.origin()).isEqualTo(Origin.SWAPPED);
        assertThat(swapped.getFoodName()).isEqualTo("Frango");
        assertThat(swapped.getPlanned()).isEqualTo(entry.getPlanned());
    }

    @Test
    void goingBackToThePlannedFoodIsThePlanAgain() {
        Food rice = Food.custom("Arroz", HUNDRED_GRAMS, RICE);
        DiaryEntry entry = DiaryEntry.fromPlan(rice, DATE, "Almoço", grams("150"), 0);

        DiaryEntry back = entry.withFood(chicken(), grams("120")).withFood(rice, grams("150"));

        assertThat(back.origin()).isEqualTo(Origin.PLANNED);
    }

    @Test
    void skippingASwappedEntryCountsAsSkipped() {
        DiaryEntry skipped = planEntry("150").withFood(chicken(), grams("0"));

        assertThat(skipped.origin()).isEqualTo(Origin.SKIPPED);
    }

    @Test
    void withFoodTakesANewSnapshotAndKeepsTheRest() {
        DiaryEntry entry = planEntry("150");
        Food chicken = chicken();

        DiaryEntry swapped = entry.withFood(chicken, grams("120"));

        assertThat(swapped.getFoodId()).isEqualTo(chicken.getId());
        assertThat(swapped.getFoodName()).isEqualTo("Frango");
        assertThat(swapped.getFoodReference()).isEqualTo(HUNDRED_GRAMS);
        assertThat(swapped.getNutrients()).isEqualTo(ONES);
        assertThat(swapped.getQuantity()).isEqualTo(grams("120"));
        assertThat(swapped.macros().kcal()).isEqualByComparingTo("1.2");
        assertThat(swapped.getId()).isEqualTo(entry.getId());
        assertThat(swapped.getDate()).isEqualTo(entry.getDate());
        assertThat(swapped.getMealName()).isEqualTo(entry.getMealName());
        assertThat(entry.getFoodName()).isEqualTo("Arroz");
    }

    @Test
    void withFoodRejectsAQuantityInAnotherUnit() {
        DiaryEntry entry = planEntry("150");
        Food milkshake = Food.custom("Milkshake", new Quantity(BigDecimal.ONE, MeasurementUnit.PORTION), ONES);

        assertThatThrownBy(() -> entry.withFood(milkshake, grams("1")))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage("Quantity must be in PORTION");
    }

    @Test
    void anExtraEntryStillRejectsZeroWhenItsFoodChanges() {
        DiaryEntry entry = riceEntry("150");

        assertThatThrownBy(() -> entry.withFood(chicken(), grams("0")))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage("Quantity must be positive");
    }

    @Test
    void withMealNameChangesOnlyTheNameAndTrimsIt() {
        DiaryEntry entry = planEntry("150");

        DiaryEntry renamed = entry.withMealName("  Pós-treino ");

        assertThat(renamed.getMealName()).isEqualTo("Pós-treino");
        assertThat(renamed.getId()).isEqualTo(entry.getId());
        assertThat(renamed.getQuantity()).isEqualTo(entry.getQuantity());
        assertThat(renamed.getPlanned()).isEqualTo(entry.getPlanned());
        assertThat(entry.getMealName()).isEqualTo("Almoço");
    }

    @Test
    void withMealNameKeepsTheValidationRule() {
        DiaryEntry entry = riceEntry("150");

        assertThatThrownBy(() -> entry.withMealName("  "))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage("Meal name must have between 1 and 50 characters");
    }

    private static DiaryEntry planEntry(String grams) {
        return DiaryEntry.fromPlan(Food.custom("Arroz", HUNDRED_GRAMS, RICE), DATE, "Almoço", grams(grams), 0);
    }

    private static Food chicken() {
        return Food.custom("Frango", HUNDRED_GRAMS, ONES);
    }

    private static DiaryEntry riceEntry(String grams) {
        return DiaryEntry.log(Food.custom("Arroz", HUNDRED_GRAMS, RICE), DATE, "Almoço", grams(grams));
    }

    private static Quantity grams(String amount) {
        return new Quantity(new BigDecimal(amount), MeasurementUnit.GRAM);
    }
}
