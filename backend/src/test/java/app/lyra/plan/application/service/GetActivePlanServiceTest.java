package app.lyra.plan.application.service;

import app.lyra.plan.domain.DietPlan;
import org.junit.jupiter.api.Test;

import static app.lyra.plan.application.service.PlanTestData.lunchAndDinner;
import static app.lyra.plan.application.service.PlanTestData.targets;
import static org.assertj.core.api.Assertions.assertThat;

class GetActivePlanServiceTest {

    private final InMemoryPlanRepository repository = new InMemoryPlanRepository();
    private final GetActivePlanService service = new GetActivePlanService(repository);

    @Test
    void returnsThePlanThatIsActive() {
        DietPlan active = DietPlan.create("Cutting", targets(), lunchAndDinner()).activated();
        repository.save(DietPlan.create("Bulking", targets(), lunchAndDinner()));
        repository.save(active);

        assertThat(service.getActivePlan()).contains(active);
    }

    @Test
    void returnsNothingWhenNoPlanIsActive() {
        repository.save(DietPlan.create("Cutting", targets(), lunchAndDinner()));

        assertThat(service.getActivePlan()).isEmpty();
    }

    @Test
    void returnsNothingWhenThereAreNoPlans() {
        assertThat(service.getActivePlan()).isEmpty();
    }
}
