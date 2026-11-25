package app.lyra.identity.application.service;

import app.lyra.diary.domain.DiaryEntry;
import app.lyra.food.domain.Food;
import app.lyra.food.domain.MeasurementUnit;
import app.lyra.food.domain.Nutrients;
import app.lyra.food.domain.Quantity;
import app.lyra.identity.application.port.in.ExportAccountUseCase.AccountExport;
import app.lyra.identity.domain.AuthProvider;
import app.lyra.identity.domain.User;
import app.lyra.identity.domain.UserNotFoundException;
import app.lyra.plan.domain.DailyTargets;
import app.lyra.plan.domain.DietPlan;
import app.lyra.plan.domain.PlannedItem;
import app.lyra.plan.domain.PlannedMeal;
import app.lyra.shared.domain.UserId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExportAccountServiceTest {

    private static final Quantity HUNDRED_GRAMS = new Quantity(new BigDecimal("100"), MeasurementUnit.GRAM);
    private static final Nutrients NUTRIENTS = new Nutrients(BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, null, null);
    private static final Food CAKE = Food.custom("Bolo", HUNDRED_GRAMS, NUTRIENTS);
    private static final DietPlan PLAN = DietPlan.create("Cutting",
            new DailyTargets(new BigDecimal("2000"), new BigDecimal("140"), new BigDecimal("200"), new BigDecimal("60")),
            List.of(new PlannedMeal("Almoço", null, List.of(new PlannedItem(CAKE.getId(), HUNDRED_GRAMS)))));
    private static final DiaryEntry ENTRY = DiaryEntry.log(CAKE, LocalDate.of(2026, 11, 19), "Almoço", HUNDRED_GRAMS);

    private final InMemoryUserRepository repository = new InMemoryUserRepository();

    @Test
    void gathersTheUserAndWhatEachModuleKnowsAboutThem() {
        User ana = User.create(AuthProvider.GOOGLE, "sub-1", "ana@example.com", "Ana");
        repository.save(ana);
        ExportAccountService service = new ExportAccountService(
                repository, user -> List.of(CAKE), user -> List.of(PLAN), user -> List.of(ENTRY));

        AccountExport export = service.export(ana.getId());

        assertThat(export.user()).isEqualTo(ana);
        assertThat(export.customFoods()).containsExactly(CAKE);
        assertThat(export.plans()).containsExactly(PLAN);
        assertThat(export.diaryEntries()).containsExactly(ENTRY);
    }

    @Test
    void asksEachModuleForTheDataOfTheUserThatIsExporting() {
        User ana = User.create(AuthProvider.GOOGLE, "sub-1", "ana@example.com", "Ana");
        repository.save(ana);
        ExportAccountService service = new ExportAccountService(
                repository,
                user -> user.equals(ana.getId()) ? List.of(CAKE) : List.of(),
                user -> user.equals(ana.getId()) ? List.of(PLAN) : List.of(),
                user -> user.equals(ana.getId()) ? List.of(ENTRY) : List.of());

        AccountExport export = service.export(ana.getId());

        assertThat(export.customFoods()).hasSize(1);
        assertThat(export.plans()).hasSize(1);
        assertThat(export.diaryEntries()).hasSize(1);
    }

    @Test
    void failsWhenThereIsNoUserWithThatId() {
        UserId unknown = UserId.random();
        ExportAccountService service = new ExportAccountService(repository, user -> List.of(), user -> List.of(), user -> List.of());

        assertThatThrownBy(() -> service.export(unknown))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found: " + unknown);
    }
}
