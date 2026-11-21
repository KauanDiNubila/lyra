package app.lyra.diary.adapter.in.web;

import app.lyra.diary.application.port.in.ChangeEntryQuantityUseCase;
import app.lyra.diary.application.port.in.DeleteEntryUseCase;
import app.lyra.diary.application.port.in.GetDiaryDayUseCase;
import app.lyra.diary.application.port.in.LogEntryUseCase;
import app.lyra.diary.domain.DiaryDay;
import app.lyra.diary.domain.DiaryEntry;
import app.lyra.diary.domain.DiaryEntryNotFoundException;
import app.lyra.food.domain.Food;
import app.lyra.food.domain.FoodNotFoundException;
import app.lyra.food.domain.FoodSource;
import app.lyra.food.domain.MeasurementUnit;
import app.lyra.food.domain.Nutrients;
import app.lyra.food.domain.Quantity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DiaryController.class)
@Import(DiaryControllerTest.FakeUseCases.class)
class DiaryControllerTest {

    private static final LocalDate DATE = LocalDate.of(2026, 11, 19);
    private static final UUID RICE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID LUNCH_ID = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID DINNER_ID = UUID.fromString("00000000-0000-0000-0000-0000000000a2");
    private static final Quantity HUNDRED_GRAMS = new Quantity(new BigDecimal("100"), MeasurementUnit.GRAM);
    private static final Food RICE = new Food(RICE_ID, "Arroz", FoodSource.CUSTOM, HUNDRED_GRAMS,
            new Nutrients(new BigDecimal("128"), new BigDecimal("2"), new BigDecimal("28"), new BigDecimal("0.2"), null, null));
    private static final DiaryEntry LUNCH = entry(LUNCH_ID, "Almoço", "100");
    private static final DiaryEntry DINNER = entry(DINNER_ID, "Jantar", "50");

    @Autowired
    private MockMvc mvc;

    @TestConfiguration
    static class FakeUseCases {

        @Bean
        GetDiaryDayUseCase getDiaryDay() {
            return date -> new DiaryDay(date, date.equals(DATE) ? List.of(LUNCH, DINNER) : List.of());
        }

        @Bean
        LogEntryUseCase logEntry() {
            return command -> {
                if (!command.foodId().equals(RICE_ID)) {
                    throw new FoodNotFoundException(command.foodId());
                }
                return DiaryEntry.log(RICE, command.date(), command.mealName(), command.quantity());
            };
        }

        @Bean
        ChangeEntryQuantityUseCase changeEntryQuantity() {
            return (id, quantity) -> {
                if (!id.equals(LUNCH_ID)) {
                    throw new DiaryEntryNotFoundException(id);
                }
                return LUNCH.withQuantity(quantity);
            };
        }

        @Bean
        DeleteEntryUseCase deleteEntry() {
            return id -> {
                if (!id.equals(LUNCH_ID)) {
                    throw new DiaryEntryNotFoundException(id);
                }
            };
        }
    }

