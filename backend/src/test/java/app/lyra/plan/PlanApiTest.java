package app.lyra.plan;

import app.lyra.Concurrently;
import app.lyra.TestSessions;
import app.lyra.TestcontainersConfiguration;
import app.lyra.shared.domain.UserId;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class PlanApiTest {

    private static final UserId USER = UserId.random();

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcClient jdbc;

    @AfterEach
    void removeThePlansTheTestCreated() {
        jdbc.sql("delete from diet_plan").update();
    }

    @Test
    void aWholePlanFromCreatingToReplacing() throws Exception {
        List<String> foods = tacoFoodIds();
        String id = createPlan(plan("Cutting",
                meal("Almoço", "12:30", item(foods.get(0), "150"), item(foods.get(1), "100")),
                meal("Jantar", null, item(foods.get(0), "100"))));

        mvc.perform(get("/api/plans/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Cutting"))
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.targets.kcal").value(2000))
                .andExpect(jsonPath("$.meals.length()").value(2))
                .andExpect(jsonPath("$.meals[0].name").value("Almoço"))
                .andExpect(jsonPath("$.meals[0].time").value("12:30:00"))
                .andExpect(jsonPath("$.meals[0].items.length()").value(2))
                .andExpect(jsonPath("$.meals[0].items[0].foodName").isNotEmpty())
                .andExpect(jsonPath("$.meals[0].items[0].quantity.amount").value(150))
                .andExpect(jsonPath("$.meals[1].name").value("Jantar"));

        mvc.perform(get("/api/plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(id))
                .andExpect(jsonPath("$[0].name").value("Cutting"))
                .andExpect(jsonPath("$[0].active").value(false));

        mvc.perform(put("/api/plans/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content(plan("Bulking", meal("Café da manhã", null, item(foods.get(2), "50")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("Bulking"));

        mvc.perform(get("/api/plans/" + id))
                .andExpect(jsonPath("$.meals.length()").value(1))
                .andExpect(jsonPath("$.meals[0].name").value("Café da manhã"))
                .andExpect(jsonPath("$.meals[0].items[0].quantity.amount").value(50));
    }

    @Test
    void activatingAPlanDeactivatesTheOneThatWasActive() throws Exception {
        List<String> foods = tacoFoodIds();
        String first = createPlan(plan("Primeiro", meal("Almoço", null, item(foods.get(0), "100"))));
        String second = createPlan(plan("Segundo", meal("Almoço", null, item(foods.get(0), "100"))));

        mvc.perform(post("/api/plans/" + first + "/activation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
        assertThat(activePlanIds()).containsExactly(first);

        mvc.perform(post("/api/plans/" + second + "/activation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(second))
                .andExpect(jsonPath("$.active").value(true));

        assertThat(activePlanIds()).containsExactly(second);
        mvc.perform(get("/api/plans/" + first)).andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void activatingThePlanThatIsAlreadyActiveChangesNothing() throws Exception {
        String id = createPlan(plan("Cutting", meal("Almoço", null, item(tacoFoodIds().get(0), "100"))));

        mvc.perform(post("/api/plans/" + id + "/activation")).andExpect(status().isOk());
        mvc.perform(post("/api/plans/" + id + "/activation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));

        assertThat(activePlanIds()).containsExactly(id);
    }

    @Test
    void activatingTwoPlansAtTheSameTimeLeavesExactlyOneActive() throws Exception {
        String food = tacoFoodIds().get(0);
        String first = createPlan(plan("Primeiro", meal("Almoço", null, item(food, "100"))));
        String second = createPlan(plan("Segundo", meal("Almoço", null, item(food, "100"))));

        for (int round = 0; round < 10; round++) {
            List<Integer> statuses = Concurrently.run(List.of(() -> activate(first), () -> activate(second)));

            assertThat(statuses).containsOnly(200);
            assertThat(activePlanIds()).hasSize(1);
        }
    }

    @Test
    void replacingTheActivePlanWhileAnotherIsActivatedLeavesExactlyOneActive() throws Exception {
        String food = tacoFoodIds().get(0);
        String first = createPlan(plan("Primeiro", meal("Almoço", null, item(food, "100"))));
        String second = createPlan(plan("Segundo", meal("Almoço", null, item(food, "100"))));

        for (int round = 0; round < 10; round++) {
            assertThat(activate(first)).isEqualTo(200);

            List<Integer> statuses = Concurrently.run(List.of(
                    () -> activate(second),
                    () -> replace(first, plan("Primeiro", meal("Almoço", null, item(food, "50"))))));

            assertThat(statuses).containsOnly(200);
            assertThat(activePlanIds()).hasSize(1);
        }
    }

    @Test
    void replacingAPlanLeavesNoMealOrItemBehind() throws Exception {
        List<String> foods = tacoFoodIds();
        String id = createPlan(plan("Cutting",
                meal("Café da manhã", null, item(foods.get(0), "50")),
                meal("Almoço", null, item(foods.get(0), "150"), item(foods.get(1), "100")),
                meal("Jantar", null, item(foods.get(1), "100"))));
        assertThat(count("planned_meal")).isEqualTo(3);
        assertThat(count("planned_item")).isEqualTo(4);

        mvc.perform(put("/api/plans/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content(plan("Cutting", meal("Almoço", null, item(foods.get(0), "150")))))
                .andExpect(status().isOk());

        assertThat(count("planned_meal")).isEqualTo(1);
        assertThat(count("planned_item")).isEqualTo(1);
    }

    @Test
    void anUnknownFoodOrPlanIsNotFound() throws Exception {
        String unknown = UUID.randomUUID().toString();
        String body = plan("Cutting", meal("Almoço", null, item(unknown, "100")));

        mvc.perform(post("/api/plans").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Food not found: " + unknown));
        mvc.perform(get("/api/plans/" + unknown))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Plan not found: " + unknown));
        mvc.perform(put("/api/plans/" + unknown).contentType(MediaType.APPLICATION_JSON)
                        .content(plan("Cutting", meal("Almoço", null, item(tacoFoodIds().get(0), "100")))))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/plans/" + unknown + "/activation")).andExpect(status().isNotFound());
    }

    @Test
    void anInvalidPlanIsABadRequest() throws Exception {
        String food = tacoFoodIds().get(0);

        mvc.perform(post("/api/plans").contentType(MediaType.APPLICATION_JSON)
                        .content(plan(" ", meal("Almoço", null, item(food, "100")))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Plan name must have between 1 and 100 characters"));
        mvc.perform(post("/api/plans").contentType(MediaType.APPLICATION_JSON)
                        .content(plan("Cutting", meal("Almoço", null, item(food, "0")))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("The planned quantity must be positive"));
        mvc.perform(post("/api/plans").contentType(MediaType.APPLICATION_JSON)
                        .content(plan("Cutting", meal("Almoço", null, item(food, "100")), meal("Almoço", null, item(food, "50")))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Meal names must be different inside a plan"));
    }

    @Test
    void aUnitThatIsNotTheUnitOfTheFoodIsRejectedAndDoesNotChangeTheSavedPlan() throws Exception {
        String food = tacoFoodIds().get(0);
        String id = createPlan(plan("Cutting", meal("Almoço", null, item(food, "100"))));
        String inMilliliters = plan("Bulking", meal("Almoço", null,
                "{\"foodId\": \"" + food + "\", \"quantity\": {\"amount\": 100, \"unit\": \"MILLILITER\"}}"));

        mvc.perform(post("/api/plans").contentType(MediaType.APPLICATION_JSON).content(inMilliliters))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("must be in GRAM")));
        mvc.perform(put("/api/plans/" + id).contentType(MediaType.APPLICATION_JSON).content(inMilliliters))
                .andExpect(status().isBadRequest());

        mvc.perform(get("/api/plans/" + id)).andExpect(jsonPath("$.name").value("Cutting"));
        mvc.perform(get("/api/plans")).andExpect(jsonPath("$.length()").value(1));
    }

    private List<String> tacoFoodIds() throws Exception {
        String body = mvc.perform(get("/api/foods").with(TestSessions.of(USER)).param("query", "cozido"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$[*].id");
    }

    private String createPlan(String json) throws Exception {
        String body = mvc.perform(post("/api/plans").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private int activate(String id) throws Exception {
        return mvc.perform(post("/api/plans/" + id + "/activation")).andReturn().getResponse().getStatus();
    }

    private int replace(String id, String json) throws Exception {
        return mvc.perform(put("/api/plans/" + id).contentType(MediaType.APPLICATION_JSON).content(json))
                .andReturn().getResponse().getStatus();
    }

    private List<String> activePlanIds() throws Exception {
        String body = mvc.perform(get("/api/plans")).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$[?(@.active == true)].id");
    }

    private int count(String table) {
        return jdbc.sql("select count(*) from " + table).query(Integer.class).single();
    }

    private static String plan(String name, String... meals) {
        return """
                {"name": "%s",
                 "targets": {"kcal": 2000, "protein": 140, "carbohydrate": 200, "fat": 60},
                 "meals": [%s]}
                """.formatted(name, String.join(",", meals));
    }

    private static String meal(String name, String time, String... items) {
        String timeField = time == null ? "" : "\"time\": \"" + time + "\", ";
        return "{\"name\": \"%s\", %s\"items\": [%s]}".formatted(name, timeField, String.join(",", items));
    }

    private static String item(String foodId, String grams) {
        return "{\"foodId\": \"%s\", \"quantity\": {\"amount\": %s, \"unit\": \"GRAM\"}}".formatted(foodId, grams);
    }
}
