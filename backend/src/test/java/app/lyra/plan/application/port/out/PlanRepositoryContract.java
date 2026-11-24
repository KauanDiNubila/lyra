package app.lyra.plan.application.port.out;

import app.lyra.food.domain.MeasurementUnit;
import app.lyra.food.domain.Quantity;
import app.lyra.plan.domain.DailyTargets;
import app.lyra.plan.domain.DietPlan;
import app.lyra.plan.domain.PlannedItem;
import app.lyra.plan.domain.PlannedMeal;
import app.lyra.shared.domain.UserId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public interface PlanRepositoryContract {

    PlanRepository repository();

    UUID savedFoodId();

    UserId owner();

    UserId otherOwner();

    @Test
    default void savesAPlanAndFindsItByIdWithAllItsContent() {
        DietPlan plan = plan("Cutting");

        repository().save(owner(), plan);

        DietPlan found = repository().findById(owner(), plan.getId()).orElseThrow();
        assertThat(found.getId()).isEqualTo(plan.getId());
        assertThat(found.getName()).isEqualTo("Cutting");
        assertThat(found.isActive()).isFalse();
        assertThat(found.getTargets().kcal()).isEqualByComparingTo("2000");
        assertThat(found.getTargets().protein()).isEqualByComparingTo("140.5");
        assertThat(found.getTargets().carbohydrate()).isEqualByComparingTo("200");
        assertThat(found.getTargets().fat()).isEqualByComparingTo("60");
        assertThat(found.getMeals()).extracting(PlannedMeal::name).containsExactly("Almoço", "Jantar");
        assertThat(found.getMeals().get(0).time()).isEqualTo(LocalTime.of(12, 30));
        assertThat(found.getMeals().get(1).time()).isNull();
        List<PlannedItem> lunch = found.getMeals().get(0).items();
        assertThat(lunch).extracting(PlannedItem::foodId)
                .containsExactlyElementsOf(plan.getMeals().get(0).items().stream().map(PlannedItem::foodId).toList());
        assertThat(lunch.get(0).quantity().amount()).isEqualByComparingTo("150");
        assertThat(lunch.get(0).quantity().unit()).isEqualTo(MeasurementUnit.GRAM);
        assertThat(lunch.get(1).quantity().amount()).isEqualByComparingTo("1.5");
        assertThat(lunch.get(1).quantity().unit()).isEqualTo(MeasurementUnit.PORTION);
        assertThat(found.getMeals().get(1).items()).hasSize(1);
    }

    @Test
    default void findsNothingForAnUnknownId() {
        assertThat(repository().findById(owner(), UUID.randomUUID())).isEmpty();
    }

    @Test
    default void listsThePlansInTheOrderTheyWereCreated() {
        DietPlan first = plan("Primeiro");
        DietPlan second = plan("Segundo");
        DietPlan third = plan("Terceiro");
        repository().save(owner(), first);
        repository().save(owner(), second);
        repository().save(owner(), third);

        assertThat(repository().findAll(owner())).containsExactly(first, second, third);
    }

    @Test
    default void listsNothingWhenThereAreNoPlans() {
        assertThat(repository().findAll(owner())).isEmpty();
    }

    @Test
    default void findsTheActivePlanOnlyWhileOnePlanIsActive() {
        DietPlan plan = plan("Cutting");
        repository().save(owner(), plan);
        assertThat(repository().findActive(owner())).isEmpty();

        repository().save(owner(), plan.activated());
        assertThat(repository().findActive(owner())).contains(plan);
        assertThat(repository().findActive(owner()).orElseThrow().isActive()).isTrue();

        repository().save(owner(), plan.deactivated());
        assertThat(repository().findActive(owner())).isEmpty();
    }

    @Test
    default void savingAgainWithTheSameIdReplacesTheContentAndKeepsThePlace() {
        DietPlan first = plan("Primeiro");
        DietPlan second = plan("Segundo");
        repository().save(owner(), first);
        repository().save(owner(), second);
        PlannedMeal onlyMeal = new PlannedMeal("Café da manhã", null, List.of(item("50")));

        repository().save(owner(), first.withContent("Novo nome", targets("1800"), List.of(onlyMeal)));

        assertThat(repository().findAll(owner())).containsExactly(first, second);
        DietPlan found = repository().findById(owner(), first.getId()).orElseThrow();
        assertThat(found.getName()).isEqualTo("Novo nome");
        assertThat(found.getTargets().kcal()).isEqualByComparingTo("1800");
        assertThat(found.getMeals()).extracting(PlannedMeal::name).containsExactly("Café da manhã");
        assertThat(found.getMeals().get(0).items()).hasSize(1);
        assertThat(found.getMeals().get(0).items().get(0).quantity().amount()).isEqualByComparingTo("50");
    }

    @Test
    default void anotherUsersPlanIsNeitherFoundNorListed() {
        DietPlan plan = plan("Cutting");
        repository().save(owner(), plan);

        assertThat(repository().findById(otherOwner(), plan.getId())).isEmpty();
        assertThat(repository().findAll(otherOwner())).isEmpty();
    }


    @Test
    default void refusesToSaveOverAPlanOfAnotherUser() {
        DietPlan plan = plan("Cutting");
        repository().save(owner(), plan);

        assertThatThrownBy(() -> repository().save(otherOwner(), plan.withContent("Roubado", plan.getTargets(), plan.getMeals())))
                .isInstanceOf(IllegalStateException.class);
        assertThat(repository().findById(owner(), plan.getId()).orElseThrow().getName()).isEqualTo("Cutting");
    }
    @Test
    default void eachUserHasTheirOwnActivePlan() {
        DietPlan mine = plan("Minha");
        DietPlan theirs = plan("Dele");
        repository().save(owner(), mine.activated());
        repository().save(otherOwner(), theirs.activated());

        assertThat(repository().findActive(owner()).orElseThrow().getName()).isEqualTo("Minha");
        assertThat(repository().findActive(otherOwner()).orElseThrow().getName()).isEqualTo("Dele");
    }

    default DietPlan plan(String name) {
        PlannedMeal lunch = new PlannedMeal("Almoço", LocalTime.of(12, 30), List.of(
                item("150"),
                new PlannedItem(savedFoodId(), new Quantity(new BigDecimal("1.5"), MeasurementUnit.PORTION))));
        PlannedMeal dinner = new PlannedMeal("Jantar", null, List.of(item("100")));
        return DietPlan.create(name, targets("2000"), List.of(lunch, dinner));
    }

    default PlannedItem item(String grams) {
        return new PlannedItem(savedFoodId(), new Quantity(new BigDecimal(grams), MeasurementUnit.GRAM));
    }

    private static DailyTargets targets(String kcal) {
        return new DailyTargets(new BigDecimal(kcal), new BigDecimal("140.5"), new BigDecimal("200"), new BigDecimal("60"));
    }
}
