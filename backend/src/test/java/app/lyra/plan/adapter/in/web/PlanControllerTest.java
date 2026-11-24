package app.lyra.plan.adapter.in.web;

import app.lyra.food.application.port.in.FindFoodUseCase;
import app.lyra.food.domain.Food;
import app.lyra.food.domain.FoodNotFoundException;
import app.lyra.food.domain.FoodSource;
import app.lyra.food.domain.MeasurementUnit;
import app.lyra.food.domain.Nutrients;
import app.lyra.food.domain.Quantity;
import app.lyra.plan.application.port.in.ActivatePlanUseCase;
import app.lyra.plan.application.port.in.CreatePlanUseCase;
import app.lyra.plan.application.port.in.GetPlanUseCase;
import app.lyra.plan.application.port.in.ListPlansUseCase;
import app.lyra.plan.application.port.in.ReplacePlanUseCase;
import app.lyra.plan.domain.DailyTargets;
import app.lyra.plan.domain.DietPlan;
import app.lyra.plan.domain.PlanNotFoundException;
import app.lyra.plan.domain.PlannedItem;
import app.lyra.plan.domain.PlannedMeal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PlanController.class)
@Import({PlanControllerTest.FakeUseCases.class, PlanResponseFactory.class})
class PlanControllerTest {

    private static final UUID RICE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID PLAN_ID = UUID.fromString("00000000-0000-0000-0000-0000000000b1");
    private static final Quantity HUNDRED_GRAMS = new Quantity(new BigDecimal("100"), MeasurementUnit.GRAM);
    private static final Food RICE = new Food(RICE_ID, "Arroz, tipo 1, cozido", FoodSource.TACO, HUNDRED_GRAMS,
            new Nutrients(BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, null, null));
    private static final DailyTargets TARGETS = new DailyTargets(new BigDecimal("2000"), new BigDecimal("140"),
            new BigDecimal("200"), new BigDecimal("60"));
    private static final DietPlan PLAN = new DietPlan(PLAN_ID, "Cutting", TARGETS, List.of(
            new PlannedMeal("Almoço", LocalTime.of(12, 30), List.of(
                    new PlannedItem(RICE_ID, new Quantity(new BigDecimal("150"), MeasurementUnit.GRAM)),
                    new PlannedItem(RICE_ID, new Quantity(new BigDecimal("50"), MeasurementUnit.GRAM)))),
            new PlannedMeal("Jantar", null, List.of(
                    new PlannedItem(RICE_ID, new Quantity(new BigDecimal("100"), MeasurementUnit.GRAM))))), false);

    private static final String VALID_PLAN = """
            {"name": "Bulking",
             "targets": {"kcal": 3000, "protein": 180, "carbohydrate": 400, "fat": 90},
             "meals": [{"name": "Almoço", "time": "12:30",
                        "items": [{"foodId": "00000000-0000-0000-0000-000000000001",
                                   "quantity": {"amount": 200, "unit": "GRAM"}}]}]}
            """;

    @Autowired
    private MockMvc mvc;

    @TestConfiguration
    static class FakeUseCases {

        @Bean
        FindFoodUseCase findFood() {
            return id -> {
                if (!id.equals(RICE_ID)) {
                    throw new FoodNotFoundException(id);
                }
                return RICE;
            };
        }

        @Bean
        ListPlansUseCase listPlans() {
            return () -> List.of(PLAN, PLAN.activated());
        }

        @Bean
        GetPlanUseCase getPlan() {
            return id -> {
                if (!id.equals(PLAN_ID)) {
                    throw new PlanNotFoundException(id);
                }
                return PLAN;
            };
        }

        @Bean
        CreatePlanUseCase createPlan(FindFoodUseCase findFood) {
            return (name, targets, meals) -> {
                meals.forEach(meal -> meal.items().forEach(item -> findFood.findById(item.foodId())));
                return DietPlan.create(name, targets, meals);
            };
        }

        @Bean
        ReplacePlanUseCase replacePlan() {
            return (id, name, targets, meals) -> {
                if (!id.equals(PLAN_ID)) {
                    throw new PlanNotFoundException(id);
                }
                return PLAN.withContent(name, targets, meals);
            };
        }

        @Bean
        ActivatePlanUseCase activatePlan() {
            return id -> {
                if (!id.equals(PLAN_ID)) {
                    throw new PlanNotFoundException(id);
                }
                return PLAN.activated();
            };
        }
    }

    @Test
    void listReturnsASummaryOfEachPlan() throws Exception {
        mvc.perform(get("/api/plans"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        [{"id": "00000000-0000-0000-0000-0000000000b1", "name": "Cutting", "active": false},
                         {"id": "00000000-0000-0000-0000-0000000000b1", "name": "Cutting", "active": true}]
                        """))
                .andExpect(jsonPath("$[0].meals").doesNotExist());
    }

    @Test
    void getReturnsThePlanWithTheFoodNames() throws Exception {
        mvc.perform(get("/api/plans/" + PLAN_ID))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"id": "00000000-0000-0000-0000-0000000000b1", "name": "Cutting", "active": false,
                         "targets": {"kcal": 2000, "protein": 140, "carbohydrate": 200, "fat": 60},
                         "meals": [
                           {"name": "Almoço", "time": "12:30:00",
                            "items": [
                              {"foodId": "00000000-0000-0000-0000-000000000001", "foodName": "Arroz, tipo 1, cozido",
                               "quantity": {"amount": 150, "unit": "GRAM"}},
                              {"foodId": "00000000-0000-0000-0000-000000000001", "foodName": "Arroz, tipo 1, cozido",
                               "quantity": {"amount": 50, "unit": "GRAM"}}]},
                           {"name": "Jantar", "items": [{"foodName": "Arroz, tipo 1, cozido"}]}]}
                        """))
                .andExpect(jsonPath("$.meals[1].time").doesNotExist());
    }

