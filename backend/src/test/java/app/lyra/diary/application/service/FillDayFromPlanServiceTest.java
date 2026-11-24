package app.lyra.diary.application.service;

import app.lyra.diary.domain.DiaryDay;
import app.lyra.diary.domain.DiaryEntry;
import app.lyra.diary.domain.NoActivePlanException;
import app.lyra.diary.domain.Origin;
import app.lyra.food.application.port.in.FindFoodsByIdsUseCase;
import app.lyra.food.domain.Food;
import app.lyra.food.domain.FoodNotFoundException;
import app.lyra.food.domain.MeasurementUnit;
import app.lyra.food.domain.Nutrients;
import app.lyra.food.domain.Quantity;
import app.lyra.plan.application.port.in.GetActivePlanUseCase;
import app.lyra.plan.domain.DailyTargets;
import app.lyra.plan.domain.DietPlan;
import app.lyra.plan.domain.PlannedItem;
import app.lyra.plan.domain.PlannedMeal;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FillDayFromPlanServiceTest {

    private static final LocalDate DATE = LocalDate.of(2026, 11, 19);
    private static final Food RICE = Food.custom("Arroz", grams("100"),
            new Nutrients(new BigDecimal("128.2585"), new BigDecimal("2.5208"), new BigDecimal("28.0598"),
                    new BigDecimal("0.227"), null, null));
    private static final Food CHICKEN = Food.custom("Frango", grams("100"),
            new Nutrients(new BigDecimal("159"), new BigDecimal("32"), BigDecimal.ZERO, new BigDecimal("2.5"), null, null));
    private static final DietPlan PLAN = DietPlan.create("Cutting",
            new DailyTargets(new BigDecimal("2000"), new BigDecimal("140"), new BigDecimal("200"), new BigDecimal("60")),
            List.of(
                    new PlannedMeal("Almoço", null, List.of(item(RICE, "150"), item(CHICKEN, "100"))),
                    new PlannedMeal("Jantar", null, List.of(item(RICE, "100")))))
            .activated();

    private final InMemoryDiaryEntryRepository repository = new InMemoryDiaryEntryRepository();

    @Test
    void fillsAnEmptyDayWithOneEntryForEachItemOfThePlan() {
        DiaryDay day = service(Optional.of(PLAN)).fillDay(DATE);

        assertThat(day.date()).isEqualTo(DATE);
        assertThat(day.entries()).extracting(DiaryEntry::getMealName).containsExactly("Almoço", "Almoço", "Jantar");
        assertThat(day.entries()).extracting(DiaryEntry::getFoodName).containsExactly("Arroz", "Frango", "Arroz");
        assertThat(day.entries()).extracting(entry -> entry.getQuantity().amount())
                .usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .containsExactly(new BigDecimal("150"), new BigDecimal("100"), new BigDecimal("100"));
        assertThat(day.entries()).extracting(entry -> entry.getPlanned().orElseThrow().slot()).containsExactly(0, 1, 2);
        assertThat(day.entries()).extracting(DiaryEntry::origin).containsOnly(Origin.PLANNED);
        assertThat(repository.findByDate(DATE)).isEqualTo(day.entries());
    }

    @Test
    void copiesTheFoodAndTheMacrosThatWerePlanned() {
        DiaryEntry rice = service(Optional.of(PLAN)).fillDay(DATE).entries().get(0);

        assertThat(rice.getFoodId()).isEqualTo(RICE.getId());
        assertThat(rice.getNutrients()).isEqualTo(RICE.getNutrients());
        assertThat(rice.getPlanned().orElseThrow().foodId()).isEqualTo(RICE.getId());
        assertThat(rice.getPlanned().orElseThrow().macros().kcal()).isEqualByComparingTo("192.38775");
        assertThat(rice.getPlanned().orElseThrow().macros()).isEqualTo(rice.macros());
    }

    @Test
    void leavesADayThatAlreadyHasEntriesAsItIs() {
        DiaryEntry extra = DiaryEntry.log(CHICKEN, DATE, "Lanche", grams("50"));
        repository.save(extra);

        DiaryDay day = service(Optional.of(PLAN)).fillDay(DATE);

        assertThat(day.entries()).containsExactly(extra);
        assertThat(repository.findByDate(DATE)).containsExactly(extra);
    }

    @Test
    void fillingTwiceDoesNotDuplicateTheEntries() {
        FillDayFromPlanService service = service(Optional.of(PLAN));

        DiaryDay first = service.fillDay(DATE);
        DiaryDay second = service.fillDay(DATE);

        assertThat(second.entries()).isEqualTo(first.entries());
        assertThat(repository.findByDate(DATE)).hasSize(3);
    }

    @Test
    void fillsOnlyTheDateThatWasAsked() {
        service(Optional.of(PLAN)).fillDay(DATE);

        assertThat(repository.findByDate(DATE.plusDays(1))).isEmpty();
        assertThat(repository.findByDate(DATE.minusDays(1))).isEmpty();
    }

    @Test
    void failsWhenThereIsNoActivePlanAndSavesNothing() {
        FillDayFromPlanService service = service(Optional.empty());

        assertThatThrownBy(() -> service.fillDay(DATE))
                .isInstanceOf(NoActivePlanException.class)
                .hasMessage("There is no active plan to fill the day");
        assertThat(repository.findByDate(DATE)).isEmpty();
    }

    @Test
    void failsWithoutAnActivePlanEvenWhenTheDayIsAlreadyFilled() {
        repository.save(DiaryEntry.log(CHICKEN, DATE, "Lanche", grams("50")));
        FillDayFromPlanService service = service(Optional.empty());

        assertThatThrownBy(() -> service.fillDay(DATE)).isInstanceOf(NoActivePlanException.class);
    }

    @Test
    void savesNothingWhenAFoodOfThePlanIsMissing() {
        FindFoodsByIdsUseCase missingFood = ids -> {
            throw new FoodNotFoundException(CHICKEN.getId());
        };
        FillDayFromPlanService service = new FillDayFromPlanService(() -> Optional.of(PLAN), missingFood, repository);

        assertThatThrownBy(() -> service.fillDay(DATE)).isInstanceOf(FoodNotFoundException.class);
        assertThat(repository.findByDate(DATE)).isEmpty();
    }

    private FillDayFromPlanService service(Optional<DietPlan> activePlan) {
        GetActivePlanUseCase getActivePlan = () -> activePlan;
        FindFoodsByIdsUseCase findFoods = ids -> Map.of(RICE.getId(), RICE, CHICKEN.getId(), CHICKEN);
        return new FillDayFromPlanService(getActivePlan, findFoods, repository);
    }

    private static PlannedItem item(Food food, String grams) {
        return new PlannedItem(food.getId(), grams(grams));
    }

    private static Quantity grams(String amount) {
        return new Quantity(new BigDecimal(amount), MeasurementUnit.GRAM);
    }
}
