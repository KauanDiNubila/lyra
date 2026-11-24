package app.lyra.plan.application.service;

import app.lyra.plan.domain.DietPlan;
import app.lyra.plan.domain.PlanNotFoundException;
import app.lyra.plan.domain.PlannedMeal;
import app.lyra.shared.domain.InvalidInputException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static app.lyra.plan.application.service.PlanTestData.RICE;
import static app.lyra.plan.application.service.PlanTestData.lunchAndDinner;
import static app.lyra.plan.application.service.PlanTestData.meal;
import static app.lyra.plan.application.service.PlanTestData.riceInGrams;
import static app.lyra.plan.application.service.PlanTestData.riceInPortions;
import static app.lyra.plan.application.service.PlanTestData.targets;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReplacePlanServiceTest {

    private final InMemoryPlanRepository repository = new InMemoryPlanRepository();
    private final ReplacePlanService service = new ReplacePlanService(repository, new PlanFoodChecker(ids -> Map.of(RICE.getId(), RICE)));

    @Test
    void replacesTheContentKeepingTheIdAndTheActiveState() {
        DietPlan active = DietPlan.create("Cutting", targets(), lunchAndDinner()).activated();
        repository.save(active);

        DietPlan changed = service.replace(active.getId(), "Bulking", targets(), List.of(meal("Lanche", riceInGrams("50"))));

        assertThat(changed.getId()).isEqualTo(active.getId());
        assertThat(changed.isActive()).isTrue();
        assertThat(changed.getName()).isEqualTo("Bulking");
        assertThat(repository.findById(active.getId()).orElseThrow().getMeals()).extracting(PlannedMeal::name)
                .containsExactly("Lanche");
    }

    @Test
    void failsWhenThereIsNoPlanWithThatId() {
        UUID unknown = UUID.randomUUID();
        List<PlannedMeal> meals = lunchAndDinner();

        assertThatThrownBy(() -> service.replace(unknown, "Cutting", targets(), meals))
                .isInstanceOf(PlanNotFoundException.class)
                .hasMessage("Plan not found: " + unknown);
    }

    @Test
    void keepsThePlanAsItWasWhenTheNewContentIsInvalid() {
        DietPlan plan = DietPlan.create("Cutting", targets(), lunchAndDinner());
        repository.save(plan);
        UUID id = plan.getId();
        List<PlannedMeal> wrongUnit = List.of(meal("Almoço", riceInPortions("1")));

        assertThatThrownBy(() -> service.replace(id, "Bulking", targets(), wrongUnit)).isInstanceOf(InvalidInputException.class);
        assertThat(repository.findById(id).orElseThrow().getName()).isEqualTo("Cutting");
        assertThat(repository.findById(id).orElseThrow().getMeals()).hasSize(2);
    }
}
