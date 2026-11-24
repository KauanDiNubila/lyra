package app.lyra.plan.application.service;

import app.lyra.food.application.port.in.FindFoodsByIdsUseCase;
import app.lyra.food.domain.FoodNotFoundException;
import app.lyra.plan.domain.DietPlan;
import app.lyra.plan.domain.PlannedMeal;
import app.lyra.shared.domain.InvalidInputException;
import app.lyra.shared.domain.UserId;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
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

class CreatePlanServiceTest {

    private static final UserId USER = UserId.random();

    private final InMemoryPlanRepository repository = new InMemoryPlanRepository();

    private CreatePlanService serviceFinding(FindFoodsByIdsUseCase findFoods) {
        return new CreatePlanService(repository, new PlanFoodChecker(findFoods));
    }

    @Test
    void savesAnInactivePlanAndReturnsIt() {
        DietPlan plan = serviceFinding((userId, ids) -> Map.of(RICE.getId(), RICE)).create(USER, "  Cutting ", targets(), lunchAndDinner());

        assertThat(plan.getName()).isEqualTo("Cutting");
        assertThat(plan.isActive()).isFalse();
        assertThat(repository.findById(USER, plan.getId())).contains(plan);
    }

    @Test
    void savesNothingWhenAFoodDoesNotExist() {
        FindFoodsByIdsUseCase unknownFood = (userId, ids) -> {
            throw new FoodNotFoundException(ids.iterator().next());
        };
        CreatePlanService service = serviceFinding(unknownFood);
        List<PlannedMeal> meals = lunchAndDinner();

        assertThatThrownBy(() -> service.create(USER, "Cutting", targets(), meals)).isInstanceOf(FoodNotFoundException.class);
        assertThat(repository.findAll(USER)).isEmpty();
    }

    @Test
    void savesNothingWhenTheUnitIsNotTheFoodUnit() {
        CreatePlanService service = serviceFinding((userId, ids) -> Map.of(RICE.getId(), RICE));
        List<PlannedMeal> meals = List.of(meal("Almoço", riceInPortions("1")));

        assertThatThrownBy(() -> service.create(USER, "Cutting", targets(), meals))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage("Quantity of Arroz must be in GRAM");
        assertThat(repository.findAll(USER)).isEmpty();
    }

    @Test
    void rejectsAnInvalidPlanBeforeLookingUpAnyFood() {
        List<Collection<UUID>> requests = new ArrayList<>();
        CreatePlanService service = serviceFinding((userId, ids) -> {
            requests.add(ids);
            return Map.of(RICE.getId(), RICE);
        });

        assertThatThrownBy(() -> service.create(USER, "Cutting", targets(), List.of())).isInstanceOf(InvalidInputException.class);
        assertThat(requests).isEmpty();
    }

    @Test
    void asksForAllTheFoodsOfThePlanInOneCallWithoutRepeatingIds() {
        List<Collection<UUID>> requests = new ArrayList<>();
        CreatePlanService service = serviceFinding((userId, ids) -> {
            requests.add(ids);
            return Map.of(RICE.getId(), RICE);
        });

        service.create(USER, "Cutting", targets(), List.of(
                meal("Almoço", riceInGrams("150"), riceInGrams("50")),
                meal("Jantar", riceInGrams("100"))));

        assertThat(requests).hasSize(1);
        assertThat(requests.getFirst()).containsExactly(RICE.getId());
    }
}
