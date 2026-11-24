package app.lyra.plan.application.service;

import app.lyra.plan.domain.DietPlan;
import app.lyra.shared.domain.UserId;
import org.junit.jupiter.api.Test;

import static app.lyra.plan.application.service.PlanTestData.lunchAndDinner;
import static app.lyra.plan.application.service.PlanTestData.targets;
import static org.assertj.core.api.Assertions.assertThat;

class GetActivePlanServiceTest {

    private static final UserId USER = UserId.random();

    private final InMemoryPlanRepository repository = new InMemoryPlanRepository();
    private final GetActivePlanService service = new GetActivePlanService(repository);

    @Test
    void returnsThePlanThatIsActive() {
        DietPlan active = DietPlan.create("Cutting", targets(), lunchAndDinner()).activated();
        repository.save(USER, DietPlan.create("Bulking", targets(), lunchAndDinner()));
        repository.save(USER, active);

        assertThat(service.getActivePlan(USER)).contains(active);
    }

    @Test
    void returnsNothingWhenNoPlanIsActive() {
        repository.save(USER, DietPlan.create("Cutting", targets(), lunchAndDinner()));

        assertThat(service.getActivePlan(USER)).isEmpty();
    }

    @Test
    void returnsNothingWhenThereAreNoPlans() {
        assertThat(service.getActivePlan(USER)).isEmpty();
    }
}
