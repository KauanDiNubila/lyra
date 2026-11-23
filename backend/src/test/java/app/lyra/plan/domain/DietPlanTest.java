package app.lyra.plan.domain;

import app.lyra.shared.domain.InvalidInputException;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static app.lyra.plan.domain.PlanFixtures.meal;
import static app.lyra.plan.domain.PlanFixtures.targets;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DietPlanTest {

    @Test
    void aNewPlanStartsInactiveWithATrimmedName() {
        DietPlan plan = DietPlan.create("  Cutting  ", targets(), List.of(meal("Almoço"), meal("Jantar")));

        assertThat(plan.getId()).isNotNull();
        assertThat(plan.getName()).isEqualTo("Cutting");
        assertThat(plan.isActive()).isFalse();
        assertThat(plan.getTargets()).isEqualTo(targets());
        assertThat(plan.getMeals()).extracting(PlannedMeal::name).containsExactly("Almoço", "Jantar");
    }

    @Test
    void rejectsABlankOrTooLongName() {
        List<PlannedMeal> meals = List.of(meal("Almoço"));

        for (String invalid : new String[]{"", "   ", "x".repeat(101)}) {
            assertThatThrownBy(() -> DietPlan.create(invalid, targets(), meals))
                    .isInstanceOf(InvalidInputException.class)
                    .hasMessage("Plan name must have between 1 and 100 characters");
        }
    }

    @Test
    void acceptsANameOfOneHundredCharacters() {
        assertThat(DietPlan.create("x".repeat(100), targets(), List.of(meal("Almoço"))).getName()).hasSize(100);
    }

    @Test
    void needsAtLeastOneMeal() {
        assertThatThrownBy(() -> DietPlan.create("Cutting", targets(), List.of()))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage("A plan needs at least one meal");
    }

    @Test
    void rejectsTwoMealsWithTheSameName() {
        List<PlannedMeal> meals = List.of(meal("Almoço"), meal(" Almoço "));

        assertThatThrownBy(() -> DietPlan.create("Cutting", targets(), meals))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage("Meal names must be different inside a plan");
    }

    @Test
    void treatsMealNamesThatDifferOnlyInCaseAsDifferentMeals() {
        DietPlan plan = DietPlan.create("Cutting", targets(), List.of(meal("Almoço"), meal("almoço")));

        assertThat(plan.getMeals()).hasSize(2);
    }

    @Test
    void requiresEveryOtherField() {
        UUID id = UUID.randomUUID();
        List<PlannedMeal> meals = List.of(meal("Almoço"));
        List<ThrowingCallable> missing = List.of(
                () -> new DietPlan(null, "Cutting", targets(), meals, false),
                () -> new DietPlan(id, null, targets(), meals, false),
                () -> new DietPlan(id, "Cutting", null, meals, false),
                () -> new DietPlan(id, "Cutting", targets(), null, false));

        for (ThrowingCallable create : missing) {
            assertThatThrownBy(create).isInstanceOf(InvalidInputException.class);
        }
    }

    @Test
    void keepsACopyOfTheMeals() {
        List<PlannedMeal> meals = new ArrayList<>(List.of(meal("Almoço")));
        DietPlan plan = DietPlan.create("Cutting", targets(), meals);

        meals.add(meal("Jantar"));

        assertThat(plan.getMeals()).hasSize(1);
        assertThatThrownBy(() -> plan.getMeals().add(meal("Jantar"))).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void withContentReplacesTheContentAndKeepsTheIdAndTheState() {
        DietPlan active = DietPlan.create("Cutting", targets(), List.of(meal("Almoço"))).activated();

        DietPlan changed = active.withContent("Bulking", targets(), List.of(meal("Jantar")));

        assertThat(changed.getId()).isEqualTo(active.getId());
        assertThat(changed.isActive()).isTrue();
        assertThat(changed.getName()).isEqualTo("Bulking");
        assertThat(changed.getMeals()).extracting(PlannedMeal::name).containsExactly("Jantar");
        assertThat(active.getName()).isEqualTo("Cutting");
    }

    @Test
    void withContentKeepsTheValidationRules() {
        DietPlan plan = DietPlan.create("Cutting", targets(), List.of(meal("Almoço")));
        List<PlannedMeal> none = List.of();

        assertThatThrownBy(() -> plan.withContent("Cutting", targets(), none)).isInstanceOf(InvalidInputException.class);
    }

    @Test
    void activatesAndDeactivatesWithoutChangingTheContent() {
        DietPlan plan = DietPlan.create("Cutting", targets(), List.of(meal("Almoço")));

        DietPlan active = plan.activated();
        DietPlan inactive = active.deactivated();

        assertThat(active.isActive()).isTrue();
        assertThat(active.getName()).isEqualTo("Cutting");
        assertThat(active.getMeals()).isEqualTo(plan.getMeals());
        assertThat(inactive.isActive()).isFalse();
        assertThat(plan.isActive()).isFalse();
    }

    @Test
    void twoPlansWithTheSameIdAreTheSamePlan() {
        DietPlan plan = DietPlan.create("Cutting", targets(), List.of(meal("Almoço")));

        assertThat(plan.activated()).isEqualTo(plan);
        assertThat(plan.activated().hashCode()).isEqualTo(plan.hashCode());
        assertThat(DietPlan.create("Cutting", targets(), List.of(meal("Almoço")))).isNotEqualTo(plan);
    }
}