    @Test
    void getOfAnUnknownPlanIsNotFound() throws Exception {
        mvc.perform(get("/api/plans/00000000-0000-0000-0000-0000000000ff"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Not found"))
                .andExpect(jsonPath("$.detail").value("Plan not found: 00000000-0000-0000-0000-0000000000ff"));
    }

    @Test
    void createReturnsTheNewInactivePlan() throws Exception {
        mvc.perform(post("/api/plans").contentType(MediaType.APPLICATION_JSON).content(VALID_PLAN))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Bulking"))
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.targets.kcal").value(3000))
                .andExpect(jsonPath("$.meals[0].time").value("12:30:00"))
                .andExpect(jsonPath("$.meals[0].items[0].foodName").value("Arroz, tipo 1, cozido"))
                .andExpect(jsonPath("$.meals[0].items[0].quantity.amount").value(200));
    }

    @Test
    void createWithAnUnknownFoodIsNotFound() throws Exception {
        String unknownFood = VALID_PLAN.replace("00000000-0000-0000-0000-000000000001", "00000000-0000-0000-0000-000000000002");

        mvc.perform(post("/api/plans").contentType(MediaType.APPLICATION_JSON).content(unknownFood))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Food not found: 00000000-0000-0000-0000-000000000002"));
    }

    static Stream<Arguments> invalidPlans() {
        String targets = "\"targets\": {\"kcal\": 2000, \"protein\": 140, \"carbohydrate\": 200, \"fat\": 60}";
        String item = "{\"foodId\": \"00000000-0000-0000-0000-000000000001\", \"quantity\": {\"amount\": 100, \"unit\": \"GRAM\"}}";
        String meals = "\"meals\": [{\"name\": \"Almoço\", \"items\": [" + item + "]}]";
        return Stream.of(
                Arguments.of("{\"name\": \" \", " + targets + ", " + meals + "}", "Plan name must have between 1 and 100 characters"),
                Arguments.of("{\"name\": \"Cutting\", " + meals + "}", "targets are required"),
                Arguments.of("{\"name\": \"Cutting\", " + targets + "}", "meals are required"),
                Arguments.of("{\"name\": \"Cutting\", " + targets + ", \"meals\": [null]}", "every meal needs its data"),
                Arguments.of("{\"name\": \"Cutting\", " + targets + ", \"meals\": [{\"name\": \"Almoço\"}]}", "A meal needs at least one item"),
                Arguments.of("{\"name\": \"Cutting\", " + targets + ", \"meals\": [{\"name\": \"Almoço\", \"items\": [null]}]}", "every item needs its data"),
                Arguments.of("{\"name\": \"Cutting\", " + targets + ", \"meals\": [{\"name\": \"Almoço\", \"items\": [{\"quantity\": {\"amount\": 100, \"unit\": \"GRAM\"}}]}]}", "A planned item needs a food and a quantity"),
                Arguments.of("{\"name\": \"Cutting\", " + targets + ", \"meals\": [{\"name\": \"Almoço\", \"items\": [{\"foodId\": \"00000000-0000-0000-0000-000000000001\"}]}]}", "A planned item needs a food and a quantity"),
                Arguments.of("{\"name\": \"Cutting\", \"targets\": {\"kcal\": -1, \"protein\": 1, \"carbohydrate\": 1, \"fat\": 1}, " + meals + "}", "The kcal target must be zero or positive"));
    }

    @ParameterizedTest
    @MethodSource("invalidPlans")
    void createWithAnInvalidPlanIsABadRequest(String body, String detail) throws Exception {
        mvc.perform(post("/api/plans").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid input"))
                .andExpect(jsonPath("$.detail").value(detail));
    }

    @Test
    void createWithAUnitThatDoesNotExistIsABadRequest() throws Exception {
        mvc.perform(post("/api/plans").contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PLAN.replace("GRAM", "TEASPOON")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void replaceReturnsThePlanWithTheNewContent() throws Exception {
        mvc.perform(put("/api/plans/" + PLAN_ID).contentType(MediaType.APPLICATION_JSON).content(VALID_PLAN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(PLAN_ID.toString()))
                .andExpect(jsonPath("$.name").value("Bulking"))
                .andExpect(jsonPath("$.meals.length()").value(1));
    }

    @Test
    void replaceOfAnUnknownPlanIsNotFound() throws Exception {
        mvc.perform(put("/api/plans/00000000-0000-0000-0000-0000000000ff").contentType(MediaType.APPLICATION_JSON).content(VALID_PLAN))
                .andExpect(status().isNotFound());
    }

    @Test
    void replaceWithoutTargetsIsABadRequest() throws Exception {
        mvc.perform(put("/api/plans/" + PLAN_ID).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Bulking\", \"meals\": []}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("targets are required"));
    }

    @Test
    void activationReturnsTheActivePlan() throws Exception {
        mvc.perform(post("/api/plans/" + PLAN_ID + "/activation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(PLAN_ID.toString()))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void activationOfAnUnknownPlanIsNotFound() throws Exception {
        mvc.perform(post("/api/plans/00000000-0000-0000-0000-0000000000ff/activation"))
                .andExpect(status().isNotFound());
    }
}
