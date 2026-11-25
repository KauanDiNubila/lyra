package app.lyra.food.adapter.out.openfoodfacts;

import app.lyra.food.domain.Barcode;
import app.lyra.food.domain.Food;
import app.lyra.food.domain.FoodSource;
import app.lyra.food.domain.ProductNotFoundException;
import app.lyra.shared.domain.ServiceUnavailableException;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenFoodFactsCatalogTest {

    private static final String USER_AGENT = "Lyra/test (https://example.test/lyra)";
    private static final Barcode BARCODE = new Barcode("3017620422003");
    private static final String PRODUCT_PATH = "/api/v2/product/3017620422003.json";

    @RegisterExtension
    static final WireMockExtension SERVER = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private OpenFoodFactsCatalog catalog;

    @BeforeEach
    void connectToTheStubbedServer() {
        catalog = new OpenFoodFactsCatalog(new OpenFoodFactsProperties(
                SERVER.baseUrl(), USER_AGENT, Duration.ofSeconds(1), Duration.ofMillis(500)));
    }

    private static void answer(int status, String body) {
        SERVER.stubFor(get(urlPathEqualTo(PRODUCT_PATH)).willReturn(
                aResponse().withStatus(status).withHeader("Content-Type", "application/json").withBody(body)));
    }

    private static final String NUTELLA = """
            {"code": "3017620422003", "status": 1, "status_verbose": "product found",
             "product": {"product_name": "Nutella", "brands": "Ferrero", "product_quantity_unit": "g",
                         "nutriments": {"energy-kcal_100g": 539, "proteins_100g": 6.3, "carbohydrates_100g": 57.5,
                                        "fat_100g": 30.9, "fiber_100g": 0, "sodium_100g": 0.0428}}}
            """;

    @Test
    void findsTheProductAndMapsItToAFood() {
        answer(200, NUTELLA);

        Optional<Food> found = catalog.findByBarcode(BARCODE);

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Nutella (Ferrero)");
        assertThat(found.get().getSource()).isEqualTo(FoodSource.OPEN_FOOD_FACTS);
        assertThat(found.get().getNutrients().kcal()).isEqualByComparingTo("539");
        assertThat(found.get().getNutrients().sodium()).isEqualByComparingTo("42.8");
    }

    @Test
    void identifiesItselfAndAsksOnlyForTheFieldsItNeeds() {
        answer(200, NUTELLA);

        catalog.findByBarcode(BARCODE);

        SERVER.verify(1, getRequestedFor(urlPathEqualTo(PRODUCT_PATH))
                .withHeader("User-Agent", equalTo(USER_AGENT))
                .withQueryParam("fields", containing("nutriments")));
        assertThat(SERVER.getAllServeEvents()).hasSize(1);
    }

    @Test
    void sendsNothingButTheBarcode() {
        answer(200, NUTELLA);

        catalog.findByBarcode(BARCODE);

        var request = SERVER.getAllServeEvents().getFirst().getRequest();
        assertThat(request.getUrl()).startsWith(PRODUCT_PATH + "?fields=");
        assertThat(request.getHeaders().keys()).doesNotContain("Cookie", "Authorization");
    }

    @Test
    void answersNothingWhenTheSourceSaysTheProductIsUnknown() {
        answer(200, "{\"code\": \"3017620422003\", \"status\": 0, \"status_verbose\": \"product not found\"}");

        assertThat(catalog.findByBarcode(BARCODE)).isEmpty();
    }

    @Test
    void answersNothingOnANotFoundStatus() {
        answer(404, "{}");

        assertThat(catalog.findByBarcode(BARCODE)).isEmpty();
    }

    @Test
    void failsWithoutDataWhenTheProductHasNoNutritionValues() {
        answer(200, "{\"status\": 1, \"product\": {\"product_name\": \"Suco\", \"nutriments\": {}}}");

        assertThatThrownBy(() -> catalog.findByBarcode(BARCODE)).isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void isUnavailableWhenTheSourceAnswersWithAServerError() {
        answer(500, "{}");

        assertThatThrownBy(() -> catalog.findByBarcode(BARCODE))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessage("The product source is unavailable, try again later");
    }


    @Test
    void logsAWarningWithTheBarcodeAndTheCauseWhenTheSourceFails() {
        Logger logger = (Logger) LoggerFactory.getLogger(OpenFoodFactsCatalog.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            answer(500, "{}");

            assertThatThrownBy(() -> catalog.findByBarcode(BARCODE)).isInstanceOf(ServiceUnavailableException.class);

            assertThat(appender.list).singleElement().satisfies(event -> {
                assertThat(event.getLevel()).isEqualTo(Level.WARN);
                assertThat(event.getFormattedMessage()).contains(BARCODE.value());
                assertThat(event.getThrowableProxy()).isNotNull();
            });
        } finally {
            logger.detachAppender(appender);
        }
    }
    @Test
    void isUnavailableWhenTheSourceLimitsTheUse() {
        answer(429, "{}");

        assertThatThrownBy(() -> catalog.findByBarcode(BARCODE)).isInstanceOf(ServiceUnavailableException.class);
    }

    @Test
    void isUnavailableWhenTheSourceTakesLongerThanTheReadLimit() {
        SERVER.stubFor(get(urlPathEqualTo(PRODUCT_PATH)).willReturn(
                aResponse().withStatus(200).withFixedDelay(2000).withBody(NUTELLA)));

        assertThatThrownBy(() -> catalog.findByBarcode(BARCODE)).isInstanceOf(ServiceUnavailableException.class);
    }

    @Test
    void isUnavailableWhenTheConnectionIsCut() {
        SERVER.stubFor(get(urlPathEqualTo(PRODUCT_PATH)).willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

        assertThatThrownBy(() -> catalog.findByBarcode(BARCODE)).isInstanceOf(ServiceUnavailableException.class);
    }

    @Test
    void isUnavailableWhenTheAnswerIsNotJson() {
        answer(200, "<html>maintenance</html>");

        assertThatThrownBy(() -> catalog.findByBarcode(BARCODE)).isInstanceOf(ServiceUnavailableException.class);
    }

    @Test
    void isUnavailableWhenNobodyListensAtTheAddress() {
        OpenFoodFactsCatalog offline = new OpenFoodFactsCatalog(new OpenFoodFactsProperties(
                "http://localhost:1", USER_AGENT, Duration.ofMillis(300), Duration.ofMillis(300)));

        assertThatThrownBy(() -> offline.findByBarcode(BARCODE)).isInstanceOf(ServiceUnavailableException.class);
    }
}
