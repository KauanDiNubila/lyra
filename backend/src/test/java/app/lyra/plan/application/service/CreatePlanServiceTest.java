package app.lyra.plan.application.service;

import app.lyra.food.application.port.in.FindFoodUseCase;
import app.lyra.food.domain.FoodNotFoundException;
import app.lyra.plan.domain.DietPlan;
import app.lyra.plan.domain.PlannedMeal;
import app.lyra.shared.domain.InvalidInputException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static app.lyra.plan.application.service.PlanTestData.RICE;
import static app.lyra.plan.application.service.PlanTestData.lunchAndDinner;
import static app.lyra.plan.application.service.PlanTestData.meal;
import static app.lyra.plan.application.service.PlanTestData.riceInGrams;
import static app.lyra.plan.application.service.PlanTestData.riceInPortions;
import static app.lyra.plan.application.service.PlanTestData.targets;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CreatePlanServiceTest {

    private final InMemoryPlanRepository repository = new InMemoryPlanRepository();

    private CreatePlanService serviceFinding(FindFoodUseCase findFood) {
        return new CreatePlanService(repository, new PlanFoodChecker(findFood));
    }

    @Test
    void savesAnInactivePlanAndReturnsIt() {
        DietPlan plan = serviceFinding(id -> RICE).create("  Cutting ", targets(), lunchAndDinner());

        assertThat(plan.getName()).isEqualTo("Cutting");
        assertThat(plan.isActive()).isFalse();
        assertThat(repository.findById(plan.getId())).contains(plan);
    }

    @Test
    void savesNothingWhenAFoodDoesNotExist() {
        FindFoodUseCase unknownFood = id -> {
            throw new FoodNotFoundException(id);
        };
        CreatePlanService service = serviceFinding(unknownFood);
        List<PlannedMeal> meals = lunchAndDinner();

        assertThatThrownBy(() -> service.create("Cutting", targets(), meals)).isInstanceOf(FoodNotFoundException.class);
        assertThat(repository.findAll()).isEmpty();
    }

    @Test
    void savesNothingWhenTheUnitIsNotTheFoodUnit() {
        CreatePlanService service = serviceFinding(id -> RICE);
        List<PlannedMeal> meals = List.of(meal("Almoço", riceInPortions("1")));

        assertThatThrownBy(() -> service.create("Cutting", targets(), meals))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage("Quantity of Arroz must be in GRAM");
        assertThat(repository.findAll()).isEmpty();
    }

    @Test
    void rejectsAnInvalidPlanBeforeLookingUpAnyFood() {
        AtomicInteger lookups = new AtomicInteger();
        CreatePlanService service = serviceFinding(id -> {
            lookups.incrementAndGet();
            return RICE;
        });

        assertThatThrownBy(() -> service.create("Cutting", targets(), List.of())).isInstanceOf(InvalidInputException.class);
        assertThat(lookups).hasValue(0);
    }

    @Test
    void looksUpARepeatedFoodOnce() {
        AtomicInteger lookups = new AtomicInteger();
        CreatePlanService service = serviceFinding(id -> {
            lookups.incrementAndGet();
            return RICE;
        });

        service.create("Cutting", targets(), List.of(
                meal("Almoço", riceInGrams("150"), riceInGrams("50")),
                meal("Jantar", riceInGrams("100"))));

        assertThat(lookups).hasValue(1);
    }
}
