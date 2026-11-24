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

    private static final UserId USER = UserId.random();

    private final InMemoryPlanRepository repository = new InMemoryPlanRepository();

    @Test
    void getReturnsThePlanWithThatId() {
        DietPlan plan = DietPlan.create("Cutting", targets(), lunchAndDinner());
        repository.save(USER, plan);
        repository.save(USER, DietPlan.create("Bulking", targets(), lunchAndDinner()));

        assertThat(new GetPlanService(repository).getPlan(USER, plan.getId())).isEqualTo(plan);
    }

    @Test
    void getFailsWhenThereIsNoPlanWithThatId() {
        UUID unknown = UUID.randomUUID();
        GetPlanService service = new GetPlanService(repository);

        assertThatThrownBy(() -> service.getPlan(USER, unknown))
                .isInstanceOf(PlanNotFoundException.class)
                .hasMessage("Plan not found: " + unknown);
    }

    @Test
    void listReturnsThePlansInTheOrderTheyWereSaved() {
        DietPlan first = DietPlan.create("Cutting", targets(), lunchAndDinner());
        DietPlan second = DietPlan.create("Bulking", targets(), lunchAndDinner());
        repository.save(USER, first);
        repository.save(USER, second);

        assertThat(new ListPlansService(repository).listPlans(USER)).containsExactly(first, second);
    }

    @Test
    void listReturnsNothingWhenThereAreNoPlans() {
        assertThat(new ListPlansService(repository).listPlans(USER)).isEmpty();
    }
}
