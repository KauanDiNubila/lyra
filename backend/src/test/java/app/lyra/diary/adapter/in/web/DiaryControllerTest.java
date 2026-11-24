package app.lyra.diary.adapter.in.web;

import app.lyra.diary.application.port.in.ChangeEntryUseCase;
import app.lyra.diary.application.port.in.ChangeEntryUseCase.ChangeEntryCommand;
import app.lyra.diary.application.port.in.DeleteEntryUseCase;
import app.lyra.diary.application.port.in.FillDayFromPlanUseCase;
import app.lyra.diary.application.port.in.GetDiaryDayUseCase;
import app.lyra.diary.application.port.in.LogEntryUseCase;
import app.lyra.diary.domain.DiaryDay;
import app.lyra.diary.domain.DiaryEntry;
import app.lyra.diary.domain.DiaryEntryNotFoundException;
import app.lyra.diary.domain.Macros;
import app.lyra.diary.domain.NoActivePlanException;
import app.lyra.food.domain.Food;
import app.lyra.food.domain.FoodNotFoundException;
import app.lyra.food.domain.FoodSource;
import app.lyra.food.domain.MeasurementUnit;
import app.lyra.food.domain.Nutrients;
import app.lyra.food.domain.Quantity;
import app.lyra.shared.domain.InvalidInputException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DiaryController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(DiaryControllerTest.FakeUseCases.class)
class DiaryControllerTest {

    private static final LocalDate DATE = LocalDate.of(2026, 11, 19);
    private static final UUID RICE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID LUNCH_ID = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID DINNER_ID = UUID.fromString("00000000-0000-0000-0000-0000000000a2");
    private static final Quantity HUNDRED_GRAMS = new Quantity(new BigDecimal("100"), MeasurementUnit.GRAM);
    private static final Food RICE = new Food(RICE_ID, "Arroz", FoodSource.CUSTOM, HUNDRED_GRAMS,
            new Nutrients(new BigDecimal("128"), new BigDecimal("2"), new BigDecimal("28"), new BigDecimal("0.2"), null, null));
    private static final UUID PLAN_ENTRY_ID = UUID.fromString("00000000-0000-0000-0000-0000000000a3");
    private static final LocalDate PLAN_DATE = LocalDate.of(2026, 11, 25);
    private static final LocalDate NO_PLAN_DATE = LocalDate.of(2026, 11, 26);
    private static final Macros TARGETS = new Macros(new BigDecimal("2000"), new BigDecimal("140"),
            new BigDecimal("200"), new BigDecimal("60"));
    private static final DiaryEntry LUNCH = entry(LUNCH_ID, "Almoço", "100");
    private static final DiaryEntry DINNER = entry(DINNER_ID, "Jantar", "50");
    private static final AtomicReference<ChangeEntryCommand> LAST_CHANGE = new AtomicReference<>();

    @Autowired
    private MockMvc mvc;

    @TestConfiguration
    static class FakeUseCases {

        @Bean
        GetDiaryDayUseCase getDiaryDay() {
            return date -> {
                if (date.equals(DATE)) {
                    return new DiaryDay(date, List.of(LUNCH, DINNER));
                }
                if (date.equals(PLAN_DATE)) {
                    return planDay(date);
                }
                return new DiaryDay(date, List.of());
            };
        }

