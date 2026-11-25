package app.lyra.food;

import app.lyra.TestSessions;
import app.lyra.TestUsers;
import app.lyra.TestcontainersConfiguration;
import app.lyra.food.application.port.out.ProductCatalog;
import app.lyra.food.domain.Barcode;
import app.lyra.food.domain.Food;
import app.lyra.food.domain.FoodSource;
import app.lyra.food.domain.MeasurementUnit;
import app.lyra.food.domain.Nutrients;
import app.lyra.food.domain.ProductNotFoundException;
import app.lyra.food.domain.Quantity;
import app.lyra.shared.domain.ServiceUnavailableException;
import app.lyra.shared.domain.UserId;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, BarcodeApiTest.FakeSource.class})
class BarcodeApiTest {

    private static final String KNOWN = "7891000100103";
    private static final String UNKNOWN = "00000000";
    private static final String INCOMPLETE = "88888888";
    private static final String DOWN = "99999999";
    private static final List<Barcode> ASKED = new CopyOnWriteArrayList<>();

    @TestConfiguration
    static class FakeSource {

        @Bean
        @Primary
        ProductCatalog productCatalog() {
            return barcode -> {
                ASKED.add(barcode);
                return switch (barcode.value()) {
                    case UNKNOWN -> Optional.empty();
                    case INCOMPLETE -> throw ProductNotFoundException.incomplete(barcode);
                    case DOWN -> throw new ServiceUnavailableException("The product source is unavailable, try again later", null);
                    default -> Optional.of(product("Biscoito de teste (Marca)"));
                };
            };
        }

        private static Food product(String name) {
            return new Food(UUID.randomUUID(), name, FoodSource.OPEN_FOOD_FACTS,
                    new Quantity(new BigDecimal("100"), MeasurementUnit.GRAM),
                    new Nutrients(new BigDecimal("450"), new BigDecimal("7"), new BigDecimal("70"),
                            new BigDecimal("15"), new BigDecimal("2"), new BigDecimal("300")));
        }
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcClient jdbc;

    private UserId ana;
    private UserId bruno;

    @BeforeEach
    void twoUsers() {
        ASKED.clear();
        ana = TestUsers.insert(jdbc);
        bruno = TestUsers.insert(jdbc);
    }

    @AfterEach
    void removeWhatTheTestCreated() {
        TestUsers.deleteEverything(jdbc);
    }

    private ResultActions lookup(UserId user, String code) throws Exception {
        return mvc.perform(get("/api/foods/barcode/" + code).with(TestSessions.of(user)));
    }

    private int storedProducts() {
        return jdbc.sql("select count(*) from food where source = 'OPEN_FOOD_FACTS'").query(Integer.class).single();
    }

    @Test
    void aProductIsFetchedOnceAndSharedByEveryUser() throws Exception {
        String first = lookup(ana, KNOWN)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("OPEN_FOOD_FACTS"))
                .andExpect(jsonPath("$.name").value("Biscoito de teste (Marca)"))
                .andExpect(jsonPath("$.referenceAmount.unit").value("GRAM"))
                .andExpect(jsonPath("$.nutrients.sodium").value(300))
                .andReturn().getResponse().getContentAsString();
        String second = lookup(bruno, KNOWN).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        assertThat((String) JsonPath.read(second, "$.id")).isEqualTo(JsonPath.read(first, "$.id"));
        assertThat(ASKED).hasSize(1);
        assertThat(storedProducts()).isEqualTo(1);
        assertThat(jdbc.sql("select count(*) from food where source_reference = 'off:" + KNOWN + "' and user_id is null")
                .query(Integer.class).single()).isEqualTo(1);
    }

    @Test
    void theStoredProductAppearsInTheTextSearchOfEveryUser() throws Exception {
        lookup(ana, KNOWN).andExpect(status().isOk());

        mvc.perform(get("/api/foods").param("query", "biscoito de teste").with(TestSessions.of(bruno)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].source").value("OPEN_FOOD_FACTS"));
    }

    @Test
    void anUnknownProductAnswersNotFoundAndStoresNothing() throws Exception {
        lookup(ana, UNKNOWN)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Product not found: " + UNKNOWN));

        assertThat(storedProducts()).isZero();
    }

    @Test
    void aProductWithoutUsableDataAnswersNotFoundWithTheReason() throws Exception {
        lookup(ana, INCOMPLETE)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Product " + INCOMPLETE + " has no usable name or nutrition data"));

        assertThat(storedProducts()).isZero();
    }

    @Test
    void aSourceThatIsDownAnswersServiceUnavailableAndStoresNothing() throws Exception {
        lookup(ana, DOWN)
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.title").value("Service unavailable"));

        assertThat(storedProducts()).isZero();
    }

    @Test
    void anInvalidCodeAnswersBadRequestWithoutAskingTheSource() throws Exception {
        lookup(ana, "12ab")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Barcode must have between 8 and 14 digits"));

        assertThat(ASKED).isEmpty();
    }
}
