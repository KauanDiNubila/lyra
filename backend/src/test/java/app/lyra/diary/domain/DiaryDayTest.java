package app.lyra.diary.domain;

import app.lyra.diary.domain.DiaryDay.Meal;
import app.lyra.food.domain.Food;
import app.lyra.food.domain.MeasurementUnit;
import app.lyra.food.domain.Nutrients;
import app.lyra.food.domain.Quantity;
import app.lyra.shared.domain.InvalidInputException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DiaryDayTest {

    private static final LocalDate DATE = LocalDate.of(2026, 11, 19);
    private static final Quantity HUNDRED_GRAMS = grams("100");
    private static final Food RICE = food("Arroz", "128", "2", "28", "0.2");
    private static final Food BEANS = food("Feijão", "76", "5", "14", "0.5");

    @Test
    void groupsEntriesByMealInTheOrderTheyAppear() {
        DiaryEntry lunchRice = DiaryEntry.log(RICE, DATE, "Almoço", grams("100"));
        DiaryEntry breakfastRice = DiaryEntry.log(RICE, DATE, "Café da manhã", grams("50"));
        DiaryEntry lunchBeans = DiaryEntry.log(BEANS, DATE, "Almoço", grams("100"));

        List<Meal> meals = new DiaryDay(DATE, List.of(lunchRice, breakfastRice, lunchBeans)).meals();

        assertThat(meals).extracting(Meal::name).containsExactly("Almoço", "Café da manhã");
        assertThat(meals.get(0).entries()).containsExactly(lunchRice, lunchBeans);
        assertThat(meals.get(1).entries()).containsExactly(breakfastRice);
    }

    @Test
    void sumsTheMacrosOfEachMeal() {
        DiaryDay day = new DiaryDay(DATE, List.of(
                DiaryEntry.log(RICE, DATE, "Almoço", grams("100")),
                DiaryEntry.log(BEANS, DATE, "Almoço", grams("200")),
                DiaryEntry.log(RICE, DATE, "Jantar", grams("50"))));

        Macros lunch = day.meals().get(0).total();
        Macros dinner = day.meals().get(1).total();

        assertThat(lunch.kcal()).isEqualByComparingTo("280");
        assertThat(lunch.protein()).isEqualByComparingTo("12");
        assertThat(dinner.kcal()).isEqualByComparingTo("64");
    }

    @Test
    void sumsTheMacrosOfTheWholeDay() {
        DiaryDay day = new DiaryDay(DATE, List.of(
                DiaryEntry.log(RICE, DATE, "Almoço", grams("100")),
                DiaryEntry.log(BEANS, DATE, "Almoço", grams("200")),
                DiaryEntry.log(RICE, DATE, "Jantar", grams("50"))));

        Macros total = day.total();

        assertThat(total.kcal()).isEqualByComparingTo("344");
        assertThat(total.protein()).isEqualByComparingTo("13");
        assertThat(total.carbohydrate()).isEqualByComparingTo("70");
        assertThat(total.fat()).isEqualByComparingTo("1.3");
    }

    @Test
    void anEmptyDayHasNoMealsAndZeroMacros() {
        DiaryDay day = new DiaryDay(DATE, List.of());

        assertThat(day.meals()).isEmpty();
        assertThat(day.total()).isEqualTo(Macros.ZERO);
    }

    @Test
    void rejectsAnEntryFromAnotherDate() {
        DiaryEntry yesterday = DiaryEntry.log(RICE, DATE.minusDays(1), "Almoço", grams("100"));
        List<DiaryEntry> entries = List.of(yesterday);

        assertThatThrownBy(() -> new DiaryDay(DATE, entries))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage("Every entry must be from 2026-11-19");
    }

    @Test
    void requiresDateAndEntries() {
        assertThatThrownBy(() -> new DiaryDay(null, List.of())).isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> new DiaryDay(DATE, null)).isInstanceOf(InvalidInputException.class);
    }

    @Test
    void theEntriesOfAMealCannotBeChanged() {
        DiaryEntry lunch = DiaryEntry.log(RICE, DATE, "Almoço", grams("100"));
        List<DiaryEntry> lunchEntries = new DiaryDay(DATE, List.of(lunch)).meals().getFirst().entries();

        assertThatThrownBy(() -> lunchEntries.add(lunch)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void sumsWhatWasPlannedIncludingTheSkippedAndLeavingOutTheExtras() {
        DiaryDay day = dayWithAdjustedSkippedAndExtra(Optional.empty());

        Macros planned = day.plannedTotal();

        assertThat(planned.kcal()).isEqualByComparingTo("268");
        assertThat(planned.protein()).isEqualByComparingTo("8");
    }

    @Test
    void theDifferenceFromThePlanIsWhatWasEatenMinusWhatWasPlanned() {
        DiaryDay day = dayWithAdjustedSkippedAndExtra(Optional.empty());

        Macros difference = day.differenceFromPlan();

        assertThat(day.total().kcal()).isEqualByComparingTo("358");
        assertThat(difference.kcal()).isEqualByComparingTo("90");
        assertThat(difference.protein()).isEqualByComparingTo("-0.5");
    }

    @Test
    void eachMealHasItsOwnPlannedTotalAndDifference() {
        List<Meal> meals = dayWithAdjustedSkippedAndExtra(Optional.empty()).meals();

        assertThat(meals).extracting(Meal::name).containsExactly("Almoço", "Jantar", "Lanche");
        assertThat(meals.get(0).plannedTotal().kcal()).isEqualByComparingTo("192");
        assertThat(meals.get(0).differenceFromPlan().kcal()).isEqualByComparingTo("128");
        assertThat(meals.get(1).plannedTotal().kcal()).isEqualByComparingTo("76");
        assertThat(meals.get(1).differenceFromPlan().kcal()).isEqualByComparingTo("-76");
        assertThat(meals.get(2).plannedTotal()).isEqualTo(Macros.ZERO);
        assertThat(meals.get(2).differenceFromPlan().kcal()).isEqualByComparingTo("38");
    }

    @Test
    void theDifferenceFromTheTargetsIsWhatWasEatenMinusTheTargets() {
        Macros targets = new Macros(new BigDecimal("2000"), new BigDecimal("140"), new BigDecimal("200"), new BigDecimal("60"));

        DiaryDay day = dayWithAdjustedSkippedAndExtra(Optional.of(targets));

        assertThat(day.targets()).contains(targets);
        assertThat(day.differenceFromTargets().orElseThrow().kcal()).isEqualByComparingTo("-1642");
        assertThat(day.differenceFromTargets().orElseThrow().protein()).isEqualByComparingTo("-132.5");
    }

    @Test
    void hasNoDifferenceFromTargetsWhenThereAreNoTargets() {
        assertThat(dayWithAdjustedSkippedAndExtra(Optional.empty()).differenceFromTargets()).isEmpty();
    }

    @Test
    void anEmptyDayHasNothingPlannedAndNoDifference() {
        DiaryDay day = new DiaryDay(DATE, List.of());

        assertThat(day.plannedTotal()).isEqualTo(Macros.ZERO);
        assertThat(day.differenceFromPlan().kcal()).isEqualByComparingTo("0");
    }

    @Test
    void requiresTheOptionalTargets() {
        assertThatThrownBy(() -> new DiaryDay(DATE, List.of(), null)).isInstanceOf(InvalidInputException.class);
    }

    private static DiaryDay dayWithAdjustedSkippedAndExtra(Optional<Macros> targets) {
        DiaryEntry adjusted = DiaryEntry.fromPlan(RICE, DATE, "Almoço", grams("150"), 0).withQuantity(grams("250"));
        DiaryEntry skipped = DiaryEntry.fromPlan(BEANS, DATE, "Jantar", grams("100"), 1).withQuantity(grams("0"));
        DiaryEntry extra = DiaryEntry.log(BEANS, DATE, "Lanche", grams("50"));
        return new DiaryDay(DATE, List.of(adjusted, skipped, extra), targets);
    }

    private static Food food(String name, String kcal, String protein, String carbohydrate, String fat) {
        return Food.custom(name, HUNDRED_GRAMS, new Nutrients(new BigDecimal(kcal), new BigDecimal(protein),
                new BigDecimal(carbohydrate), new BigDecimal(fat), null, null));
    }

    private static Quantity grams(String amount) {
        return new Quantity(new BigDecimal(amount), MeasurementUnit.GRAM);
    }
}