        @Bean
        FillDayFromPlanUseCase fillDayFromPlan() {
            return date -> {
                if (date.equals(NO_PLAN_DATE)) {
                    throw new NoActivePlanException();
                }
                return planDay(date);
            };
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
        ChangeEntryUseCase changeEntry() {
            return (id, command) -> {
                if (!id.equals(LUNCH_ID)) {
                    throw new DiaryEntryNotFoundException(id);
                }
                LAST_CHANGE.set(command);
                DiaryEntry changed = LUNCH;
                if (command.foodId() != null) {
                    changed = LUNCH.withFood(RICE, command.quantity());
                } else if (command.quantity() != null) {
                    changed = LUNCH.withQuantity(command.quantity());
                }
                return command.mealName() == null ? changed : changed.withMealName(command.mealName());
            };
        }

        @Bean
        DeleteEntryUseCase deleteEntry() {
            return id -> {
                if (id.equals(PLAN_ENTRY_ID)) {
                    throw new InvalidInputException("An entry of the plan cannot be deleted: change its quantity to zero to skip it");
                }
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
                .andExpect(jsonPath("$.detail").value("Nothing to change: send a quantity, a food or a meal"));
    }

    @Test
    void changeAcceptsTheFoodTheQuantityAndTheMealTogether() throws Exception {
        mvc.perform(patch("/api/diary/entries/00000000-0000-0000-0000-0000000000a1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"foodId": "00000000-0000-0000-0000-000000000001", "mealName": "Pós-treino",
                                 "quantity": {"amount": 80, "unit": "GRAM"}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mealName").value("Pós-treino"))
                .andExpect(jsonPath("$.quantity.amount").value(80))
                .andExpect(jsonPath("$.origin").value("EXTRA"));

        assertThat(LAST_CHANGE.get())
                .isEqualTo(new ChangeEntryCommand(RICE_ID, grams("80"), "Pós-treino"));
    }

    @Test
    void changeOfOnlyTheMealIsAccepted() throws Exception {
        mvc.perform(patch("/api/diary/entries/00000000-0000-0000-0000-0000000000a1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mealName\": \"Lanche\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mealName").value("Lanche"))
                .andExpect(jsonPath("$.quantity.amount").value(100));
    }

    @Test
    void changeOfTheFoodWithoutTheQuantityIsABadRequest() throws Exception {
        mvc.perform(patch("/api/diary/entries/00000000-0000-0000-0000-0000000000a1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"foodId\": \"00000000-0000-0000-0000-000000000001\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Changing the food needs the quantity"));
    }

    @Test
    void dayFromThePlanShowsTheOriginThePlannedAndTheDifferences() throws Exception {
        mvc.perform(get("/api/diary/2026-11-25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meals[0].entries[0].origin").value("ADJUSTED"))
                .andExpect(jsonPath("$.meals[0].entries[0].planned.foodId").value("00000000-0000-0000-0000-000000000001"))
                .andExpect(jsonPath("$.meals[0].entries[0].planned.foodName").value("Arroz"))
                .andExpect(jsonPath("$.meals[0].entries[0].planned.quantity.amount").value(150))
                .andExpect(jsonPath("$.meals[0].entries[0].planned.quantity.unit").value("GRAM"))
                .andExpect(jsonPath("$.meals[0].entries[0].planned.macros.kcal").value(192))
                .andExpect(jsonPath("$.meals[0].plannedTotal.kcal").value(192))
                .andExpect(jsonPath("$.meals[0].differenceFromPlan.kcal").value(128))
                .andExpect(jsonPath("$.meals[1].entries[0].origin").value("SKIPPED"))
                .andExpect(jsonPath("$.meals[1].entries[0].quantity.amount").value(0))
                .andExpect(jsonPath("$.meals[1].differenceFromPlan.kcal").value(-128))
                .andExpect(jsonPath("$.meals[2].entries[0].origin").value("EXTRA"))
                .andExpect(jsonPath("$.meals[2].entries[0].planned").value(nullValue()))
                .andExpect(jsonPath("$.total.kcal").value(384))
                .andExpect(jsonPath("$.plannedTotal.kcal").value(320))
                .andExpect(jsonPath("$.differenceFromPlan.kcal").value(64))
                .andExpect(jsonPath("$.targets.kcal").value(2000))
                .andExpect(jsonPath("$.differenceFromTargets.kcal").value(-1616));
    }

    @Test
    void dayWithoutAPlanHasNoTargetsAndNothingPlanned() throws Exception {
        mvc.perform(get("/api/diary/2026-11-19"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meals[0].entries[0].origin").value("EXTRA"))
                .andExpect(jsonPath("$.meals[0].entries[0].planned").value(nullValue()))
                .andExpect(jsonPath("$.plannedTotal.kcal").value(0))
                .andExpect(jsonPath("$.targets").value(nullValue()))
                .andExpect(jsonPath("$.differenceFromTargets").value(nullValue()));
    }

    @Test
    void fillingTheDayReturnsTheDayFilledFromThePlan() throws Exception {
        mvc.perform(post("/api/diary/2026-11-25/plan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2026-11-25"))
                .andExpect(jsonPath("$.meals[0].entries[0].origin").value("ADJUSTED"))
                .andExpect(jsonPath("$.targets.kcal").value(2000));
    }

    @Test
    void fillingTheDayWithoutAnActivePlanIsNotFound() throws Exception {
        mvc.perform(post("/api/diary/2026-11-26/plan"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Not found"))
                .andExpect(jsonPath("$.detail").value("There is no active plan to fill the day"));
    }

    @Test
    void fillingTheDayOfAMalformedDateIsABadRequest() throws Exception {
        mvc.perform(post("/api/diary/not-a-date/plan")).andExpect(status().isBadRequest());
    }

    @Test
    void deletingAnEntryOfThePlanIsABadRequestThatExplainsHowToSkipIt() throws Exception {
        mvc.perform(delete("/api/diary/entries/00000000-0000-0000-0000-0000000000a3"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid input"))
                .andExpect(jsonPath("$.detail").value("An entry of the plan cannot be deleted: change its quantity to zero to skip it"));
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

    private static DiaryDay planDay(LocalDate date) {
        DiaryEntry adjusted = DiaryEntry.fromPlan(RICE, date, "Almoço", grams("150"), 0).withQuantity(grams("250"));
        DiaryEntry skipped = DiaryEntry.fromPlan(RICE, date, "Jantar", grams("100"), 1).withQuantity(grams("0"));
        DiaryEntry extra = DiaryEntry.log(RICE, date, "Lanche", grams("50"));
        return new DiaryDay(date, List.of(adjusted, skipped, extra), Optional.of(TARGETS));
    }

    private static Quantity grams(String amount) {
        return new Quantity(new BigDecimal(amount), MeasurementUnit.GRAM);
    }

    private static DiaryEntry entry(UUID id, String mealName, String grams) {
        return new DiaryEntry(id, DATE, mealName, RICE_ID, RICE.getName(), RICE.getReferenceAmount(),
                RICE.getNutrients(), new Quantity(new BigDecimal(grams), MeasurementUnit.GRAM));
    }
}
