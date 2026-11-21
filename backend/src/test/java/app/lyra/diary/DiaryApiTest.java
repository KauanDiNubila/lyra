package app.lyra.diary;

import app.lyra.TestcontainersConfiguration;
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

import java.util.UUID;

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

    private static final String DAY = "/api/diary/2026-11-19";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcClient jdbc;

    @AfterEach
    void removeWhatTheTestCreated() {
        jdbc.sql("delete from diary_entry").update();
        jdbc.sql("delete from food where source_reference is null").update();
    }

    @Test
    void aWholeDayFromLoggingToDeleting() throws Exception {
        String foodId = createFood();
        String lunchId = logEntry(foodId, "Almoço", "200");
        String dinnerId = logEntry(foodId, "Jantar", "50");

        mvc.perform(get(DAY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meals[0].name").value("Almoço"))
                .andExpect(jsonPath("$.meals[0].total.kcal").value(200))
                .andExpect(jsonPath("$.meals[1].name").value("Jantar"))
                .andExpect(jsonPath("$.meals[1].total.kcal").value(50))
                .andExpect(jsonPath("$.total.kcal").value(250))
                .andExpect(jsonPath("$.total.protein").value(25));

        mvc.perform(patch("/api/diary/entries/" + lunchId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\": {\"amount\": 100, \"unit\": \"GRAM\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.macros.kcal").value(100));

        mvc.perform(get(DAY))
                .andExpect(jsonPath("$.meals[0].total.kcal").value(100))
                .andExpect(jsonPath("$.total.kcal").value(150));

        mvc.perform(delete("/api/diary/entries/" + dinnerId)).andExpect(status().isNoContent());

        mvc.perform(get(DAY))
                .andExpect(jsonPath("$.meals.length()").value(1))
                .andExpect(jsonPath("$.total.kcal").value(100));
    }

    @Test
    void aTacoFoodGivesItsPublishedValuesForTheQuantityEaten() throws Exception {
        String search = mvc.perform(get("/api/foods").param("query", "arroz tipo 1 cozido"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String riceId = JsonPath.read(search, "$[0].id");

        mvc.perform(post(DAY + "/entries").contentType(MediaType.APPLICATION_JSON).content(entryJson(riceId, "Almoço", "150")))
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

        mvc.perform(get(DAY))
                .andExpect(jsonPath("$.meals[0].entries[0].macros.kcal").value(100))
                .andExpect(jsonPath("$.total.kcal").value(100));
    }

    @Test
    void entriesKeepTheOrderTheyWereLogged() throws Exception {
        String foodId = createFood();
        String first = logEntry(foodId, "Almoço", "100");
        String second = logEntry(foodId, "Jantar", "100");
        String third = logEntry(foodId, "Almoço", "100");

        mvc.perform(patch("/api/diary/entries/" + first).contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantity\": {\"amount\": 300, \"unit\": \"GRAM\"}}")).andExpect(status().isOk());

        mvc.perform(get(DAY))
                .andExpect(jsonPath("$.meals[0].name").value("Almoço"))
                .andExpect(jsonPath("$.meals[0].entries[0].id").value(first))
                .andExpect(jsonPath("$.meals[0].entries[1].id").value(third))
                .andExpect(jsonPath("$.meals[1].name").value("Jantar"))
                .andExpect(jsonPath("$.meals[1].entries[0].id").value(second));
    }

    @Test
    void anUnknownFoodOrEntryIsNotFound() throws Exception {
        String unknown = UUID.randomUUID().toString();

        mvc.perform(post(DAY + "/entries").contentType(MediaType.APPLICATION_JSON).content(entryJson(unknown, "Almoço", "100")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Food not found: " + unknown));
        mvc.perform(patch("/api/diary/entries/" + unknown).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\": {\"amount\": 100, \"unit\": \"GRAM\"}}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Diary entry not found: " + unknown));
        mvc.perform(delete("/api/diary/entries/" + unknown)).andExpect(status().isNotFound());
    }

    @Test
    void invalidRequestsAreBadRequests() throws Exception {
        String foodId = createFood();

        mvc.perform(post(DAY + "/entries").contentType(MediaType.APPLICATION_JSON).content("""
                        {"foodId": "%s", "mealName": "Almoço", "quantity": {"amount": 100, "unit": "MILLILITER"}}
                        """.formatted(foodId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Quantity must be in GRAM"));
        mvc.perform(post(DAY + "/entries").contentType(MediaType.APPLICATION_JSON).content("""
                        {"mealName": "Almoço", "quantity": {"amount": 100, "unit": "GRAM"}}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("food id is required"));
    }

    private String createFood() throws Exception {
        String body = mvc.perform(post("/api/foods").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "Alimento de teste", "referenceAmount": {"amount": 100, "unit": "GRAM"},
                         "nutrients": {"kcal": 100, "protein": 10, "carbohydrate": 20, "fat": 5}}
                        """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private String logEntry(String foodId, String mealName, String grams) throws Exception {
        String body = mvc.perform(post(DAY + "/entries").contentType(MediaType.APPLICATION_JSON).content(entryJson(foodId, mealName, grams)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private static String entryJson(String foodId, String mealName, String grams) {
        return """
                {"foodId": "%s", "mealName": "%s", "quantity": {"amount": %s, "unit": "GRAM"}}
                """.formatted(foodId, mealName, grams);
    }
}
