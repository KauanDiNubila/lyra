package app.lyra.diary;

import app.lyra.Concurrently;
import app.lyra.TestSessions;
import app.lyra.TestcontainersConfiguration;
import app.lyra.TestUsers;
import app.lyra.shared.domain.UserId;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class DiaryApiTest {

    private UserId user;

    private static final String DAY = "/api/diary/2026-11-19";

    @Autowired
    private MockMvc mvc;

    private ResultActions perform(MockHttpServletRequestBuilder request) throws Exception {
        return mvc.perform(request.with(TestSessions.of(user)));
    }

    @Autowired
    private JdbcClient jdbc;

    @BeforeEach
    void signInAUser() {
        user = TestUsers.insert(jdbc);
    }

    @AfterEach
    void removeWhatTheTestCreated() {
        TestUsers.deleteEverything(jdbc);
    }

    @Test
    void aQuickAddCreatesTheFoodAndLogsTheEntryInOneStep() throws Exception {
        perform(post(DAY + "/quick-entries").contentType(MediaType.APPLICATION_JSON).content(quickAddJson("Almoço", "Big Mac Teste", 2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.foodName").value("Big Mac Teste"))
                .andExpect(jsonPath("$.quantity.amount").value(2))
                .andExpect(jsonPath("$.quantity.unit").value("PORTION"))
                .andExpect(jsonPath("$.macros.kcal").value(1100));

        perform(get(DAY))
                .andExpect(jsonPath("$.meals[0].name").value("Almoço"))
                .andExpect(jsonPath("$.total.kcal").value(1100))
                .andExpect(jsonPath("$.total.protein").value(50));
        perform(get("/api/foods").param("query", "big mac teste"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].source").value("CUSTOM"))
                .andExpect(jsonPath("$[0].referenceAmount.unit").value("PORTION"))
                .andExpect(jsonPath("$[0].nutrients.kcal").value(550))
                .andExpect(jsonPath("$[0].nutrients.fiber").isEmpty());
    }

    @Test
    void aQuickAddThatFailsLeavesNoFoodBehind() throws Exception {
        perform(post(DAY + "/quick-entries").contentType(MediaType.APPLICATION_JSON).content(quickAddJson("  ", "Big Mac Teste", 1)))
                .andExpect(status().isBadRequest());
        perform(post(DAY + "/quick-entries").contentType(MediaType.APPLICATION_JSON).content(quickAddJson("Almoço", "Big Mac Teste", 0)))
                .andExpect(status().isBadRequest());

        assertThat(jdbc.sql("select count(*) from food where user_id = :user").param("user", user.value())
                .query(Integer.class).single()).isZero();
        assertThat(count("diary_entry")).isZero();
    }

    @Test
    void aWholeDayFromLoggingToDeleting() throws Exception {
        String foodId = createFood();
        String lunchId = logEntry(foodId, "Almoço", "200");
        String dinnerId = logEntry(foodId, "Jantar", "50");

        perform(get(DAY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meals[0].name").value("Almoço"))
                .andExpect(jsonPath("$.meals[0].total.kcal").value(200))
                .andExpect(jsonPath("$.meals[1].name").value("Jantar"))
                .andExpect(jsonPath("$.meals[1].total.kcal").value(50))
                .andExpect(jsonPath("$.total.kcal").value(250))
                .andExpect(jsonPath("$.total.protein").value(25));

        perform(patch("/api/diary/entries/" + lunchId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\": {\"amount\": 100, \"unit\": \"GRAM\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.macros.kcal").value(100));

        perform(get(DAY))
                .andExpect(jsonPath("$.meals[0].total.kcal").value(100))
                .andExpect(jsonPath("$.total.kcal").value(150));

        perform(delete("/api/diary/entries/" + dinnerId)).andExpect(status().isNoContent());

        perform(get(DAY))
                .andExpect(jsonPath("$.meals.length()").value(1))
                .andExpect(jsonPath("$.total.kcal").value(100));
    }

    @Test
    void aTacoFoodGivesItsPublishedValuesForTheQuantityEaten() throws Exception {
        String search = perform(get("/api/foods").param("query", "arroz tipo 1 cozido"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String riceId = JsonPath.read(search, "$[0].id");

        perform(post(DAY + "/entries").contentType(MediaType.APPLICATION_JSON).content(entryJson(riceId, "Almoço", "150")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.foodName").value("Arroz, tipo 1, cozido"))
                .andExpect(jsonPath("$.macros.kcal").value(192.38775))
                .andExpect(jsonPath("$.macros.protein").value(3.7812))
                .andExpect(jsonPath("$.macros.carbohydrate").value(42.0897))
                .andExpect(jsonPath("$.macros.fat").value(0.3405));
    }

    @Test
    void anEntryKeepsTheValuesOfTheFoodAtTheTimeItWasLogged() throws Exception {
        String foodId = createFood();
        logEntry(foodId, "Almoço", "100");

        jdbc.sql("update food set kcal = 999 where id = :id").param("id", UUID.fromString(foodId)).update();

        perform(get(DAY))
                .andExpect(jsonPath("$.meals[0].entries[0].macros.kcal").value(100))
                .andExpect(jsonPath("$.total.kcal").value(100));
    }

    @Test
    void entriesKeepTheOrderTheyWereLogged() throws Exception {
        String foodId = createFood();
        String first = logEntry(foodId, "Almoço", "100");
        String second = logEntry(foodId, "Jantar", "100");
        String third = logEntry(foodId, "Almoço", "100");

        perform(patch("/api/diary/entries/" + first).contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantity\": {\"amount\": 300, \"unit\": \"GRAM\"}}")).andExpect(status().isOk());

        perform(get(DAY))
                .andExpect(jsonPath("$.meals[0].name").value("Almoço"))
                .andExpect(jsonPath("$.meals[0].entries[0].id").value(first))
                .andExpect(jsonPath("$.meals[0].entries[1].id").value(third))
                .andExpect(jsonPath("$.meals[1].name").value("Jantar"))
                .andExpect(jsonPath("$.meals[1].entries[0].id").value(second));
    }

    @Test
    void anUnknownFoodOrEntryIsNotFound() throws Exception {
        String unknown = UUID.randomUUID().toString();

        perform(post(DAY + "/entries").contentType(MediaType.APPLICATION_JSON).content(entryJson(unknown, "Almoço", "100")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Food not found: " + unknown));
        perform(patch("/api/diary/entries/" + unknown).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\": {\"amount\": 100, \"unit\": \"GRAM\"}}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Diary entry not found: " + unknown));
        perform(delete("/api/diary/entries/" + unknown)).andExpect(status().isNotFound());
    }

    @Test
    void invalidRequestsAreBadRequests() throws Exception {
        String foodId = createFood();

        perform(post(DAY + "/entries").contentType(MediaType.APPLICATION_JSON).content("""
                        {"foodId": "%s", "mealName": "Almoço", "quantity": {"amount": 100, "unit": "MILLILITER"}}
                        """.formatted(foodId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Quantity must be in GRAM"));
        perform(post(DAY + "/entries").contentType(MediaType.APPLICATION_JSON).content("""
                        {"mealName": "Almoço", "quantity": {"amount": 100, "unit": "GRAM"}}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("food id is required"));
    }

    @Test
    void aDayFilledFromThePlanThenAdjustedSwappedSkippedAndRenamed() throws Exception {
        String rice = createFood("Arroz de teste", 100);
        String chicken = createFood("Frango de teste", 200);
        createActivePlan(threeItems(rice, chicken));

        String filled = perform(post(DAY + "/plan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meals.length()").value(2))
                .andExpect(jsonPath("$.meals[0].name").value("Almoço"))
                .andExpect(jsonPath("$.meals[0].entries[0].origin").value("PLANNED"))
                .andExpect(jsonPath("$.meals[0].entries[0].planned.quantity.amount").value(150))
                .andExpect(jsonPath("$.meals[1].entries.length()").value(2))
                .andExpect(jsonPath("$.total.kcal").value(350))
                .andExpect(jsonPath("$.plannedTotal.kcal").value(350))
                .andExpect(jsonPath("$.differenceFromPlan.kcal").value(0))
                .andExpect(jsonPath("$.targets.kcal").value(2000))
                .andExpect(jsonPath("$.differenceFromTargets.kcal").value(-1650))
                .andReturn().getResponse().getContentAsString();
        String lunchRice = JsonPath.read(filled, "$.meals[0].entries[0].id");
        String dinnerRice = JsonPath.read(filled, "$.meals[1].entries[0].id");
        String dinnerChicken = JsonPath.read(filled, "$.meals[1].entries[1].id");

        perform(get("/api/diary/2026-11-20")).andExpect(jsonPath("$.meals").isEmpty());

        patchEntry(lunchRice, "{\"quantity\": {\"amount\": 250, \"unit\": \"GRAM\"}}")
                .andExpect(jsonPath("$.origin").value("ADJUSTED"))
                .andExpect(jsonPath("$.macros.kcal").value(250))
                .andExpect(jsonPath("$.planned.macros.kcal").value(150));
        patchEntry(dinnerRice, "{\"foodId\": \"%s\", \"quantity\": {\"amount\": 100, \"unit\": \"GRAM\"}}".formatted(chicken))
                .andExpect(jsonPath("$.origin").value("SWAPPED"))
                .andExpect(jsonPath("$.foodName").value("Frango de teste"))
                .andExpect(jsonPath("$.planned.foodId").value(rice))
                .andExpect(jsonPath("$.planned.foodName").value("Arroz de teste"))
                .andExpect(jsonPath("$.macros.kcal").value(200));
        patchEntry(dinnerChicken, "{\"quantity\": {\"amount\": 0, \"unit\": \"GRAM\"}}")
                .andExpect(jsonPath("$.origin").value("SKIPPED"))
                .andExpect(jsonPath("$.macros.kcal").value(0));
        patchEntry(lunchRice, "{\"mealName\": \"Pós-treino\"}")
                .andExpect(jsonPath("$.mealName").value("Pós-treino"))
                .andExpect(jsonPath("$.origin").value("ADJUSTED"));

        perform(get(DAY))
                .andExpect(jsonPath("$.meals[0].name").value("Pós-treino"))
                .andExpect(jsonPath("$.total.kcal").value(450))
                .andExpect(jsonPath("$.plannedTotal.kcal").value(350))
                .andExpect(jsonPath("$.differenceFromPlan.kcal").value(100))
                .andExpect(jsonPath("$.meals[1].plannedTotal.kcal").value(200))
                .andExpect(jsonPath("$.meals[1].differenceFromPlan.kcal").value(0));

        perform(delete("/api/diary/entries/" + lunchRice))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("An entry of the plan cannot be deleted: change its quantity to zero to skip it"));
        String extra = logEntry(rice, "Lanche", "50");
        perform(delete("/api/diary/entries/" + extra)).andExpect(status().isNoContent());
    }

    @Test
    void fillingAgainOrFillingADayThatHasEntriesChangesNothing() throws Exception {
        String rice = createFood("Arroz de teste", 100);
        String chicken = createFood("Frango de teste", 200);
        createActivePlan(threeItems(rice, chicken));

        String first = perform(post(DAY + "/plan")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String second = perform(post(DAY + "/plan")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        assertThat(second).isEqualTo(first);
        assertThat(count("diary_entry")).isEqualTo(3);

        String otherDay = "/api/diary/2026-11-21";
        perform(post(otherDay + "/entries").contentType(MediaType.APPLICATION_JSON).content(entryJson(rice, "Lanche", "50")))
                .andExpect(status().isCreated());
        perform(post(otherDay + "/plan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meals.length()").value(1))
                .andExpect(jsonPath("$.meals[0].entries[0].origin").value("EXTRA"));
        assertThat(count("diary_entry")).isEqualTo(4);
    }

    @Test
    void fillingWithoutAnActivePlanIsNotFound() throws Exception {
        perform(post(DAY + "/plan"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("There is no active plan to fill the day"));
        assertThat(count("diary_entry")).isZero();
    }

    @Test
    void twoFillsOfTheSameDayAtTheSameTimeFillItOnce() throws Exception {
        String rice = createFood("Arroz de teste", 100);
        String chicken = createFood("Frango de teste", 200);
        createActivePlan(threeItems(rice, chicken));

        for (int day = 1; day <= 10; day++) {
            String path = "/api/diary/2026-12-%02d/plan".formatted(day);

            List<Integer> statuses = Concurrently.run(List.of(() -> fill(path), () -> fill(path)));

            assertThat(statuses).containsOnly(200);
            assertThat(count("diary_entry")).isEqualTo(3 * day);
        }
    }

    private int fill(String path) throws Exception {
        return perform(post(path)).andReturn().getResponse().getStatus();
    }

    private ResultActions patchEntry(String entryId, String body) throws Exception {
        return perform(patch("/api/diary/entries/" + entryId).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
    }

    private void createActivePlan(String meals) throws Exception {
        String body = perform(post("/api/plans").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "Plano de teste",
                         "targets": {"kcal": 2000, "protein": 140, "carbohydrate": 200, "fat": 60},
                         "meals": %s}
                        """.formatted(meals)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String planId = JsonPath.read(body, "$.id");
        perform(post("/api/plans/" + planId + "/activation")).andExpect(status().isOk());
    }

    private static String threeItems(String firstFood, String secondFood) {
        return """
                [{"name": "Almoço", "items": [{"foodId": "%1$s", "quantity": {"amount": 150, "unit": "GRAM"}}]},
                 {"name": "Jantar", "items": [{"foodId": "%1$s", "quantity": {"amount": 100, "unit": "GRAM"}},
                                              {"foodId": "%2$s", "quantity": {"amount": 50, "unit": "GRAM"}}]}]
                """.formatted(firstFood, secondFood);
    }

    private int count(String table) {
        return jdbc.sql("select count(*) from " + table).query(Integer.class).single();
    }

    private String createFood() throws Exception {
        return createFood("Alimento de teste", 100);
    }

    private String createFood(String name, int kcal) throws Exception {
        String body = perform(post("/api/foods").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "%s", "referenceAmount": {"amount": 100, "unit": "GRAM"},
                         "nutrients": {"kcal": %d, "protein": 10, "carbohydrate": 20, "fat": 5}}
                        """.formatted(name, kcal)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private String logEntry(String foodId, String mealName, String grams) throws Exception {
        String body = perform(post(DAY + "/entries").contentType(MediaType.APPLICATION_JSON).content(entryJson(foodId, mealName, grams)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private static String quickAddJson(String mealName, String name, int portions) {
        return """
                {"mealName": "%s", "name": "%s", "portions": %d,
                 "macros": {"kcal": 550, "protein": 25, "carbohydrate": 45, "fat": 30}}
                """.formatted(mealName, name, portions);
    }

    private static String entryJson(String foodId, String mealName, String grams) {
        return """
                {"foodId": "%s", "mealName": "%s", "quantity": {"amount": %s, "unit": "GRAM"}}
                """.formatted(foodId, mealName, grams);
    }
}
