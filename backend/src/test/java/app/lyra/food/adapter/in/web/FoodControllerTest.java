package app.lyra.food.adapter.in.web;

import app.lyra.SignedInControllerTest;
import app.lyra.TestSessions;
import app.lyra.food.application.port.in.CreateCustomFoodUseCase;
import app.lyra.food.application.port.in.LookupFoodByBarcodeUseCase;
import app.lyra.food.application.port.in.SearchFoodsUseCase;
import app.lyra.food.domain.Barcode;
import app.lyra.food.domain.Food;
import app.lyra.food.domain.FoodSearchQuery;
import app.lyra.food.domain.FoodSource;
import app.lyra.food.domain.MeasurementUnit;
import app.lyra.food.domain.Nutrients;
import app.lyra.food.domain.ProductNotFoundException;
import app.lyra.food.domain.Quantity;
import app.lyra.shared.domain.ServiceUnavailableException;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FoodController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(FoodControllerTest.FakeUseCases.class)
class FoodControllerTest extends SignedInControllerTest {

    private static final UUID RICE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final Food RICE = new Food(RICE_ID, "Arroz, tipo 1, cozido", FoodSource.TACO,
            new Quantity(new BigDecimal("100"), MeasurementUnit.GRAM),
            new Nutrients(new BigDecimal("128.2585"), new BigDecimal("2.5208"), new BigDecimal("28.0598"),
                    new BigDecimal("0.227"), new BigDecimal("1.561"), null));
    private static final String KNOWN_CODE = "7891000100103";
    private static final String DOWN_CODE = "99999999";
    private static final Food PRODUCT = new Food(UUID.fromString("00000000-0000-0000-0000-000000000002"),
            "Biscoito (Marca)", FoodSource.OPEN_FOOD_FACTS, new Quantity(new BigDecimal("100"), MeasurementUnit.GRAM),
            new Nutrients(new BigDecimal("450"), new BigDecimal("7"), new BigDecimal("70"), new BigDecimal("15"),
                    new BigDecimal("2"), new BigDecimal("300")));


    @Autowired
    private MockMvc mvc;

    @TestConfiguration
    static class FakeUseCases {

        @Bean
        SearchFoodsUseCase searchFoods() {
            return (userId, query) -> {
                CALLERS.add(userId);
                FoodSearchQuery.of(query);
                return List.of(RICE);
            };
        }

        @Bean
        LookupFoodByBarcodeUseCase lookupByBarcode() {
            return (userId, code) -> {
                CALLERS.add(userId);
                Barcode barcode = new Barcode(code);
                if (code.equals(KNOWN_CODE)) {
                    return PRODUCT;
                }
                if (code.equals(DOWN_CODE)) {
                    throw new ServiceUnavailableException("The product source is unavailable, try again later", null);
                }
                throw ProductNotFoundException.missing(barcode);
            };
        }

        @Bean
        CreateCustomFoodUseCase createCustomFood() {
            return (userId, name, amount, nutrients) -> {
                CALLERS.add(userId);
                return Food.custom(name, amount, nutrients);
            };
        }
    }

    @Test
    void withoutASignedInUserTheAnswerIsAnUnauthorizedProblem() throws Exception {
        TestSessions.signOut();

        mvc.perform(get("/api/foods").param("query", "arroz"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Unauthorized"));
    }

    @Test
    void barcodeReturnsTheProductWithItsSourceAndNutrients() throws Exception {
        mvc.perform(get("/api/foods/barcode/" + KNOWN_CODE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Biscoito (Marca)"))
                .andExpect(jsonPath("$.source").value("OPEN_FOOD_FACTS"))
                .andExpect(jsonPath("$.referenceAmount.amount").value(100))
                .andExpect(jsonPath("$.referenceAmount.unit").value("GRAM"))
                .andExpect(jsonPath("$.nutrients.kcal").value(450))
                .andExpect(jsonPath("$.nutrients.sodium").value(300));
    }

    @Test
    void barcodeOfAnUnknownProductIsANotFoundProblem() throws Exception {
        mvc.perform(get("/api/foods/barcode/00000000"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Not found"))
                .andExpect(jsonPath("$.detail").value("Product not found: 00000000"));
    }

    @Test
    void barcodeThatIsNotEightToFourteenDigitsIsABadRequestProblem() throws Exception {
        mvc.perform(get("/api/foods/barcode/12ab"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Barcode must have between 8 and 14 digits"));
    }

    @Test
    void barcodeWhenTheProductSourceIsDownIsAServiceUnavailableProblem() throws Exception {
        mvc.perform(get("/api/foods/barcode/" + DOWN_CODE))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Service unavailable"))
                .andExpect(jsonPath("$.detail").value("The product source is unavailable, try again later"));
    }

    @Test
    void barcodeAsksTheUseCaseOnBehalfOfTheSignedInUser() throws Exception {
        mvc.perform(get("/api/foods/barcode/" + KNOWN_CODE)).andExpect(status().isOk());

        assertThat(CALLERS).containsExactly(SIGNED_IN);
    }

    @Test
    void searchAsksTheUseCaseOnBehalfOfTheSignedInUser() throws Exception {
        mvc.perform(get("/api/foods").param("query", "arroz")).andExpect(status().isOk());

        assertThat(CALLERS).containsExactly(SIGNED_IN);
    }

    @Test
    void createAsksTheUseCaseOnBehalfOfTheSignedInUser() throws Exception {
        mvc.perform(post("/api/foods").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "Milkshake", "referenceAmount": {"amount": 1, "unit": "PORTION"},
                         "nutrients": {"kcal": 1, "protein": 1, "carbohydrate": 1, "fat": 1}}
                        """))
                .andExpect(status().isCreated());

        assertThat(CALLERS).containsExactly(SIGNED_IN);
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
