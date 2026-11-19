package app.lyra.food.adapter.in.web;

import app.lyra.food.application.port.in.CreateCustomFoodUseCase;
import app.lyra.food.application.port.in.SearchFoodsUseCase;
import app.lyra.food.domain.Food;
import app.lyra.food.domain.FoodSearchQuery;
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
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FoodController.class)
@Import(FoodControllerTest.FakeUseCases.class)
class FoodControllerTest {

    private static final UUID RICE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final Food RICE = new Food(RICE_ID, "Arroz, tipo 1, cozido", FoodSource.TACO,
            new Quantity(new BigDecimal("100"), MeasurementUnit.GRAM),
            new Nutrients(new BigDecimal("128.2585"), new BigDecimal("2.5208"), new BigDecimal("28.0598"),
                    new BigDecimal("0.227"), new BigDecimal("1.561"), null));

    @Autowired
    private MockMvc mvc;

    @TestConfiguration
    static class FakeUseCases {

        @Bean
        SearchFoodsUseCase searchFoods() {
            return query -> {
                FoodSearchQuery.of(query);
                return List.of(RICE);
            };
        }

        @Bean
        CreateCustomFoodUseCase createCustomFood() {
            return Food::custom;
        }
    }

    @Test
    void searchReturnsFoodsWithReferenceAmountAndNutrients() throws Exception {
        mvc.perform(get("/api/foods").param("query", "arroz"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        [{
                          "id": "00000000-0000-0000-0000-000000000001",
                          "name": "Arroz, tipo 1, cozido",
                          "source": "TACO",
                          "referenceAmount": {"amount": 100, "unit": "GRAM"},
                          "nutrients": {"kcal": 128.2585, "protein": 2.5208, "carbohydrate": 28.0598,
                                        "fat": 0.227, "fiber": 1.561, "sodium": null}
                        }]
                        """, true));
    }

    @Test
    void searchWithAQueryThatIsTooShortIsABadRequestProblem() throws Exception {
        mvc.perform(get("/api/foods").param("query", "a"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Invalid input"))
                .andExpect(jsonPath("$.detail").value("Search query must have at least 2 characters"));
    }

    @Test
    void searchWithoutAQueryIsABadRequestProblem() throws Exception {
        mvc.perform(get("/api/foods"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Bad Request"));
    }

    @Test
    void createReturnsTheNewCustomFood() throws Exception {
        mvc.perform(post("/api/foods").contentType(MediaType.APPLICATION_JSON).content("""
                        {
                          "name": "Milkshake McDonald's",
                          "referenceAmount": {"amount": 1, "unit": "PORTION"},
                          "nutrients": {"kcal": 590, "protein": 13, "carbohydrate": 98, "fat": 16}
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.source").value("CUSTOM"))
                .andExpect(jsonPath("$.name").value("Milkshake McDonald's"))
                .andExpect(jsonPath("$.referenceAmount.unit").value("PORTION"))
                .andExpect(jsonPath("$.nutrients.kcal").value(590))
                .andExpect(jsonPath("$.nutrients.fiber").isEmpty());
    }

    @Test
    void createWithInvalidDomainValuesIsABadRequestProblem() throws Exception {
        mvc.perform(post("/api/foods").contentType(MediaType.APPLICATION_JSON).content("""
                        {
                          "name": "Milkshake",
                          "referenceAmount": {"amount": 1, "unit": "PORTION"},
                          "nutrients": {"kcal": -1, "protein": 13, "carbohydrate": 98, "fat": 16}
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("kcal must not be negative"));
    }

    @Test
    void createWithoutTheReferenceAmountIsAnInvalidInputProblem() throws Exception {
        mvc.perform(post("/api/foods").contentType(MediaType.APPLICATION_JSON).content("""
                        {
                          "name": "Milkshake",
                          "nutrients": {"kcal": 1, "protein": 1, "carbohydrate": 1, "fat": 1}
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Invalid input"))
                .andExpect(jsonPath("$.detail").value("reference amount is required"));
    }

    @Test
    void createWithoutTheNutrientsIsAnInvalidInputProblem() throws Exception {
        mvc.perform(post("/api/foods").contentType(MediaType.APPLICATION_JSON).content("""
                        {
                          "name": "Milkshake",
                          "referenceAmount": {"amount": 1, "unit": "PORTION"}
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Invalid input"))
                .andExpect(jsonPath("$.detail").value("nutrients are required"));
    }

    @Test
    void createWithoutANameIsAnInvalidInputProblem() throws Exception {
        mvc.perform(post("/api/foods").contentType(MediaType.APPLICATION_JSON).content("""
                        {
                          "referenceAmount": {"amount": 1, "unit": "PORTION"},
                          "nutrients": {"kcal": 1, "protein": 1, "carbohydrate": 1, "fat": 1}
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid input"))
                .andExpect(jsonPath("$.detail").value("Food name must have between 2 and 200 characters"));
    }

    @Test
    void createWithAnUnknownUnitIsABadRequestProblem() throws Exception {
        mvc.perform(post("/api/foods").contentType(MediaType.APPLICATION_JSON).content("""
                        {
                          "name": "Milkshake",
                          "referenceAmount": {"amount": 1, "unit": "CUP"},
                          "nutrients": {"kcal": 1, "protein": 1, "carbohydrate": 1, "fat": 1}
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Bad Request"));
    }

    @Test
    void createWithMalformedJsonIsABadRequestProblem() throws Exception {
        mvc.perform(post("/api/foods").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Bad Request"));
    }
}