    @Test
    void dayGroupsEntriesByMealWithTotals() throws Exception {
        mvc.perform(get("/api/diary/2026-11-19"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                          "date": "2026-11-19",
                          "meals": [
                            {"name": "Almoço",
                             "entries": [{"id": "00000000-0000-0000-0000-0000000000a1", "date": "2026-11-19",
                                          "mealName": "Almoço", "foodId": "00000000-0000-0000-0000-000000000001",
                                          "foodName": "Arroz", "quantity": {"amount": 100, "unit": "GRAM"},
                                          "macros": {"kcal": 128, "protein": 2, "carbohydrate": 28, "fat": 0.2}}],
                             "total": {"kcal": 128, "protein": 2, "carbohydrate": 28, "fat": 0.2}},
                            {"name": "Jantar",
                             "entries": [{"id": "00000000-0000-0000-0000-0000000000a2"}],
                             "total": {"kcal": 64, "protein": 1, "carbohydrate": 14, "fat": 0.1}}
                          ],
                          "total": {"kcal": 192, "protein": 3, "carbohydrate": 42, "fat": 0.3}
                        }
                        """));
    }

    @Test
    void emptyDayHasNoMealsAndZeroTotals() throws Exception {
        mvc.perform(get("/api/diary/2026-11-20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meals").isEmpty())
                .andExpect(jsonPath("$.total.kcal").value(0));
    }

    @Test
    void dayWithAMalformedDateIsABadRequest() throws Exception {
        mvc.perform(get("/api/diary/not-a-date")).andExpect(status().isBadRequest());
    }

    @Test
    void logCreatesTheEntry() throws Exception {
        mvc.perform(post("/api/diary/2026-11-19/entries").contentType(MediaType.APPLICATION_JSON).content("""
                        {"foodId": "00000000-0000-0000-0000-000000000001", "mealName": "Almoço",
                         "quantity": {"amount": 150, "unit": "GRAM"}}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.date").value("2026-11-19"))
                .andExpect(jsonPath("$.mealName").value("Almoço"))
                .andExpect(jsonPath("$.foodName").value("Arroz"))
                .andExpect(jsonPath("$.quantity.amount").value(150))
                .andExpect(jsonPath("$.macros.kcal").value(192));
    }

    @Test
    void logWithAnUnknownFoodIsNotFound() throws Exception {
        mvc.perform(post("/api/diary/2026-11-19/entries").contentType(MediaType.APPLICATION_JSON).content("""
                        {"foodId": "00000000-0000-0000-0000-000000000002", "mealName": "Almoço",
                         "quantity": {"amount": 150, "unit": "GRAM"}}
                        """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Not found"))
                .andExpect(jsonPath("$.detail").value("Food not found: 00000000-0000-0000-0000-000000000002"));
    }

    @Test
    void logWithAnotherUnitIsABadRequest() throws Exception {
        mvc.perform(post("/api/diary/2026-11-19/entries").contentType(MediaType.APPLICATION_JSON).content("""
                        {"foodId": "00000000-0000-0000-0000-000000000001", "mealName": "Almoço",
                         "quantity": {"amount": 150, "unit": "MILLILITER"}}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid input"))
                .andExpect(jsonPath("$.detail").value("Quantity must be in GRAM"));
    }

    @Test
    void logWithoutAFoodIdIsABadRequest() throws Exception {
        mvc.perform(post("/api/diary/2026-11-19/entries").contentType(MediaType.APPLICATION_JSON).content("""
                        {"mealName": "Almoço", "quantity": {"amount": 150, "unit": "GRAM"}}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("food id is required"));
    }

    @Test
    void logWithoutAQuantityIsABadRequest() throws Exception {
        mvc.perform(post("/api/diary/2026-11-19/entries").contentType(MediaType.APPLICATION_JSON).content("""
                        {"foodId": "00000000-0000-0000-0000-000000000001", "mealName": "Almoço"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("quantity is required"));
    }

    @Test
    void changeQuantityReturnsTheEntryWithTheNewQuantity() throws Exception {
        mvc.perform(patch("/api/diary/entries/00000000-0000-0000-0000-0000000000a1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"quantity": {"amount": 200, "unit": "GRAM"}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity.amount").value(200))
                .andExpect(jsonPath("$.macros.kcal").value(256));
    }

    @Test
    void changeQuantityOfAnUnknownEntryIsNotFound() throws Exception {
        mvc.perform(patch("/api/diary/entries/00000000-0000-0000-0000-0000000000ff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"quantity": {"amount": 200, "unit": "GRAM"}}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Diary entry not found: 00000000-0000-0000-0000-0000000000ff"));
    }

    @Test
    void changeQuantityToZeroIsABadRequest() throws Exception {
        mvc.perform(patch("/api/diary/entries/00000000-0000-0000-0000-0000000000a1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"quantity": {"amount": 0, "unit": "GRAM"}}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Quantity must be positive"));
    }

    @Test
    void changeQuantityWithoutAQuantityIsABadRequest() throws Exception {
        mvc.perform(patch("/api/diary/entries/00000000-0000-0000-0000-0000000000a1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("quantity is required"));
    }

    @Test
    void deleteRemovesTheEntry() throws Exception {
        mvc.perform(delete("/api/diary/entries/00000000-0000-0000-0000-0000000000a1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteOfAnUnknownEntryIsNotFound() throws Exception {
        mvc.perform(delete("/api/diary/entries/00000000-0000-0000-0000-0000000000ff"))
                .andExpect(status().isNotFound());
    }

    private static DiaryEntry entry(UUID id, String mealName, String grams) {
        return new DiaryEntry(id, DATE, mealName, RICE_ID, RICE.getName(), RICE.getReferenceAmount(),
                RICE.getNutrients(), new Quantity(new BigDecimal(grams), MeasurementUnit.GRAM));
    }
}
