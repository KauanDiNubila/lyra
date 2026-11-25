package app.lyra;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class DataOwnershipApiTest {

    private static final String DAY = "/api/diary/2026-11-19";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcClient jdbc;

    private UserId ana;
    private UserId bruno;
    private String anaFood;
    private String anaPlan;
    private String anaEntry;

    @BeforeEach
    void anaHasAFoodAPlanAndAnEntry() throws Exception {
        ana = TestUsers.insert(jdbc);
        bruno = TestUsers.insert(jdbc);
        anaFood = createFood(ana, "Quimera da Ana");
        anaPlan = createPlan(ana, "Plano da Ana", tacoFood(ana));
        perform(ana, post("/api/plans/" + anaPlan + "/activation")).andExpect(status().isOk());
        anaEntry = logEntry(ana, anaFood);
    }

    @AfterEach
    void removeWhatTheTestCreated() {
        TestUsers.deleteEverything(jdbc);
    }

    @Test
    void aCustomFoodOfAnotherUserIsNotSearchableButTacoFoodsAre() throws Exception {
        perform(ana, get("/api/foods").param("query", "quimera da ana"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        perform(bruno, get("/api/foods").param("query", "quimera da ana"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        perform(bruno, get("/api/foods").param("query", "arroz tipo 1 cozido"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].source").value("TACO"));
    }

    @Test
    void aCustomFoodOfAnotherUserCannotBeUsedInAPlanOrAnEntry() throws Exception {
        perform(bruno, post("/api/plans").contentType(MediaType.APPLICATION_JSON).content(planJson("Do Bruno", anaFood)))
                .andExpect(status().isNotFound());
        perform(bruno, post(DAY + "/entries").contentType(MediaType.APPLICATION_JSON).content(entryJson(anaFood)))
                .andExpect(status().isNotFound());
    }

    @Test
    void anotherUsersPlanIsNeitherListedNorReadableNorChangeable() throws Exception {
        perform(bruno, get("/api/plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        perform(bruno, get("/api/plans/" + anaPlan)).andExpect(status().isNotFound());
        perform(bruno, put("/api/plans/" + anaPlan).contentType(MediaType.APPLICATION_JSON)
                        .content(planJson("Roubado", "00000000-0000-0000-0000-000000000000")))
                .andExpect(status().isNotFound());
        perform(bruno, post("/api/plans/" + anaPlan + "/activation")).andExpect(status().isNotFound());

        perform(ana, get("/api/plans/" + anaPlan))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Plano da Ana"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void eachUserHasTheirOwnActivePlan() throws Exception {
        String brunoPlan = createPlan(bruno, "Plano do Bruno", tacoFood(bruno));
        perform(bruno, post("/api/plans/" + brunoPlan + "/activation")).andExpect(status().isOk());

        perform(ana, get("/api/plans/" + anaPlan)).andExpect(jsonPath("$.active").value(true));
        perform(bruno, get("/api/plans/" + brunoPlan)).andExpect(jsonPath("$.active").value(true));
        perform(ana, get("/api/plans")).andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void theDiaryOfAnotherUserIsEmptyAndItsEntriesCannotBeChangedOrDeleted() throws Exception {
        perform(ana, get(DAY))
                .andExpect(jsonPath("$.meals[0].entries", hasSize(1)))
                .andExpect(jsonPath("$.targets").isNotEmpty());
        perform(bruno, get(DAY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meals", hasSize(0)))
                .andExpect(jsonPath("$.targets").doesNotExist());
        perform(bruno, patch("/api/diary/entries/" + anaEntry).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mealName\": \"Roubado\"}"))
                .andExpect(status().isNotFound());
        perform(bruno, delete("/api/diary/entries/" + anaEntry)).andExpect(status().isNotFound());

        perform(ana, get(DAY)).andExpect(jsonPath("$.meals[0].entries", hasSize(1)));
    }

    @Test
    void fillingTheDayUsesOnlyThePlanOfWhoAsks() throws Exception {
        perform(bruno, post(DAY + "/plan")).andExpect(status().isNotFound());

        String brunoPlan = createPlan(bruno, "Plano do Bruno", tacoFood(bruno));
        perform(bruno, post("/api/plans/" + brunoPlan + "/activation")).andExpect(status().isOk());
        perform(bruno, post(DAY + "/plan")).andExpect(status().isOk());

        perform(ana, get(DAY)).andExpect(jsonPath("$.meals[0].entries", hasSize(1)));
        perform(bruno, get(DAY)).andExpect(jsonPath("$.meals[0].entries[0].planned").isNotEmpty());
    }

    @Test
    void deletingAUserRemovesEverythingTheyOwnAndNothingOfTheOthers() throws Exception {
        createFood(bruno, "Quimera do Bruno");
        String brunoPlan = createPlan(bruno, "Plano do Bruno", tacoFood(bruno));
        perform(bruno, post("/api/plans/" + brunoPlan + "/activation")).andExpect(status().isOk());

        perform(ana, delete("/api/me")).andExpect(status().isNoContent());

        for (String table : new String[] {"app_user", "food", "diet_plan", "diary_entry"}) {
            assertThat(rowsOf(table, "app_user".equals(table) ? "id" : "user_id", ana)).as(table + " of the deleted user").isZero();
        }
        assertThat(rowsOf("food", "user_id", bruno)).isEqualTo(1);
        assertThat(rowsOf("diet_plan", "user_id", bruno)).isEqualTo(1);
        perform(bruno, get("/api/me/export"))
                .andExpect(jsonPath("$.customFoods", hasSize(1)))
                .andExpect(jsonPath("$.plans", hasSize(1)));
    }

    @Test
    void theExportHasOnlyTheDataOfWhoAsks() throws Exception {
        perform(ana, get("/api/me/export"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"lyra-export.json\""))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.account.id").value(ana.toString()))
                .andExpect(jsonPath("$.customFoods", hasSize(1)))
                .andExpect(jsonPath("$.customFoods[0].name").value("Quimera da Ana"))
                .andExpect(jsonPath("$.plans", hasSize(1)))
                .andExpect(jsonPath("$.plans[0].name").value("Plano da Ana"))
                .andExpect(jsonPath("$.plans[0].active").value(true))
                .andExpect(jsonPath("$.diaryEntries", hasSize(1)))
                .andExpect(jsonPath("$.diaryEntries[0].foodId").value(anaFood));
        perform(bruno, get("/api/me/export"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.account.id").value(bruno.toString()))
                .andExpect(jsonPath("$.customFoods", hasSize(0)))
                .andExpect(jsonPath("$.plans", hasSize(0)))
                .andExpect(jsonPath("$.diaryEntries", hasSize(0)));
    }

    @Test
    void theExportOfAFilledDayKeepsWhatWasPlanned() throws Exception {
        perform(bruno, post("/api/plans/" + createPlan(bruno, "Plano do Bruno", tacoFood(bruno)) + "/activation"))
                .andExpect(status().isOk());
        perform(bruno, post(DAY + "/plan")).andExpect(status().isOk());

        perform(bruno, get("/api/me/export"))
                .andExpect(jsonPath("$.plans[0].meals[0].items", hasSize(1)))
                .andExpect(jsonPath("$.diaryEntries", hasSize(1)))
                .andExpect(jsonPath("$.diaryEntries[0].planned.slot").value(0))
                .andExpect(jsonPath("$.diaryEntries[0].planned.macros.kcal").isNotEmpty());
    }

    @Test
    void eachUserSeesOnlyWhoTheyAre() throws Exception {
        perform(ana, get("/api/me")).andExpect(jsonPath("$.id").value(ana.toString()));
        perform(bruno, get("/api/me")).andExpect(jsonPath("$.id").value(bruno.toString()));
    }

    private int rowsOf(String table, String column, UserId user) {
        return jdbc.sql("select count(*) from " + table + " where " + column + " = :id")
                .param("id", user.value())
                .query(Integer.class)
                .single();
    }

    private ResultActions perform(UserId user, MockHttpServletRequestBuilder request) throws Exception {
        return mvc.perform(request.with(TestSessions.of(user)));
    }

    private String createFood(UserId user, String name) throws Exception {
        String body = perform(user, post("/api/foods").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "%s", "referenceAmount": {"amount": 100, "unit": "GRAM"},
                         "nutrients": {"kcal": 100, "protein": 10, "carbohydrate": 20, "fat": 5}}
                        """.formatted(name)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private String tacoFood(UserId user) throws Exception {
        String body = perform(user, get("/api/foods").param("query", "arroz tipo 1 cozido"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$[0].id");
    }

    private String createPlan(UserId user, String name, String foodId) throws Exception {
        String body = perform(user, post("/api/plans").contentType(MediaType.APPLICATION_JSON).content(planJson(name, foodId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private String logEntry(UserId user, String foodId) throws Exception {
        String body = perform(user, post(DAY + "/entries").contentType(MediaType.APPLICATION_JSON).content(entryJson(foodId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private static String planJson(String name, String foodId) {
        return """
                {"name": "%s", "targets": {"kcal": 2000, "protein": 140, "carbohydrate": 200, "fat": 60},
                 "meals": [{"name": "Almoço", "time": "12:30",
                            "items": [{"foodId": "%s", "quantity": {"amount": 100, "unit": "GRAM"}}]}]}
                """.formatted(name, foodId);
    }

    private static String entryJson(String foodId) {
        return """
                {"foodId": "%s", "mealName": "Almoço", "quantity": {"amount": 100, "unit": "GRAM"}}
                """.formatted(foodId);
    }
}
