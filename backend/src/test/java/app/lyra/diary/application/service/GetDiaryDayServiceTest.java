package app.lyra.diary.application.service;

import app.lyra.diary.domain.DiaryDay;
import app.lyra.diary.domain.DiaryEntry;
import app.lyra.diary.domain.Macros;
import app.lyra.food.domain.Food;
import app.lyra.food.domain.MeasurementUnit;
import app.lyra.food.domain.Nutrients;
import app.lyra.food.domain.Quantity;
import app.lyra.plan.application.port.in.GetActivePlanUseCase;
import app.lyra.plan.domain.DailyTargets;
import app.lyra.plan.domain.DietPlan;
import app.lyra.plan.domain.PlannedItem;
import app.lyra.plan.domain.PlannedMeal;
import app.lyra.shared.domain.UserId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class GetDiaryDayServiceTest {

    private static final UserId USER = UserId.random();

    private static final LocalDate DATE = LocalDate.of(2026, 11, 19);
    private static final Food RICE = Food.custom("Arroz", grams("100"),
            new Nutrients(new BigDecimal("128"), new BigDecimal("2"), new BigDecimal("28"), new BigDecimal("0.2"), null, null));
    private static final DietPlan ACTIVE_PLAN = DietPlan.create("Cutting",
            new DailyTargets(new BigDecimal("2000"), new BigDecimal("140"), new BigDecimal("200"), new BigDecimal("60")),
            List.of(new PlannedMeal("Almoço", null, List.of(new PlannedItem(RICE.getId(), grams("150"))))))
            .activated();

    private final InMemoryDiaryEntryRepository repository = new InMemoryDiaryEntryRepository();

    @Test
    void returnsTheEntriesOfThatDateOnly() {
        DiaryEntry today = DiaryEntry.log(RICE, DATE, "Almoço", grams("100"));
        repository.save(USER, today);
        repository.save(USER, DiaryEntry.log(RICE, DATE.minusDays(1), "Almoço", grams("100")));

        DiaryDay day = service(Optional.empty()).getDay(USER, DATE);

        assertThat(day.date()).isEqualTo(DATE);
        assertThat(day.entries()).containsExactly(today);
    }

    @Test
    void returnsAnEmptyDayWhenNothingWasEaten() {
        DiaryDay day = service(Optional.empty()).getDay(USER, DATE);

        assertThat(day.entries()).isEmpty();
        assertThat(day.total()).isEqualTo(Macros.ZERO);
    }

    @Test
    void takesTheTargetsFromThePlanThatIsActiveNow() {
        repository.save(USER, DiaryEntry.log(RICE, DATE, "Almoço", grams("100")));

        DiaryDay day = service(Optional.of(ACTIVE_PLAN)).getDay(USER, DATE);

        Macros targets = day.targets().orElseThrow();
        assertThat(targets.kcal()).isEqualByComparingTo("2000");
        assertThat(targets.protein()).isEqualByComparingTo("140");
        assertThat(targets.carbohydrate()).isEqualByComparingTo("200");
        assertThat(targets.fat()).isEqualByComparingTo("60");
        assertThat(day.differenceFromTargets().orElseThrow().kcal()).isEqualByComparingTo("-1872");
    }

    @Test
    void hasNoTargetsWhenNoPlanIsActive() {
        DiaryDay day = service(Optional.empty()).getDay(USER, DATE);

        assertThat(day.targets()).isEmpty();
        assertThat(day.differenceFromTargets()).isEmpty();
    }

    @Test
    void doesNotFillTheDayItReads() {
        service(Optional.of(ACTIVE_PLAN)).getDay(USER, DATE);

        assertThat(repository.findByDate(USER, DATE)).isEmpty();
    }

    private GetDiaryDayService service(Optional<DietPlan> activePlan) {
        GetActivePlanUseCase getActivePlan = userId -> activePlan;
        return new GetDiaryDayService(getActivePlan, repository);
    }

    private static Quantity grams(String amount) {
        return new Quantity(new BigDecimal(amount), MeasurementUnit.GRAM);
    }
}
