package app.lyra.plan.application.service;

import app.lyra.plan.domain.DietPlan;
import app.lyra.plan.domain.PlanNotFoundException;
import app.lyra.shared.domain.UserId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static app.lyra.plan.application.service.PlanTestData.lunchAndDinner;
import static app.lyra.plan.application.service.PlanTestData.targets;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GetAndListPlansServiceTest {

    private final InMemoryPlanRepository repository = new InMemoryPlanRepository();

    @Test
    void getReturnsThePlanWithThatId() {
        DietPlan plan = DietPlan.create("Cutting", targets(), lunchAndDinner());
        repository.save(plan);
        repository.save(DietPlan.create("Bulking", targets(), lunchAndDinner()));

        assertThat(new GetPlanService(repository).getPlan(UserId.random(), plan.getId())).isEqualTo(plan);
    }

    @Test
    void getFailsWhenThereIsNoPlanWithThatId() {
        UUID unknown = UUID.randomUUID();
        GetPlanService service = new GetPlanService(repository);

        assertThatThrownBy(() -> service.getPlan(UserId.random(), unknown))
                .isInstanceOf(PlanNotFoundException.class)
                .hasMessage("Plan not found: " + unknown);
    }

    @Test
    void listReturnsThePlansInTheOrderTheyWereSaved() {
        DietPlan first = DietPlan.create("Cutting", targets(), lunchAndDinner());
        DietPlan second = DietPlan.create("Bulking", targets(), lunchAndDinner());
        repository.save(first);
        repository.save(second);

        assertThat(new ListPlansService(repository).listPlans(UserId.random())).containsExactly(first, second);
    }

    @Test
    void listReturnsNothingWhenThereAreNoPlans() {
        assertThat(new ListPlansService(repository).listPlans(UserId.random())).isEmpty();
    }
}
