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

class ActivatePlanServiceTest {

    private final InMemoryPlanRepository repository = new InMemoryPlanRepository();
    private final ActivatePlanService service = new ActivatePlanService(repository);

    @Test
    void activatesAPlanWhenNoneIsActive() {
        DietPlan plan = DietPlan.create("Cutting", targets(), lunchAndDinner());
        repository.save(plan);

        DietPlan activated = service.activate(UserId.random(), plan.getId());

        assertThat(activated.isActive()).isTrue();
        assertThat(repository.findActive()).contains(plan);
    }

    @Test
    void deactivatesTheCurrentPlanBeforeActivatingTheOther() {
        DietPlan current = DietPlan.create("Cutting", targets(), lunchAndDinner()).activated();
        DietPlan other = DietPlan.create("Bulking", targets(), lunchAndDinner());
        repository.save(current);
        repository.save(other);

        service.activate(UserId.random(), other.getId());

        assertThat(repository.findById(current.getId()).orElseThrow().isActive()).isFalse();
        assertThat(repository.findById(other.getId()).orElseThrow().isActive()).isTrue();
        assertThat(repository.findAll()).filteredOn(DietPlan::isActive).hasSize(1);
    }

    @Test
    void activatingThePlanThatIsAlreadyActiveChangesNothing() {
        DietPlan active = DietPlan.create("Cutting", targets(), lunchAndDinner()).activated();
        repository.save(active);

        DietPlan result = service.activate(UserId.random(), active.getId());

        assertThat(result.isActive()).isTrue();
        assertThat(repository.findAll()).hasSize(1);
        assertThat(repository.findActive()).contains(active);
    }

    @Test
    void failsWhenThereIsNoPlanWithThatId() {
        UUID unknown = UUID.randomUUID();

        assertThatThrownBy(() -> service.activate(UserId.random(), unknown))
                .isInstanceOf(PlanNotFoundException.class)
                .hasMessage("Plan not found: " + unknown);
    }
}
